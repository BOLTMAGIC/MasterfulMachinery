package io.ticticboom.mods.mm.compat.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import com.wintercogs.appliedpneumatics.common.me.keys.AirKey;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.pneumaticcraft.air.PneumaticAirPortIngredient;
import io.ticticboom.mods.mm.port.pneumaticcraft.air.PneumaticAirPortStorage;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.recipe.input.consume.ConsumeRecipeIngredientEntry;

import java.util.List;

final class AirBridge implements Ae2KeyBridge {

    @Override
    public String type() {
        return "air";
    }

    @Override
    public GenericStack input(IPortIngredient ingredient, boolean perTick, int ticks) {
        if (!(ingredient instanceof PneumaticAirPortIngredient air) || air.getAir() <= 0) {
            return null;
        }
        return new GenericStack(AirKey.INSTANCE, air.getAir());
    }

    @Override
    public boolean handles(AEKey key) {
        return key instanceof AirKey;
    }

    @Override
    public boolean insert(Ae2PushContext context, AEKey key, long amount) {
        List<PneumaticAirPortStorage> ports = context.storages().getInputStorages(PneumaticAirPortStorage.class);
        if (ports.isEmpty()) {
            return false;
        }
        int count = ports.size();
        long[] delivered = new long[count];
        long remaining = amount;
        for (int i = 0; i < count && remaining > 0; i++) {
            long take = Math.min(remaining, room(ports.get(i)));
            delivered[i] = take;
            remaining -= take;
        }
        if (remaining > 0) {
            return false;
        }
        float bar = requiredBar(context.recipe());
        long[] topUp = new long[count];
        long deficit = 0;
        for (int i = 0; i < count; i++) {
            var port = ports.get(i);
            long target = (long) Math.ceil(bar * port.getVolume());
            long missing = Math.max(0, target - (port.getAir() + delivered[i]));
            if (missing > room(port) - delivered[i]) {
                return false;
            }
            topUp[i] = missing;
            deficit += missing;
        }
        if (deficit > 0) {
            var me = context.me();
            if (me == null || me.extract(AirKey.INSTANCE, deficit, Actionable.SIMULATE, context.source()) < deficit) {
                return false;
            }
            if (!context.simulate()) {
                me.extract(AirKey.INSTANCE, deficit, Actionable.MODULATE, context.source());
            }
        }
        if (!context.simulate()) {
            for (int i = 0; i < count; i++) {
                long add = delivered[i] + topUp[i];
                if (add > 0) {
                    ports.get(i).addAir((int) add);
                }
            }
        }
        return true;
    }

    @Override
    public void pushOutputs(RecipeStorages storages, MEStorage me, IActionSource source) {
        for (PneumaticAirPortStorage storage : storages.getOutputStorages(PneumaticAirPortStorage.class)) {
            int air = storage.getAir();
            if (air <= 0) {
                continue;
            }
            long inserted = me.insert(AirKey.INSTANCE, air, Actionable.MODULATE, source);
            if (inserted > 0) {
                storage.addAir(-(int) inserted);
            }
        }
    }

    private static long room(PneumaticAirPortStorage port) {
        long safe = (long) Math.floor(port.model.tier().getDangerPressure() * port.getVolume());
        return Math.max(0, safe - port.getAir());
    }

    private static float requiredBar(RecipeModel recipe) {
        float bar = 0;
        for (var input : recipe.inputs().inputs()) {
            if (input instanceof ConsumeRecipeIngredientEntry entry && entry.getIngredient() instanceof PneumaticAirPortIngredient air) {
                bar = Math.max(bar, air.getBar());
            }
        }
        return bar;
    }
}
