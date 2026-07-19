/*
 * FlaxClient native mouse bridge (Linux).
 *
 * Injects real mouse events via /dev/uinput and queries the *physical* button
 * state by reading each real evdev device's key bits directly (EVIOCGKEY).
 * Reading evdev per-device avoids the trap of X11's master pointer state,
 * which reflects the LAST event on the master (so our own virtual release
 * clears it even while the user is still physically holding the button).
 *
 * Requires input-group access to /dev/uinput and /dev/input/event*. See
 * FlaxClientLauncher/setup-uinput.sh.
 */

#include <jni.h>
#include <linux/uinput.h>
#include <linux/input.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <time.h>
#include <dirent.h>
#include <stdio.h>

static int g_fd = -1;

#define MAX_MICE 16
static int  g_mice_fds[MAX_MICE];
static int  g_mice_count = -1;
static long g_mice_probed_at = 0;

static long now_ms(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (long) ts.tv_sec * 1000L + ts.tv_nsec / 1000000L;
}

static int emit(int fd, int type, int code, int value) {
    struct input_event ev;
    memset(&ev, 0, sizeof(ev));
    ev.type = type;
    ev.code = code;
    ev.value = value;
    ssize_t n = write(fd, &ev, sizeof(ev));
    return (n == (ssize_t) sizeof(ev)) ? 0 : -1;
}

static int has_bit(const unsigned long *bits, int nbits, int bit) {
    if (bit < 0 || bit >= nbits) return 0;
    return (bits[bit / (8 * (int) sizeof(long))] >> (bit % (8 * (int) sizeof(long)))) & 1;
}

static void close_mice(void) {
    for (int i = 0; i < g_mice_count; i++) {
        if (g_mice_fds[i] >= 0) close(g_mice_fds[i]);
        g_mice_fds[i] = -1;
    }
    g_mice_count = 0;
}

/*
 * Enumerate /dev/input/event* and keep open a fd for every device that
 * (a) exposes BTN_LEFT and (b) isn't our own virtual mouse. Cheap ioctls
 * on those fds give us the physical button state on demand.
 */
static void probe_mice(void) {
    close_mice();
    g_mice_count = 0;
    g_mice_probed_at = now_ms();

    DIR *d = opendir("/dev/input");
    if (!d) return;

    struct dirent *ent;
    while ((ent = readdir(d)) != NULL && g_mice_count < MAX_MICE) {
        if (strncmp(ent->d_name, "event", 5) != 0) continue;
        char path[320];
        snprintf(path, sizeof(path), "/dev/input/%s", ent->d_name);
        int fd = open(path, O_RDONLY | O_NONBLOCK);
        if (fd < 0) continue;

        char name[256];
        memset(name, 0, sizeof(name));
        if (ioctl(fd, EVIOCGNAME(sizeof(name) - 1), name) < 0) { close(fd); continue; }
        if (strstr(name, "FlaxClient") != NULL) { close(fd); continue; }

        unsigned long keybits[(KEY_MAX + 8 * sizeof(long)) / (8 * sizeof(long))];
        memset(keybits, 0, sizeof(keybits));
        if (ioctl(fd, EVIOCGBIT(EV_KEY, sizeof(keybits)), keybits) < 0) { close(fd); continue; }
        if (!has_bit(keybits, KEY_MAX, BTN_LEFT)) { close(fd); continue; }

        g_mice_fds[g_mice_count++] = fd;
    }
    closedir(d);
}

JNIEXPORT jint JNICALL
Java_me_eldodebug_soar_utils_mouse_NativeMouseBridge_nativeInit(JNIEnv *env, jclass cls) {
    if (g_fd >= 0) return 0;

    int fd = open("/dev/uinput", O_WRONLY | O_NONBLOCK);
    if (fd < 0) return -(errno ? errno : 1);

    if (ioctl(fd, UI_SET_EVBIT, EV_KEY) < 0)                { close(fd); return -101; }
    if (ioctl(fd, UI_SET_KEYBIT, BTN_LEFT) < 0)             { close(fd); return -102; }
    if (ioctl(fd, UI_SET_KEYBIT, BTN_RIGHT) < 0)            { close(fd); return -103; }
    if (ioctl(fd, UI_SET_KEYBIT, BTN_MIDDLE) < 0)           { close(fd); return -104; }
    if (ioctl(fd, UI_SET_EVBIT, EV_REL) < 0)                { close(fd); return -105; }
    if (ioctl(fd, UI_SET_RELBIT, REL_X) < 0)                { close(fd); return -106; }
    if (ioctl(fd, UI_SET_RELBIT, REL_Y) < 0)                { close(fd); return -107; }
    if (ioctl(fd, UI_SET_RELBIT, REL_WHEEL) < 0)            { close(fd); return -108; }
    if (ioctl(fd, UI_SET_EVBIT, EV_SYN) < 0)                { close(fd); return -109; }

    struct uinput_setup usetup;
    memset(&usetup, 0, sizeof(usetup));
    usetup.id.bustype = BUS_USB;
    usetup.id.vendor  = 0x46C1;
    usetup.id.product = 0x0001;
    usetup.id.version = 1;
    strncpy(usetup.name, "FlaxClient Virtual Mouse", UINPUT_MAX_NAME_SIZE - 1);

    if (ioctl(fd, UI_DEV_SETUP, &usetup) < 0) { close(fd); return -110; }
    if (ioctl(fd, UI_DEV_CREATE) < 0)         { close(fd); return -111; }

    struct timespec ts = { .tv_sec = 0, .tv_nsec = 300L * 1000L * 1000L };
    nanosleep(&ts, NULL);

    g_fd = fd;

    for (int i = 0; i < MAX_MICE; i++) g_mice_fds[i] = -1;
    probe_mice();
    return 0;
}

/*
 * Emit press+release in a single JNI call. Sending both from C rather than
 * bouncing through Java twice halves the syscall + JNI overhead per click.
 */
JNIEXPORT jint JNICALL
Java_me_eldodebug_soar_utils_mouse_NativeMouseBridge_nativeClick(JNIEnv *env, jclass cls,
                                                                  jint button) {
    if (g_fd < 0) return -1;
    int code = BTN_LEFT;
    if (button == 1) code = BTN_RIGHT;
    else if (button == 2) code = BTN_MIDDLE;

    if (emit(g_fd, EV_KEY, code, 1) < 0)       return -2;
    if (emit(g_fd, EV_SYN, SYN_REPORT, 0) < 0) return -3;
    if (emit(g_fd, EV_KEY, code, 0) < 0)       return -4;
    if (emit(g_fd, EV_SYN, SYN_REPORT, 0) < 0) return -5;
    return 0;
}

JNIEXPORT jint JNICALL
Java_me_eldodebug_soar_utils_mouse_NativeMouseBridge_nativeButton(JNIEnv *env, jclass cls,
                                                                   jint button, jboolean down) {
    if (g_fd < 0) return -1;
    int code = BTN_LEFT;
    if (button == 1) code = BTN_RIGHT;
    else if (button == 2) code = BTN_MIDDLE;

    if (emit(g_fd, EV_KEY, code, down ? 1 : 0) < 0) return -2;
    if (emit(g_fd, EV_SYN, SYN_REPORT, 0) < 0)      return -3;
    return 0;
}

JNIEXPORT jint JNICALL
Java_me_eldodebug_soar_utils_mouse_NativeMouseBridge_nativeMove(JNIEnv *env, jclass cls,
                                                                 jint dx, jint dy) {
    if (g_fd < 0) return -1;
    if (dx != 0 && emit(g_fd, EV_REL, REL_X, dx) < 0) return -2;
    if (dy != 0 && emit(g_fd, EV_REL, REL_Y, dy) < 0) return -3;
    if (emit(g_fd, EV_SYN, SYN_REPORT, 0) < 0)        return -4;
    return 0;
}

/*
 * Returns 1 if any real (non-virtual) mouse currently has the requested
 * button pressed, 0 otherwise. -1 if we don't have any evdev device open.
 * button: 0 = left, 1 = right, 2 = middle.
 */
JNIEXPORT jint JNICALL
Java_me_eldodebug_soar_utils_mouse_NativeMouseBridge_nativeQueryButton(JNIEnv *env, jclass cls,
                                                                        jint button) {
    long t = now_ms();
    /* Re-scan periodically so a mouse plugged in mid-session becomes visible. */
    if (g_mice_count < 0 || t - g_mice_probed_at > 3000L) {
        probe_mice();
    }
    if (g_mice_count <= 0) return -1;

    int code = BTN_LEFT;
    if (button == 1) code = BTN_RIGHT;
    else if (button == 2) code = BTN_MIDDLE;

    unsigned long keystate[(KEY_MAX + 8 * sizeof(long)) / (8 * sizeof(long))];
    for (int i = 0; i < g_mice_count; i++) {
        int fd = g_mice_fds[i];
        if (fd < 0) continue;
        memset(keystate, 0, sizeof(keystate));
        if (ioctl(fd, EVIOCGKEY(sizeof(keystate)), keystate) < 0) continue;
        if (has_bit(keystate, KEY_MAX, code)) return 1;
    }
    return 0;
}

/*
 * Diagnostic string: uinput fd status, detected physical mouse device count,
 * their names, and BTN_LEFT presence per device. Read at startup by the client
 * so any misconfiguration surfaces in the log.
 */
JNIEXPORT jstring JNICALL
Java_me_eldodebug_soar_utils_mouse_NativeMouseBridge_nativeStatus(JNIEnv *env, jclass cls) {
    if (g_mice_count < 0) probe_mice();

    char buf[2048];
    int pos = 0;
    pos += snprintf(buf + pos, sizeof(buf) - pos, "uinput_fd=%d mice_count=%d [", g_fd, g_mice_count);
    for (int i = 0; i < g_mice_count && pos < (int) sizeof(buf) - 128; i++) {
        int fd = g_mice_fds[i];
        char name[256] = {0};
        ioctl(fd, EVIOCGNAME(sizeof(name) - 1), name);
        unsigned long keystate[(KEY_MAX + 8 * sizeof(long)) / (8 * sizeof(long))];
        memset(keystate, 0, sizeof(keystate));
        int state = -1;
        if (ioctl(fd, EVIOCGKEY(sizeof(keystate)), keystate) >= 0) {
            state = has_bit(keystate, KEY_MAX, BTN_LEFT);
        }
        pos += snprintf(buf + pos, sizeof(buf) - pos, "%s{'%s' left=%d}",
                        i > 0 ? "," : "", name, state);
    }
    snprintf(buf + pos, sizeof(buf) - pos, "]");
    return (*env)->NewStringUTF(env, buf);
}

JNIEXPORT void JNICALL
Java_me_eldodebug_soar_utils_mouse_NativeMouseBridge_nativeShutdown(JNIEnv *env, jclass cls) {
    close_mice();
    if (g_fd >= 0) {
        ioctl(g_fd, UI_DEV_DESTROY);
        close(g_fd);
        g_fd = -1;
    }
}
