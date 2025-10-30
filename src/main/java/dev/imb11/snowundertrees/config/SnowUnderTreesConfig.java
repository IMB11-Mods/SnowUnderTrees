package dev.imb11.snowundertrees.config;

import com.google.gson.GsonBuilder;
import dev.imb11.mru.yacl.ConfigHelper;
import dev.imb11.mru.yacl.EntryType;
import dev.imb11.snowundertrees.SnowUnderTrees;
import dev.imb11.snowundertrees.compat.SereneSeasonsEntrypoint;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SnowUnderTreesConfig {
    private static final ConfigHelper CONFIG_HELPER = new ConfigHelper("snowundertrees", "config");
    public static ConfigClassHandler<SnowUnderTreesConfig> CONFIG_CLASS_HANDLER = ConfigClassHandler
            .createBuilder(SnowUnderTreesConfig.class)
            .id(SnowUnderTrees.id("config"))
            .serializer(config -> GsonConfigSerializerBuilder
                    .create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("snowundertrees.config.json"))
                    .appendGsonBuilder(GsonBuilder::setPrettyPrinting).build())
            .build();

    @SerialEntry
    public boolean enableBiomeFeature = true;

    @SerialEntry
    public boolean enableWhenSnowing = true;

    @SerialEntry
    public List<String> supportedBiomes = List.of(
            "minecraft:snowy_plains",
            "minecraft:ice_spikes",
            "minecraft:snowy_taiga",
            "minecraft:snowy_beach",
            "minecraft:snowy_slopes",
            "minecraft:jagged_peaks",
            "minecraft:frozen_peaks",
            "biomesoplenty:auroral_garden",
            "biomesoplenty:muskeg",
            "biomesoplenty:snowy_fir_clearing",
            "biomesoplenty:snowy_coniferous_forest",
            "biomesoplenty:fir_clearing",
            "biomesoplenty:coniferous_forest",
            "terralith:alpha_islands_winter",
            "terralith:alpine_grove",
            "terralith:emerald_peaks",
            "terralith:frozen_cliffs",
            "terralith:glacial_chasm",
            "terralith:rocky_shrubland",
            "terralith:scarlet_mountains",
            "terralith:skylands_winter",
            "terralith:snowy_badlands",
            "terralith:snowy_cherry_grove",
            "terralith:snowy_maple_forest",
            "terralith:snowy_shield",
            "terralith:wintry_forest",
            "terralith:wintry_lowlands",
            "byg:cardinal_tundra",
            "byg:shattered_glacier",
            "byg:frosted_taiga",
            "byg:frosted_coniferous_forest",
            "traverse:snowy_coniferous_forest",
            "traverse:snowy_coniferous_wooded_hills",
            "traverse:snowy_high_coniferous_forest",
            "woods_and_mires:snowy_fell",
            "woods_and_mires:snowy_pine_forest",
            "regions_unexplored:cold_deciduous_forest",
            "regions_unexplored:cold_boreal_taiga",
            "regions_unexplored:icy_heights",
            "regions_unexplored:frozen_pine_taiga",
            "regions_unexplored:frozen_tundra",
            "regions_unexplored:spires",
            "terrestria:snowy_hemlock_forest",
            "terrestria:snowy_hemlock_clearing",
            "terrestria:caldera",
            "terrestria:caldera_foothils",
            "wilderwild:frozen_caves",
            "wilderwild:snowy_dying_forest",
            "wilderwild:snowy_dying_mixed_forest",
            "wilderwild:snowy_old_growth_pine_taiga",
            "promenade:glacarian_taiga",
            "wythers:snowy_bog",
            "wythers:snowy_canyon",
            "wythers:snowy_fen",
            "wythers:snowy_peaks",
            "wythers:snowy_thermal_taiga",
            "wythers:snowy_tundra",
            "wythers:crimson_tundra",
            "wythers:deep_snowy_taiga",
            "wythers:highlands",
            "wythers:icy_crags",
            "wythers:icy_shore",
            "wythers:icy_volcano",
            "wythers:ice_cap",
            "sensible:snowy_woodland",
            "cynic:snowy_dark_forest",
            "cynic:snowy_forest",
            "cynic:snowy_forest_strand",
            "cynic:snowy_stony_shore",
            "cynic:snowy_strand",
            "snowbound:boreal_forest"
    );

    @SerialEntry
    public boolean respectSeasonMods = true;
    @SerialEntry
    public boolean meltSnowSeasonally = true;

    public static SnowUnderTreesConfig get() {
        return CONFIG_CLASS_HANDLER.instance();
    }

    public static void load() {
        // Check if "snowundertrees.json5" exists, and rename it to "snowundertrees.config.json"
        Path oldConfigPath = FabricLoader.getInstance().getConfigDir().resolve("snowundertrees.json5");
        Path newConfigPath = FabricLoader.getInstance().getConfigDir().resolve("snowundertrees.config.json");

        if (oldConfigPath.toFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            oldConfigPath.toFile().renameTo(newConfigPath.toFile());
        }

        CONFIG_CLASS_HANDLER.load();
    }

    public static YetAnotherConfigLib getInstance() {
        return YetAnotherConfigLib.create(CONFIG_CLASS_HANDLER, (SnowUnderTreesConfig defaults, SnowUnderTreesConfig config, YetAnotherConfigLib.Builder builder) -> {
            Option<Boolean> enableWhenSnowingOption = CONFIG_HELPER.get("enableWhenSnowing", defaults.enableWhenSnowing, () -> config.enableWhenSnowing, (v) -> config.enableWhenSnowing = v, true);
            Option<Boolean> respectSeasonModsOption = CONFIG_HELPER.get("respectSeasonMods", defaults.respectSeasonMods, () -> config.respectSeasonMods, (v) -> config.respectSeasonMods = v);
            Option<Boolean> meltSnowSeasonallyOption = CONFIG_HELPER.get("meltSnowSeasonally", defaults.meltSnowSeasonally, () -> config.meltSnowSeasonally, (v) -> config.meltSnowSeasonally = v);

            var supportedBiomesOption = ListOption.<String>createBuilder()
                    .name(CONFIG_HELPER.getText(EntryType.OPTION_NAME, "supportedBiomes"))
                    .description(CONFIG_HELPER.get("supportedBiomes", false))
                    .controller(StringControllerBuilder::create)
                    .initial("minecraft:plains")
                    .binding(defaults.supportedBiomes, () -> config.supportedBiomes, (v) -> config.supportedBiomes = v)
                    .build();

            var enableBiomeFeatureOption = Option.<Boolean>createBuilder()
                    .name(CONFIG_HELPER.getText(EntryType.OPTION_NAME, "enableBiomeFeature"))
                    .description(CONFIG_HELPER.get("enableBiomeFeature", true))
                    .binding(defaults.enableBiomeFeature, () -> config.enableBiomeFeature, (v) -> config.enableBiomeFeature = v)
                    .listener((opt, val) -> {
                        enableWhenSnowingOption.setAvailable(val);
                        supportedBiomesOption.setAvailable(val);
                        respectSeasonModsOption.setAvailable(val);
                        meltSnowSeasonallyOption.setAvailable(val);
                    })
                    .controller(opt -> BooleanControllerBuilder.create(opt).yesNoFormatter().coloured(true))
                    .build();

            var options = new ArrayList<Option<?>>(List.of(
                    enableBiomeFeatureOption,
                    enableWhenSnowingOption
            ));

            if (SereneSeasonsEntrypoint.isSereneSeasonsLoaded) {
                options.add(respectSeasonModsOption);
                options.add(meltSnowSeasonallyOption);
            }

            return builder
                    .title(Component.translatable("snowundertrees.config.title"))
                    .category(ConfigCategory.createBuilder()
                            .name(Component.translatable("snowundertrees.config.title"))
                            .options(options)
                            .group(supportedBiomesOption)
                            .build());
        });

    }
}
