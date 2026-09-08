package me.eldodebug.soar.utils.buffer;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.management.nanovg.NanoVGManager;

/** Applies alpha directly inside one NanoVG frame without an intermediate FBO. */
public class ScreenAlpha {

    public void wrap(Runnable task, float alphaProgress) {
        NanoVGManager nvg = Glide.getInstance().getNanoVGManager();
        nvg.setupAndDraw(() -> {
            nvg.save();
            try {
                nvg.setAlpha(Math.max(0.0F, Math.min(alphaProgress, 1.0F)));
                task.run();
            } finally {
                nvg.restore();
            }
        });
    }

    public void close() {
        // Direct NanoVG alpha does not own an intermediate framebuffer.
    }
}
