package dev.imb11.snowundertrees.world;

import dev.imb11.snowundertrees.mixins.ThreadedAnvilChunkStorageInvoker;
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
    private static final Map<ServerLevel, TrackedChunkPositions> WORLDS = new IdentityHashMap<>();

    private LoadedChunkTracker() {
    }

    public static void initialize() {
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk, newlyGenerated) ->
                WORLDS.computeIfAbsent(world, ignored -> new TrackedChunkPositions())
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
        positions.forEach(position -> {
            ChunkHolder holder = chunkMap.invokeGetVisibleChunkIfPresent(position);
            if (holder == null) return;
            var result = holder.getEntityTickingChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK);
            if (!result.isSuccess()) return;
            LevelChunk chunk = result.orElse(null);
            if (chunk != null) action.accept(chunk);
        });
    }
}
