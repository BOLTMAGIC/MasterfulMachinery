package io.ticticboom.mods.mm.client.gui.widgets;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.util.NineSliceUtil;
import io.ticticboom.mods.mm.client.gui.AbstractWidget;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class TilingBackgroundGui extends AbstractWidget {

    public TilingBackgroundGui(GuiPos pos) {
        super(pos);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int tileSize = 4;
        NineSliceUtil.blitNineSliced(guiGraphics, Ref.UiTextures.TILING_GUI, position.x(), position.y(), position.w(), position.h(), tileSize, tileSize, tileSize, tileSize, 12, 12, 0, 0,12, 12);
    }
}
