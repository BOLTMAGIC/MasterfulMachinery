package io.ticticboom.mods.mm.pattern;

import io.ticticboom.mods.mm.compat.mekanism.PatternChemicals;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

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
        BlockEntity target = level.getBlockEntity(pos);
        if (target == null) {
            return run.getBuffer().isEmpty();
        }
        for (PatternIngredient left : run.getBuffer().leftovers()) {
            long given = switch (left.kind()) {
                case ITEM -> giveItems(target, run, left);
                case FLUID -> giveFluid(target, run, left);
                default -> MEKANISM ? PatternChemicals.give(target, run.getReturnFace(), left) : 0;
            };
            if (given > 0) {
                run.getBuffer().remove(left.withAmount(given));
            }
        }
        return run.getBuffer().isEmpty();
    }

    private static long giveItems(BlockEntity target, PatternRun run, PatternIngredient items) {
        var handler = target.getCapability(ForgeCapabilities.ITEM_HANDLER, run.getReturnFace()).resolve().orElse(null);
        if (handler == null) {
            return 0;
        }
        var template = new ItemStack(ForgeRegistries.ITEMS.getValue(items.id()));
        template.setTag(items.nbt() == null ? null : items.nbt().copy());
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

    private static long giveFluid(BlockEntity target, PatternRun run, PatternIngredient fluid) {
        var handler = target.getCapability(ForgeCapabilities.FLUID_HANDLER, run.getReturnFace()).resolve().orElse(null);
        if (handler == null) {
            return 0;
        }
        int amount = (int) Math.min(Integer.MAX_VALUE, fluid.amount());
        return handler.fill(new FluidStack(ForgeRegistries.FLUIDS.getValue(fluid.id()), amount), IFluidHandler.FluidAction.EXECUTE);
    }
}
