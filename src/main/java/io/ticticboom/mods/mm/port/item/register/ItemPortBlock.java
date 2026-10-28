package io.ticticboom.mods.mm.port.item.register;

import io.ticticboom.mods.mm.networklink.NetworkLink;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.datagen.provider.MMBlockstateProvider;
import net.minecraft.network.chat.MutableComponent;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortBlock;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.util.BlockUtils;
import io.ticticboom.mods.mm.util.PortUtils;
import io.ticticboom.mods.mm.util.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemPortBlock extends Block implements IPortBlock, EntityBlock {
    private final PortModel model;
    private final RegistryGroupHolder groupHolder;
    private final boolean isInput;

    public ItemPortBlock(PortModel model, RegistryGroupHolder groupHolder, boolean isInput) {
        super(BlockUtils.createBlockProperties());
        this.model = model;
        this.groupHolder = groupHolder;
        this.isInput = isInput;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return BlockUtils.commonUse(state, level, pos, player, hitResult, ItemPortBlockEntity.class, null);
    }

    @Override
    public PortModel getModel() {
        return model;
    }

    @Override
    public void generateModel(MMBlockstateProvider provider) {
        PortUtils.commonGenerateModel(provider, groupHolder, isInput, Ref.Textures.INPUT_ITEM_PORT_OVERLAY, Ref.Textures.OUTPUT_ITEM_PORT_OVERLAY);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return groupHolder.getBe().get().create(blockPos, blockState);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        WorldUtil.scheduleNearbyValidation(level, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> entityType) {
        return (a, b, c, d) -> {
            if (d instanceof ItemPortBlockEntity pbe) {
                pbe.tick();
            }
        };
    }

    /**
     * Matches the block item: the generated block.mm.<id> entry loses a pack-supplied translation
     * key and hardcodes the English Input/Output suffix, so the model name is used directly.
     */
    @Override
    public MutableComponent getName() {
        var name = model.displayName();
        return name != null ? name.copy() : super.getName();
    }
}
