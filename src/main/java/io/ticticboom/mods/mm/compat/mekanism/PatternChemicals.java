package io.ticticboom.mods.mm.compat.mekanism;

import io.ticticboom.mods.mm.cap.MekCapabilities;
import io.ticticboom.mods.mm.pattern.PatternIngredient;
import io.ticticboom.mods.mm.pattern.PatternIngredient.Kind;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorage;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorageModel;
import mekanism.api.Action;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.ChemicalStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * Mekanism chemicals in a Pattern Port buffer: one MM chemical storage per buffer side. Only used with Mekanism
 * installed (a pattern can only carry chemicals with Applied Mekanistics, which needs Mekanism).
 */
public final class PatternChemicals {
    private PatternChemicals() {
    }

    /** An empty chemical storage holding up to {@code capacity} mB. */
    public static IPortStorage storageFor(long capacity) {
        return new MekanismChemicalPortStorage(new MekanismChemicalPortStorageModel(capacity, () -> false), () -> {});
    }

    /** @return false (nothing added) if it doesn't all fit */
    public static boolean insert(IPortStorage storage, PatternIngredient chemical) {
        ChemicalStack stack = stack(chemical);
        if (stack == null || !(storage instanceof MekanismChemicalPortStorage tank)) {
            return false;
        }
        if (!tank.insert(stack, Action.SIMULATE).isEmpty()) {
            return false;
        }
        tank.insert(stack, Action.EXECUTE);
        return true;
    }

    /** What the storage holds, or null when it's empty. */
    @Nullable
    public static PatternIngredient contents(IPortStorage storage) {
        var stack = ((MekanismChemicalPortStorage) storage).chemicalTank.getStack();
        if (stack.isEmpty()) {
            return null;
        }
        return new PatternIngredient(Kind.CHEMICAL, stack.getTypeRegistryName(), stack.getAmount());
    }

    /** Takes up to {@code amount} out of the storage. */
    public static void remove(IPortStorage storage, long amount) {
        ((MekanismChemicalPortStorage) storage).extract(amount, Action.EXECUTE);
    }

    /** Hands the chemical to the block's Mekanism handler on that side; @return how much it took */
    public static long give(ServerLevel level, BlockPos pos, Direction side, PatternIngredient chemical) {
        ChemicalStack stack = stack(chemical);
        if (stack == null) {
            return 0;
        }
        var handler = level.getCapability(MekCapabilities.CHEMICAL, pos, side);
        if (handler == null) {
            return 0;
        }
        ChemicalStack left = handler.insertChemical(stack, Action.EXECUTE);
        return chemical.amount() - left.getAmount();
    }

    @Nullable
    static ChemicalStack stack(PatternIngredient chemical) {
        if (chemical.kind() != Kind.CHEMICAL) {
            return null;
        }
        var type = MekanismAPI.CHEMICAL_REGISTRY.get(chemical.id());
        if (type == null || type.isEmptyType()) {
            return null;
        }
        return new ChemicalStack(type, chemical.amount());
    }
}
