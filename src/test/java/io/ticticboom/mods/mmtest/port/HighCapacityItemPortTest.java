package io.ticticboom.mods.mmtest.port;

import io.ticticboom.mods.mm.port.item.ItemPortParser;
import io.ticticboom.mods.mm.port.item.ItemPortStorageModel;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HighCapacityItemPortTest {
    @Test
    void regularPortsKeepTheirConfiguredGrid() {
        var model = new ItemPortStorageModel(6, 9, () -> false, 0, 1);
        assertEquals(6, model.rows());
        assertEquals(9, model.columns());
    }

    @Test
    void fiveHundredTwelveItemPortsUseAtMostFortyEightSlots() {
        var model = new ItemPortStorageModel(6, 9, () -> false, 512, 2);
        assertEquals(6, model.rows());
        assertEquals(8, model.columns());
        assertEquals(512, model.slotCapacity());
    }

    @Test
    void sixteenKItemPortsUseTheSameGridLimitAsFiveHundredTwelve() {
        var model = new ItemPortStorageModel(6, 9, () -> false, 16_384, 3);
        assertEquals(6, model.rows());
        assertEquals(8, model.columns());
        assertEquals(16_384, model.slotCapacity());
    }

    @Test
    void jsonPortsKeepSixteenKCapacity() {
        var json = new JsonObject();
        json.addProperty("rows", 6);
        json.addProperty("columns", 9);
        json.addProperty("slotCapacity", 16_384);
        var model = (ItemPortStorageModel) new ItemPortParser().parseStorage(json).getModel();
        assertEquals(16_384, model.slotCapacity());
        assertEquals(6, model.rows());
        assertEquals(8, model.columns());
    }

    @Test
    void alreadySmallPortsDoNotGrow() {
        var model = new ItemPortStorageModel(2, 2, () -> false, 16_384, 3);
        assertEquals(2, model.rows());
        assertEquals(2, model.columns());
    }

    @Test
    void highCapacityPortsShrinkToColossalGridWithoutGrowingSmallerPorts() {
        var model = new ItemPortStorageModel(8, 12, () -> false, 16_384, 10);
        assertEquals(6, model.rows());
        assertEquals(8, model.columns());
    }
}
