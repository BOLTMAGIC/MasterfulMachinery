package io.ticticboom.mods.mm.client;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.blaze3d.systems.RenderSystem;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import net.minecraft.client.Minecraft;

public class RenderUtil {
    private static final Minecraft mc = Minecraft.getInstance();

    public static void setViewport(GuiPos viewportPos) {
        var gScale = mc.getWindow().getGuiScale();
        var screenH = (float) mc.getWindow().getHeight();
        int scaledWidth = (int) (viewportPos.w() * gScale);
        int scaledHeight = (int) (viewportPos.h() * gScale);
        var adjustedY = (int)(screenH - ((viewportPos.y() * gScale) + scaledHeight));
        var scaledX = (int) (viewportPos.x() * gScale);
        RenderSystem.viewport(scaledX, adjustedY, scaledWidth, scaledHeight);
    }

    public static void resetViewport() {
        RenderSystem.viewport(0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight());
    }
}
