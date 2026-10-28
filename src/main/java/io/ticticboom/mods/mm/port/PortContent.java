package io.ticticboom.mods.mm.port;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public record PortContent(Kind kind, ItemStack item, FluidStack fluid, @Nullable Identifier sprite, int tint,
                          @Nullable Component name, long amount, long capacity, String unit) {

    public enum Kind { ITEM, FLUID, CHEMICAL, ENERGY }

    public static PortContent item(ItemStack stack, long count) {
        return new PortContent(Kind.ITEM, stack, FluidStack.EMPTY, null, 0xFFFFFFFF, stack.getHoverName(), count, 0, "");
    }

    public static PortContent fluid(FluidStack stack, long capacity) {
        return new PortContent(Kind.FLUID, ItemStack.EMPTY, stack, null, 0xFFFFFFFF,
                stack.isEmpty() ? null : stack.getHoverName(), stack.getAmount(), capacity, "mB");
    }

    public static PortContent chemical(@Nullable Component name, @Nullable Identifier sprite, int tint, long amount, long capacity) {
        return new PortContent(Kind.CHEMICAL, ItemStack.EMPTY, FluidStack.EMPTY, sprite, tint, name, amount, capacity, "mB");
    }

    public static PortContent energy(long amount, long capacity) {
        return new PortContent(Kind.ENERGY, ItemStack.EMPTY, FluidStack.EMPTY, null, 0xFFFFFFFF, null, amount, capacity, "FE");
    }

    public static PortContent gauge(Component name, int color, long amount, long capacity, String unit) {
        return new PortContent(Kind.CHEMICAL, ItemStack.EMPTY, FluidStack.EMPTY, null, color, name, amount, capacity, unit);
    }

    public boolean isTank() {
        return kind != Kind.ITEM;
    }
}
