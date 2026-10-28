package io.ticticboom.mods.mm.compat.ae2;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.replication.matter.MatterTypes;
import io.ticticboom.mods.mm.port.replication.matter.ReplicationMatterPortIngredient;
import io.ticticboom.mods.mm.port.replication.matter.ReplicationMatterPortStorage;

final class MatterBridge implements Ae2KeyBridge {

    @Override
    public String type() {
        return "matter";
    }

    @Override
    public GenericStack input(IPortIngredient ingredient, boolean perTick, int ticks) {
        if (!(ingredient instanceof ReplicationMatterPortIngredient matter)) {
            return null;
        }
        var type = MatterTypes.get(matter.getMatterId());
        var key = type == null ? null : MatterKeys.of(type);
        long amount = (long) matter.getAmountRange().max() * (perTick ? Math.max(1, ticks) : 1);
        return key == null || amount <= 0 ? null : new GenericStack(key, amount);
    }

    @Override
    public GenericStack output(IPortIngredient ingredient) {
        return null;
    }

    @Override
    public boolean handles(AEKey key) {
        return MatterKeys.typeOf(key) != null;
    }

    @Override
    public boolean insert(Ae2PushContext context, AEKey key, long amount) {
        var type = MatterKeys.typeOf(key);
        long remaining = amount;
        for (ReplicationMatterPortStorage storage : context.storages().getInputStorages(ReplicationMatterPortStorage.class)) {
            remaining -= storage.internalInsert(type, (int) Math.min(remaining, Integer.MAX_VALUE), context.simulate());
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }
}
