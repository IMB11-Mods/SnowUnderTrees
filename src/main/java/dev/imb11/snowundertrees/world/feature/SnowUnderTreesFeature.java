package dev.imb11.snowundertrees.world.feature;

import com.mojang.serialization.MapCodec;
import dev.imb11.snowundertrees.compat.SereneSeasonsEntrypoint;
import dev.imb11.snowundertrees.config.SnowUnderTreesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

public class SnowUnderTreesFeature implements Feature {

    public static final SnowUnderTreesFeature INSTANCE = new SnowUnderTreesFeature();
    public static final MapCodec<SnowUnderTreesFeature> CODEC = MapCodec.unit(INSTANCE);

    public SnowUnderTreesFeature() {

    }

    @Override
    public MapCodec<? extends Feature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel world, ChunkGenerator chunkGenerator, RandomSource random, BlockPos origin) {
        if (!SnowUnderTreesConfig.get().enableBiomeFeature) {
            return false;
        }

        if (SereneSeasonsEntrypoint.isSeasonIntegrationEnabled(world.getLevel())) {
            if(!SereneSeasonsEntrypoint.shouldPlaceSnow(world.getLevel(), origin)) {
                return false;
            }
        }

        BlockPos.MutableBlockPos currentPos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos belowPos = new BlockPos.MutableBlockPos();
        for (int xOffset = 0; xOffset < 16; xOffset++) {
            for (int zOffset = 0; zOffset < 16; zOffset++) {
                int x = origin.getX() + xOffset;
                int z = origin.getZ() + zOffset;

                // Find top surfaces
                int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
                currentPos.set(x, y, z);

                // Early exit if not leaves
                if (!(world.getBlockState(currentPos).getBlock() instanceof LeavesBlock)) {
                    continue; // Skip to the next iteration if not leaves
                }

                // Find ground below leaves
                y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                currentPos.setY(y);

                // Biome check for snow suitability
                Biome biome = world.getBiome(currentPos).value();
                if (!biome.shouldSnow(world, currentPos)) {
                    continue; // Skip if this biome doesn't allow snow
                }

                // Snow placement
                world.setBlock(currentPos, Blocks.SNOW.defaultBlockState(), 2);

                belowPos.set(x, y - 1, z);
                BlockState belowState = world.getBlockState(belowPos);
                if (belowState.hasProperty(SnowyBlock.SNOWY)) {
                    world.setBlock(belowPos, belowState.setValue(SnowyBlock.SNOWY, true), 2);
                }
            }
        }

        return true;
    }
}
