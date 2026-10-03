package dev.imb11.snowundertrees.world;

import dev.imb11.snowundertrees.compat.SereneSeasonsEntrypoint;
import dev.imb11.snowundertrees.config.SnowUnderTreesConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

public class WorldTickHandler implements ServerTickEvents.StartLevelTick {
    @Override
    public void onStartTick(ServerLevel world) {

        var config = SnowUnderTreesConfig.get();
        boolean snowfall = config.enableWhenSnowing && world.isRaining();
        int meltInterval = SereneSeasonsEntrypoint.getMeltingInterval(world);
        LoadedChunkTracker.processTick(world, snowfall, meltInterval, config.maxChunkVisitsPerTick,
                chunk -> processChunk(world, chunk),
                chunk -> SereneSeasonsEntrypoint.meltSnowInChunk(world, chunk));
    }

    private void processChunk(ServerLevel world, LevelChunk chunk) {
        if (!shouldProcessChunk(world)) return;

        BlockPos randomPos = findRandomSurfacePosition(world, chunk);
        if (randomPos == null) return;

        BlockPos snowPlacementPos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, randomPos);
        if (canPlaceSnow(world, snowPlacementPos)) {
            placeSnowLayers(world, snowPlacementPos);
        }
    }

    private boolean shouldProcessChunk(ServerLevel world) {
        return world.getRandom().nextInt(4) == 0;
    }

    private BlockPos findRandomSurfacePosition(ServerLevel world, LevelChunk chunk) {
        BlockPos randomPos = world.getBlockRandomPos(chunk.getPos().getMinBlockX(), 0, chunk.getPos().getMinBlockZ(), 15);
        if (world.getBlockState(world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, randomPos).below()).getBlock() instanceof LeavesBlock) {
            return randomPos;
        }
        return null; // Return null if we didn't find a suitable starting point
    }

    private boolean canPlaceSnow(ServerLevel world, BlockPos pos) {
        if (!world.isInsideBuildHeight(pos.getY())
                || !world.isEmptyBlock(pos)
                || world.getBrightness(LightLayer.BLOCK, pos) >= 10
                || !Blocks.SNOW.defaultBlockState().canSurvive(world, pos)) {
            return false;
        }

        var biome = world.getBiome(pos);
        if (SereneSeasonsEntrypoint.isSeasonIntegrationEnabled(world)) {
            return biome.value().hasPrecipitation()
                    && SereneSeasonsEntrypoint.shouldPlaceSnow(world, pos);
        }

        return biome.unwrapKey()
                .map(key -> SnowUnderTreesConfig.get().supportsBiome(key.identifier()))
                .orElse(false)
                && biome.value().shouldSnow(world, pos);
    }

    private void placeSnowLayers(ServerLevel world, BlockPos pos) {
        world.setBlockAndUpdate(pos, Blocks.SNOW.defaultBlockState());
        BlockPos belowPos = pos.below();
        BlockState belowState = world.getBlockState(belowPos);
        if (belowState.isFaceSturdy(world, belowPos, Direction.UP) && belowState.hasProperty(SnowyBlock.SNOWY)) {
            world.setBlock(belowPos, belowState.setValue(SnowyBlock.SNOWY, true), 2);
        }
    }
}
