package io.ticticboom.mods.mm.compat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.ticticboom.mods.mm.compat.oritech.BlackHoleShield;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "rearth.oritech.block.entity.accelerator.BlackHoleBlockEntity")
public abstract class BlackHoleBlockEntityMixin {

    @Shadow
    private int waitTicks;

    @Inject(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;withinManhattan(Lnet/minecraft/core/BlockPos;III)Ljava/lang/Iterable;"), cancellable = true)
    private void mm$stayContained(CallbackInfo ci, @Local(argsOnly = true) Level level, @Local(argsOnly = true, ordinal = 0) BlockPos pos) {
        BlackHoleShield.validateNearby(level, pos);
        if (BlackHoleShield.isShielded(level, pos)) {
            waitTicks = BlackHoleShield.CONTAINED_WAIT_TICKS;
            ci.cancel();
        }
    }

    @WrapOperation(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean mm$skipFormedStructures(BlockState state, TagKey<Block> tag, Operation<Boolean> original,
                                            @Local(argsOnly = true) Level level, @Local(ordinal = 1) BlockPos candidate) {
        return original.call(state, tag) || BlackHoleShield.isShielded(level, candidate);
    }
}
