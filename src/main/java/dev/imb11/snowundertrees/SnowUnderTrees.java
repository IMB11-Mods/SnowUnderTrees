package dev.imb11.snowundertrees;

import dev.imb11.snowundertrees.compat.SereneSeasonsEntrypoint;
import dev.imb11.snowundertrees.config.SnowUnderTreesConfig;
import dev.imb11.snowundertrees.world.SnowUnderTreesWorldgen;
import dev.imb11.snowundertrees.world.WorldTickHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.ResourceLocation;

public class SnowUnderTrees implements ModInitializer {
	public static final String MOD_ID = "snowundertrees";

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		SnowUnderTreesConfig.load();
		SnowUnderTreesWorldgen.initialize();
		SereneSeasonsEntrypoint.initialize();

		ServerTickEvents.START_WORLD_TICK.register(new WorldTickHandler());
	}
}
