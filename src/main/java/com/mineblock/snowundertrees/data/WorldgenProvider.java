package com.mineblock.snowundertrees.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

class WorldgenProvider extends FabricDynamicRegistryProvider {
    WorldgenProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    /**
     * This method is called by the Fabric Datagen module to generate data.
     * @param registries a {@link RegistryWrapper.WrapperLookup} instance
     * @param entries a {@link net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider.Entries} instance
     */
    @Override
    protected void configure(RegistryWrapper.WrapperLookup registries, Entries entries) {

        // Add all the registries we want to generate data for here.
        entries.addAll(registries.getWrapperOrThrow(RegistryKeys.CONFIGURED_FEATURE));
        entries.addAll(registries.getWrapperOrThrow(RegistryKeys.PLACED_FEATURE));
        entries.addAll(registries.getWrapperOrThrow(RegistryKeys.BIOME));
        entries.addAll(registries.getWrapperOrThrow(RegistryKeys.STRUCTURE));
        entries.addAll(registries.getWrapperOrThrow(RegistryKeys.STRUCTURE_SET));
        entries.addAll(registries.getWrapperOrThrow(RegistryKeys.TEMPLATE_POOL));
    }

    @Override
    public String getName() {
        return "Snow Under Trees World Generation";
    }
}