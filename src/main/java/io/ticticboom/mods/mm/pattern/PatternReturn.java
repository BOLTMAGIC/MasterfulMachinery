package io.ticticboom.mods.mm.pattern;

import io.ticticboom.mods.mm.cap.MMCapabilities;
// [26.1-pending] import: import io.ticticboom.mods.mm.compat.mekanism.PatternChemicals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Gives what a finished pattern run holds back to the pattern provider that pushed it, through the provider's item,
 * fluid and (with Mekanism) chemical handlers. Whatever it doesn't take stays in the buffer for the next try.
 */
public final class PatternReturn {
    private static final boolean MEKANISM = ModList.get().isLoaded("mekanism");

    private PatternReturn() {
    }

    /** @return true once the buffer is empty */
    public static boolean drain(ServerLevel level, PatternRun run) {
        BlockPos pos = run.getReturnPos();
        // never load a chunk for this; the provider may be gone or unloaded for now
        if (!level.isLoaded(pos)) {
            return false;
        }
        if (level.getBlockEntity(pos) == null) {
            return run.getBuffer().isEmpty();
        }
        for (PatternIngredient left : run.getBuffer().leftovers()) {
            long given = switch (left.kind()) {
                case ITEM -> giveItems(level, run, left);
                case FLUID -> giveFluid(level, run, left);
                case CHEMICAL -> 0; // [26.1-pending] Mekanism: MEKANISM ? PatternChemicals.give(level, pos, run.getReturnFace(), left) : 0
            };
            if (given > 0) {
                run.getBuffer().remove(left.withAmount(given));
            }
        }
        return run.getBuffer().isEmpty();
    }

    private static long giveItems(ServerLevel level, PatternRun run, PatternIngredient items) {
        var handler = MMCapabilities.item(level, run.getReturnPos(), run.getReturnFace());
        if (handler == null) {
            return 0;
        }
        var template = new ItemStack(BuiltInRegistries.ITEM.getValue(items.id()));
        template.applyComponents(items.components());
        long given = 0;
        while (given < items.amount()) {
            int batch = (int) Math.min(template.getMaxStackSize(), items.amount() - given);
            ItemStack left = ItemHandlerHelper.insertItemStacked(handler, template.copyWithCount(batch), false);
            given += batch - left.getCount();
            if (!left.isEmpty()) {
                break;
            }
        }
        return given;
    }

    private static long giveFluid(ServerLevel level, PatternRun run, PatternIngredient fluid) {
        var handler = MMCapabilities.fluid(level, run.getReturnPos(), run.getReturnFace());
        if (handler == null) {
            return 0;
        }
        int amount = (int) Math.min(Integer.MAX_VALUE, fluid.amount());
        return handler.fill(new FluidStack(BuiltInRegistries.FLUID.getValue(fluid.id()), amount), IFluidHandler.FluidAction.EXECUTE);
    }
}
