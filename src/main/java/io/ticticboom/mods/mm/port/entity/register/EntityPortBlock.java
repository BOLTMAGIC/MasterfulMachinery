package io.ticticboom.mods.mm.port.entity.register;

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
import org.jetbrains.annotations.Nullable;

public class EntityPortBlock extends Block implements IPortBlock, EntityBlock {
    @Getter
    private final PortModel model;
    private final RegistryGroupHolder groupHolder;
    private final boolean isInput;

    public EntityPortBlock(PortModel model, RegistryGroupHolder groupHolder, boolean isInput) {
        super(BlockUtils.createBlockProperties());
        this.model = model;
        this.groupHolder = groupHolder;
        this.isInput = isInput;
    }

    @Override
    public void generateModel(MMBlockstateProvider provider) {
        PortUtils.commonGenerateModel(provider, groupHolder, isInput, Ref.Textures.INPUT_ENTITY_PORT_OVERLAY, Ref.Textures.OUTPUT_ENTITY_PORT_OVERLAY);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return groupHolder.getBe().get().create(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return BlockUtils.commonUse(state, level, pos, player, hitResult, EntityPortBlockEntity.class, null);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> entityType) {
        return (a, b, c, d) -> {
            if (d instanceof EntityPortBlockEntity pbe) {
                pbe.tick();
            }
        };
    }

    @Override
    public MutableComponent getName() {
        var name = model.displayName();
        return name != null ? name.copy() : super.getName();
    }
}
