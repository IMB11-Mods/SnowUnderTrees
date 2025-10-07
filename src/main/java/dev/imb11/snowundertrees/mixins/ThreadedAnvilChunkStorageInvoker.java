package dev.imb11.snowundertrees.mixins;

import net.minecraft.server.world.ChunkHolder;
import net.minecraft.world.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.stream.Stream;

@Mixin(net.minecraft.server.world.ServerChunkLoadingManager.class)
public interface ThreadedAnvilChunkStorageInvoker {
    //? if >1.21.8 {
    @Invoker("getChunkHolders")
    Stream<ChunkHolder> invokeEntryIterator(ChunkStatus status);
    //?} else {
    /*@Invoker("entryIterator")
    Iterable<ChunkHolder> invokeEntryIterator();
    *///?}
}
