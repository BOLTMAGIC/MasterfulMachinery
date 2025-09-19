package io.ticticboom.mods.mm.client.structure;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import lombok.Getter;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

@Getter
public class RenderTransform {
    private final GuiPos viewportPos;

    public RenderTransform(GuiPos viewportPos) {
        this.viewportPos = viewportPos;
    }

    private void preRender(int viewX, int viewY) {
        RenderSystem.enableDepthTest();
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        var s = 1000;
        modelViewStack.scale(s, s, s);
        var quat = new Quaternionf().rotateXYZ((float) Math.toRadians(30), (float) Math.toRadians(30), 0);
        modelViewStack.mulPose(quat);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.backupProjectionMatrix();
        var proj = new Matrix4f().identity();
        var screenW = (float) mc.getWindow().getWidth();
        var screenH = (float) mc.getWindow().getHeight();
        RenderSystem.viewport(screenW - position.x(), screenH - position.y(), position.w(), position.h());
        proj.setPerspective((float) Math.toRadians(85), screenW / screenH, 0.01f, 1000000f);
        RenderSystem.setProjectionMatrix(proj, RenderSystem.getVertexSorting());
    }

    private void postRender() {
        RenderSystem.restoreProjectionMatrix();
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSyst
    }
}
