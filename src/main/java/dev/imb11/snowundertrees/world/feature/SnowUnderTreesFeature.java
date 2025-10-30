package dev.imb11.snowundertrees.world.feature;

import com.mojang.serialization.Codec;
import dev.imb11.snowundertrees.compat.SereneSeasonsEntrypoint;
import dev.imb11.snowundertrees.config.SnowUnderTreesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class SnowUnderTreesFeature extends Feature<NoneFeatureConfiguration> {

    public SnowUnderTreesFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!SnowUnderTreesConfig.get().enableBiomeFeature) {
            return false;
        }

        if(SnowUnderTreesConfig.get().respectSeasonMods && SereneSeasonsEntrypoint.isSereneSeasonsLoaded) {
            if(!SereneSeasonsEntrypoint.shouldPlaceSnow(context.level().getLevel(), context.origin())) {
                return false;
            }
        }

        BlockPos origin = context.origin();
        WorldGenLevel world = context.level();

        // Iterate within a 16x16 area around the feature origin
        for (int xOffset = 0; xOffset < 16; xOffset++) {
            for (int zOffset = 0; zOffset < 16; zOffset++) {
                int x = origin.getX() + xOffset;
                int z = origin.getZ() + zOffset;

                // Find top surfaces
                int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
                BlockPos currentPos = new BlockPos(x, y, z);

                // Early exit if not leaves
                if (!(world.getBlockState(currentPos).getBlock() instanceof LeavesBlock)) {
                    continue; // Skip to the next iteration if not leaves
                }

                // Find ground below leaves
                y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                currentPos = currentPos.atY(y);

                // Biome check for snow suitability
                Biome biome = world.getBiome(currentPos).value();
                if (!biome.shouldSnow(world, currentPos)) {
                    continue; // Skip if this biome doesn't allow snow
                }

                // Snow placement
                world.setBlock(currentPos, Blocks.SNOW.defaultBlockState(), 2);

                BlockPos belowPos = currentPos.below();
                BlockState belowState = world.getBlockState(belowPos);
                if (belowState.hasProperty(SnowyDirtBlock.SNOWY)) {
                    world.setBlock(belowPos, belowState.setValue(SnowyDirtBlock.SNOWY, true), 2);
                }
            }
        }

        return true;
    }
}