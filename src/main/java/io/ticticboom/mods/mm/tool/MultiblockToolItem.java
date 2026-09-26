package io.ticticboom.mods.mm.tool;

import io.ticticboom.mods.mm.config.MMConfigSetup;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Carries a chosen Masterful Machinery structure, a 54-slot block store and FE; right-click builds it
 * in the world (see the plan's Task 4), Shift+use opens the gallery/settings screen (Task 6).
 */
public class MultiblockToolItem extends Item {
    private static final int BAR_COLOR = 0x3399FF;

    public MultiblockToolItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ICapabilityProvider() {
            private final LazyOptional<IEnergyStorage> energy = LazyOptional.of(() ->
                    new ToolEnergy(stack, MMConfigSetup.COMMON.toolEnergyCapacity.get()));

            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                return cap == ForgeCapabilities.ENERGY ? energy.cast() : LazyOptional.empty();
            }
        };
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int capacity = MMConfigSetup.COMMON.toolEnergyCapacity.get();
        int energy = new ToolEnergy(stack, capacity).getEnergyStored();
        return Math.round(13.0F * energy / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOR;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation structure = ToolData.structure(stack);
        if (structure != null) {
            tooltip.add(Component.translatable("tooltip.mm.multiblock_tool.structure", structure.toString()).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.mm.multiblock_tool.no_structure").withStyle(ChatFormatting.DARK_GRAY));
        }
        int capacity = MMConfigSetup.COMMON.toolEnergyCapacity.get();
        int energy = new ToolEnergy(stack, capacity).getEnergyStored();
        tooltip.add(Component.translatable("tooltip.mm.multiblock_tool.energy", energy, capacity).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mm.multiblock_tool.usage").withStyle(ChatFormatting.DARK_GRAY));
    }
}
