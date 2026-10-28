package io.ticticboom.mods.mm.port.projecte.emc.register;

import io.ticticboom.mods.mm.menu.MMContainerMenu;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortMenu;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.util.MenuUtils;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class ProjectEEmcPortMenu extends MMContainerMenu implements IPortMenu {
    @Getter
    private final PortModel model;
    private final ProjectEEmcPortBlockEntity be;

    public ProjectEEmcPortMenu(PortModel model, RegistryGroupHolder groupHolder, int windowId, Inventory inv, ProjectEEmcPortBlockEntity be) {
        super(groupHolder.getMenu().get(), groupHolder.getBlock().get(), windowId, MenuUtils.createAccessFromBlockEntity(be), be.getEmcStorage().hasKleinSlot() ? 1 : 0);
        this.model = model;
        this.be = be;
        be.getStorage().setupContainer(this, inv, model);
    }

    public ProjectEEmcPortMenu(PortModel model, RegistryGroupHolder groupHolder, int windowId, Inventory inv, FriendlyByteBuf buf) {
        this(model, groupHolder, windowId, inv, (ProjectEEmcPortBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    @Override
    public <T> T getBlockEntity() {
        return (T) be;
    }
}
