
package dev.imb11.snowundertrees.compat;

import dev.imb11.snowundertrees.config.SnowUnderTreesConfig;
import dev.imb11.snowundertrees.mixins.ThreadedAnvilChunkStorageInvoker;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
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

    public static boolean isBiomeSuitable(ServerLevel world, BlockPos biomeCheckPos) {
        return SeasonHooks.coldEnoughToSnowSeasonal(world, biomeCheckPos
            //? if >=1.21.2 {
                , world.getSeaLevel()
            //?}
        );
    }

    public static boolean isWarmEnoughToRainSeasonal(ServerLevel world, BlockPos pos) {
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

    public static void attemptMeltSnow(ServerLevel serverWorld) {
        if (isWinter(serverWorld) && !SnowUnderTreesConfig.get().meltSnowSeasonally) return;
        if (!shouldMeltSnow(serverWorld, SeasonHelper.getSeasonState(serverWorld).getSubSeason())) return;

        ThreadedAnvilChunkStorageInvoker chunkStorage = (ThreadedAnvilChunkStorageInvoker) serverWorld.getChunkSource().chunkMap;

        for (ChunkHolder chunkHolder : chunkStorage.invokeEntryIterator(
                //? if >1.21.8
                ChunkStatus.EMPTY).toList(
                )) {
            var optionalChunk = chunkHolder.getEntityTickingChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK);

            if (!optionalChunk.isSuccess()) continue;
            LevelChunk chunk = optionalChunk.orElseThrow(() -> new IllegalStateException("Chunk is not present"));

            if (!serverWorld.shouldTickBlocksAt(chunk.getPos().toLong())) continue;

            BlockPos randomPosition = serverWorld.getBlockRandomPos(chunk.getPos().getMinBlockX(), 0, chunk.getPos().getMinBlockZ(), 15);
            BlockPos heightmapPosition = serverWorld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, randomPosition).below();
            BlockState blockState = serverWorld.getBlockState(heightmapPosition);
            if (!blockState.is(BlockTags.LEAVES)) {
                continue;
            }

            BlockPos pos = serverWorld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, randomPosition);
            if (!isWarmEnoughToRainSeasonal(serverWorld, pos)) {
                continue;
            }

            BlockState before = serverWorld.getBlockState(pos);
            if (!before.is(Blocks.SNOW)) {
                continue;
            }

            serverWorld.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());

            BlockPos downPos = pos.below();
            BlockState below = serverWorld.getBlockState(downPos);

            if (below.hasProperty(SnowyDirtBlock.SNOWY)) {
                serverWorld.setBlock(downPos, below.setValue(SnowyDirtBlock.SNOWY, false), 2);
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

    private static boolean shouldMeltSnow(ServerLevel world, Season.SubSeason subSeason) {
        int chance = MELT_CHANCES.getOrDefault(subSeason, -1);
        if (chance == -1) return false;
        var rnd = world.random.nextInt(0, chance);
        return rnd == 0;
    }

    public static boolean isWinter(Level world) {
        return SeasonHelper.getSeasonState(world).getSeason() == Season.WINTER;
    }

    public static boolean shouldPlaceSnow(Level world, BlockPos pos) {
        if (isSereneSeasonsLoaded) {
            return ModConfig.seasons.generateSnowAndIce && isBiomeSuitable((ServerLevel) world, pos);
        } else {
            return false;
        }
    }
}