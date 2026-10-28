package io.ticticboom.mods.mm.port.botania.mana.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.datagen.provider.MMBlockstateProvider;
import net.minecraft.network.chat.MutableComponent;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortBlock;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.util.BlockUtils;
import io.ticticboom.mods.mm.util.PortUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BotaniaManaPortBlock extends Block implements IPortBlock, EntityBlock {

    private final PortModel model;
    private final RegistryGroupHolder groupHolder;

    public BotaniaManaPortBlock(PortModel model, RegistryGroupHolder groupHolder) {
        super(BlockUtils.createBlockProperties(groupHolder.getBlock().getId()));
        this.model = model;
        this.groupHolder = groupHolder;
    }

    @Override
    public void generateModel(MMBlockstateProvider provider) {
        PortUtils.commonGenerateModel(provider, groupHolder, model.input(), Ref.Textures.INPUT_BOTANIA_MANA_PORT_OVERLAY, Ref.Textures.OUTPUT_BOTANIA_MANA_PORT_OVERLAY);
    }

    @Override
    public PortModel getModel() {
        return model;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return groupHolder.getBe().get().create(blockPos, blockState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_153212_, BlockState p_153213_, BlockEntityType<T> p_153214_) {
        return (a, b, c, d) -> {
            if (d instanceof BotaniaManaPortBlockEntity bm) {
                // tick() drove Botania mana-network registration and is stubbed out; see
                // BotaniaManaPortBlockEntity for what to restore.
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
