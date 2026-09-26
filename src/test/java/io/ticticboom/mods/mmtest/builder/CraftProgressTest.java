package io.ticticboom.mods.mmtest.builder;

import io.ticticboom.mods.mm.builder.me.CraftProgress;
import io.ticticboom.mods.mm.builder.me.CraftProgress.Verdict;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CraftProgressTest {

    @Test
    void ourCpuCraftingWaits() {
        assertEquals(Verdict.WAIT, CraftProgress.judge(3, 7, true, true, false));
    }

    @Test
    void itemsThereIsDone() {
        assertEquals(Verdict.DONE, CraftProgress.judge(7, 7, false, false, false));
    }

    @Test
    void itemsThereWhileStillCraftingIsDone() {
        // the CPU may already run someone else's next job for the same item
        assertEquals(Verdict.DONE, CraftProgress.judge(9, 7, true, true, true));
    }

    @Test
    void noJobAndNoItemsIsShort() {
        // cancelled, used up, or the stock not caught up yet: the handle gives it grace polls
        assertEquals(Verdict.SHORT, CraftProgress.judge(3, 7, false, false, false));
    }

    @Test
    void unidentifiedCpuCraftingTheItemWaits() {
        // an addon CPU (or AE2's own between pattern pushes) that AE2's isRequesting doesn't report: the CPU's job
        // status names the item, so the craft is still on its way
        assertEquals(Verdict.WAIT, CraftProgress.judge(3, 7, false, true, false));
    }

    @Test
    void requestingAloneStillWaits() {
        assertEquals(Verdict.WAIT, CraftProgress.judge(3, 7, false, false, true));
    }
}
