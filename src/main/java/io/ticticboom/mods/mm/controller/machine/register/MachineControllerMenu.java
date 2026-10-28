package io.ticticboom.mods.mm.controller.machine.register;

import io.ticticboom.mods.mm.controller.IControllerBlockEntity;
import io.ticticboom.mods.mm.menu.MMContainerMenu;
import io.ticticboom.mods.mm.model.ControllerModel;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.structure.StructureDiagnosis;
import io.ticticboom.mods.mm.util.BlockUtils;
import io.ticticboom.mods.mm.util.MenuUtils;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class MachineControllerMenu extends MMContainerMenu {

    @Getter
    private final ControllerModel model;
    @Getter
    private final IControllerBlockEntity be;
    @Getter
    private List<BlockPos> portPositions = List.of();
    @Getter
    private StructureDiagnosis diagnosis = StructureDiagnosis.NONE;

    public MachineControllerMenu(ControllerModel model, RegistryGroupHolder groupHolder, int windowId, Inventory inv,
            IControllerBlockEntity be) {
        super(groupHolder.getMenu().get(), groupHolder.getBlock().get(), windowId, MenuUtils.createAccessFromBlockEntity(be.getBlockEntity()), 0);
        this.model = model;
        this.be = be;
        BlockUtils.setupPlayerInventory(this, inv, -1, -1);
    }

    public MachineControllerMenu(ControllerModel model, RegistryGroupHolder groupHolder, int windowId, Inventory inv,
            RegistryFriendlyByteBuf buf) {
        this(model, groupHolder, windowId, inv,
                (IControllerBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()));
        this.portPositions = buf.readList(BlockPos.STREAM_CODEC);
        this.diagnosis = StructureDiagnosis.read(buf);
    }

    public static void writeOpenData(RegistryFriendlyByteBuf buf, MachineControllerBlockEntity controller) {
        buf.writeBlockPos(controller.getBlockPos());
        buf.writeCollection(controller.getPortPositions(), BlockPos.STREAM_CODEC);
        controller.diagnoseStructure().write(buf);
    }
}
