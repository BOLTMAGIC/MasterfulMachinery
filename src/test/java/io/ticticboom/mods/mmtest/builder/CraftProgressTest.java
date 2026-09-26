package io.ticticboom.mods.mmtest.builder;

import io.ticticboom.mods.mm.builder.me.CraftProgress;
import io.ticticboom.mods.mm.builder.me.CraftProgress.Verdict;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CraftProgressTest {

    @Test
    void knownCpuBusyWaits() {
        assertEquals(Verdict.WAIT, CraftProgress.judge(true, true, 3, 7, false));
    }

    @Test
    void knownCpuIdleWithItemsIsDone() {
        assertEquals(Verdict.DONE, CraftProgress.judge(true, false, 7, 7, false));
    }

    @Test
    void knownCpuIdleWithoutItemsIsShort() {
        // cancelled, or the stock not caught up yet: the handle gives it a grace poll
        assertEquals(Verdict.SHORT, CraftProgress.judge(true, false, 3, 7, true));
    }

    @Test
    void itemsThereWhileCpuBusyIsDone() {
        // the CPU may already run someone else's next job
        assertEquals(Verdict.DONE, CraftProgress.judge(true, true, 9, 7, true));
    }

    @Test
    void unknownCpuNeverDoneBeforeItemsArrive() {
        // an addon CPU that can't be told apart: followed by whether the network still crafts the item
        assertEquals(Verdict.WAIT, CraftProgress.judge(false, false, 3, 7, true));
        assertEquals(Verdict.DONE, CraftProgress.judge(false, false, 7, 7, true));
        assertEquals(Verdict.SHORT, CraftProgress.judge(false, false, 3, 7, false));
    }

    @Test
    void unknownCpuIgnoresBusyFlag() {
        assertEquals(Verdict.SHORT, CraftProgress.judge(false, true, 3, 7, false));
    }
}
