package io.ticticboom.mods.mm.builder.me;

public final class CraftProgress {
    public enum Verdict {
        WAIT,
        DONE,
        SHORT
    }

    private CraftProgress() {
    }

    public static Verdict judge(long stock, long target, boolean ourCpuCrafting, boolean anyCpuCrafting, boolean requesting) {
        if (stock >= target) {
            return Verdict.DONE;
        }
        return ourCpuCrafting || anyCpuCrafting || requesting ? Verdict.WAIT : Verdict.SHORT;
    }
}
