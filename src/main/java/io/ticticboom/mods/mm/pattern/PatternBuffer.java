package io.ticticboom.mods.mm.pattern;

// [26.1-pending] import: import io.ticticboom.mods.mm.compat.mekanism.PatternChemicals;
import io.ticticboom.mods.mm.pattern.PatternIngredient.Kind;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.fluid.FluidPortStorage;
import io.ticticboom.mods.mm.port.fluid.FluidPortStorageModel;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.port.item.ItemPortStorageModel;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.output.DisplayedOutput;
import io.ticticboom.mods.mm.recipe.output.IRecipeOutputEntry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * What one pattern run holds: the inputs AE2 pushed (consumed from here instead of the input ports) and the outputs
 * the run made (given back to the pattern provider from here). Each side has an item storage, a fluid storage and,
 * when it needs one, a Mekanism chemical storage.
 */
public final class PatternBuffer {
    private static final int SLOTS = 9;
    private static final int ITEM_SLOT_CAPACITY = 16_384;
    private static final int FLUID_TANK_CAPACITY = 1_000_000;
    private static final long CHEMICAL_CAPACITY = 1_000_000;

    private final Side in;
    private final Side out;

    private PatternBuffer(Side in, Side out) {
        this.in = in;
        this.out = out;
    }

    /** An empty buffer with room for the pattern's inputs and the recipe's outputs. */
    public static PatternBuffer forRun(PatternSpec spec, RecipeModel recipe) {
        var in = new Side();
        for (PatternIngredient input : spec.inputs()) {
            in.needs(input.kind());
        }
        var out = new Side();
        for (IRecipeOutputEntry entry : recipe.outputs().outputs()) {
            for (DisplayedOutput output : entry.displayedOutputs()) {
                PatternIngredient made = output.ingredient().asPatternIngredient();
                if (made != null) {
                    out.needs(made.kind());
                }
            }
        }
        return new PatternBuffer(in, out);
    }

    /** Adds to the inputs; false (nothing added) if it doesn't fit. */
    public boolean insert(PatternIngredient stack) {
        return in.insert(stack);
    }

    /** Where the run's inputs are consumed from, before the machine's ports. */
    public List<IPortStorage> inputs() {
        return in.storages();
    }

    /** Where the run's outputs go instead of the machine's ports. */
    public List<IPortStorage> outputs() {
        return out.storages();
    }

    /** Everything still in the buffer: unused inputs first, then the outputs. */
    public List<PatternIngredient> leftovers() {
        var all = new ArrayList<>(in.contents());
        all.addAll(out.contents());
        return all;
    }

    public boolean isEmpty() {
        return leftovers().isEmpty();
    }

    /** Takes this much out of the buffer, from the unused inputs first. */
    public void remove(PatternIngredient stack) {
        long left = in.remove(stack, stack.amount());
        if (left > 0) {
            out.remove(stack, left);
        }
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        tag.put("In", in.save(registries));
        tag.put("Out", out.save(registries));
        return tag;
    }

    public static PatternBuffer load(CompoundTag tag, HolderLookup.Provider registries) {
        return new PatternBuffer(Side.load(tag.getCompoundOrEmpty("In"), registries), Side.load(tag.getCompoundOrEmpty("Out"), registries));
    }

    private static final class Side {
        final ItemPortStorage items = new ItemPortStorage(new ItemPortStorageModel(1, SLOTS, () -> false, ITEM_SLOT_CAPACITY, 0), () -> {});
        final FluidPortStorage fluids = new FluidPortStorage(new FluidPortStorageModel(1, SLOTS, FLUID_TANK_CAPACITY, () -> false, 0, List.of()), () -> {});
        // made when first needed, so Mekanism classes are only touched with a chemical in the pattern or recipe
        @Nullable
        IPortStorage chemicals;

        void needs(Kind kind) {
            if (kind == Kind.CHEMICAL && chemicals == null) {
                // [26.1-pending] Mekanism has no 26.1.2 release yet: no chemical storage, chemicals aren't accepted
                // chemicals = PatternChemicals.storageFor(CHEMICAL_CAPACITY);
            }
        }

        boolean insert(PatternIngredient stack) {
            int amount = (int) Math.min(Integer.MAX_VALUE, stack.amount());
            return switch (stack.kind()) {
                case ITEM -> {
                    var probe = new ItemStack(BuiltInRegistries.ITEM.getValue(stack.id()));
                    probe.applyComponents(stack.components());
                    if (probe.isEmpty() || items.canInsert(probe, amount) > 0) {
                        yield false;
                    }
                    items.insert(probe, amount);
                    yield true;
                }
                case FLUID -> {
                    var fluid = new FluidStack(BuiltInRegistries.FLUID.getValue(stack.id()), amount);
                    if (fluid.isEmpty() || fluids.getHandler().fill(fluid, IFluidHandler.FluidAction.SIMULATE) < amount) {
                        yield false;
                    }
                    fluids.getHandler().fill(fluid, IFluidHandler.FluidAction.EXECUTE);
                    yield true;
                }
                // [26.1-pending] Mekanism: needs(Kind.CHEMICAL); yield PatternChemicals.insert(chemicals, stack);
                case CHEMICAL -> false;
            };
        }

        /** @return how much of {@code amount} it didn't have */
        long remove(PatternIngredient stack, long amount) {
            long has = 0;
            for (PatternIngredient held : contents()) {
                if (held.sameResource(stack)) has = held.amount();
            }
            int taking = (int) Math.min(Math.min(has, amount), Integer.MAX_VALUE);
            if (taking > 0) {
                switch (stack.kind()) {
                    case ITEM -> items.getHandler().extractMatching(BuiltInRegistries.ITEM.getValue(stack.id()),
                            s -> s.getComponentsPatch().equals(stack.components()), taking, false);
                    case FLUID -> fluids.getHandler().drain(new FluidStack(BuiltInRegistries.FLUID.getValue(stack.id()), taking), IFluidHandler.FluidAction.EXECUTE);
                    case CHEMICAL -> { } // [26.1-pending] Mekanism: PatternChemicals.remove(chemicals, taking);
                }
            }
            return amount - taking;
        }

        List<IPortStorage> storages() {
            var all = new ArrayList<IPortStorage>();
            all.add(items);
            all.add(fluids);
            if (chemicals != null) all.add(chemicals);
            return all;
        }

        List<PatternIngredient> contents() {
            var found = new ArrayList<PatternIngredient>();
            var handler = items.getHandler();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    add(found, new PatternIngredient(Kind.ITEM, BuiltInRegistries.ITEM.getKey(stack.getItem()), handler.getActualCount(slot),
                            stack.getComponentsPatch()));
                }
            }
            for (int tank = 0; tank < fluids.getHandler().getTanks(); tank++) {
                FluidStack stack = fluids.getHandler().getFluidInTank(tank);
                if (!stack.isEmpty()) {
                    add(found, new PatternIngredient(Kind.FLUID, BuiltInRegistries.FLUID.getKey(stack.getFluid()), stack.getAmount()));
                }
            }
            // [26.1-pending] Mekanism: chemicals is never made, PatternChemicals.contents(chemicals) would list it here
            return found;
        }

        private static void add(List<PatternIngredient> found, PatternIngredient stack) {
            for (int i = 0; i < found.size(); i++) {
                if (found.get(i).sameResource(stack)) {
                    found.set(i, found.get(i).withAmount(found.get(i).amount() + stack.amount()));
                    return;
                }
            }
            found.add(stack);
        }

        CompoundTag save(HolderLookup.Provider registries) {
            var tag = new CompoundTag();
            tag.put("Items", items.save(new CompoundTag(), registries));
            tag.put("Fluids", fluids.save(new CompoundTag(), registries));
            if (chemicals != null) {
                tag.put("Chemicals", chemicals.save(new CompoundTag(), registries));
            }
            return tag;
        }

        static Side load(CompoundTag tag, HolderLookup.Provider registries) {
            var side = new Side();
            side.items.load(tag.getCompoundOrEmpty("Items"), registries);
            side.fluids.load(tag.getCompoundOrEmpty("Fluids"), registries);
            if (tag.contains("Chemicals") && side.chemicals != null) {
                side.needs(Kind.CHEMICAL);
                side.chemicals.load(tag.getCompoundOrEmpty("Chemicals"), registries);
            }
            return side;
        }
    }
}
