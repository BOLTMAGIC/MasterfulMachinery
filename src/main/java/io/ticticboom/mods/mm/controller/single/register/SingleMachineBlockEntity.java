package io.ticticboom.mods.mm.controller.single.register;

import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerMenu;
import io.ticticboom.mods.mm.controller.single.SingleMachineSlot;
import io.ticticboom.mods.mm.controller.single.SingleMachines;
// [26.1-pending] import: import io.ticticboom.mods.mm.gateway.GatewayChemicalHandler;
import io.ticticboom.mods.mm.gateway.GatewayEnergyHandler;
import io.ticticboom.mods.mm.gateway.GatewayFluidHandler;
import io.ticticboom.mods.mm.gateway.GatewayItemHandler;
import io.ticticboom.mods.mm.model.ControllerModel;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.fluid.FluidPortStorage;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SingleMachineBlockEntity extends MachineControllerBlockEntity {

    @Getter
    private final List<SingleMachineSlot> slots;
    @Getter
    private final List<IPortStorage> storages = new ArrayList<>();
    private final List<IPortStorage> inputs = new ArrayList<>();
    private final List<IPortStorage> outputs = new ArrayList<>();
    private final RecipeStorages recipeStorages;
    private long changes = 0;

    private final IItemHandler itemHandler = new GatewayItemHandler(() -> inputs, () -> outputs);
    private final IFluidHandler fluidHandler = new GatewayFluidHandler(() -> inputs, () -> outputs);
    private final IEnergyStorage energyHandler = new GatewayEnergyHandler(() -> inputs, () -> outputs);
    private Object chemicalHandler;

    public SingleMachineBlockEntity(ControllerModel model, RegistryGroupHolder groupHolder, BlockPos pos, BlockState state) {
        super(model, groupHolder, pos, state);
        this.slots = SingleMachines.slots(model);
        for (SingleMachineSlot slot : slots) {
            var storage = slot.factory().createPortStorage(this::storageChanged);
            if (storage instanceof FluidPortStorage fluids) {
                fluids.getHandler().setOneFluidPerTank(true);
            }
            storages.add(storage);
            (slot.input() ? inputs : outputs).add(storage);
        }
        this.recipeStorages = new RecipeStorages(inputs, outputs, () -> changes);
    }

    private void storageChanged() {
        changes++;
        markChanged();
    }

    @Override
    protected RecipeStorages collectStorages(Rotation rotation) {
        return recipeStorages;
    }

    public IItemHandler itemHandler() {
        return itemHandler;
    }

    public IFluidHandler fluidHandler() {
        return fluidHandler;
    }

    public IEnergyStorage energyHandler() {
        return energyHandler;
    }

    public Object chemicalHandler() {
        if (chemicalHandler == null) {
            // [26.1-pending] Mekanism has no 26.1.2 release yet: no chemical handler (only MekCapabilities asks for it)
            // chemicalHandler = new GatewayChemicalHandler(() -> inputs, () -> outputs);
            return null;
        }
        return chemicalHandler;
    }

    public void dropContents(Level level, BlockPos pos) {
        for (IPortStorage storage : storages) {
            if (storage instanceof ItemPortStorage items) {
                Containers.dropContents(level, pos, items.getHandler().getStacks());
            }
        }
    }

    public void openSlots(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider((windowId, inv, p) -> new SingleMachineMenu(windowId, inv, this), getName()),
                buf -> buf.writeBlockPos(getBlockPos()));
    }

    public void openController(ServerPlayer player) {
        player.openMenu(this, buf -> MachineControllerMenu.writeOpenData(buf, this));
    }

    @Override
    protected void saveLegacy(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.saveLegacy(tag, registries);
        var slotsTag = new CompoundTag();
        for (int i = 0; i < slots.size(); i++) {
            slotsTag.put(slots.get(i).id(), storages.get(i).save(new CompoundTag(), registries));
        }
        tag.put("Slots", slotsTag);
    }

    @Override
    protected void loadLegacy(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        super.loadLegacy(tag, registries);
        var slotsTag = tag.getCompoundOrEmpty("Slots");
        for (int i = 0; i < slots.size(); i++) {
            if (slotsTag.contains(slots.get(i).id())) {
                storages.get(i).load(slotsTag.getCompoundOrEmpty(slots.get(i).id()), registries);
            }
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            dropContents(level, pos);
        }
        super.preRemoveSideEffects(pos, state);
    }
}
