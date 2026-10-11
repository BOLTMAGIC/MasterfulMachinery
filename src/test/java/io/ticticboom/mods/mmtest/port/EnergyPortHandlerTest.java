package io.ticticboom.mods.mmtest.port;

import io.ticticboom.mods.mm.port.energy.EnergyPortHandler;
import net.minecraft.nbt.IntTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnergyPortHandlerTest {

    private static final int MAX = Integer.MAX_VALUE;

    private static EnergyPortHandler handler(int capacity) {
        return new EnergyPortHandler(capacity, capacity, capacity, () -> {});
    }

    @Test
    void receiveNearIntMaxFillsUpToCapacity() {
        // a Gigantic port (capacity Integer.MAX_VALUE) getting 64M from a recipe: energy + amount overflowed int and wrapped negative
        var handler = handler(MAX);
        handler.unboundedReceiveEnergy(2_118_065_408, false);
        assertEquals(MAX - 2_118_065_408, handler.unboundedReceiveEnergy(64_000_000, false));
        assertEquals(MAX, handler.getEnergyStored());
    }

    @Test
    void simulatedReceiveNearIntMaxReportsOnlyTheRoomLeft() {
        // canOutput simulates first: it must see the port as (nearly) full, or the machine keeps running into it
        var handler = handler(MAX);
        handler.unboundedReceiveEnergy(2_118_065_408, false);
        assertEquals(MAX - 2_118_065_408, handler.unboundedReceiveEnergy(64_000_000, true));
        assertEquals(2_118_065_408, handler.getEnergyStored());
    }

    @Test
    void fullPortReceivesNothing() {
        var handler = handler(MAX);
        handler.unboundedReceiveEnergy(MAX, false);
        assertEquals(0, handler.unboundedReceiveEnergy(64_000_000, false));
        assertEquals(MAX, handler.getEnergyStored());
    }

    @Test
    void negativeSavedEnergyLoadsAsEmpty() {
        // worlds saved while the overflow bug was hit hold a negative amount
        var handler = handler(MAX);
        handler.deserializeNBT(IntTag.valueOf(-2_112_901_888));
        assertEquals(0, handler.getEnergyStored());
    }

    @Test
    void savedEnergyAboveCapacityLoadsAsFull() {
        var handler = handler(1_000);
        handler.deserializeNBT(IntTag.valueOf(5_000));
        assertEquals(1_000, handler.getEnergyStored());
    }
}
