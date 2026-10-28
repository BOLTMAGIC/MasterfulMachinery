package io.ticticboom.mods.mm.port.pneumaticcraft.air.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.pneumaticcraft.air.PneumaticAirPortStorage;
import io.ticticboom.mods.mm.port.pneumaticcraft.air.PneumaticAirPortStorageModel;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.pressure.PressureTier;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerMachine;
import me.desht.pneumaticcraft.api.upgrade.PNCUpgrade;
import me.desht.pneumaticcraft.common.block.entity.AbstractAirHandlingBlockEntity;
import me.desht.pneumaticcraft.common.block.entity.AbstractPneumaticCraftBlockEntity;
import me.desht.pneumaticcraft.common.block.entity.AbstractTickingBlockEntity;
import me.desht.pneumaticcraft.common.capabilities.MachineAirHandler;
import me.desht.pneumaticcraft.common.inventory.handler.BaseItemStackHandler;
import me.desht.pneumaticcraft.common.item.AssemblyProgramItem;
import me.desht.pneumaticcraft.common.upgrades.ModUpgrades;
import me.desht.pneumaticcraft.common.util.DirectionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;

public class PneumaticAirPortBlockEntity extends AbstractTickingBlockEntity implements IPortBlockEntity{
    private final PortModel model;
    private final RegistryGroupHolder groupHolder;
    private final boolean isInput;

    private final PneumaticAirPortStorage storage;

    private final ItemStackHandler itemHandler = new BaseItemStackHandler(this, 1) {
        public boolean isItemValid(int slot, ItemStack itemStack) {
            return itemStack.isEmpty() || itemStack.getItem() instanceof AssemblyProgramItem;
        }
    };

    public PneumaticAirPortBlockEntity(PortModel model, RegistryGroupHolder groupHolder, boolean isInput, BlockPos pos, BlockState state) {
        super(groupHolder.getBe().get(), pos, state);
        this.model = model;
        this.groupHolder = groupHolder;
        this.isInput = isInput;

        storage = (PneumaticAirPortStorage) model.config().createPortStorage(this::setChanged);
    }


    @Override
    public IPortStorage getStorage() {
        return storage;
    }

    public PneumaticAirPortStorageModel getStorageModel() {
        return (PneumaticAirPortStorageModel) storage.getStorageModel();
    }

    @Override
    public boolean isInput() {
        return isInput;
    }

    @Override
    public PortModel getModel() {
        return model;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("port.mm.pneumaticcraft_air.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
        return new PneumaticAirPortMenu(model, groupHolder, isInput, windowId, inv, this);
    }


    @Override
    public boolean hasMenu() {
        return true;
    }
    public IItemHandler getPrimaryInventory() {
        return null;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setChanged() {
        if (level == null || level.isClientSide()){
            return;
        }
        super.setChanged();
        level.sendBlockUpdated(getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
    }



    @Override
    public void saveLegacy(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put(Ref.NBT_STORAGE_KEY, storage.save(new CompoundTag(), registries));
        super.saveLegacy(tag, registries);
    }

    @Override
    public void loadLegacy(CompoundTag tag, HolderLookup.Provider registries) {
        storage.load(tag.getCompound(Ref.NBT_STORAGE_KEY), registries);
        super.loadLegacy(tag, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        saveLegacy(tag, registries);
        return tag;
    }




    public void onNeighborBlockUpdate(BlockPos fromPos) {
        this.setChanged();
        super.onNeighborBlockUpdate(fromPos);
        storage.onNeighborBlockUpdate();
    }

    public void tickCommonPre() {
        super.tickCommonPre();
        this.storage.airHandlerMap.keySet().forEach((handler) -> {
            handler.tick(this);
        });
    }


}
