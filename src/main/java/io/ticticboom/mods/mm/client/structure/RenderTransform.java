package io.ticticboom.mods.mm.client.structure;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import io.ticticboom.mods.mm.client.RenderUtil;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

@Getter
public class RenderTransform {
    @Setter
    private GuiPos viewportPos;
    private static Minecraft mc = Minecraft.getInstance();

    public RenderTransform() {
    }



    public void preRender(float xRot, float yRot, int extent, Matrix4f viewMatrix) {
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        var s = 95.5f;
        modelViewStack.scale(s, s, s);
        modelViewStack.translate(0,0, 100 - extent);
        modelViewStack.mulPoseMatrix(viewMatrix);
        var quat = new Quaternionf().rotateXYZ((float) Math.toRadians(xRot), (float) Math.toRadians(yRot), 0);
        modelViewStack.mulPose(quat);
        RenderSystem.applyModelViewMatrix();

        // projection
        RenderSystem.backupProjectionMatrix();
        var proj = new Matrix4f().identity();
        proj.setPerspective((float) Math.toRadians(85), (float) viewportPos.w() / viewportPos.h(), 0.01f, 1000000f);
        RenderSystem.setProjectionMatrix(proj, VertexSorting.DISTANCE_TO_ORIGIN);

        setupViewport();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.clear(256, Minecraft.ON_OSX);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Lighting.setupFor3DItems();


    }

    private void setupViewport() {
       RenderUtil.setViewport(viewportPos);
    }

    public void postRender() {
        RenderSystem.restoreProjectionMatrix();
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.popPose();
        Lighting.setupForFlatItems();
        RenderSystem.disableBlend();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.disableDepthTest();
    }
}
