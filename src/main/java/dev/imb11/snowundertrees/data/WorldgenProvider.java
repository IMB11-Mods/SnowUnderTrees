//? >1.21.2 {
package dev.imb11.snowundertrees.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;

import java.util.concurrent.CompletableFuture;

class WorldgenProvider extends FabricDynamicRegistryProvider {
    WorldgenProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    /**
     * This method is called by the Fabric Datagen module to generate data.
     *
     * @param registries a {@link HolderLookup.Provider} instance
     * @param entries    a {@link Entries} instance
     */
    @Override
    protected void configure(HolderLookup.Provider registries, Entries entries) {

        // Add all the registries we want to generate data for here.
        entries.addAll(registries.lookupOrThrow(Registries.CONFIGURED_FEATURE));
        entries.addAll(registries.lookupOrThrow(Registries.PLACED_FEATURE));
        entries.addAll(registries.lookupOrThrow(Registries.BIOME));
        entries.addAll(registries.lookupOrThrow(Registries.STRUCTURE));
        entries.addAll(registries.lookupOrThrow(Registries.STRUCTURE_SET));
        entries.addAll(registries.lookupOrThrow(Registries.TEMPLATE_POOL));
    }

    @Override
    public String getName() {
        return "Snow Under Trees World Generation";
    }
}
//?}