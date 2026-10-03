package dev.imb11.snowundertrees.world;

import dev.imb11.snowundertrees.compat.SereneSeasonsEntrypoint;
import dev.imb11.snowundertrees.config.SnowUnderTreesConfig;
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

public class WorldTickHandler {
    private SnowDiagnostics diagnostics;
    private boolean nativeMode;
    private boolean nativeSnowfall;
    private boolean active;

    public void onStartTick(ServerLevel world) {
        diagnostics = SnowDiagnostics.ENABLED ? LoadedChunkTracker.diagnostics(world) : null;
        long started = diagnostics == null ? 0 : diagnostics.beginTick();
        var config = SnowUnderTreesConfig.get();
        boolean snowfall = config.enableWhenSnowing && world.isRaining();
        int meltInterval = SereneSeasonsEntrypoint.getMeltingInterval(world);
        nativeMode = config.maxChunkVisitsPerTick <= 0 && meltInterval == 0;
        nativeSnowfall = nativeMode && snowfall;
        active = snowfall || meltInterval > 0;
        LoadedChunkTracker.processTick(world, snowfall && !nativeMode, meltInterval, config.maxChunkVisitsPerTick,
                chunk -> processChunk(world, chunk, diagnostics),
                chunk -> SereneSeasonsEntrypoint.meltSnowInChunk(world, chunk));
        if (diagnostics != null) diagnostics.endScheduler(started);
    }

    public void onChunkTick(ServerLevel world, LevelChunk chunk) {
        if (!nativeSnowfall) return;
        long visitStarted = diagnostics == null ? 0 : diagnostics.beginVisit();
        if (diagnostics != null) {
            diagnostics.count(SnowDiagnostics.Counter.SNOW_VISITS);
            diagnostics.count(SnowDiagnostics.Counter.NATIVE_SNOW_VISITS);
        }
        long started = diagnostics == null ? 0 : diagnostics.startStage();
        boolean process = world.getRandom().nextInt(4) == 0;
        if (diagnostics != null) diagnostics.endStage(SnowDiagnostics.Stage.RANDOM_CHECK, started);
        if (process) {
            if (diagnostics != null) diagnostics.count(SnowDiagnostics.Counter.SNOW_READY);
            processChunk(world, chunk, diagnostics);
        } else if (diagnostics != null) {
            diagnostics.count(SnowDiagnostics.Counter.RANDOM_REJECTED);
        }
        if (diagnostics != null) diagnostics.endStage(SnowDiagnostics.Stage.VISIT, visitStarted);
    }

    public void onEndTick(ServerLevel world) {
        nativeSnowfall = false;
        if (diagnostics != null) diagnostics.endTick(world, active, nativeMode);
    }

    private void processChunk(ServerLevel world, LevelChunk chunk, SnowDiagnostics diagnostics) {
        long started = diagnostics == null ? 0 : diagnostics.startStage();
        BlockPos randomPos = findRandomSurfacePosition(world, chunk);
        if (diagnostics != null) diagnostics.endStage(SnowDiagnostics.Stage.CANOPY_SEARCH, started);
        if (randomPos == null) {
            if (diagnostics != null) diagnostics.count(SnowDiagnostics.Counter.NO_CANOPY);
            return;
        }

        started = diagnostics == null ? 0 : diagnostics.startStage();
        BlockPos snowPlacementPos = randomPos.atY(chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                randomPos.getX() & 15, randomPos.getZ() & 15) + 1);
        if (diagnostics != null) diagnostics.endStage(SnowDiagnostics.Stage.GROUND_HEIGHT, started);
        started = diagnostics == null ? 0 : diagnostics.startStage();
        boolean canPlace = canPlaceSnow(world, chunk, snowPlacementPos, diagnostics);
        if (diagnostics != null) diagnostics.endStage(SnowDiagnostics.Stage.PLACEMENT_CHECKS, started);
        if (canPlace) {
            if (diagnostics != null) diagnostics.count(SnowDiagnostics.Counter.PLACEMENT_ATTEMPTS);
            started = diagnostics == null ? 0 : diagnostics.startStage();
            boolean placed = placeSnowLayers(world, snowPlacementPos);
            if (diagnostics != null) {
                diagnostics.endStage(SnowDiagnostics.Stage.SNOW_PLACEMENT, started);
                if (placed) diagnostics.count(SnowDiagnostics.Counter.SNOW_PLACED);
            }
        }
    }

    private BlockPos findRandomSurfacePosition(ServerLevel world, LevelChunk chunk) {
        BlockPos randomPos = world.getBlockRandomPos(chunk.getPos().getMinBlockX(), 0, chunk.getPos().getMinBlockZ(), 15);
        BlockPos canopyPos = randomPos.atY(chunk.getHeight(Heightmap.Types.MOTION_BLOCKING,
                randomPos.getX() & 15, randomPos.getZ() & 15));
        if (chunk.getBlockState(canopyPos).getBlock() instanceof LeavesBlock) {
            return randomPos;
        }
        return null; // Return null if we didn't find a suitable starting point
    }

    private boolean canPlaceSnow(ServerLevel world, LevelChunk chunk, BlockPos pos, SnowDiagnostics diagnostics) {
        if (!world.isInsideBuildHeight(pos.getY())) {
            if (diagnostics != null) diagnostics.count(SnowDiagnostics.Counter.OUTSIDE_HEIGHT);
            return false;
        }
        if (!chunk.getBlockState(pos).isAir()) {
            if (diagnostics != null) diagnostics.count(SnowDiagnostics.Counter.OCCUPIED);
            return false;
        }

        var biome = world.getBiome(pos);
        if (SereneSeasonsEntrypoint.isSeasonIntegrationEnabled(world)) {
            boolean suitable = biome.value().hasPrecipitation()
                    && world.getBrightness(LightLayer.BLOCK, pos) < 10
                    && Blocks.SNOW.defaultBlockState().canSurvive(world, pos)
                    && SereneSeasonsEntrypoint.shouldPlaceSnow(world, pos);
            if (!suitable && diagnostics != null) diagnostics.count(SnowDiagnostics.Counter.SEASON_REJECTED);
            return suitable;
        }

        boolean supported = biome.unwrapKey()
                .map(key -> SnowUnderTreesConfig.get().supportsBiome(key.identifier()))
                .orElse(false);
        if (!supported) {
            if (diagnostics != null) diagnostics.count(SnowDiagnostics.Counter.UNSUPPORTED_BIOME);
            return false;
        }
        boolean suitable = biome.value().shouldSnow(world, pos);
        if (!suitable && diagnostics != null) diagnostics.count(SnowDiagnostics.Counter.VANILLA_REJECTED);
        return suitable;
    }

    private boolean placeSnowLayers(ServerLevel world, BlockPos pos) {
        boolean placed = world.setBlockAndUpdate(pos, Blocks.SNOW.defaultBlockState());
        BlockPos belowPos = pos.below();
        BlockState belowState = world.getBlockState(belowPos);
        if (belowState.isFaceSturdy(world, belowPos, Direction.UP) && belowState.hasProperty(SnowyBlock.SNOWY)) {
            world.setBlock(belowPos, belowState.setValue(SnowyBlock.SNOWY, true), 2);
        }
        return placed;
    }
}
