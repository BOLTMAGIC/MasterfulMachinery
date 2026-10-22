package io.ticticboom.mods.mm.pattern;

import io.ticticboom.mods.mm.compat.mekanism.PatternChemicals;
import io.ticticboom.mods.mm.pattern.PatternIngredient.Kind;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.fluid.FluidPortStorage;
import io.ticticboom.mods.mm.port.fluid.FluidPortStorageModel;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.port.item.ItemPortStorageModel;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.output.DisplayedOutput;
import io.ticticboom.mods.mm.recipe.output.IRecipeOutputEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * What one pattern run holds: the inputs AE2 pushed (consumed from here instead of the input ports) and the outputs
 * the run made (given back to the pattern provider from here). Each side has an item storage, a fluid storage and a
 * storage per kind of Mekanism chemical it needs.
 */
public final class PatternBuffer {
    private static final int SLOTS = 9;
    private static final int ITEM_SLOT_CAPACITY = 16_384;
    private static final int FLUID_TANK_CAPACITY = 1_000_000;
    private static final long CHEMICAL_CAPACITY = 1_000_000;
    private static final Set<Kind> CHEMICALS = Set.of(Kind.GAS, Kind.INFUSION, Kind.PIGMENT, Kind.SLURRY);

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
            in.chemicalStorage(input.kind());
        }
        var out = new Side();
        for (IRecipeOutputEntry entry : recipe.outputs().outputs()) {
            for (DisplayedOutput output : entry.displayedOutputs()) {
                PatternIngredient made = output.ingredient().asPatternIngredient();
                if (made != null) {
                    out.chemicalStorage(made.kind());
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

    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.put("In", in.save());
        tag.put("Out", out.save());
        return tag;
    }

    public static PatternBuffer load(CompoundTag tag) {
        return new PatternBuffer(Side.load(tag.getCompound("In")), Side.load(tag.getCompound("Out")));
    }

    private static final class Side {
        final ItemPortStorage items = new ItemPortStorage(new ItemPortStorageModel(1, SLOTS, () -> false, ITEM_SLOT_CAPACITY, 0), () -> {});
        final FluidPortStorage fluids = new FluidPortStorage(new FluidPortStorageModel(1, SLOTS, FLUID_TANK_CAPACITY, () -> false, 0), () -> {});
        final Map<Kind, IPortStorage> chemicals = new EnumMap<>(Kind.class);

        /** The storage for this kind of chemical, made when first needed; null for items and fluids. */
        IPortStorage chemicalStorage(Kind kind) {
            if (!CHEMICALS.contains(kind)) {
                return null;
            }
            return chemicals.computeIfAbsent(kind, k -> PatternChemicals.storageFor(k, CHEMICAL_CAPACITY));
        }

        boolean insert(PatternIngredient stack) {
            int amount = (int) Math.min(Integer.MAX_VALUE, stack.amount());
            return switch (stack.kind()) {
                case ITEM -> {
                    var item = ForgeRegistries.ITEMS.getValue(stack.id());
                    var probe = new ItemStack(item);
                    probe.setTag(stack.nbt() == null ? null : stack.nbt().copy());
                    if (probe.isEmpty() || items.canInsert(probe, amount) > 0) {
                        yield false;
                    }
                    items.insert(probe, amount);
                    yield true;
                }
                case FLUID -> {
                    var fluid = new FluidStack(ForgeRegistries.FLUIDS.getValue(stack.id()), amount);
                    if (fluid.isEmpty() || fluids.getHandler().fill(fluid, IFluidHandler.FluidAction.SIMULATE) < amount) {
                        yield false;
                    }
                    fluids.getHandler().fill(fluid, IFluidHandler.FluidAction.EXECUTE);
                    yield true;
                }
                default -> PatternChemicals.insert(chemicalStorage(stack.kind()), stack);
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
                    case ITEM -> items.getHandler().extractMatching(ForgeRegistries.ITEMS.getValue(stack.id()),
                            s -> Objects.equals(s.getTag(), stack.nbt()), taking, false);
                    case FLUID -> fluids.getHandler().drain(new FluidStack(ForgeRegistries.FLUIDS.getValue(stack.id()), taking), IFluidHandler.FluidAction.EXECUTE);
                    default -> PatternChemicals.remove(chemicals.get(stack.kind()), taking);
                }
            }
            return amount - taking;
        }

        List<IPortStorage> storages() {
            var all = new ArrayList<IPortStorage>();
            all.add(items);
            all.add(fluids);
            all.addAll(chemicals.values());
            return all;
        }

        List<PatternIngredient> contents() {
            var found = new ArrayList<PatternIngredient>();
            var handler = items.getHandler();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    add(found, new PatternIngredient(Kind.ITEM, ForgeRegistries.ITEMS.getKey(stack.getItem()), handler.getActualCount(slot),
                            stack.getTag() == null ? null : stack.getTag().copy()));
                }
            }
            for (int tank = 0; tank < fluids.getHandler().getTanks(); tank++) {
                FluidStack stack = fluids.getHandler().getFluidInTank(tank);
                if (!stack.isEmpty()) {
                    add(found, new PatternIngredient(Kind.FLUID, ForgeRegistries.FLUIDS.getKey(stack.getFluid()), stack.getAmount(), null));
                }
            }
            chemicals.forEach((kind, storage) -> {
                PatternIngredient chemical = PatternChemicals.contents(storage, kind);
                if (chemical != null) {
                    found.add(chemical);
                }
            });
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

        CompoundTag save() {
            var tag = new CompoundTag();
            tag.put("Items", items.save(new CompoundTag()));
            tag.put("Fluids", fluids.save(new CompoundTag()));
            var list = new ListTag();
            chemicals.forEach((kind, storage) -> {
                var entry = storage.save(new CompoundTag());
                entry.putString("Kind", kind.name());
                list.add(entry);
            });
            tag.put("Chemicals", list);
            return tag;
        }

        static Side load(CompoundTag tag) {
            var side = new Side();
            side.items.load(tag.getCompound("Items"));
            side.fluids.load(tag.getCompound("Fluids"));
            for (Tag element : tag.getList("Chemicals", Tag.TAG_COMPOUND)) {
                var entry = (CompoundTag) element;
                side.chemicalStorage(Kind.valueOf(entry.getString("Kind"))).load(entry);
            }
            return side;
        }
    }
}
