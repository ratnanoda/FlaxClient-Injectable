package me.eldodebug.soar.utils.mouse;

import net.minecraft.client.settings.KeyBinding;

/**
 * Queues a click through Minecraft 1.8.9's own KeyBinding input state.
 *
 * LWJGL mouse events are translated into the same setKeyBindState/onTick pair
 * by Minecraft. Keeping synthetic clicks on this path lets the normal client
 * input loop decide when to run attack/use-item behavior instead of directly
 * invoking gameplay methods from a mod callback.
 */
public final class MinecraftMouseInput {

    private MinecraftMouseInput() {}

    public static void click(KeyBinding binding) {
        if(binding == null) {
            return;
        }

        int keyCode = binding.getKeyCode();
        boolean wasDown = binding.isKeyDown();

        KeyBinding.setKeyBindState(keyCode, true);
        KeyBinding.onTick(keyCode);
        KeyBinding.setKeyBindState(keyCode, wasDown);
    }
}
