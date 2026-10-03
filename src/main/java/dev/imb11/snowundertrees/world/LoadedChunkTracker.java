package dev.imb11.snowundertrees.world;

import dev.imb11.snowundertrees.mixins.ThreadedAnvilChunkStorageInvoker;
import it.unimi.dsi.fastutil.HashCommon;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class LoadedChunkTracker {
    private static final int MELT_PHASE_COUNT = 48;
    private static final Map<ServerLevel, WorldChunks> WORLDS = new IdentityHashMap<>();

    private LoadedChunkTracker() {
    }

    public static void initialize() {
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk, newlyGenerated) ->
                WORLDS.computeIfAbsent(world, ignored -> new WorldChunks())
                        .setLoaded(chunk.getPos().pack(), true));
        ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> {
            var positions = WORLDS.get(world);
            if (positions != null) positions.setLoaded(chunk.getPos().pack(), false);
        });
        ServerLevelEvents.UNLOAD.register((server, world) -> WORLDS.remove(world));
        ServerLifecycleEvents.SERVER_STOPPED.register(server ->
                WORLDS.keySet().removeIf(world -> world.getServer() == server));
    }

    public static void forEachEntityTickingChunk(ServerLevel world, Consumer<LevelChunk> action) {
        var positions = WORLDS.get(world);
        if (positions == null) return;

        var chunkMap = (ThreadedAnvilChunkStorageInvoker) world.getChunkSource().chunkMap;
        positions.loaded.forEach(position -> processEntityTickingChunk(chunkMap, position, action));
    }

    public static void forEachDueMeltingChunk(ServerLevel world, int interval, Consumer<LevelChunk> action) {
        if (interval <= 0 || MELT_PHASE_COUNT % interval != 0) {
            throw new IllegalArgumentException("Unsupported melting interval: " + interval);
        }
        var positions = WORLDS.get(world);
        if (positions == null) return;

        long tick = world.getGameTime();
        if (positions.lastMeltTick == tick) return;
        positions.lastMeltTick = tick;

        var chunkMap = (ThreadedAnvilChunkStorageInvoker) world.getChunkSource().chunkMap;
        int phase = (int) Math.floorMod(tick, (long) interval);
        for (int bucket = phase; bucket < MELT_PHASE_COUNT; bucket += interval) {
            positions.melting[bucket].forEach(position -> processEntityTickingChunk(chunkMap, position, action));
        }
    }

    private static void processEntityTickingChunk(ThreadedAnvilChunkStorageInvoker chunkMap, long position,
                                                  Consumer<LevelChunk> action) {
        ChunkHolder holder = chunkMap.invokeGetVisibleChunkIfPresent(position);
        if (holder == null) return;
        var result = holder.getEntityTickingChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK);
        if (!result.isSuccess()) return;
        LevelChunk chunk = result.orElse(null);
        if (chunk != null) action.accept(chunk);
    }

    private static final class WorldChunks {
        private final TrackedChunkPositions loaded = new TrackedChunkPositions();
        private final TrackedChunkPositions[] melting = new TrackedChunkPositions[MELT_PHASE_COUNT];
        private long lastMeltTick = Long.MIN_VALUE;

        private WorldChunks() {
            for (int i = 0; i < melting.length; i++) {
                melting[i] = new TrackedChunkPositions();
            }
        }

        private void setLoaded(long position, boolean isLoaded) {
            loaded.setLoaded(position, isLoaded);
            int phase = (int) Math.floorMod(HashCommon.mix(position), (long) MELT_PHASE_COUNT);
            melting[phase].setLoaded(position, isLoaded);
        }
    }
}
