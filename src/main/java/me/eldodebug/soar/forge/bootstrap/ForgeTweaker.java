package me.eldodebug.soar.forge.bootstrap;

import me.eldodebug.soar.injection.mixin.GlideTweaker;
/**
 * Forge-only LaunchWrapper entry point.
 *
 * LaunchWrapper excludes the package containing a tweaker from its transforming
 * class loader. Keeping this class outside the mixin package prevents Forge from
 * excluding all of Flax's mixin classes along with the entry point.
 */
public final class ForgeTweaker extends GlideTweaker {
}
