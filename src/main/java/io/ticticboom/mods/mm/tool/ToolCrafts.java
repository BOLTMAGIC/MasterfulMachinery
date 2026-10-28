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

public final class ToolCrafts {
    private static final int SHOWN = 2;

    private ToolCrafts() {
    }

    public static Component request(Player player, ItemStack tool, @Nullable MeAccess me, Map<Item, Integer> missing) {
        boolean craft = me != null && ToolData.autoCraft(tool);
        int crafting = 0;
        List<Component> problems = new ArrayList<>();
        for (Map.Entry<Item, Integer> e : missing.entrySet()) {
            Item item = e.getKey();
            int count = e.getValue();
            long coming = Math.max(CraftTracker.coming(player, item), me == null ? 0 : me.requestedAmount(item));
            if (coming > 0) {
                crafting += count;
                if (craft && count > coming && me.isCraftable(item)) {
                    CraftHandle extra = me.requestCraft(item, (int) (count - coming));
                    CraftTracker.add(player, extra);
                    if (extra.state() == CraftHandle.State.FAILED) {
                        crafting -= extra.amount();
                        problems.add(CraftTracker.entry(item, extra.amount(), extra.failure()));
                    }
                }
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
            text.append(Component.translatable("message.mm.tool.crafting", crafting, CraftTracker.itemsWord(crafting)));
        }
        if (!problems.isEmpty()) {
            if (crafting > 0) {
                text.append(Component.literal(" · "));
            }
            text.append(craft ? list(problems) : missing(me, tool, list(problems)));
        }
        return text;
    }

    private static Component missing(@Nullable MeAccess me, ItemStack tool, Component list) {
        MutableComponent text = Component.translatable("message.mm.assemble.missing", list);
        if (me == null && MeAccessFactory.supported() && ToolData.network(tool) == null) {
            text.append(Component.literal(" ")).append(Component.translatable("message.mm.tool.no_me"));
        }
        return text;
    }

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
