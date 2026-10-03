package dev.imb11.snowundertrees.world;

import dev.imb11.snowundertrees.mixins.ThreadedAnvilChunkStorageInvoker;
import it.unimi.dsi.fastutil.HashCommon;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Arrays;
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
        ServerLevelEvents.UNLOAD.register((server, world) -> {
            var positions = WORLDS.remove(world);
            if (positions != null && positions.diagnostics != null) positions.diagnostics.report(world);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server ->
                WORLDS.entrySet().removeIf(entry -> {
                    if (entry.getKey().getServer() != server) return false;
                    if (entry.getValue().diagnostics != null) entry.getValue().diagnostics.report(entry.getKey());
                    return true;
                }));
    }

    static SnowDiagnostics diagnostics(ServerLevel world) {
        return WORLDS.computeIfAbsent(world, ignored -> new WorldChunks()).diagnostics;
    }

    public static void processTick(ServerLevel world, boolean snowfall, int meltInterval, int limit,
                                   RandomSource snowRandom, Consumer<LevelChunk> snowAction, Consumer<LevelChunk> meltAction) {
        if (meltInterval < 0 || (meltInterval > 0 && MELT_PHASE_COUNT % meltInterval != 0)) {
            throw new IllegalArgumentException("Unsupported melting interval: " + meltInterval);
        }
        var positions = WORLDS.get(world);
        if (positions == null) return;
        SnowDiagnostics diagnostics = SnowDiagnostics.ENABLED ? positions.diagnostics : null;
        long tick = world.getGameTime();
        if (positions.lastTick == tick) return;
        positions.lastTick = tick;

        if (meltInterval != positions.meltInterval) {
            Arrays.fill(positions.pendingMelting, 0);
            positions.meltInterval = meltInterval;
        }
        if (!snowfall && meltInterval == 0) return;
        if (meltInterval > 0) {
            int phase = (int) Math.floorMod(tick, (long) meltInterval);
            for (int bucket = phase; bucket < MELT_PHASE_COUNT; bucket += meltInterval) {
                if (positions.pendingMelting[bucket] == 0) {
                    positions.pendingMelting[bucket] = positions.melting[bucket].size();
                }
            }
        }

        int snowRemaining = snowfall ? positions.loaded.size() : 0;
        long allowance = limit <= 0 ? Long.MAX_VALUE : limit;
        var chunkMap = (ThreadedAnvilChunkStorageInvoker) world.getChunkSource().chunkMap;
        while (allowance > 0) {
            long visitStarted = diagnostics == null ? 0 : diagnostics.beginVisit();
            long started = visitStarted;
            snowRemaining = Math.min(snowRemaining, positions.loaded.size());
            int meltBucket = meltInterval > 0 ? positions.nextMeltingBucket() : -1;
            boolean snowWaiting = snowRemaining > 0;
            boolean meltWaiting = meltBucket >= 0;
            if (!snowWaiting && !meltWaiting) break;

            boolean melt = meltWaiting && (!snowWaiting || positions.meltNext);
            long position;
            if (melt) {
                position = positions.melting[meltBucket].next();
                positions.pendingMelting[meltBucket]--;
                positions.nextMeltBucket = (meltBucket + 1) % MELT_PHASE_COUNT;
            } else {
                position = positions.loaded.next();
                snowRemaining--;
            }
            positions.meltNext = !melt;
            allowance--;
            if (diagnostics != null) {
                diagnostics.endStage(SnowDiagnostics.Stage.SCHEDULING, started);
                diagnostics.count(melt ? SnowDiagnostics.Counter.MELT_VISITS : SnowDiagnostics.Counter.SNOW_VISITS);
            }
            if (!melt) {
                started = diagnostics == null ? 0 : diagnostics.startStage();
                boolean process = snowRandom.nextInt(4) == 0;
                if (diagnostics != null) diagnostics.endStage(SnowDiagnostics.Stage.RANDOM_CHECK, started);
                if (!process) {
                    if (diagnostics != null) {
                        diagnostics.count(SnowDiagnostics.Counter.RANDOM_REJECTED);
                        diagnostics.endStage(SnowDiagnostics.Stage.VISIT, visitStarted);
                    }
                    continue;
                }
                if (diagnostics != null) diagnostics.count(SnowDiagnostics.Counter.SNOW_READINESS_CHECKS);
            }
            started = diagnostics == null ? 0 : diagnostics.startStage();
            LevelChunk chunk = getEntityTickingChunk(chunkMap, position);
            if (diagnostics != null) diagnostics.endStage(SnowDiagnostics.Stage.CHUNK_READINESS, started);
            if (chunk == null) {
                if (diagnostics != null) diagnostics.endStage(SnowDiagnostics.Stage.VISIT, visitStarted);
                continue;
            }
            if (diagnostics != null) {
                diagnostics.count(melt ? SnowDiagnostics.Counter.MELT_READY : SnowDiagnostics.Counter.SNOW_READY);
            }
            if (melt) {
                started = diagnostics == null ? 0 : diagnostics.startStage();
                meltAction.accept(chunk);
                if (diagnostics != null) diagnostics.endStage(SnowDiagnostics.Stage.MELTING, started);
            } else {
                snowAction.accept(chunk);
            }
            if (diagnostics != null) diagnostics.endStage(SnowDiagnostics.Stage.VISIT, visitStarted);
        }
    }

    private static LevelChunk getEntityTickingChunk(ThreadedAnvilChunkStorageInvoker chunkMap, long position) {
        ChunkHolder holder = chunkMap.invokeGetVisibleChunkIfPresent(position);
        if (holder == null) return null;
        var result = holder.getEntityTickingChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK);
        return result.isSuccess() ? result.orElse(null) : null;
    }

    private static final class WorldChunks {
        private final SnowDiagnostics diagnostics = SnowDiagnostics.ENABLED ? new SnowDiagnostics() : null;
        private final TrackedChunkPositions loaded = new TrackedChunkPositions();
        private final TrackedChunkPositions[] melting = new TrackedChunkPositions[MELT_PHASE_COUNT];
        private final int[] pendingMelting = new int[MELT_PHASE_COUNT];
        private long lastTick = Long.MIN_VALUE;
        private int meltInterval;
        private int nextMeltBucket;
        private boolean meltNext = true;

        private WorldChunks() {
            for (int i = 0; i < melting.length; i++) {
                melting[i] = new TrackedChunkPositions();
            }
        }

        private int nextMeltingBucket() {
            for (int offset = 0; offset < MELT_PHASE_COUNT; offset++) {
                int bucket = (nextMeltBucket + offset) % MELT_PHASE_COUNT;
                pendingMelting[bucket] = Math.min(pendingMelting[bucket], melting[bucket].size());
                if (pendingMelting[bucket] > 0) return bucket;
            }
            return -1;
        }

        private void setLoaded(long position, boolean isLoaded) {
            loaded.setLoaded(position, isLoaded);
            int phase = (int) Math.floorMod(HashCommon.mix(position), (long) MELT_PHASE_COUNT);
            melting[phase].setLoaded(position, isLoaded);
        }
    }
}
