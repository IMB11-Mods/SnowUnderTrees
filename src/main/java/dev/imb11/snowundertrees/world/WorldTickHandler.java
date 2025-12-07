package dev.imb11.snowundertrees.world;

import dev.imb11.snowundertrees.compat.SereneSeasonsEntrypoint;
import dev.imb11.snowundertrees.config.SnowUnderTreesConfig;
import dev.imb11.snowundertrees.mixins.ThreadedAnvilChunkStorageInvoker;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

public class WorldTickHandler implements ServerTickEvents.StartWorldTick {
    @Override
    public void onStartTick(ServerLevel world) {

        if(SereneSeasonsEntrypoint.isSereneSeasonsLoaded) {
            SereneSeasonsEntrypoint.attemptMeltSnow(world);
        }

        if (!SnowUnderTreesConfig.get().enableBiomeFeature || !SnowUnderTreesConfig.get().enableWhenSnowing || !world.isRaining()) {
            return;
        }

        ThreadedAnvilChunkStorageInvoker chunkStorage = (ThreadedAnvilChunkStorageInvoker) world.getChunkSource().chunkMap;

        Iterable<ChunkHolder> chunkHolders = chunkStorage.invokeEntryIterator(
                //? if >1.21.8
                ChunkStatus.EMPTY).toList(
                );
        chunkHolders.forEach(chunkHolder -> processChunk(world, chunkHolder));
    }

    private void processChunk(ServerLevel world, ChunkHolder chunkHolder) {
        ChunkResult<LevelChunk> optionalChunk = chunkHolder.getEntityTickingChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK);

        if (optionalChunk.isSuccess() && shouldProcessChunk(world)) {
            LevelChunk chunk = optionalChunk.orElse(null);
            if (chunk == null) {
                return;
            }

            // Early biome eligibility check
            if (!isBiomeSuitable(world, chunk)) {
                return; // Skip to next chunk if biome doesn't support snow
            }

            BlockPos randomPos = findRandomSurfacePosition(world, chunk);
            if (randomPos != null) { // Guard in case no eligible surface position was found
                BlockPos snowPlacementPos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, randomPos);

                if (canPlaceSnow(world, snowPlacementPos)) {
                    placeSnowLayers(world, snowPlacementPos);
                }
            }
        }
    }

    private boolean shouldProcessChunk(ServerLevel world) {
        return world.random.nextInt(4) == 0; // 25% chance for chunk processing
    }

    public static boolean isBiomeSuitable(ServerLevel world, LevelChunk chunk) {
        BlockPos biomeCheckPos = world.getBlockRandomPos(chunk.getPos().getMinBlockX(), 0, chunk.getPos().getMinBlockZ(), 15);
        Biome biome = world.getBiome(biomeCheckPos).value();

        var registryManager = world.registryAccess();
        Registry<Biome> biomeRegistry
        //? if <1.21.2 {
        /*= registryManager.registryOrThrow(Registries.BIOME);
        *///?} else {
        = registryManager.lookupOrThrow(Registries.BIOME);
        //?}

        Identifier biomeId = biomeRegistry.getResourceKey(biome).get().identifier();

        boolean isSupported = SnowUnderTreesConfig.get().supportedBiomes.contains(biomeId.toString());

        if(SereneSeasonsEntrypoint.isSereneSeasonsLoaded) {
            return SereneSeasonsEntrypoint.isBiomeSuitable(world, biomeCheckPos)
                    || isSupported;
        }

        return isSupported;
    }

    private BlockPos findRandomSurfacePosition(ServerLevel world, LevelChunk chunk) {
        BlockPos randomPos = world.getBlockRandomPos(chunk.getPos().getMinBlockX(), 0, chunk.getPos().getMinBlockZ(), 15);
        if (world.getBlockState(world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, randomPos).below()).getBlock() instanceof LeavesBlock) {
            return randomPos;
        }
        return null; // Return null if we didn't find a suitable starting point
    }

    private boolean canPlaceSnow(ServerLevel world, BlockPos pos) {
        Biome biome = world.getBiome(pos).value();
        return biome.shouldSnow(world, pos) && world.isEmptyBlock(pos);
    }

    private void placeSnowLayers(ServerLevel world, BlockPos pos) {
        world.setBlockAndUpdate(pos, Blocks.SNOW.defaultBlockState());
        BlockPos belowPos = pos.below();
        BlockState belowState = world.getBlockState(belowPos);
        if (belowState.isFaceSturdy(world, belowPos, Direction.UP) && belowState.hasProperty(SnowyDirtBlock.SNOWY)) {
            world.setBlock(belowPos, belowState.setValue(SnowyDirtBlock.SNOWY, true), 2);
        }
    }
}