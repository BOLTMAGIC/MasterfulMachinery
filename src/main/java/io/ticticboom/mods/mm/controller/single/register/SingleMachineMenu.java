package io.ticticboom.mods.mm.controller.single.register;

import io.ticticboom.mods.mm.menu.MMContainerMenu;
import io.ticticboom.mods.mm.model.ControllerModel;
import io.ticticboom.mods.mm.port.item.ItemPortContainer;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.util.BlockUtils;
import io.ticticboom.mods.mm.util.MenuUtils;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class SingleMachineMenu extends MMContainerMenu {

    @Getter
    private final ControllerModel model;
    @Getter
    private final SingleMachineBlockEntity machine;
    @Getter
    private final SingleMachineLayout layout;

    public SingleMachineMenu(int windowId, Inventory inv, SingleMachineBlockEntity machine) {
        super(MMRegisters.SINGLE_MACHINE_MENU.get(), machine.getBlockState().getBlock(), windowId, MenuUtils.createAccessFromBlockEntity(machine), itemSlots(machine));
        this.model = machine.getModel();
        this.machine = machine;
        this.layout = SingleMachineLayout.of(machine);
        for (var group : layout.groups()) {
            if (group.storage() instanceof ItemPortStorage items) {
                var container = new ItemPortContainer(items.getHandler());
                for (int slot = 0; slot < items.getHandler().getSlots(); slot++) {
                    addSlot(new SingleMachineItemSlot(container, slot, group.cellX(slot) + 1, group.cellY(slot) + 1, group.input()));
                }
            }
        }
        BlockUtils.setupPlayerInventory(this, inv, layout.inventoryXOffset(), layout.inventoryYOffset());
    }

    public SingleMachineMenu(int windowId, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(windowId, inv, (SingleMachineBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    private static int itemSlots(SingleMachineBlockEntity machine) {
        int count = 0;
        for (var storage : machine.getStorages()) {
            if (storage instanceof ItemPortStorage items) {
                count += items.getHandler().getSlots();
            }
        }
        return count;
    }
}
