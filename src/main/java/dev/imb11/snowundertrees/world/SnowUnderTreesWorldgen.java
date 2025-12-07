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
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class SnowUnderTreesWorldgen {
    private static final Feature<NoneFeatureConfiguration> SNOW_UNDER_TREES_FEATURE = new SnowUnderTreesFeature(NoneFeatureConfiguration.CODEC);
    private static final ConfiguredFeature<?, ?> SNOW_UNDER_TREES_CONFIGURED = new ConfiguredFeature<>(SNOW_UNDER_TREES_FEATURE, new NoneFeatureConfiguration());
    private static final ResourceKey<ConfiguredFeature<?, ?>> SNOW_UNDER_TREES_CONFIGURED_KEY = ResourceKey.create(Registries.CONFIGURED_FEATURE, SnowUnderTrees.id("snow_under_trees"));
    private static final ResourceKey<PlacedFeature> SNOW_UNDER_TREES_PLACED_KEY = ResourceKey.create(Registries.PLACED_FEATURE, SnowUnderTrees.id("snow_under_trees"));

    public static void initialize() {
        // Register the core feature
        Registry.register(BuiltInRegistries.FEATURE, SnowUnderTrees.id("snow_under_trees"), SNOW_UNDER_TREES_FEATURE);

        // Add biome modification
        BiomeModifications.addFeature(biome -> shouldAddSnow(biome.getBiomeKey()), GenerationStep.Decoration.TOP_LAYER_MODIFICATION, SNOW_UNDER_TREES_PLACED_KEY);
    }

    private static boolean shouldAddSnow(ResourceKey<Biome> biomeKey) {
        return SnowUnderTreesConfig.get().enableBiomeFeature &&
                SnowUnderTreesConfig.get().supportedBiomes.contains(biomeKey.identifier().toString());
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> configuredKey() {
        return SNOW_UNDER_TREES_CONFIGURED_KEY;
    }

    public static ConfiguredFeature<?, ?> configuredFeature() {
        return SNOW_UNDER_TREES_CONFIGURED;
    }

    public static ResourceKey<PlacedFeature> placedKey() {
        return SNOW_UNDER_TREES_PLACED_KEY;
    }

    public static Feature<?> feature() {
        return SNOW_UNDER_TREES_FEATURE;
    }
}
