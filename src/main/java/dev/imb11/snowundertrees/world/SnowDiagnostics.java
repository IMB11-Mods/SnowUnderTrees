package dev.imb11.snowundertrees.world;

import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

final class SnowDiagnostics {
    static final boolean ENABLED = Boolean.getBoolean("snowundertrees.profile");
    private static final Logger LOGGER = LoggerFactory.getLogger("SnowUnderTrees/Diagnostics");
    private static final int REPORT_TICKS = 600;
    private static final int SAMPLE_INTERVAL = 256;

    enum Counter {
        SNOW_VISITS, SNOW_READINESS_CHECKS, SNOW_READY, MELT_VISITS, MELT_READY, RANDOM_REJECTED, NO_CANOPY,
        OUTSIDE_HEIGHT, OCCUPIED, UNSUPPORTED_BIOME, VANILLA_REJECTED, SEASON_REJECTED,
        PLACEMENT_ATTEMPTS, SNOW_PLACED
    }

    enum Stage {
        SCHEDULING, CHUNK_READINESS, RANDOM_CHECK, CANOPY_SEARCH, GROUND_HEIGHT,
        PLACEMENT_CHECKS, SNOW_PLACEMENT, MELTING
    }

    private final long[] counters = new long[Counter.values().length];
    private final long[] stageNanos = new long[Stage.values().length];
    private final long[] stageSamples = new long[Stage.values().length];
    private Instant windowStart;
    private int ticks;
    private int activeTicks;
    private long handlerNanos;
    private long activeNanos;
    private long maxHandlerNanos;
    private int visitsUntilSample;
    private boolean sampleVisit;

    long beginTick() {
        if (windowStart == null) windowStart = Instant.now();
        visitsUntilSample = ThreadLocalRandom.current().nextInt(SAMPLE_INTERVAL);
        sampleVisit = false;
        return System.nanoTime();
    }

    long beginVisit() {
        sampleVisit = visitsUntilSample-- == 0;
        if (sampleVisit) visitsUntilSample = SAMPLE_INTERVAL - 1;
        return startStage();
    }

    long startStage() {
        return sampleVisit ? System.nanoTime() : 0;
    }

    void endStage(Stage stage, long started) {
        if (started == 0) return;
        long elapsed = System.nanoTime() - started;
        stageNanos[stage.ordinal()] += elapsed;
        stageSamples[stage.ordinal()]++;
    }

    void count(Counter counter) {
        counters[counter.ordinal()]++;
    }

    void endTick(ServerLevel world, long started, boolean active) {
        long elapsed = System.nanoTime() - started;
        handlerNanos += elapsed;
        maxHandlerNanos = Math.max(maxHandlerNanos, elapsed);
        ticks++;
        if (active) {
            activeTicks++;
            activeNanos += elapsed;
        }
        if (ticks == REPORT_TICKS) report(world);
    }

    void report(ServerLevel world) {
        if (ticks == 0) return;
        if (activeTicks > 0) {
            StringBuilder counts = new StringBuilder();
            for (Counter counter : Counter.values()) {
                if (!counts.isEmpty()) counts.append(' ');
                counts.append(counter.name().toLowerCase(Locale.ROOT)).append('=')
                        .append(counters[counter.ordinal()]);
            }
            counts.append(" snow_unavailable=")
                    .append(counters[Counter.SNOW_READINESS_CHECKS.ordinal()] - counters[Counter.SNOW_READY.ordinal()]);
            counts.append(" melt_unavailable=")
                    .append(counters[Counter.MELT_VISITS.ordinal()] - counters[Counter.MELT_READY.ordinal()]);

            StringBuilder timings = new StringBuilder();
            for (Stage stage : Stage.values()) {
                if (!timings.isEmpty()) timings.append(' ');
                int index = stage.ordinal();
                timings.append(stage.name().toLowerCase(Locale.ROOT)).append("{samples=")
                        .append(stageSamples[index]).append(",total_ns=").append(stageNanos[index])
                        .append(",mean_ns=").append(stageSamples[index] == 0 ? "n/a"
                                : format((double) stageNanos[index] / stageSamples[index])).append('}');
            }
            LOGGER.info("window_start={} window_end={} dimension={} ticks={} active_ticks={} "
                            + "handler_mean_ms={} handler_active_mean_ms={} handler_max_ms={} "
                            + "stage_sample_every={} counts=[{}] sampled_stages=[{}]",
                    windowStart, Instant.now(), world.dimension().identifier(), ticks, activeTicks,
                    format(handlerNanos / (ticks * 1_000_000.0)),
                    format(activeNanos / (activeTicks * 1_000_000.0)),
                    format(maxHandlerNanos / 1_000_000.0), SAMPLE_INTERVAL, counts, timings);
        }
        Arrays.fill(counters, 0);
        Arrays.fill(stageNanos, 0);
        Arrays.fill(stageSamples, 0);
        windowStart = null;
        ticks = 0;
        activeTicks = 0;
        handlerNanos = 0;
        activeNanos = 0;
        maxHandlerNanos = 0;
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
