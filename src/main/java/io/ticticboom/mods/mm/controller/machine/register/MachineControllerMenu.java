package io.ticticboom.mods.mm.controller.machine.register;

import io.ticticboom.mods.mm.controller.IControllerBlockEntity;
import io.ticticboom.mods.mm.menu.MMContainerMenu;
import io.ticticboom.mods.mm.model.ControllerModel;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.util.BlockUtils;
import io.ticticboom.mods.mm.util.MenuUtils;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.DataSlot;

import io.ticticboom.mods.mm.structure.StructureDiagnosis;

import java.util.List;

public class MachineControllerMenu extends MMContainerMenu {

    @Getter
    private final ControllerModel model;
    private final Inventory inv;
    @Getter
    private final IControllerBlockEntity be;
    /** Ports of the formed machine, inputs first; sent by the server when the screen opens (client only). */
    @Getter
    private List<BlockPos> portPositions = List.of();
    /** Why the multiblock isn't formed; sent by the server when the screen opens (client only). */
    @Getter
    private StructureDiagnosis diagnosis = StructureDiagnosis.NONE;

    public MachineControllerMenu(ControllerModel model, RegistryGroupHolder groupHolder, int windowId, Inventory inv,
            IControllerBlockEntity be) {
        super(groupHolder.getMenu().get(), groupHolder.getBlock().get(), windowId, MenuUtils.createAccessFromBlockEntity(be.getBlockEntity()), 0);
        this.model = model;
        this.inv = inv;
        this.be = be;
        BlockUtils.setupPlayerInventory(this, inv, -1, -1);

    }

    public MachineControllerMenu(ControllerModel model, RegistryGroupHolder groupHolder, int windowId, Inventory inv,
            FriendlyByteBuf buf) {
        this(model, groupHolder, windowId, inv,
                (IControllerBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()));
        this.portPositions = buf.readList(FriendlyByteBuf::readBlockPos);
        this.diagnosis = StructureDiagnosis.read(buf);
    }

    /**
     * Written by the server when the screen opens: the controller position, its formed machine's ports,
     * then what keeps the machine from forming.
     */
    public static void writeOpenData(FriendlyByteBuf buf, MachineControllerBlockEntity controller) {
        buf.writeBlockPos(controller.getBlockPos());
        var structure = controller.getStructure();
        List<BlockPos> ports = structure == null || controller.getLevel() == null
                ? List.of() : structure.getPortPositions(controller.getLevel(), controller.getBlockPos());
        buf.writeCollection(ports, FriendlyByteBuf::writeBlockPos);
        controller.diagnoseStructure().write(buf);
    }
}
