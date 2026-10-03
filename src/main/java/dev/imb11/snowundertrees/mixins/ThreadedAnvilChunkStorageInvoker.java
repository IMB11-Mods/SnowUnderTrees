package dev.imb11.snowundertrees.mixins;


import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;


@Mixin(ChunkMap.class)
public interface ThreadedAnvilChunkStorageInvoker {
    @Invoker("getVisibleChunkIfPresent")
    ChunkHolder invokeGetVisibleChunkIfPresent(long position);
}
