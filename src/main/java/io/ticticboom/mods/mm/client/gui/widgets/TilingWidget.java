package io.ticticboom.mods.mm.client.gui.widgets;

import io.ticticboom.mods.mm.client.gui.AbstractWidget;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class TilingWidget {
    private final ResourceLocation texture;
    private final int u;
    private final int v;
    private final int texWidth;
    private final int texHeight;

    @Setter
    @Getter
    protected int renderWidth;

    @Setter
    @Getter
    protected int renderHeight;

    public TilingWidget(ResourceLocation texture, int u, int v, int texWidth, int texHeight) {
        this.texture = texture;
        this.u = u;
        this.v = v;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
    }

//    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        var iterWidth = this.renderWidth / this.texWidth;
        var iterHeight = this.renderWidth / this.texWidth;

        for (int i = 0; i < iterWidth; i++) {
//            guiGraphics.blit(this.texture, this.xPos + (i * this.texWidth), this.yPos, this.u, this.v, this.texWidth, this.texHeight);
        }
    }
}
