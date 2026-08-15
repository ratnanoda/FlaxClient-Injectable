package me.eldodebug.soar.management.command.impl;

import me.eldodebug.soar.logger.GlideLogger;
import me.eldodebug.soar.management.command.Command;

/** Opens the Forge-only fog distance GUI without linking it into Lunar. */
public final class FogCommand extends Command {

    public FogCommand() {
        super("fog");
    }

    @Override
    public void onCommand(String message) {
        if (!Boolean.getBoolean("flax.runtime.forge")) {
            return;
        }

        try {
            Class<?> screen = Class.forName("me.eldodebug.soar.forge.gui.GuiFogSettings");
            screen.getMethod("open").invoke(null);
        } catch (Exception e) {
            GlideLogger.error("Failed to open Forge fog settings", e);
        }
    }
}
