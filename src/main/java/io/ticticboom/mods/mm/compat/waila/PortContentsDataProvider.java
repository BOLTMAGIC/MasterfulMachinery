package io.ticticboom.mods.mm.compat.waila;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElementHelper;

public class PortContentsDataProvider implements IServerDataProvider<BlockAccessor>, IBlockComponentProvider {
    public static final Identifier UID = Ref.id("port_contents");
    public static final String KEY = "PortContents";
    public static final String FILL_KEY = "PortFill";
    public static final String FILL_COLOR_KEY = "PortFillColor";
    private static final int DEFAULT_FILL_COLOR = 0xFF35C9E6;
    private static final int DEFAULT_FILL_SHADE = 0xFF14657A;

    public static final PortContentsDataProvider INSTANCE = new PortContentsDataProvider();

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof IPortBlockEntity port) || port.getStorage() == null) {
            return;
        }
        var lines = port.getStorage().describeContents();
        if (lines.isEmpty()) {
            return;
        }
        var fill = port.getStorage().fillRatio();
        if (fill >= 0) {
            data.putFloat(FILL_KEY, (float) fill);
            var color = port.getStorage().fillColor();
            if (color != 0) {
                data.putInt(FILL_COLOR_KEY, color);
            }
        }
        var registries = accessor.getLevel().registryAccess();
        var list = new ListTag();
        for (var line : lines) {
            list.add(StringTag.valueOf(Component.Serializer.toJson(line, registries)));
        }
        data.put(KEY, list);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        var data = accessor.getServerData();
        if (!data.contains(KEY)) {
            return;
        }
        var registries = accessor.getLevel().registryAccess();
        var list = data.getList(KEY, Tag.TAG_STRING);
        var fill = data.contains(FILL_KEY) ? data.getFloat(FILL_KEY) : -1;
        for (int i = 0; i < list.size(); i++) {
            var line = Component.Serializer.fromJson(list.getString(i), registries);
            if (line == null) {
                continue;
            }
            if (i == 0 && fill >= 0) {
                var helper = IElementHelper.get();
                var color = data.contains(FILL_COLOR_KEY) ? data.getInt(FILL_COLOR_KEY) : 0;
                var style = color == 0
                        ? helper.progressStyle().color(DEFAULT_FILL_COLOR, DEFAULT_FILL_SHADE).textColor(0xFFFFFFFF)
                        : helper.progressStyle().color(0xFF000000 | color, shade(color)).textColor(0xFFFFFFFF);
                tooltip.add(helper.progress(fill, line, style, BoxStyle.getNestedBox(), true));
            } else {
                tooltip.add(line);
            }
        }
    }

    private static int shade(int color) {
        int r = (color >> 16 & 0xFF) * 2 / 5;
        int g = (color >> 8 & 0xFF) * 2 / 5;
        int b = (color & 0xFF) * 2 / 5;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
