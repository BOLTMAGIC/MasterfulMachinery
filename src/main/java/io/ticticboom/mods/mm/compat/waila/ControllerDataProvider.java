package io.ticticboom.mods.mm.compat.waila;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade tooltip of a machine controller: state, recipe progress, parallel recipes, redstone mode and owner.
 */
public class ControllerDataProvider implements IServerDataProvider<BlockAccessor>, IBlockComponentProvider {
    public static final ResourceLocation UID = Ref.id("controller_progress");
    public static final String STATUS_KEY = "Status";
    public static final String PROGRESS_KEY = "Progress";
    public static final String RUNNING_KEY = "Running";
    public static final String LIMIT_KEY = "ParallelLimit";
    public static final String REDSTONE_KEY = "RedstoneMode";
    public static final String OWNER_KEY = "Owner";

    public static final ControllerDataProvider INSTANCE = new ControllerDataProvider();

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor blockAccessor) {
        if (!(blockAccessor.getBlockEntity() instanceof MachineControllerBlockEntity cbe)) {
            return;
        }
        String status = cbe.statusKey();
        data.putString(STATUS_KEY, status);
        if (cbe.getRecipeState() != null) {
            data.putFloat(PROGRESS_KEY, (float) cbe.getRecipeState().getTickPercentage());
        }
        if (!"not_formed".equals(status)) {
            data.putInt(RUNNING_KEY, cbe.getActiveRecipeCount());
            data.putInt(LIMIT_KEY, cbe.getDisplayedParallelLimit());
        }
        data.putString(REDSTONE_KEY, cbe.getRedstoneModeName().toLowerCase());
        if (cbe.getNetworkLink() != null) {
            data.putString(OWNER_KEY, cbe.getNetworkLink().ownerName());
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        CompoundTag data = blockAccessor.getServerData();
        if (!data.contains(STATUS_KEY)) {
            return;
        }
        String status = data.getString(STATUS_KEY);
        tooltip.add(Component.translatable("gui.mm.controller.status." + status).withStyle(statusColor(status)));
        if (data.contains(PROGRESS_KEY)) {
            tooltip.add(Component.translatable("jade.mm.controller.progress", String.format("%.0f%%", data.getFloat(PROGRESS_KEY))));
        }
        if (data.contains(LIMIT_KEY)) {
            tooltip.add(Component.translatable("jade.mm.controller.parallel", data.getInt(RUNNING_KEY), data.getInt(LIMIT_KEY)));
        }
        String redstone = data.getString(REDSTONE_KEY);
        if (!redstone.isEmpty() && !"ignored".equals(redstone)) {
            tooltip.add(Component.translatable("jade.mm.controller.redstone",
                    Component.translatable("gui.mm.controller.redstone." + redstone)));
        }
        if (data.contains(OWNER_KEY)) {
            tooltip.add(Component.translatable("jade.mm.controller.owner", data.getString(OWNER_KEY)).withStyle(ChatFormatting.AQUA));
        }
    }

    static ChatFormatting statusColor(String status) {
        return switch (status) {
            case "not_formed" -> ChatFormatting.RED;
            case "paused" -> ChatFormatting.GOLD;
            case "running" -> ChatFormatting.GREEN;
            default -> ChatFormatting.YELLOW;
        };
    }
}
