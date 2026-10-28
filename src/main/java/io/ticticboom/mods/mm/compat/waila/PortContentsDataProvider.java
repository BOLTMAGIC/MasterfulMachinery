package io.ticticboom.mods.mm.compat.waila;

import io.ticticboom.mods.mm.util.LegacyNbt;
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
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.view.ProgressView;

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
            list.add(StringTag.valueOf(LegacyNbt.componentToJson(line, registries)));
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
        var list = data.getListOrEmpty(KEY);
        float fill = data.contains(FILL_KEY) ? data.getFloatOr(FILL_KEY, 0f) : -1;
        for (int i = 0; i < list.size(); i++) {
            var line = LegacyNbt.componentFromJson(list.getStringOr(i, ""), registries);
            if (line == null) {
                continue;
            }
            if (i == 0 && fill >= 0) {
                // Jade 26 colours a bar by its part's theme colour
                int color = data.contains(FILL_COLOR_KEY) ? 0xFF000000 | data.getIntOr(FILL_COLOR_KEY, 0) : DEFAULT_FILL_COLOR;
                var view = new ProgressView(ProgressView.Part.of(fill, color), line, JadeUI.progressStyle(), BoxStyle.nestedBox());
                tooltip.add(JadeUI.progress(view));
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
