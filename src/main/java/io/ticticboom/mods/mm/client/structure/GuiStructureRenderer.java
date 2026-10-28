package io.ticticboom.mods.mm.client.structure;

import java.util.function.Supplier;
import io.ticticboom.mods.mm.builder.structure.BuildableStructure;
import io.ticticboom.mods.mm.client.RenderUtil;
import io.ticticboom.mods.mm.client.blueprint.state.BlueprintStructureViewState;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

    private static GuiStructureRenderer hovered = null;

    public static GuiStructureRenderer getHovered() {
        return hovered;
    }

    private final Supplier<List<PositionedCyclingBlockRenderer>> partsFactory;
    private List<PositionedCyclingBlockRenderer> parts;
    private Set<PositionedCyclingBlockRenderer> enclosedParts = Set.of();
    private final AutoTransform viewTransform;
    private final GuiRenderEnvSetup renderSetup = new GuiRenderEnvSetup();
    private final StructureRenderYSliceProcessor ySliceProcessor = new StructureRenderYSliceProcessor();
    private final GuiStructureLevel previewWorld = new GuiStructureLevel();
    private int worldGeneration = 0;
    private boolean previewWorldDirty = true;

    private float boundingRadius = 1;

    @Getter
    private Vector3i structureSize = new Vector3i(0);
    @Getter
    private Vector3i minBound = new Vector3i(0);
    @Getter
    private Vector3i maxBound = new Vector3i(0);
    private boolean isInitialized = false;


    public GuiStructureRenderer(StructureModel model) {
        this(new AutoTransform(model), () -> {
            model.layout().setup(model);
            List<PositionedCyclingBlockRenderer> list = new ArrayList<>(new GuiStructureLayout(model.layout()).createBlockRenderers());
            list.add(model.controllerUiRenderer());
            return list;
        });
    }

    private GuiStructureRenderer(AutoTransform viewTransform, Supplier<List<PositionedCyclingBlockRenderer>> partsFactory) {
        this.viewTransform = viewTransform;
        this.partsFactory = partsFactory;
        parts = new ArrayList<>();
    }

    public static GuiStructureRenderer ofBlocks(List<BuildableStructure.Placement> blocks) {
        return new GuiStructureRenderer(new AutoTransform(blocks.stream().map(BuildableStructure.Placement::pos).toList()), () -> {
            List<PositionedCyclingBlockRenderer> list = new ArrayList<>(blocks.size());
            for (BuildableStructure.Placement placement : blocks) {
                GuiBlockRenderer renderer = new GuiBlockRenderer(placement.state());
                renderer.setupAt(placement.pos());
                list.add(new PositionedCyclingBlockRenderer(List.of(renderer), placement.pos()));
            }
            return list;
        });
    }

    public void init() {
        if (!isInitialized) {
            parts = partsFactory.get();
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

        boundingRadius = 0.5f * (float) Math.sqrt(sq(extentX + 1) + sq(extentY + 1) + sq(extentZ + 1));
        viewTransform.setCenter(new Vector3f((minX + maxX) / 2f, (minY + maxY) / 2f, (minZ + maxZ) / 2f));
    }

    private static float sq(int v) {
        return (float) v * v;
    }

    private GuiPos hoverBounds;

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
        this.hoverBounds = viewport;
        renderSetup.setViewportPos(viewport);
    }

    public void setHoverBounds(GuiPos bounds) {
        this.hoverBounds = bounds;
    }

    public void scroll(double delta) {
        viewTransform.zoom(delta);
    }

    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        extractRenderState(gfx, mouseX, mouseY, true);
    }

    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, boolean overView) {
        if (shouldEnsureValidated) {
            StructureManager.validateAllPieces();
            shouldEnsureValidated = false;
        }

        if (hoverBounds != null && hoverBounds.contains(mouseX, mouseY)) {
            hovered = this;
        } else if (hovered == this) {
            hovered = null;
        }

        viewTransform.run(mouseX, mouseY, overView);
        updatePreviewWorld();
        renderSetup.preRender((float) viewTransform.getYRotation(), (float) viewTransform.getXRotation(), boundingRadius, viewTransform.getViewTransform());
        for (PositionedCyclingBlockRenderer part : parts) {
            if (!canRenderPart(part)) {
                continue;
            }
            GuiBlockRenderer next = part.part.next();
            next.extractRenderState(gfx, mouseX, mouseY, viewTransform, previewWorld, worldGeneration);
        }
        gfx.bufferSource().endBatch();
        renderSetup.postRender();
        RenderUtil.resetViewport();
    }

    private void updatePreviewWorld() {
        boolean changed = previewWorldDirty;
        for (PositionedCyclingBlockRenderer part : parts) {
            int before = part.part.getIndex();
            part.part.tick();
            changed |= part.part.getIndex() != before;
        }
        if (!changed) {
            return;
        }
        previewWorldDirty = false;
        previewWorld.clear();
        for (PositionedCyclingBlockRenderer part : parts) {
            if (!ySliceProcessor.canProcess(part)) continue;
            GuiBlockRenderer block = part.part.next();
            previewWorld.put(part.pos, block.getState(), block.getBlockEntity());
        }
        worldGeneration++;
    }

    public void setupViewState(BlueprintStructureViewState state) {
        setYSlice(state.isShouldSlice(), state.getYSlice());
    }

    public void setYSlice(boolean shouldSlice, int ySlice) {
        if (ySliceProcessor.isShouldSlice() != shouldSlice ||
                (shouldSlice && ySliceProcessor.getYSlice() != ySlice)) {
            previewWorldDirty = true;
        }
        ySliceProcessor.setShouldSlice(shouldSlice);
        ySliceProcessor.setYSlice(ySlice);
    }

    public void zoom(double scrollDelta) {
        viewTransform.zoom(scrollDelta);
    }

    private boolean canRenderPart(PositionedCyclingBlockRenderer part) {
        if (!ySliceProcessor.isShouldSlice() && enclosedParts.contains(part)) {
            return false;
        }
        return ySliceProcessor.canProcess(part);
    }

    public void resetTransforms() {
        viewTransform.reset();
    }

}
