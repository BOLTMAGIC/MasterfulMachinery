package io.ticticboom.mods.mm.builder.me;

/**
 * Where a submitted craft stands, judged once per poll from what a network shows publicly (no AE2 types here, so it can
 * be tested on its own). A craft is done when its items are in the network, however its job ended.
 */
public final class CraftProgress {
    public enum Verdict {
        /** Still being crafted. */
        WAIT,
        /** The items are in the network. */
        DONE,
        /** The job is over but the items are not there (cancelled, or the stock not caught up yet). */
        SHORT
    }

    private CraftProgress() {
    }

    /**
     * @param cpuKnown   whether the crafting CPU that took the job was identified
     * @param cpuBusy    that CPU still runs a job and is still on the network (ignored when not known)
     * @param stock      the item's stock in the network now
     * @param target     the stock at submission plus the requested amount
     * @param requesting the network still crafts the item in any job (what an unidentified CPU is followed by)
     */
    public static Verdict judge(boolean cpuKnown, boolean cpuBusy, long stock, long target, boolean requesting) {
        if (stock >= target) {
            return Verdict.DONE;
        }
        return (cpuKnown ? cpuBusy : requesting) ? Verdict.WAIT : Verdict.SHORT;
    }
}
