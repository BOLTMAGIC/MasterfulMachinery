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
        /** No job crafts the item any more but the items are not there (cancelled, used up, or the stock lagging). */
        SHORT
    }

    private CraftProgress() {
    }

    /**
     * @param stock          the item's stock in the network now
     * @param target         the stock at submission plus the requested amount
     * @param ourCpuCrafting the CPU identified as ours is busy crafting this item
     * @param anyCpuCrafting some CPU on the network says it is crafting this item (covers CPUs that could not be
     *                       identified, e.g. an addon's)
     * @param requesting     the network reports the item as being crafted (AE2 only knows this for its own CPUs, and
     *                       only while a pattern's items are out, so it is one signal among others)
     */
    public static Verdict judge(long stock, long target, boolean ourCpuCrafting, boolean anyCpuCrafting, boolean requesting) {
        if (stock >= target) {
            return Verdict.DONE;
        }
        return ourCpuCrafting || anyCpuCrafting || requesting ? Verdict.WAIT : Verdict.SHORT;
    }
}
