package io.ticticboom.mods.mm.port.item.register;

import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.item.ItemHandlerPairing;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.port.item.ItemPortStorageModel;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;

public class ItemPortBlockEntity extends AbstractPortBlockEntity {
    private final RegistryGroupHolder groupHolder;
    private final PortModel model;

    private final ItemPortStorage storage;

    @Getter
    private final boolean input;

    private final boolean shouldAutoPush;

    private final HashMap<BlockPos, ItemHandlerPairing> autoPushNeighbors = new HashMap<>();
    private final ArrayList<BlockPos> neighborsPaired = new ArrayList<>();

    public ItemPortBlockEntity(RegistryGroupHolder groupHolder, PortModel model, boolean input, BlockPos pos,
                               BlockState state) {
        super(groupHolder.getBe().get(), pos, state);
        this.groupHolder = groupHolder;
        this.model = model;
        storage = (ItemPortStorage) model.config().createPortStorage(this::setChanged);
        this.input = input;
        this.shouldAutoPush = !input && ((ItemPortStorageModel) storage.getStorageModel()).autoPush().get();
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return storage.getCapability(cap);
    }

    @Override
    public IPortStorage getStorage() {
        return storage;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Item Port");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new ItemPortMenu(model, groupHolder, input, i, inventory, this);
    }

    @Override
    public PortModel getModel() {
        return model;
    }

    public void tick() {
        if (level.isClientSide) {
            return;
        }

        if (shouldAutoPush) {
            for (var cap : autoPushNeighbors.values()) {
                cap.attemptTransfer();
            }
        }
    }

    public void tryAddNeighboringHandlers() {
        if (!shouldAutoPush) {
            return;
        }

        if (!hasLevel()) {
            return;
        }

        for (Direction direction : Direction.values()) {
            BlockPos otherPos = getBlockPos().relative(direction, 1);
            tryAddNeighborHandler(otherPos);
        }
    }

    @Override
    public void onLoad() {
        tryAddNeighboringHandlers();
    }

    private void tryAddNeighborHandler(BlockPos neighborPos) {
        BlockEntity neighborBe = level.getBlockEntity(neighborPos);
        if (neighborBe == null) {
            return;
        }

        var valid = canAddAsNeighbor(neighborPos, neighborBe);
        if (!valid) {
            return;
        }

        LazyOptional<IItemHandler> neighborCap = neighborBe.getCapability(MMCapabilities.ITEM);
        if (!neighborCap.isPresent()) {
            return;
        }

        if (autoPushNeighbors.containsKey(neighborPos)) {
            ItemHandlerPairing pairing = autoPushNeighbors.get(neighborPos);
            pairing.setToHandler(neighborCap);
        } else {
            LazyOptional<IItemHandler> capability = this.getCapability(MMCapabilities.ITEM);
            autoPushNeighbors.put(neighborPos, new ItemHandlerPairing(capability, neighborCap));
        }
    }

    private boolean canAddAsNeighbor(BlockPos pos, BlockEntity be) {
        if (be instanceof IPortBlockEntity pbe) {
            return pbe.isInput();
        }
        return true;
    }
}
