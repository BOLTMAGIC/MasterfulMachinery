package io.ticticboom.mods.mm.port;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/**
 * One thing a port holds, as shown in the controller's port list: either an item with its stored
 * count, or a tank (fluid, chemical or energy) with amount and capacity. Plain data, safe on both sides.
 */
public record PortContent(Kind kind, ItemStack item, FluidStack fluid, @Nullable ResourceLocation sprite, int tint,
                          @Nullable Component name, long amount, long capacity, String unit) {

    public enum Kind { ITEM, FLUID, CHEMICAL, ENERGY }

    public static PortContent item(ItemStack stack, long count) {
        return new PortContent(Kind.ITEM, stack, FluidStack.EMPTY, null, 0xFFFFFFFF, stack.getHoverName(), count, 0, "");
    }

    public static PortContent fluid(FluidStack stack, long capacity) {
        return new PortContent(Kind.FLUID, ItemStack.EMPTY, stack, null, 0xFFFFFFFF,
                stack.isEmpty() ? null : stack.getDisplayName(), stack.getAmount(), capacity, "mB");
    }

    /**
     * @param sprite block atlas sprite of the chemical, null when the tank is empty
     */
    public static PortContent chemical(@Nullable Component name, @Nullable ResourceLocation sprite, int tint, long amount, long capacity) {
        return new PortContent(Kind.CHEMICAL, ItemStack.EMPTY, FluidStack.EMPTY, sprite, tint, name, amount, capacity, "mB");
    }

    public static PortContent energy(long amount, long capacity) {
        return new PortContent(Kind.ENERGY, ItemStack.EMPTY, FluidStack.EMPTY, null, 0xFFFFFFFF, null, amount, capacity, "FE");
    }

    public boolean isTank() {
        return kind != Kind.ITEM;
    }
}
