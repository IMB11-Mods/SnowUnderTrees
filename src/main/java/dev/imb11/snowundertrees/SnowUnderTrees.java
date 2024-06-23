package dev.imb11.snowundertrees;

import dev.imb11.snowundertrees.config.SnowUnderTreesConfig;
import dev.imb11.snowundertrees.world.SnowUnderTreesWorldgen;
import dev.imb11.snowundertrees.world.WorldTickHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.util.Identifier;

public class SnowUnderTrees implements ModInitializer {
	public static final String MOD_ID = "snowundertrees";

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		SnowUnderTreesConfig.load();
		SnowUnderTreesWorldgen.initialize();

		ServerTickEvents.START_WORLD_TICK.register(new WorldTickHandler());
	}
}
