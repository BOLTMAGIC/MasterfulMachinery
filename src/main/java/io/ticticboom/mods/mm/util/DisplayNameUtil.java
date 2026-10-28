package io.ticticboom.mods.mm.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.Nullable;

/**
 * Decides whether a data-driven block should show the name its pack supplied directly, or go on
 * using the generated {@code block.mm.<id>} lang entry.
 * <p>
 * MMLangProvider writes {@code block.mm.<id>} from the model's raw string name. That works for a
 * plain {@code "name": "Assembler"}, but when a pack uses {@code "name": {"translation": "..."}}
 * the raw string is the translation <em>key</em>, and the generated entry maps a key to another
 * key. Minecraft does not resolve that chain, so the block would show the literal key text.
 * <p>
 * Blocks named with a plain string keep going through the lang entry so a resource pack can still
 * override them; only the translation form bypasses it.
 */
public final class DisplayNameUtil {

    private DisplayNameUtil() {
    }

    /**
     * @return the pack-supplied name when it should replace the vanilla lang lookup, or null to
     *         fall back to the generated {@code block.mm.<id>} entry.
     */
    public static @Nullable Component packSuppliedName(@Nullable Component displayName) {
        if (displayName == null) {
            return null;
        }
        if (displayName.getContents() instanceof TranslatableContents) {
            return displayName;
        }
        // A styled or multi-part name carries colour and formatting that block.mm.<id> cannot,
        // so it has to be shown directly. A plain literal still goes through the lang entry.
        if (!displayName.getStyle().isEmpty() || !displayName.getSiblings().isEmpty()) {
            return displayName;
        }
        return null;
    }
}
