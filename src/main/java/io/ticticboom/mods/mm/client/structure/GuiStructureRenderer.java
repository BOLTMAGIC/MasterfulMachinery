package io.ticticboom.mods.mm.client.structure;

import io.ticticboom.mods.mm.client.RenderUtil;
import io.ticticboom.mods.mm.client.blueprint.state.BlueprintStructureViewState;
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
    private final StructureRenderYSliceProcessor ySliceProcessor = new StructureRenderYSliceProcessor();
    private int renderZoomAdjustment = 0;

    @Getter
    private Vector3i structureSize = new Vector3i(0);
    @Getter
    private Vector3i minBound = new Vector3i(0);
    @Getter
    private Vector3i maxBound = new Vector3i(0);
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
        var minX = positions.stream().map(Vec3i::getX).min(Integer::compareTo).orElse(0);
        var minY = positions.stream().map(Vec3i::getY).min(Integer::compareTo).orElse(0);
        var minZ = positions.stream().map(Vec3i::getZ).min(Integer::compareTo).orElse(0);
        minBound = new Vector3i(minX, minY, minZ);

        // max
        var maxX = positions.stream().map(Vec3i::getX).max(Integer::compareTo).orElse(0);
        var maxY = positions.stream().map(Vec3i::getY).max(Integer::compareTo).orElse(0);
        var maxZ = positions.stream().map(Vec3i::getZ).max(Integer::compareTo).orElse(0);
        maxBound = new Vector3i(maxX, maxY, maxZ);

        var extentX = maxX - minX;
        var extentY = maxY - minY;
        var extentZ = maxZ - minZ;

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
            if (!canRenderPart(part)) {
                continue;
            }
            part.part.tick();
            GuiBlockRenderer next = part.part.next();
            next.render(gfx, mouseX, mouseY, viewTransform);
        }
        renderSetup.postRender();
        RenderUtil.resetViewport();
    }
    public void setupViewState(BlueprintStructureViewState state) {
        ySliceProcessor.setShouldSlice(state.isShouldSlice());
        ySliceProcessor.setYSlice(state.getYSlice());
    }

    private boolean canRenderPart(PositionedCyclingBlockRenderer part) {
        return ySliceProcessor.canProcess(part);
    }


    public void resetTransforms() {
        viewTransform.reset();
    }

}