package io.ticticboom.mods.mm.tool;

import io.ticticboom.mods.mm.builder.me.CraftHandle;
import io.ticticboom.mods.mm.builder.me.CraftTracker;
import io.ticticboom.mods.mm.builder.me.MeAccess;
import io.ticticboom.mods.mm.builder.me.MeAccessFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * What happens when a tool build lacks materials: it does not start, and the tool's ME network (when it has one and
 * auto-craft is on) is asked to craft whatever it has a pattern for. The player confirms the build with another
 * right-click once the crafts are done; nothing is built on its own.
 */
public final class ToolCrafts {
    /** Entries named in the one-line message; the rest are counted as "+N". */
    private static final int SHOWN = 2;

    private ToolCrafts() {
    }

    /**
     * Requests crafts for missing (item -> amount short) where possible and says what is going on, in one line: e.g.
     * "Crafting 3 items · Titanium Plate ×3: no pattern" or "Missing: Oak Planks ×12 (no ME network)". An item with a
     * craft already on its way is never reported as missing: it counts as crafting.
     *
     * @param me the tool's usable network, or null
     */
    public static Component request(Player player, ItemStack tool, @Nullable MeAccess me, Map<Item, Integer> missing) {
        boolean craft = me != null && ToolData.autoCraft(tool);
        int crafting = 0;
        List<Component> problems = new ArrayList<>();
        for (Map.Entry<Item, Integer> e : missing.entrySet()) {
            Item item = e.getKey();
            int count = e.getValue();
            int coming = CraftTracker.active(player, item);
            if (coming > 0) {
                // already on its way (whatever the network says now); ask only for what that does not cover
                if (craft && count > coming && me.isCraftable(item)) {
                    CraftTracker.add(player, me.requestCraft(item, count - coming));
                }
                crafting += count;
                continue;
            }
            if (!craft) {
                problems.add(CraftTracker.amount(item, count));
                continue;
            }
            if (!me.isCraftable(item)) {
                problems.add(CraftTracker.entry(item, count, Component.translatable("message.mm.tool.craft.no_pattern")));
                continue;
            }
            CraftHandle handle = me.requestCraft(item, count);
            CraftTracker.add(player, handle);
            if (handle.state() == CraftHandle.State.FAILED) {
                problems.add(CraftTracker.entry(item, count, handle.failure()));
            } else {
                crafting += count;
            }
        }
        if (crafting == 0 && !craft) {
            return missing(me, tool, list(problems));
        }
        MutableComponent text = Component.empty();
        if (crafting > 0) {
            text.append(Component.translatable("message.mm.tool.crafting", crafting));
        }
        if (!problems.isEmpty()) {
            if (crafting > 0) {
                text.append(Component.literal(" · "));
            }
            text.append(craft ? list(problems) : missing(me, tool, list(problems)));
        }
        return text;
    }

    /** "Missing: ..." for a build that crafts nothing, and "(no ME network)" when a tool without one could have had it. */
    private static Component missing(@Nullable MeAccess me, ItemStack tool, Component list) {
        MutableComponent text = Component.translatable("message.mm.assemble.missing", list);
        if (me == null && MeAccessFactory.supported() && ToolData.network(tool) == null) {
            text.append(Component.literal(" ")).append(Component.translatable("message.mm.tool.no_me"));
        }
        return text;
    }

    /** The first {@value #SHOWN} entries, then "+N" for the rest. */
    private static Component list(List<Component> entries) {
        MutableComponent list = Component.empty();
        for (int i = 0; i < entries.size(); i++) {
            if (i == SHOWN) {
                list.append(Component.literal(" +" + (entries.size() - SHOWN)));
                break;
            }
            if (i > 0) {
                list.append(Component.literal(", "));
            }
            list.append(entries.get(i));
        }
        return list;
    }
}
