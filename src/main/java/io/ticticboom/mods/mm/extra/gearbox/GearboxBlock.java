package io.ticticboom.mods.mm.extra.gearbox;

import io.ticticboom.mods.mm.util.DisplayNameUtil;
import net.minecraft.network.chat.MutableComponent;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.extra.ExtraBlockModel;
import io.ticticboom.mods.mm.extra.IExtraBlock;
import io.ticticboom.mods.mm.datagen.provider.MMBlockstateProvider;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.util.BlockUtils;
import net.minecraft.world.level.block.Block;

public class GearboxBlock extends Block implements IExtraBlock {

    private final ExtraBlockModel model;
    private final RegistryGroupHolder groupHolder;

    public GearboxBlock(ExtraBlockModel model, RegistryGroupHolder groupHolder) {
        super(BlockUtils.createBlockProperties(groupHolder.getBlock().getId()));
        this.model = model;
        this.groupHolder = groupHolder;
    }

    @Override
    public void generateModel(MMBlockstateProvider provider) {
        provider.connectedBlock(groupHolder.getBlock().getId(), Ref.Textures.GEARBOX_OVERLAY);
        provider.connectedState(groupHolder.getBlock().get(), Ref.Textures.GEARBOX_OVERLAY);
    }

    @Override
    public ExtraBlockModel getModel() {
        return model;
    }

    /**
     * Matches the block item: plain string names keep using the generated block.mm.<id> entry so
     * resource packs can override them; only a pack-supplied translation key bypasses it.
     */
    @Override
    public MutableComponent getName() {
        var name = DisplayNameUtil.packSuppliedName(model.displayName());
        return name != null ? name.copy() : super.getName();
    }
}
