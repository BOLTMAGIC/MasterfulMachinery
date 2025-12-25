package io.ticticboom.mods.mm.port.item.compat;

import io.ticticboom.mods.mm.compat.kjs.builder.PortConfigBuilderJS;
import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.item.ItemPortStorageModel;

public class ItemPortConfigBuilderJS extends PortConfigBuilderJS {

    private int rows;
    private int columns;
    private boolean isAutoPushSet = false;
    private boolean autoPush = false;
    private int slotCapacity = 0; // 0 means use item default

    @Override
    public IPortStorageModel build() {
        return new ItemPortStorageModel(rows, columns, isAutoPushSet ? () -> autoPush : () -> MMConfig.DEFAULT_PORT_AUTO_PUSH, slotCapacity);
    }

    public ItemPortConfigBuilderJS rows(int rows) {
        this.rows = rows;
        return this;
    }

    public ItemPortConfigBuilderJS columns(int columns) {
        this.columns = columns;
        return this;
    }

    public ItemPortConfigBuilderJS autoPush(boolean autoPush) {
        this.autoPush = autoPush;
        this.isAutoPushSet = true;
        return this;
    }

    public ItemPortConfigBuilderJS slotCapacity(int slotCapacity) {
        this.slotCapacity = slotCapacity;
        return this;
    }
}
