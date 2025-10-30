package dev.imb11.snowundertrees.mixins;


import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.stream.Stream;

@Mixin(ChunkMap.class)
public interface ThreadedAnvilChunkStorageInvoker {
    //? if >1.21.8 {
    @Invoker("allChunksWithAtLeastStatus")
    Stream<ChunkHolder> invokeEntryIterator(ChunkStatus status);
    //?} else {
    /*@Invoker("getChunks")
    Iterable<ChunkHolder> invokeEntryIterator();
    *///?}
}
