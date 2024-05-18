package dev.imb11.snowundertrees.world;

import dev.imb11.snowundertrees.config.SnowUnderTreesConfig;
import dev.imb11.snowundertrees.mixins.ThreadedAnvilChunkStorageInvoker;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.SnowyBlock;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;

/*? if >1.20.4 { *//*
import net.minecraft.server.world.OptionalChunk;
*//*? } */

import java.util.Optional;

public class WorldTickHandler implements ServerTickEvents.StartWorldTick {
    @Override
    public void onStartTick(ServerWorld world) {
        if (!SnowUnderTreesConfig.get().enableBiomeFeature || !SnowUnderTreesConfig.get().enableWhenSnowing || !world.isRaining()) {
            return;
        }

        ThreadedAnvilChunkStorageInvoker chunkStorage = (ThreadedAnvilChunkStorageInvoker) world.getChunkManager().threadedAnvilChunkStorage;
        Iterable<ChunkHolder> chunkHolders = chunkStorage.invokeEntryIterator();
        chunkHolders.forEach(chunkHolder -> processChunk(world, chunkHolder));
    }

    private void processChunk(ServerWorld world, ChunkHolder chunkHolder) {
        /*? if <1.20.5 { */
        Optional<WorldChunk> optionalChunk = chunkHolder.getEntityTickingFuture().getNow(ChunkHolder.UNLOADED_WORLD_CHUNK).left();
        /*? } else { *//*
        OptionalChunk<WorldChunk> optionalChunk = chunkHolder.getEntityTickingFuture().getNow(ChunkHolder.UNLOADED_WORLD_CHUNK);
        *//*? } */

        if (optionalChunk.isPresent() && shouldProcessChunk(world)) {
            WorldChunk chunk = optionalChunk.orElse(null);
            if (chunk == null) {
                return;
            }

            // Early biome eligibility check
            if (!isBiomeSuitable(world, chunk)) {
                return; // Skip to next chunk if biome doesn't support snow
            }

            BlockPos randomPos = findRandomSurfacePosition(world, chunk);
            if (randomPos != null) { // Guard in case no eligible surface position was found
                BlockPos snowPlacementPos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, randomPos);

                if (canPlaceSnow(world, snowPlacementPos)) {
                    placeSnowLayers(world, snowPlacementPos);
                }
            }
        }
    }

    private boolean shouldProcessChunk(ServerWorld world) {
        return world.random.nextInt(4) == 0; // 25% chance for chunk processing
    }

    private boolean isBiomeSuitable(ServerWorld world, WorldChunk chunk) {
        BlockPos biomeCheckPos = world.getRandomPosInChunk(chunk.getPos().getStartX(), 0, chunk.getPos().getStartZ(), 15);
        Biome biome = world.getBiome(biomeCheckPos).value();
        Identifier biomeId = world.getRegistryManager().get(RegistryKeys.BIOME).getKey(biome).get().getValue();
        return SnowUnderTreesConfig.get().supportedBiomes.contains(biomeId.toString());
    }

    private BlockPos findRandomSurfacePosition(ServerWorld world, WorldChunk chunk) {
        BlockPos randomPos = world.getRandomPosInChunk(chunk.getPos().getStartX(), 0, chunk.getPos().getStartZ(), 15);
        if (world.getBlockState(world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, randomPos).down()).getBlock() instanceof LeavesBlock) {
            return randomPos;
        }
        return null; // Return null if we didn't find a suitable starting point
    }

    private boolean canPlaceSnow(ServerWorld world, BlockPos pos) {
        Biome biome = world.getBiome(pos).value();
        return biome.canSetSnow(world, pos) && world.isAir(pos);
    }

    private void placeSnowLayers(ServerWorld world, BlockPos pos) {
        world.setBlockState(pos, Blocks.SNOW.getDefaultState());
        BlockPos belowPos = pos.down();
        BlockState belowState = world.getBlockState(belowPos);
        if (belowState.isSideSolidFullSquare(world, belowPos, Direction.UP) && belowState.contains(SnowyBlock.SNOWY)) {
            world.setBlockState(belowPos, belowState.with(SnowyBlock.SNOWY, true), 2);
        }
    }
}