#!/usr/bin/env bash
# One-time setup so the FlaxClient native mouse bridge can open /dev/uinput
# without root. Must be run with sudo. After running, log out and back in
# (or reboot) so the input-group membership takes effect.
set -euo pipefail

if [[ $EUID -ne 0 ]]; then
    echo "Run with sudo: sudo $0" >&2
    exit 1
fi

TARGET_USER="${SUDO_USER:-$(logname 2>/dev/null || echo "")}"
if [[ -z "$TARGET_USER" ]]; then
    echo "Could not detect the calling user. Re-run as: sudo -u root $0 <username>" >&2
    exit 1
fi

RULE_FILE="/etc/udev/rules.d/99-flaxclient-uinput.rules"
{
    echo 'KERNEL=="uinput", MODE="0660", GROUP="input", OPTIONS+="static_node=uinput"'
    echo 'SUBSYSTEM=="input", KERNEL=="event*", ENV{ID_INPUT_MOUSE}=="1", MODE="0660", GROUP="input", TAG+="uaccess"'
} > "$RULE_FILE"
echo "Wrote $RULE_FILE"

if ! getent group input >/dev/null; then
    groupadd input
    echo "Created group 'input'"
fi

if id -nG "$TARGET_USER" | tr ' ' '\n' | grep -qx input; then
    echo "$TARGET_USER is already in the 'input' group"
else
    usermod -aG input "$TARGET_USER"
    echo "Added $TARGET_USER to the 'input' group"
fi

udevadm control --reload
udevadm trigger --sysname-match=uinput || true

# Apply immediately to the running kernel node too, so a reboot isn't required
# for the FIRST launch after setup (subsequent boots get the rule).
if [[ -e /dev/uinput ]]; then
    chgrp input /dev/uinput || true
    chmod 0660 /dev/uinput || true
fi

# Physical button tracking is required to distinguish the held real button
# from AutoClicker's own virtual release events.
for event_node in /dev/input/event*; do
    [[ -e "$event_node" ]] || continue
    if ! udevadm info --query=property --name="$event_node" 2>/dev/null | grep -qx 'ID_INPUT_MOUSE=1'; then
        continue
    fi
    chgrp input "$event_node" || true
    chmod 0660 "$event_node" || true
done

# Make the rule effective for the current desktop session too. Group membership
# normally needs a new login; an ACL lets the calling user test immediately.
if command -v setfacl >/dev/null 2>&1; then
    [[ -e /dev/uinput ]] && setfacl -m "u:${TARGET_USER}:rw" /dev/uinput || true
    for event_node in /dev/input/event*; do
        [[ -e "$event_node" ]] || continue
        if udevadm info --query=property --name="$event_node" 2>/dev/null | grep -qx 'ID_INPUT_MOUSE=1'; then
            setfacl -m "u:${TARGET_USER}:rw" "$event_node" || true
        fi
    done
fi

echo
echo "Done. Log out and log back in (or reboot) so the group membership takes effect."
