package io.ticticboom.mods.mmtest;

import com.mojang.authlib.GameProfile;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.tool.ToolStore;
import io.ticticboom.mods.mm.tool.ToolStoreFill;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/** JEI's "+" filling the Structure Builder's store from the player's inventory ({@link ToolStoreFill}). */
@GameTestHolder("mmtest")
@PrefixGameTestTemplate(false)
public class ToolStoreFillGameTests {
    private static final String TEMPLATE = "empty";

    /** What the store already holds counts; only the rest leaves the inventory, the offhand included, never armor. */
    @GameTest(template = TEMPLATE)
    public static void onlyTheMissingPartIsMoved(GameTestHelper helper) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "store-fill-test"));
        ItemStack tool = MMRegisters.MULTIBLOCK_TOOL.get().getDefaultInstance();
        var store = new ToolStore(tool);
        store.insertItem(0, new ItemStack(Items.GLASS, 3), false);
        var inventory = player.getInventory();
        inventory.items.set(0, new ItemStack(Items.GLASS, 64));
        inventory.items.set(1, new ItemStack(Items.STONE, 2));
        inventory.offhand.set(0, new ItemStack(Items.STONE, 5));
        inventory.armor.set(0, new ItemStack(Items.GOLD_BLOCK, 1));
        inventory.items.set(2, new ItemStack(Items.IRON_BLOCK, 4));

        int moved = ToolStoreFill.fill(player, store, List.of(
                new ToolStoreFill.Need(List.of(Items.GLASS), 10),
                // either block does: stone is carried, so the stone counts
                new ToolStoreFill.Need(List.of(Items.COBBLESTONE, Items.STONE), 6),
                new ToolStoreFill.Need(List.of(Items.GOLD_BLOCK), 1)));

        check(helper, moved == 7 + 6, "expected 13 moved, got " + moved);
        check(helper, stored(store, Items.GLASS) == 10, "the store should hold 10 glass, holds " + stored(store, Items.GLASS));
        check(helper, inventory.items.get(0).getCount() == 57, "7 glass should leave the inventory, left " + inventory.items.get(0).getCount());
        check(helper, stored(store, Items.STONE) == 6, "6 stone should be stored, got " + stored(store, Items.STONE));
        check(helper, inventory.items.get(1).isEmpty() && inventory.offhand.get(0).getCount() == 1,
                "stone from the main slots first, then the offhand: " + inventory.items.get(1) + " / " + inventory.offhand.get(0));
        check(helper, inventory.armor.get(0).getCount() == 1 && stored(store, Items.GOLD_BLOCK) == 0, "armor slots are never taken from");
        check(helper, inventory.items.get(2).getCount() == 4, "what the structure doesn't need stays");

        int again = ToolStoreFill.fill(player, store, List.of(new ToolStoreFill.Need(List.of(Items.GLASS), 10)));
        check(helper, again == 0 && ToolStoreFill.movable(player, store, List.of(new ToolStoreFill.Need(List.of(Items.GLASS), 10))) == 0,
                "a full need moves nothing more");
        helper.succeed();
    }

    private static int stored(ToolStore store, Item item) {
        int total = 0;
        for (int slot = 0; slot < store.getSlots(); slot++) {
            if (store.getStackInSlot(slot).is(item)) total += store.getStackInSlot(slot).getCount();
        }
        return total;
    }

    private static void check(GameTestHelper helper, boolean ok, String message) {
        if (!ok) helper.fail(message);
    }
}
