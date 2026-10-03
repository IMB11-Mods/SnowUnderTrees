package dev.imb11.snowundertrees.mixins;

import dev.imb11.snowundertrees.world.WorldTickHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Unique
    private final WorldTickHandler snowundertrees$handler = new WorldTickHandler();

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At("HEAD"))
    private void snowundertrees$startTick(BooleanSupplier haveTime, CallbackInfo ci) {
        snowundertrees$handler.onStartTick((ServerLevel) (Object) this);
    }

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At("RETURN"))
    private void snowundertrees$endTick(BooleanSupplier haveTime, CallbackInfo ci) {
        snowundertrees$handler.onEndTick((ServerLevel) (Object) this);
    }

    @Inject(method = "tickChunk(Lnet/minecraft/world/level/chunk/LevelChunk;I)V", at = @At("HEAD"))
    private void snowundertrees$tickChunk(LevelChunk chunk, int tickSpeed, CallbackInfo ci) {
        snowundertrees$handler.onChunkTick((ServerLevel) (Object) this, chunk);
    }
}
