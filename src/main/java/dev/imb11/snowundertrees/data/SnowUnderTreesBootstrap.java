//? >1.21.2 {
package dev.imb11.snowundertrees.data;

import dev.imb11.snowundertrees.world.SnowUnderTreesWorldgen;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.PlacedFeature;

import java.util.List;

public class SnowUnderTreesBootstrap {
    /**
     * Main method for creating configured features.
     * See also <a href="https://minecraft.fandom.com/wiki/Custom_feature#Configured_Feature">Configured Feature</a>
     * on the Minecraft Wiki.
     */
    public static void configuredFeatures(Registerable<ConfiguredFeature<?, ?>> registry) {
        // Register the configured feature.
        registry.register(SnowUnderTreesWorldgen.configuredKey(), SnowUnderTreesWorldgen.configuredFeature());
    }

    /**
     * Main method for creating placed features.
     * See also <a href="https://minecraft.fandom.com/wiki/Custom_feature#Placed_Feature">Placed Feature</a>
     * on the Minecraft Wiki.
     */
    public static void placedFeatures(Registerable<PlacedFeature> registry) {
        // Get the configured feature registry
        var configuredFeatureLookup = registry.getRegistryLookup(RegistryKeys.CONFIGURED_FEATURE);

        // Register the placed feature.
        registry.register(SnowUnderTreesWorldgen.placedKey(), new PlacedFeature(configuredFeatureLookup.getOrThrow(SnowUnderTreesWorldgen.configuredKey()), List.of()));
    }
}
//?}