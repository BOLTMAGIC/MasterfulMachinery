package io.ticticboom.mods.mm.compat.mekanism;

import io.ticticboom.mods.mm.cap.MekCapabilities;
import io.ticticboom.mods.mm.pattern.PatternIngredient;
import io.ticticboom.mods.mm.pattern.PatternIngredient.Kind;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorage;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorageModel;
import io.ticticboom.mods.mm.port.mekanism.gas.MekanismGasPortStorageFactory;
import io.ticticboom.mods.mm.port.mekanism.infuse.MekanismInfusePortStorageFactory;
import io.ticticboom.mods.mm.port.mekanism.pigment.MekanismPigmentPortStorageFactory;
import io.ticticboom.mods.mm.port.mekanism.slurry.MekanismSlurryPortStorageFactory;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.api.chemical.slurry.SlurryStack;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import org.jetbrains.annotations.Nullable;

/**
 * Mekanism chemicals in a Pattern Port buffer: one MM chemical storage per kind of chemical. Only used with Mekanism
 * installed (a pattern can only carry chemicals with Applied Mekanistics, which needs Mekanism).
 */
public final class PatternChemicals {
    private PatternChemicals() {
    }

    /** An empty storage for one kind of chemical, holding up to {@code capacity} mB. */
    public static IPortStorage storageFor(Kind kind, long capacity) {
        var model = new MekanismChemicalPortStorageModel(capacity);
        var factory = switch (kind) {
            case GAS -> new MekanismGasPortStorageFactory(model);
            case INFUSION -> new MekanismInfusePortStorageFactory(model);
            case PIGMENT -> new MekanismPigmentPortStorageFactory(model);
            case SLURRY -> new MekanismSlurryPortStorageFactory(model);
            default -> throw new IllegalArgumentException(kind + " isn't a chemical");
        };
        return factory.createPortStorage(() -> {});
    }

    /** @return false (nothing added) if it doesn't all fit */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean insert(IPortStorage storage, PatternIngredient chemical) {
        ChemicalStack stack = stack(chemical);
        if (stack == null) {
            return false;
        }
        IChemicalTank tank = ((MekanismChemicalPortStorage) storage).chemicalTank;
        if (!tank.insert(stack, Action.SIMULATE, AutomationType.INTERNAL).isEmpty()) {
            return false;
        }
        tank.insert(stack, Action.EXECUTE, AutomationType.INTERNAL);
        return true;
    }

    /** What the storage holds, or null when it's empty. */
    @Nullable
    public static PatternIngredient contents(IPortStorage storage, Kind kind) {
        var tank = ((MekanismChemicalPortStorage<?, ?>) storage).chemicalTank;
        if (tank.isEmpty()) {
            return null;
        }
        ChemicalStack<?> stack = tank.getStack();
        return new PatternIngredient(kind, stack.getTypeRegistryName(), stack.getAmount(), null);
    }

    /** Takes up to {@code amount} out of the storage. */
    public static void remove(IPortStorage storage, long amount) {
        ((MekanismChemicalPortStorage<?, ?>) storage).chemicalTank.extract(amount, Action.EXECUTE, AutomationType.INTERNAL);
    }

    /** Hands the chemical to the block's Mekanism handler on that side; @return how much it took */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static long give(BlockEntity target, Direction side, PatternIngredient chemical) {
        Capability<? extends IChemicalHandler> cap = switch (chemical.kind()) {
            case GAS -> MekCapabilities.GAS;
            case INFUSION -> MekCapabilities.INFUSE;
            case PIGMENT -> MekCapabilities.PIGMENT;
            case SLURRY -> MekCapabilities.SLURRY;
            default -> null;
        };
        ChemicalStack stack = stack(chemical);
        if (cap == null || stack == null) {
            return 0;
        }
        IChemicalHandler handler = target.getCapability(cap, side).resolve().orElse(null);
        if (handler == null) {
            return 0;
        }
        ChemicalStack left = handler.insertChemical(stack, Action.EXECUTE);
        return chemical.amount() - left.getAmount();
    }

    @Nullable
    static ChemicalStack<?> stack(PatternIngredient chemical) {
        ResourceLocation id = chemical.id();
        return switch (chemical.kind()) {
            case GAS -> new GasStack(MekanismAPI.gasRegistry().getValue(id), chemical.amount());
            case INFUSION -> new InfusionStack(MekanismAPI.infuseTypeRegistry().getValue(id), chemical.amount());
            case PIGMENT -> new PigmentStack(MekanismAPI.pigmentRegistry().getValue(id), chemical.amount());
            case SLURRY -> new SlurryStack(MekanismAPI.slurryRegistry().getValue(id), chemical.amount());
            default -> null;
        };
    }
}
