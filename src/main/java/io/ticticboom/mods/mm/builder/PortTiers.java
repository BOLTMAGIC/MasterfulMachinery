package io.ticticboom.mods.mm.builder;

import io.ticticboom.mods.mm.model.PortModel;
import net.minecraft.resources.ResourceLocation;

/** Tier helpers shared by structure matching and assembly. */
public final class PortTiers {
    private PortTiers() {
    }

    /** The port's tier; ports without a tier count as 1 (same default structure matching always used). */
    public static int rankOf(PortModel model) {
        int rank = model.config().getModel().getTierRank();
        try {
            if (rank <= 0 && model.jsonConfig() != null && model.jsonConfig().has("tierRank")) {
                rank = model.jsonConfig().get("tierRank").getAsInt();
            }
        } catch (Exception ignored) {
        }
        return rank <= 0 ? 1 : rank;
    }

    public static String key(ResourceLocation portTypeId, boolean input) {
        return portTypeId + (input ? "/input" : "/output");
    }
}
