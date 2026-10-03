package dev.imb11.snowundertrees;

import dev.imb11.snowundertrees.compat.SereneSeasonsEntrypoint;
import dev.imb11.snowundertrees.config.SnowUnderTreesConfig;
import dev.imb11.snowundertrees.world.SnowUnderTreesWorldgen;
import dev.imb11.snowundertrees.world.LoadedChunkTracker;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

public class SnowUnderTrees implements ModInitializer {
	public static final String MOD_ID = "snowundertrees";

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		SnowUnderTreesConfig.load();
		SnowUnderTreesWorldgen.initialize();
		SereneSeasonsEntrypoint.initialize();
		LoadedChunkTracker.initialize();
	}
}
