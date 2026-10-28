package io.ticticboom.mods.mm.port.projecte.emc.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.datagen.provider.MMBlockstateProvider;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortBlock;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.util.BlockUtils;
import io.ticticboom.mods.mm.util.PortUtils;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class ProjectEEmcPortBlock extends Block implements IPortBlock, EntityBlock {

    public static final int MAX_FILL = 4;
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, MAX_FILL);

    @Getter
    private final PortModel model;
    private final RegistryGroupHolder groupHolder;

    public ProjectEEmcPortBlock(PortModel model, RegistryGroupHolder groupHolder) {
        super(BlockUtils.createBlockProperties(groupHolder.getBlock().getId()));
        this.model = model;
        this.groupHolder = groupHolder;
        registerDefaultState(stateDefinition.any().setValue(FILL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILL);
    }

    @Override
    public void generateModel(MMBlockstateProvider provider) {
        PortUtils.fillStageGenerateModel(provider, groupHolder, model.input(), Ref.Textures.INPUT_PROJECTE_EMC_PORT_OVERLAY, Ref.Textures.OUTPUT_PROJECTE_EMC_PORT_OVERLAY, FILL, MAX_FILL);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return BlockUtils.commonUse(state, level, pos, player, hitResult, ProjectEEmcPortBlockEntity.class, null);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return groupHolder.getBe().get().create(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> entityType) {
        if (level.isClientSide()) {
            return null;
        }
        return (a, b, c, d) -> {
            if (d instanceof ProjectEEmcPortBlockEntity port) {
                port.tick();
            }
        };
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ProjectEEmcPortBlockEntity port) {
            var stack = port.getEmcStorage().getKlein().getStackInSlot(0);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public MutableComponent getName() {
        var name = model.displayName();
        return name != null ? name.copy() : super.getName();
    }
}
