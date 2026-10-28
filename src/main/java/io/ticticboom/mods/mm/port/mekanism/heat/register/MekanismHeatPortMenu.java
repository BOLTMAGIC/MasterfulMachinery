package io.ticticboom.mods.mm.port.mekanism.heat.register;

import io.ticticboom.mods.mm.menu.MMContainerMenu;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortMenu;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.util.MenuUtils;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class MekanismHeatPortMenu extends MMContainerMenu implements IPortMenu {
    @Getter
    private final PortModel model;
    private final boolean isInput;
    private final Inventory inv;
    private final MekanismHeatPortBlockEntity be;

    public MekanismHeatPortMenu(PortModel model, RegistryGroupHolder groupHolder, boolean isInput, int windowId, Inventory inv, MekanismHeatPortBlockEntity be) {
        super(groupHolder.getMenu().get(), groupHolder.getBlock().get(), windowId, MenuUtils.createAccessFromBlockEntity(be), 0);
        this.model = model;
        this.isInput = isInput;
        this.inv = inv;
        this.be = be;
        be.getStorage().setupContainer(this, inv, model);
    }

    public MekanismHeatPortMenu(PortModel model, RegistryGroupHolder groupHolder, boolean isInput, int windowId, Inventory inv, FriendlyByteBuf buf) {
        this(model, groupHolder, isInput, windowId, inv, (MekanismHeatPortBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    @Override
    public <T> T getBlockEntity() {
        return (T) be;
    }
}
