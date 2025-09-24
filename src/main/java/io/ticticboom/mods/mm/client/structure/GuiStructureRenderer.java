package io.ticticboom.mods.mm.client.structure;

import io.ticticboom.mods.mm.client.RenderUtil;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Vec3i;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;

public class GuiStructureRenderer {
    public static boolean shouldEnsureValidated = false;
    private final StructureModel model;
    private List<PositionedCyclingBlockRenderer> parts;
    private final GuiStructureLayout guiLayout;

    private final AutoTransform viewTransform;
    private final GuiRenderEnvSetup renderSetup = new GuiRenderEnvSetup();
    private int renderZoomAdjustment = 0;

    @Getter
    private Vector3i structureSize = new Vector3i(0, 0, 0);
    private boolean isInitialized = false;


    public GuiStructureRenderer(StructureModel model) {
        this.model = model;
        viewTransform = new AutoTransform(model);
        guiLayout = new GuiStructureLayout(model.layout());
        parts = new ArrayList<>();
    }

    public void init() {
        if (!isInitialized) {
            model.layout().setup(model);
            parts = guiLayout.createBlockRenderers();
            parts.add(model.controllerUiRenderer());
            for (PositionedCyclingBlockRenderer part : parts) {
                part.part.setInterval(60);
            }
            getExtents();
            isInitialized = true;
        }
    }

    private void getExtents() {
        var positions = parts.stream().map(x -> x.pos).toList();
        // min
        var minX = Math.abs(positions.stream().map(Vec3i::getX).min(Integer::compareTo).orElse(0));
        var minY = Math.abs(positions.stream().map(Vec3i::getY).min(Integer::compareTo).orElse(0));
        var minZ = Math.abs(positions.stream().map(Vec3i::getZ).min(Integer::compareTo).orElse(0));

        // max
        var maxX = Math.abs(positions.stream().map(Vec3i::getX).max(Integer::compareTo).orElse(0));
        var maxY = Math.abs(positions.stream().map(Vec3i::getY).max(Integer::compareTo).orElse(0));
        var maxZ = Math.abs(positions.stream().map(Vec3i::getZ).max(Integer::compareTo).orElse(0));

        var extentX = maxX + minX;
        var extentY = maxY + minY;
        var extentZ = maxZ + minZ;

        structureSize = new Vector3i(extentX, extentY, extentZ);

        renderZoomAdjustment = Math.max(extentX, Math.max(extentY, extentZ));
    }

    public void setViewport(GuiPos viewport) {
        renderSetup.setViewportPos(viewport);
    }

    public void render(GuiGraphics gfx, int mouseX, int mouseY) {
        if (shouldEnsureValidated) {
            StructureManager.validateAllPieces();
            shouldEnsureValidated = false;
        }

        viewTransform.run(mouseX, mouseY);
        renderSetup.preRender((float) viewTransform.getYRotation(), (float) viewTransform.getXRotation(), renderZoomAdjustment, viewTransform.getViewTransform());
        for (PositionedCyclingBlockRenderer part : parts) {
            part.part.tick();
            GuiBlockRenderer next = part.part.next();
            next.render(gfx, mouseX, mouseY, viewTransform);
        }
        renderSetup.postRender();
        RenderUtil.resetViewport();
    }

    public void resetTransforms() {
        viewTransform.reset();
    }

}