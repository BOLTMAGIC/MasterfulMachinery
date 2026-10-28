package io.ticticboom.mods.mm.compat.waila;

import io.ticticboom.mods.mm.util.LegacyNbt;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.networklink.NetworkLink;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class PortMachineDataProvider implements IServerDataProvider<BlockAccessor>, IBlockComponentProvider {
    public static final Identifier UID = Ref.id("port_machine");
    private static final String PORT_KEY = "MMPort";
    private static final String MACHINE_KEY = "Machine";
    private static final String STATUS_KEY = "MachineStatus";
    private static final String AUTO_SIDES_KEY = "AutoIOSides";
    private static final String AUTO_PULL_KEY = "AutoIOPull";

    public static final PortMachineDataProvider INSTANCE = new PortMachineDataProvider();

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor blockAccessor) {
        if (!(blockAccessor.getBlockEntity() instanceof AbstractPortBlockEntity port)) {
            return;
        }
        data.putBoolean(PORT_KEY, true);
        var controller = port.getControllerPos() == null ? null : NetworkLink.controllerAt(blockAccessor.getLevel(), blockAccessor.getPosition());
        if (controller != null) {
            data.putString(MACHINE_KEY, LegacyNbt.componentToJson(controller.getName(), blockAccessor.getLevel().registryAccess()));
            data.putString(STATUS_KEY, controller.statusKey());
        }
        var autoIO = port.getAutoIO();
        if (autoIO != null) {
            data.putInt(AUTO_SIDES_KEY, autoIO.enabledSideCount());
            data.putBoolean(AUTO_PULL_KEY, autoIO.isPull());
        }
    }

    @Override
    public Identifier getUid() {
        return UID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        CompoundTag data = blockAccessor.getServerData();
        if (data.contains(MACHINE_KEY)) {
            String status = data.getStringOr(STATUS_KEY, "");
            Component name = LegacyNbt.componentFromJson(data.getStringOr(MACHINE_KEY, ""), blockAccessor.getLevel().registryAccess());
            tooltip.add(Component.translatable("jade.mm.port.machine", name == null ? Component.empty() : name)
                    .append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
                    .append(Component.translatable("gui.mm.controller.status." + status).withStyle(ControllerDataProvider.statusColor(status))));
        } else if (data.contains(PORT_KEY)) {
            tooltip.add(Component.translatable("jade.mm.port.no_machine").withStyle(ChatFormatting.GRAY));
        }
        if (data.contains(AUTO_SIDES_KEY)) {
            int sides = data.getIntOr(AUTO_SIDES_KEY, 0);
            if (sides == 0) {
                tooltip.add(Component.translatable("jade.mm.port.auto_io.off").withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.translatable(data.getBooleanOr(AUTO_PULL_KEY, false) ? "jade.mm.port.auto_io.pull" : "jade.mm.port.auto_io.push", sides));
            }
        }
    }
}
