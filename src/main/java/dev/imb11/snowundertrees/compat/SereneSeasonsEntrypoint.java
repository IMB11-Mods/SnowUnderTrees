
package dev.imb11.snowundertrees.compat;

import dev.imb11.snowundertrees.config.SnowUnderTreesConfig;
import dev.imb11.snowundertrees.mixins.ThreadedAnvilChunkStorageInvoker;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowyBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.init.ModConfig;
import sereneseasons.season.SeasonHooks;

import java.util.HashMap;

public class SereneSeasonsEntrypoint {
    private static final Logger LOGGER = LoggerFactory.getLogger("SnowUnderTrees/SereneSeasons");
    public static boolean isSereneSeasonsLoaded = false;

    public static boolean isBiomeSuitable(ServerWorld world, BlockPos biomeCheckPos) {
        return SeasonHooks.coldEnoughToSnowSeasonal(world, biomeCheckPos
            //? if >=1.21.2 {
                , world.getSeaLevel()
            //?}
        );
    }

    public static boolean isWarmEnoughToRainSeasonal(ServerWorld world, BlockPos pos) {
        return SeasonHooks.warmEnoughToRainSeasonal(world, pos
                //? if >=1.21.2 {
                , world.getSeaLevel()
                //?}
        );
    }

    public static void initialize() {
        if (!FabricLoader.getInstance().isModLoaded("sereneseasons")) return;

        LOGGER.info("Serene Seasons detected!");

        isSereneSeasonsLoaded = true;
    }

    public static void attemptMeltSnow(ServerWorld serverWorld) {
        if (isWinter(serverWorld) && !SnowUnderTreesConfig.get().meltSnowSeasonally) return;
        if (!shouldMeltSnow(serverWorld, SeasonHelper.getSeasonState(serverWorld).getSubSeason())) return;

        ThreadedAnvilChunkStorageInvoker chunkStorage = (ThreadedAnvilChunkStorageInvoker) serverWorld.getChunkManager().chunkLoadingManager;

        for (ChunkHolder chunkHolder : chunkStorage.invokeEntryIterator(ChunkStatus.EMPTY).toList()) {
            var optionalChunk = chunkHolder.getTickingFuture().getNow(ChunkHolder.UNLOADED_WORLD_CHUNK);

            if (!optionalChunk.isPresent()) continue;
            WorldChunk chunk = optionalChunk.orElseThrow(() -> new IllegalStateException("Chunk is not present"));

            if (!serverWorld.shouldTickBlocksInChunk(chunk.getPos().toLong())) continue;

            BlockPos randomPosition = serverWorld.getRandomPosInChunk(chunk.getPos().getStartX(), 0, chunk.getPos().getStartZ(), 15);
            BlockPos heightmapPosition = serverWorld.getTopPosition(Heightmap.Type.MOTION_BLOCKING, randomPosition).down();
            BlockState blockState = serverWorld.getBlockState(heightmapPosition);
            if (!blockState.isIn(BlockTags.LEAVES)) {
                continue;
            }

            BlockPos pos = serverWorld.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, randomPosition);
            if (!isWarmEnoughToRainSeasonal(serverWorld, pos)) {
                continue;
            }

            BlockState before = serverWorld.getBlockState(pos);
            if (!before.isOf(Blocks.SNOW)) {
                continue;
            }

            serverWorld.setBlockState(pos, Blocks.AIR.getDefaultState());

            BlockPos downPos = pos.down();
            BlockState below = serverWorld.getBlockState(downPos);

            if (below.contains(SnowyBlock.SNOWY)) {
                serverWorld.setBlockState(downPos, below.with(SnowyBlock.SNOWY, false), 2);
            }
        }
    }

    private static final HashMap<Object, Integer> MELT_CHANCES = new HashMap<>();

    static {
        if (FabricLoader.getInstance().isModLoaded("sereneseasons")) {
            MELT_CHANCES.put(Season.SubSeason.EARLY_SPRING, 16);
            MELT_CHANCES.put(Season.SubSeason.MID_SPRING, 12);
            MELT_CHANCES.put(Season.SubSeason.LATE_SPRING, 8);
            MELT_CHANCES.put(Season.SubSeason.EARLY_SUMMER, 4);
            MELT_CHANCES.put(Season.SubSeason.MID_SUMMER, 2);
            MELT_CHANCES.put(Season.SubSeason.LATE_SUMMER, 1);
            MELT_CHANCES.put(Season.SubSeason.EARLY_AUTUMN, 8);
            MELT_CHANCES.put(Season.SubSeason.MID_AUTUMN, 12);
            MELT_CHANCES.put(Season.SubSeason.LATE_AUTUMN, 16);
        }
    }

    private static boolean shouldMeltSnow(ServerWorld world, Season.SubSeason subSeason) {
        int chance = MELT_CHANCES.getOrDefault(subSeason, -1);
        if (chance == -1) return false;
        var rnd = world.random.nextBetween(0, chance);
        return rnd == 0;
    }

    public static boolean isWinter(World world) {
        return SeasonHelper.getSeasonState(world).getSeason() == Season.WINTER;
    }

    public static boolean shouldPlaceSnow(World world, BlockPos pos) {
        if (isSereneSeasonsLoaded) {
            return ModConfig.seasons.generateSnowAndIce && isBiomeSuitable((ServerWorld) world, pos);
        } else {
            return false;
        }
    }
}