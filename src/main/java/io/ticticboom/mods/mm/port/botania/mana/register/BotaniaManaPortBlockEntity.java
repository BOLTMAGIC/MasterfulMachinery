package io.ticticboom.mods.mm.port.botania.mana.register;

import io.ticticboom.mods.mm.util.LegacyNbt;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.botania.mana.BotaniaManaPortStorage;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Botania has no 1.21.1 release, so this block entity is stubbed: the mana storage, menu wiring
 * and serialisation are intact, but every hook into Botania is removed. MMPortRegistry only
 * registers the mana port when Botania is loaded, so on 1.21.1 this class is unreachable.
 *
 * <p>The restored version lives on the feature/botania-1.21.1 branch, built against a local
 * Botania build in libs/. Merge that once Botania publishes a 1.21.1 artifact.
 *
 * <p>The 1.21.1 API differs from the 1.20.1 one this was written against: ManaPool no longer has
 * getColor/setColor, ManaBlockType.POOL and ManaNetworkHandler.isPoolIn are gone because pools
 * are not part of the mana network any more, capabilities come from
 * BotaniaNeoForgeCapabilities.getBlockApiLookupById rather than being declared, and
 * WandHUD.renderHUD takes a Window and Font instead of a Minecraft.
 */
public class BotaniaManaPortBlockEntity extends BlockEntity implements IPortBlockEntity {

    private final PortModel model;
    private final RegistryGroupHolder groupHolder;
    private final BotaniaManaPortStorage storage;

    public BotaniaManaPortBlockEntity(PortModel model, RegistryGroupHolder groupHolder, BlockPos pos, BlockState state) {
        super(groupHolder.getBe().get(), pos, state);
        this.model = model;
        this.groupHolder = groupHolder;
        this.storage = (BotaniaManaPortStorage) model.config().createPortStorage(this::setChanged);
    }

    @Override
    public PortModel getModel() {
        return model;
    }

    @Override
    public IPortStorage getStorage() {
        return storage;
    }

    @Override
    public boolean isInput() {
        return model.input();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("port.mm.botania_mana.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return null;
    }

    // --- ManaPool implementation, kept unannotated while Botania is unavailable ---

    public boolean isOutputtingPower() {
        return !this.model.input();
    }

    public int getMaxMana() {
        return storage.getCapacity();
    }

    public Optional<DyeColor> getColor() {
        return Optional.of(DyeColor.CYAN);
    }

    public void setColor(Optional<DyeColor> optional) {
    }

    public Level getManaReceiverLevel() {
        return level;
    }

    public BlockPos getManaReceiverPos() {
        return this.getBlockPos();
    }

    public int getCurrentMana() {
        return storage.getStored();
    }

    public boolean isFull() {
        return storage.getStored() >= storage.getCapacity();
    }

    public void receiveMana(int i) {
        if (i < 0) {
            storage.extractMana(i, false);
        } else {
            storage.receiveMana(i, false);
        }
    }

    public boolean canReceiveManaFromBursts() {
        return this.model.input();
    }

    // --- serialisation, unaffected by Botania ---

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        var tag = new CompoundTag();
        saveLegacy(tag, LegacyNbt.registries(level));
        LegacyNbt.write(output, tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        loadLegacy(LegacyNbt.read(input), input.lookup());
    }

    protected void saveLegacy(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put(Ref.NBT_STORAGE_KEY, storage.save(new CompoundTag(), registries));
    }

    protected void loadLegacy(CompoundTag tag, HolderLookup.Provider registries) {
        storage.load(tag.getCompoundOrEmpty(Ref.NBT_STORAGE_KEY), registries);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level == null || level.isClientSide()) {
            return;
        }
        level.sendBlockUpdated(getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        saveLegacy(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
