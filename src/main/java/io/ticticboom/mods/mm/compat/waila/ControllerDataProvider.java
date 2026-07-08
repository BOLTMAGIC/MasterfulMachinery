package io.ticticboom.mods.mm.compat.waila;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class ControllerDataProvider implements IServerDataProvider<BlockAccessor>, IBlockComponentProvider {
    public static final ResourceLocation UID = Ref.id("controller_progress");
    public static final String TICK_KEY = "TickPercentage";
    public static final String REDSTONE_KEY = "RedstoneMode";

    public static final ControllerDataProvider INSTANCE = new ControllerDataProvider();

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor blockAccessor) {
        var be = blockAccessor.getBlockEntity();
        if (be instanceof MachineControllerBlockEntity cbe) {
            if (cbe.getRecipeState() != null) {
                var tickPercentage = String.format("%.2f", cbe.getRecipeState().getTickPercentage()) + "%";
                data.putString(TICK_KEY, tickPercentage);
            } else {
                data.putString(TICK_KEY, "Idle");
            }
            try {
                String mode = cbe.getRedstoneModeName();
                if (mode != null && !mode.isEmpty()) data.putString(REDSTONE_KEY, mode);
            } catch (Throwable ignored) { }
        }
    }


    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        CompoundTag data = blockAccessor.getServerData();
        if (data.contains(TICK_KEY)) {
            var progress = data.getString(TICK_KEY);
            tooltip.add(Component.literal("Progress: " + progress));
        }
        if (data.contains(REDSTONE_KEY)) {
            String m = data.getString(REDSTONE_KEY);
            if (!"IGNORED".equals(m)) {
                String friendly = switch (m) {
                    case "WITH_REDSTONE" -> "Redstone: Requires redstone";
                    case "WITHOUT_REDSTONE" -> "Redstone: Remove redstone";
                    default -> "Redstone: " + m;
                };
                tooltip.add(Component.literal(friendly));
            }
        }
    }
}
