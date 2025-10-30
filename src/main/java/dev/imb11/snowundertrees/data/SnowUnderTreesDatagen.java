//? >1.21.2 {
package dev.imb11.snowundertrees.data;

import dev.imb11.snowundertrees.SnowUnderTrees;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;

public class SnowUnderTreesDatagen implements DataGeneratorEntrypoint {

    /**
     * This method is called by the Fabric Datagen module to generate data.
     *
     * @param fabricDataGenerator The {@link FabricDataGenerator} instance
     */
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        var pack = fabricDataGenerator.createPack();
        pack.addProvider(WorldgenProvider::new);
    }

    /**
     * This method is called by the Fabric Datagen module to build registries.
     *
     * @param registryBuilder a {@link RegistrySetBuilder} instance
     */
    @Override
    public void buildRegistry(RegistrySetBuilder registryBuilder) {
        // Add all the registries we want to generate data for here.
        registryBuilder.add(Registries.CONFIGURED_FEATURE, SnowUnderTreesBootstrap::configuredFeatures);
        registryBuilder.add(Registries.PLACED_FEATURE, SnowUnderTreesBootstrap::placedFeatures);
    }

    @Override
    public String getEffectiveModId() {
        return SnowUnderTrees.MOD_ID;
    }
}
//?}