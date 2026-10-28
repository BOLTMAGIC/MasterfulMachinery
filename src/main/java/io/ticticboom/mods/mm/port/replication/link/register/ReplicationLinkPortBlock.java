package io.ticticboom.mods.mm.port.replication.link.register;

import com.hrznstudio.titanium.block_network.INetworkDirectionalConnection;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.datagen.provider.MMBlockstateProvider;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortBlock;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.util.BlockUtils;
import io.ticticboom.mods.mm.util.PortUtils;
import io.ticticboom.mods.mm.util.WorldUtil;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ReplicationLinkPortBlock extends Block implements IPortBlock, EntityBlock, INetworkDirectionalConnection {
    @Getter
    private final PortModel model;
    private final RegistryGroupHolder groupHolder;

    public ReplicationLinkPortBlock(PortModel model, RegistryGroupHolder groupHolder) {
        super(BlockUtils.createBlockProperties(groupHolder.getBlock().getId()));
        this.model = model;
        this.groupHolder = groupHolder;
    }

    @Override
    public void generateModel(MMBlockstateProvider provider) {
        PortUtils.commonGenerateModel(provider, groupHolder, model.input(), Ref.Textures.INPUT_REPLICATION_LINK_PORT_OVERLAY, Ref.Textures.OUTPUT_REPLICATION_LINK_PORT_OVERLAY);
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
            if (d instanceof ReplicationLinkPortBlockEntity link) {
                link.tick();
            }
        };
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        WorldUtil.scheduleNearbyValidation(level, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    public boolean canConnect(Level level, BlockPos pos, BlockState state, Direction direction) {
        return true;
    }

    @Override
    public MutableComponent getName() {
        var name = model.displayName();
        return name != null ? name.copy() : super.getName();
    }
}
