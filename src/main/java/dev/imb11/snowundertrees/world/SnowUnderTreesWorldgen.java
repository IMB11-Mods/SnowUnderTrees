package dev.imb11.snowundertrees.world;

import dev.imb11.snowundertrees.SnowUnderTrees;
import dev.imb11.snowundertrees.config.SnowUnderTreesConfig;
import dev.imb11.snowundertrees.world.feature.SnowUnderTreesFeature;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class SnowUnderTreesWorldgen {
    private static final Feature SNOW_UNDER_TREES_FEATURE = SnowUnderTreesFeature.INSTANCE;
    private static final Feature SNOW_UNDER_TREES_CONFIGURED = SnowUnderTreesFeature.INSTANCE;
    private static final ResourceKey<Feature> SNOW_UNDER_TREES_CONFIGURED_KEY = ResourceKey.create(Registries.FEATURE, SnowUnderTrees.id("snow_under_trees"));
    private static final ResourceKey<PlacedFeature> SNOW_UNDER_TREES_PLACED_KEY = ResourceKey.create(Registries.PLACED_FEATURE, SnowUnderTrees.id("snow_under_trees"));

    public static void initialize() {
        // Register the core feature
        Registry.register(BuiltInRegistries.FEATURE_TYPE, SnowUnderTrees.id("snow_under_trees"), SnowUnderTreesFeature.CODEC);

        // Add biome modification
        BiomeModifications.addFeature(biome -> shouldAddSnow(biome.getBiomeKey()), GenerationStep.Decoration.TOP_LAYER_MODIFICATION, SNOW_UNDER_TREES_PLACED_KEY);
    }

    private static boolean shouldAddSnow(ResourceKey<Biome> biomeKey) {
        return SnowUnderTreesConfig.get().enableBiomeFeature &&
                SnowUnderTreesConfig.get().supportedBiomes.contains(biomeKey.identifier().toString());
    }

    public static ResourceKey<Feature> configuredKey() {
        return SNOW_UNDER_TREES_CONFIGURED_KEY;
    }

    public static Feature configuredFeature() {
        return SNOW_UNDER_TREES_CONFIGURED;
    }

    public static ResourceKey<PlacedFeature> placedKey() {
        return SNOW_UNDER_TREES_PLACED_KEY;
    }

    public static Feature feature() {
        return SNOW_UNDER_TREES_FEATURE;
    }
}
