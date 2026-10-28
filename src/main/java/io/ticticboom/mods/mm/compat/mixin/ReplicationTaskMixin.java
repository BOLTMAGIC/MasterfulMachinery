package io.ticticboom.mods.mm.compat.mixin;

import com.buuz135.replication.api.task.ReplicationTask;
import com.buuz135.replication.calculation.ReplicationCalculation;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.buuz135.replication.api.task.ReplicationTask")
public abstract class ReplicationTaskMixin {

    @Inject(method = "canAcceptReplicator", at = @At("HEAD"), cancellable = true)
    private void mm$skipItemsWithoutMatter(BlockPos pos, int max, CallbackInfoReturnable<Boolean> cir) {
        var task = (ReplicationTask) (Object) this;
        if (ReplicationCalculation.getMatterCompound(task.getReplicatingStack()) == null) {
            cir.setReturnValue(false);
        }
    }
}
