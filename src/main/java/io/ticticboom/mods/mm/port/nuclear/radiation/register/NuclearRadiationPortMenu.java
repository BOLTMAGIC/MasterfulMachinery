package io.ticticboom.mods.mm.port.nuclear.radiation.register;

import io.ticticboom.mods.mm.menu.MMContainerMenu;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortMenu;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.util.MenuUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class NuclearRadiationPortMenu extends MMContainerMenu implements IPortMenu {

    private final PortModel model;
    private final NuclearRadiationPortBlockEntity be;

    public NuclearRadiationPortMenu(PortModel model, RegistryGroupHolder groupHolder, int windowId, NuclearRadiationPortBlockEntity be, Inventory inv) {
        super(groupHolder.getMenu().get(), groupHolder.getBlock().get(), windowId, MenuUtils.createAccessFromBlockEntity(be), be.isInput() ? 1 : 2);
        this.model = model;
        this.be = be;
        be.getStorage().setupContainer(this, inv, model);
    }

    public NuclearRadiationPortMenu(PortModel model, RegistryGroupHolder groupHolder, int windowId, Inventory inv, FriendlyByteBuf buf) {
        this(model, groupHolder, windowId, (NuclearRadiationPortBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()), inv);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBlockEntity() {
        return (T) be;
    }

    @Override
    public PortModel getModel() {
        return model;
    }
}
