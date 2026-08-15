package me.eldodebug.soar.injection.mixin;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import me.eldodebug.soar.injection.transformer.LwjglTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class GlideTweaker implements ITweaker {

    private final List<String> launchArguments = new ArrayList<>();
    private boolean forgeEnvironment;

	public static boolean hasOptifine = false;
	
    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {

        forgeEnvironment = isForgeEnvironment();
        System.setProperty("flax.runtime.forge", Boolean.toString(forgeEnvironment));
    	
    	try {
			Class.forName("optifine.Patcher");
			hasOptifine = true;
		}
		catch(ClassNotFoundException e) {
		}
		
        // FMLTweaker already owns and returns the Minecraft argument list. A
        // secondary tweaker must not return it again or options such as
        // --width and --height are appended twice by LaunchWrapper.
        if (forgeEnvironment) {
            return;
        }

        this.launchArguments.addAll(args);

        if (profile != null) {
            launchArguments.add("--version");
            launchArguments.add(profile);
        }

        if (assetsDir != null) {
            launchArguments.add("--assetsDir");
            launchArguments.add(assetsDir.getAbsolutePath());
        }

        if (gameDir != null) {
            launchArguments.add("--gameDir");
            launchArguments.add(gameDir.getAbsolutePath());
        }
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {

    	classLoader.registerTransformer(LwjglTransformer.class.getName());

        // The active Mixin environment changes phase during Forge startup.
        // Set the global override before bootstrapping so every phase reads the
        // refmap using the same namespace as the classes in that environment.
        System.setProperty("mixin.env.obf", forgeEnvironment ? "searge" : "notch");

        MixinBootstrap.init();

        MixinEnvironment env = MixinEnvironment.getDefaultEnvironment();
        Mixins.addConfiguration("mixins.soar.json");

        // Forge deobfuscates runtime classes to SRG names before mixins are
        // applied. The standalone/Inject distribution targets vanilla notch
        // names instead.
        env.setObfuscationContext(forgeEnvironment ? "searge" : "notch");

        env.setSide(MixinEnvironment.Side.CLIENT);

        this.unlockLwjgl();
    }

    @Override
    public String getLaunchTarget() {
        return "net.minecraft.client.main.Main";
    }

    @Override
    public String[] getLaunchArguments() {
        return launchArguments.toArray(new String[0]);
    }

    private boolean isForgeEnvironment() {
        return GlideTweaker.class.getClassLoader().getResource(
                "net/minecraftforge/fml/common/launcher/FMLTweaker.class") != null;
    }
    
    @SuppressWarnings("unchecked")
    private void unlockLwjgl() {
        try {
            Field transformerExceptions = LaunchClassLoader.class.getDeclaredField("classLoaderExceptions");
            transformerExceptions.setAccessible(true);
            Object o = transformerExceptions.get(Launch.classLoader);
            Set<String> exceptions = (Set<String>) o;
            exceptions.remove("org.lwjgl.");

            // Keep Minecraft's sealed LWJGL 2 packages on the parent loader
            // while allowing Flax's separate NanoVG/LWJGL 3 subpackages to
            // pass through LaunchClassLoader.
            exceptions.add("org.lwjgl.input.");
            exceptions.add("org.lwjgl.openal.");
            exceptions.add("org.lwjgl.opencl.");
            exceptions.add("org.lwjgl.opengl.");
            exceptions.add("org.lwjgl.opengles.");
            exceptions.add("org.lwjgl.Buffer");
            exceptions.add("org.lwjgl.DefaultSys");
            exceptions.add("org.lwjgl.J2SESys");
            exceptions.add("org.lwjgl.LWJGL");
            exceptions.add("org.lwjgl.LinuxSys");
            exceptions.add("org.lwjgl.MacOSXSys");
            exceptions.add("org.lwjgl.MemoryUtil");
            exceptions.add("org.lwjgl.PointerBuffer");
            exceptions.add("org.lwjgl.PointerWrapper");
            exceptions.add("org.lwjgl.Sys");
            exceptions.add("org.lwjgl.WindowsSys");
        } catch (NoSuchFieldException | IllegalAccessException e) {}
    }
}
