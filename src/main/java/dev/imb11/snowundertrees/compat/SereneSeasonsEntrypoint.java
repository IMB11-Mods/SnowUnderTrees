
package dev.imb11.snowundertrees.compat;

import dev.imb11.snowundertrees.config.SnowUnderTreesConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sereneseasons.api.season.Season;
import sereneseasons.api.season.SeasonHelper;
import sereneseasons.init.ModConfig;
import sereneseasons.season.SeasonHooks;


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

    public static int getMeltingInterval(ServerLevel serverWorld) {
        if (!SnowUnderTreesConfig.get().meltSnowSeasonally
                || !isSeasonIntegrationEnabled(serverWorld)
                || !ModConfig.seasons.generateSnowAndIce) return 0;
        return meltingInterval(SeasonHelper.getSeasonState(serverWorld).getSubSeason());
    }

    public static void meltSnowInChunk(ServerLevel serverWorld, LevelChunk chunk) {
        if (!serverWorld.shouldTickBlocksAt(chunk.getPos().getWorldPosition())) return;

        BlockPos randomPosition = serverWorld.getBlockRandomPos(chunk.getPos().getMinBlockX(), 0, chunk.getPos().getMinBlockZ(), 15);
        BlockPos heightmapPosition = serverWorld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, randomPosition).below();
        BlockState blockState = serverWorld.getBlockState(heightmapPosition);
        if (!blockState.is(BlockTags.LEAVES)) {
            return;
        }

        BlockPos pos = serverWorld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, randomPosition);
        BlockState before = serverWorld.getBlockState(pos);
        if (!before.is(Blocks.SNOW)) {
            return;
        }

        if (!isWarmEnoughToRainSeasonal(serverWorld, pos)) {
            return;
        }

        serverWorld.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());

        BlockPos downPos = pos.below();
        BlockState below = serverWorld.getBlockState(downPos);

        if (below.hasProperty(SnowyBlock.SNOWY)) {
            serverWorld.setBlock(downPos, below.setValue(SnowyBlock.SNOWY, false), 2);
        }
    }

    private static int meltingInterval(Season.SubSeason subSeason) {
        return switch (subSeason) {
            case EARLY_SPRING, LATE_AUTUMN -> 16;
            case MID_SPRING, MID_AUTUMN -> 12;
            case LATE_SPRING, EARLY_AUTUMN -> 8;
            case EARLY_SUMMER -> 4;
            case MID_SUMMER -> 2;
            case LATE_SUMMER -> 1;
            default -> 0;
        };
    }

    public static boolean isWinter(Level world) {
        return SeasonHelper.getSeasonState(world).getSeason() == Season.WINTER;
    }

    public static boolean isSeasonIntegrationEnabled(Level world) {
        return isSereneSeasonsLoaded && SnowUnderTreesConfig.get().respectSeasonMods
                && ModConfig.seasons.isDimensionWhitelisted(world.dimension());
    }

    public static boolean shouldPlaceSnow(Level world, BlockPos pos) {
        if (isSeasonIntegrationEnabled(world)) {
            return ModConfig.seasons.generateSnowAndIce && isBiomeSuitable((ServerLevel) world, pos);
        } else {
            return false;
        }
    }
}
