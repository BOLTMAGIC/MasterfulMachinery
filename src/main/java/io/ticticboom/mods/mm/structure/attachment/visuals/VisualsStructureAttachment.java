package io.ticticboom.mods.mm.structure.attachment.visuals;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.piece.StructurePieceSetupMetadata;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.fluid.FluidPortStorage;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.structure.attachment.StructureAttachment;
import io.ticticboom.mods.mm.structure.layout.PositionedLayoutPiece;
import io.ticticboom.mods.mm.structure.layout.StructureKeyChar;
import io.ticticboom.mods.mm.structure.layout.StructureLayout;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public class VisualsStructureAttachment extends StructureAttachment {
    @Getter
    private final List<FluidVolumeVisual> fluidVolumes;

    public VisualsStructureAttachment(List<FluidVolumeVisual> fluidVolumes) {
        this.fluidVolumes = fluidVolumes;
    }

    @Override
    public Identifier getId() {
        return Ref.StructureAttachments.VISUALS;
    }

    @Override
    public void validate(StructurePieceSetupMetadata meta) {
        var layout = meta.model().layout();
        for (FluidVolumeVisual volume : fluidVolumes) {
            if (volume.getPort() != null && !isKey(layout, volume.getPort())) {
                Ref.LOG.warn("Structure {}: fluid_volume port '{}' is not a key in the layout", meta.structureId(), volume.getPort());
            }
            if (volume.getArea() != null) {
                if (!isKey(layout, volume.getArea())) {
                    Ref.LOG.warn("Structure {}: fluid_volume area '{}' is not a key in the layout", meta.structureId(), volume.getArea());
                }
                continue;
            }
            var min = volume.min();
            var max = volume.max();
            boolean open = false;
            for (int x = min.getX(); x <= max.getX() && !open; x++) {
                for (int y = min.getY(); y <= max.getY() && !open; y++) {
                    for (int z = min.getZ(); z <= max.getZ() && !open; z++) {
                        open = layout.getCharGrid().valueFromBottom(x, y, z) == ' ';
                    }
                }
            }
            if (open) {
                Ref.LOG.warn("Structure {}: fluid_volume from {} to {} covers spaces in the layout, so blocks placed there will hide the fluid. Use a key that requires minecraft:air.",
                        meta.structureId(), volume.getFrom().toShortString(), volume.getTo().toShortString());
            }
        }
    }

    private static boolean isKey(StructureLayout layout, String key) {
        return key.length() == 1 && layout.getPieces().containsKey(new StructureKeyChar(key.charAt(0)));
    }

    public List<FluidVolumeState> snapshot(Level level, BlockPos controllerPos, StructureModel structure, Rotation rotation, RecipeStorages storages) {
        var layout = structure.layout();
        var result = new ArrayList<FluidVolumeState>(fluidVolumes.size());
        for (FluidVolumeVisual volume : fluidVolumes) {
            var tanks = volume.getPort() == null ? allFluidStorages(storages) : keyedFluidStorages(level, controllerPos, layout, rotation, volume.getPort());
            FluidStack shown = FluidStack.EMPTY;
            long amount = 0;
            long capacity = 0;
            for (FluidPortStorage tank : tanks) {
                var handler = tank.getHandler();
                for (int i = 0; i < handler.getTanks(); i++) {
                    capacity += handler.getTankCapacity(i);
                    var stack = handler.getFluidInTank(i);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    if (shown.isEmpty()) {
                        shown = stack.copyWithAmount(1);
                    }
                    if (FluidStack.isSameFluidSameComponents(shown, stack)) {
                        amount += stack.getAmount();
                    }
                }
            }
            float fill = capacity <= 0 ? 0f : (float) Math.min(1.0, (double) amount / capacity);
            result.add(new FluidVolumeState(volume.relativeCells(layout, rotation), shown, fill));
        }
        return result;
    }

    private static List<FluidPortStorage> allFluidStorages(RecipeStorages storages) {
        var result = new ArrayList<FluidPortStorage>(storages.getInputStorages(FluidPortStorage.class));
        result.addAll(storages.getOutputStorages(FluidPortStorage.class));
        return result;
    }

    private static List<FluidPortStorage> keyedFluidStorages(Level level, BlockPos controllerPos, StructureLayout layout, Rotation rotation, String key) {
        var result = new ArrayList<FluidPortStorage>();
        for (PositionedLayoutPiece piece : layout.getRotatedPositionedPieces().get(rotation)) {
            if (!key.equals(piece.piece().valueId())) {
                continue;
            }
            if (level.getBlockEntity(piece.findAbsolutePos(controllerPos)) instanceof IPortBlockEntity port
                    && port.getStorage() instanceof FluidPortStorage storage) {
                result.add(storage);
            }
        }
        return result;
    }
}
