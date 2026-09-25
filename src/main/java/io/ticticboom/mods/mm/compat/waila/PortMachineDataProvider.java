package io.ticticboom.mods.mm.compat.waila;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.common.autoio.PortAutoIO;
import io.ticticboom.mods.mm.port.common.autoio.PortSides;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade tooltip of a port: the machine it belongs to (and what that machine is doing) and its auto I/O.
 * Registered for every block; it only adds lines for MM ports.
 */
public class PortMachineDataProvider implements IServerDataProvider<BlockAccessor>, IBlockComponentProvider {
    public static final ResourceLocation UID = Ref.id("port_machine");
    private static final String PORT_KEY = "MMPort";
    private static final String MACHINE_KEY = "Machine";
    private static final String STATUS_KEY = "MachineStatus";
    private static final String AUTO_SIDES_KEY = "AutoIOSides";
    private static final String AUTO_PULL_KEY = "AutoIOPull";

    public static final PortMachineDataProvider INSTANCE = new PortMachineDataProvider();

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor blockAccessor) {
        if (!(blockAccessor.getBlockEntity() instanceof AbstractPortBlockEntity port)
                || !(blockAccessor.getLevel() instanceof ServerLevel level)) {
            return;
        }
        data.putBoolean(PORT_KEY, true);
        var controller = PortSides.findController(level, blockAccessor.getPosition(), port.getStorage());
        if (controller != null) {
            data.putString(MACHINE_KEY, controller.getModel().name());
            data.putString(STATUS_KEY, controller.statusKey());
        }
        PortAutoIO autoIO = port.getAutoIO();
        if (autoIO != null) {
            int sides = 0;
            for (Direction side : Direction.values()) {
                if (autoIO.isSideEnabled(side)) sides++;
            }
            data.putInt(AUTO_SIDES_KEY, sides);
            data.putBoolean(AUTO_PULL_KEY, autoIO.isPull());
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        CompoundTag data = blockAccessor.getServerData();
        if (data.contains(MACHINE_KEY)) {
            String status = data.getString(STATUS_KEY);
            tooltip.add(Component.translatable("jade.mm.port.machine", data.getString(MACHINE_KEY))
                    .append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
                    .append(Component.translatable("gui.mm.controller.status." + status).withStyle(ControllerDataProvider.statusColor(status))));
        } else if (data.contains(PORT_KEY)) {
            tooltip.add(Component.translatable("jade.mm.port.no_machine").withStyle(ChatFormatting.GRAY));
        }
        if (data.contains(AUTO_SIDES_KEY)) {
            int sides = data.getInt(AUTO_SIDES_KEY);
            if (sides == 0) {
                tooltip.add(Component.translatable("jade.mm.port.auto_io.off").withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.translatable(data.getBoolean(AUTO_PULL_KEY) ? "jade.mm.port.auto_io.pull" : "jade.mm.port.auto_io.push", sides));
            }
        }
    }
}
