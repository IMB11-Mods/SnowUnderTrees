//? >1.21.2 {
package dev.imb11.snowundertrees.data;

import dev.imb11.snowundertrees.world.SnowUnderTreesWorldgen;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.List;

public class SnowUnderTreesBootstrap {
    /**
     * Main method for creating configured features.
     * See also <a href="https://minecraft.fandom.com/wiki/Custom_feature#Configured_Feature">Configured Feature</a>
     * on the Minecraft Wiki.
     */
    public static void configuredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> registry) {
        // Register the configured feature.
        registry.register(SnowUnderTreesWorldgen.configuredKey(), SnowUnderTreesWorldgen.configuredFeature());
    }

    /**
     * Main method for creating placed features.
     * See also <a href="https://minecraft.fandom.com/wiki/Custom_feature#Placed_Feature">Placed Feature</a>
     * on the Minecraft Wiki.
     */
    public static void placedFeatures(BootstrapContext<PlacedFeature> registry) {
        // Get the configured feature registry
        var configuredFeatureLookup = registry.lookup(Registries.CONFIGURED_FEATURE);

        // Register the placed feature.
        registry.register(SnowUnderTreesWorldgen.placedKey(), new PlacedFeature(configuredFeatureLookup.getOrThrow(SnowUnderTreesWorldgen.configuredKey()), List.of()));
    }
}
//?}