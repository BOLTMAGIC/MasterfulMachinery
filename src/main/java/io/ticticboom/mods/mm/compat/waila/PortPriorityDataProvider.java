package io.ticticboom.mods.mm.compat.waila;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Provides the currently configured priority of a Port to Jade (Waila) tooltips.
 */
public class PortPriorityDataProvider implements IServerDataProvider<BlockAccessor>, IBlockComponentProvider {
    public static final ResourceLocation UID = Ref.id("port_priority");
    public static final String KEY = "PortPriority";

    public static final PortPriorityDataProvider INSTANCE = new PortPriorityDataProvider();

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor blockAccessor) {
        var be = blockAccessor.getBlockEntity();
        if (be instanceof IPortBlockEntity pbe) {
            // Only expose priority for outputs (server authoritative). Don't show for input ports.
            if (!pbe.isInput()) {
                var storage = pbe.getStorage();
                if (storage != null) {
                    data.putInt(KEY, storage.getPriority());
                }
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor blockAccessor, IPluginConfig iPluginConfig) {
        CompoundTag data = blockAccessor.getServerData();
        if (data.contains(KEY)) {
            int prio = data.getInt(KEY);
            tooltip.add(Component.translatable("tooltip.mm.port_priority", prio));
        }
    }
}
