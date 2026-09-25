package io.ticticboom.mods.mm.client.structure;

import io.ticticboom.mods.mm.client.RenderUtil;
import io.ticticboom.mods.mm.client.blueprint.state.BlueprintStructureViewState;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GuiStructureRenderer {
    public static boolean shouldEnsureValidated = false;
    private final StructureModel model;
    private List<PositionedCyclingBlockRenderer> parts;
    // parts fully enclosed by opaque cubes; they can't be seen in the full (unsliced) view
    private Set<PositionedCyclingBlockRenderer> enclosedParts = Set.of();
    private final GuiStructureLayout guiLayout;
    private final AutoTransform viewTransform;
    private final GuiRenderEnvSetup renderSetup = new GuiRenderEnvSetup();
    private final StructureRenderYSliceProcessor ySliceProcessor = new StructureRenderYSliceProcessor();
    // the preview's blocks as a small world for connected-texture models; rebuilt when a cycling piece changes
    private final GuiStructureLevel previewWorld = new GuiStructureLevel();
    private int worldGeneration = 0;

    // radius of the structure's bounding sphere, in blocks
    private float boundingRadius = 1;

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
                part.part.setInterval(20);
            }
            getExtents();
            findEnclosedParts();
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

        // extents are between block origins, so each axis spans extent + 1 blocks
        boundingRadius = 0.5f * (float) Math.sqrt(sq(extentX + 1) + sq(extentY + 1) + sq(extentZ + 1));
        // rotate around the middle of the structure rather than the controller
        viewTransform.setCenter(new Vector3f((minX + maxX) / 2f, (minY + maxY) / 2f, (minZ + maxZ) / 2f));
    }

    private static float sq(int v) {
        return (float) v * v;
    }

    private void findEnclosedParts() {
        var opaquePositions = new HashSet<BlockPos>();
        for (PositionedCyclingBlockRenderer part : parts) {
            if (part.part.getPart().stream().allMatch(GuiBlockRenderer::isOpaqueCube)) {
                opaquePositions.add(part.pos);
            }
        }
        var enclosed = new HashSet<PositionedCyclingBlockRenderer>();
        for (PositionedCyclingBlockRenderer part : parts) {
            boolean surrounded = true;
            for (Direction dir : Direction.values()) {
                if (!opaquePositions.contains(part.pos.relative(dir))) {
                    surrounded = false;
                    break;
                }
            }
            if (surrounded) {
                enclosed.add(part);
            }
        }
        enclosedParts = enclosed;
    }

    public void setViewport(GuiPos viewport) {
        renderSetup.setViewportPos(viewport);
    }

    public void render(GuiGraphics gfx, int mouseX, int mouseY) {
        render(gfx, mouseX, mouseY, true);
    }

    /** @param hovered whether the pointer is over this view, where mouse drags may start */
    public void render(GuiGraphics gfx, int mouseX, int mouseY, boolean hovered) {
        if (shouldEnsureValidated) {
            StructureManager.validateAllPieces();
            shouldEnsureValidated = false;
        }

        viewTransform.run(mouseX, mouseY, hovered);
        updatePreviewWorld();
        renderSetup.preRender((float) viewTransform.getYRotation(), (float) viewTransform.getXRotation(), boundingRadius, viewTransform.getViewTransform());
        for (PositionedCyclingBlockRenderer part : parts) {
            if (!canRenderPart(part)) {
                continue;
            }
            GuiBlockRenderer next = part.part.next();
            next.render(gfx, mouseX, mouseY, viewTransform, previewWorld, worldGeneration);
        }
        // one flush for the whole structure instead of one draw call per block
        gfx.bufferSource().endBatch();
        renderSetup.postRender();
        RenderUtil.resetViewport();
    }
    /**
     * Advances the cycling pieces and, when any of them changed, rebuilds the preview world the block models look at.
     * Every piece is ticked, shown or not, so a layer view sees the same neighbours as the full view.
     */
    private void updatePreviewWorld() {
        boolean changed = worldGeneration == 0;
        for (PositionedCyclingBlockRenderer part : parts) {
            int before = part.part.getIndex();
            part.part.tick();
            changed |= part.part.getIndex() != before;
        }
        if (!changed) {
            return;
        }
        previewWorld.clear();
        for (PositionedCyclingBlockRenderer part : parts) {
            GuiBlockRenderer block = part.part.next();
            previewWorld.put(part.pos, block.getState(), block.getBlockEntity());
        }
        worldGeneration++;
    }

    public void setupViewState(BlueprintStructureViewState state) {
        setYSlice(state.isShouldSlice(), state.getYSlice());
    }

    public void setYSlice(boolean shouldSlice, int ySlice) {
        ySliceProcessor.setShouldSlice(shouldSlice);
        ySliceProcessor.setYSlice(ySlice);
    }

    private boolean canRenderPart(PositionedCyclingBlockRenderer part) {
        if (!ySliceProcessor.isShouldSlice() && enclosedParts.contains(part)) {
            return false;
        }
        return ySliceProcessor.canProcess(part);
    }


    public void zoom(double scrollDelta) {
        viewTransform.zoom(scrollDelta);
    }

    public void resetTransforms() {
        viewTransform.reset();
    }

}