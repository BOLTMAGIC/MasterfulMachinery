package io.ticticboom.mods.mmtest;

import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.tool.MultiblockToolItem;
import io.ticticboom.mods.mm.tool.MultiblockToolMenu;
import io.ticticboom.mods.mm.tool.ToolEnergy;
import io.ticticboom.mods.mm.tool.ToolStore;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * The Multiblock Tool's block store (54 x 512) and FE storage, both backed by the tool item's own NBT.
 */
@GameTestHolder("mmtest")
@PrefixGameTestTemplate(false)
public class ToolGameTests {
    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public static void toolStoreHolds512AndSurvivesNbt(GameTestHelper helper) {
        ItemStack tool = MMRegisters.MULTIBLOCK_TOOL.get().getDefaultInstance();
        ToolStore store = new ToolStore(tool);

        ItemStack leftover = store.insertItem(0, new ItemStack(Blocks.COBBLESTONE, 600), false);
        check(helper, store.getStackInSlot(0).getCount() == 512, "expected 512 stored, got " + store.getStackInSlot(0).getCount());
        check(helper, leftover.getCount() == 88, "expected 88 returned, got " + leftover.getCount());

        ItemStack swordLeftover = store.insertItem(1, new ItemStack(Items.IRON_SWORD, 1), false);
        check(helper, swordLeftover.isEmpty(), "the first sword should be fully stored, leftover " + swordLeftover);
        check(helper, store.getStackInSlot(1).getCount() == 1, "expected 1 sword stored, got " + store.getStackInSlot(1).getCount());
        ItemStack secondSword = store.insertItem(1, new ItemStack(Items.IRON_SWORD, 1), false);
        check(helper, secondSword.getCount() == 1, "a second sword should not fit in the same slot, got leftover " + secondSword.getCount());

        ItemStack roundTripped = ItemStack.of(tool.save(new CompoundTag()));
        ToolStore reloaded = new ToolStore(roundTripped);
        check(helper, reloaded.getStackInSlot(0).getCount() == 512, "count 512 should survive an NBT round-trip, got " + reloaded.getStackInSlot(0).getCount());
        check(helper, reloaded.getStackInSlot(1).getCount() == 1, "the sword's count should survive an NBT round-trip, got " + reloaded.getStackInSlot(1).getCount());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void toolEnergyRoundTrip(GameTestHelper helper) {
        ItemStack tool = MMRegisters.MULTIBLOCK_TOOL.get().getDefaultInstance();
        int capacity = MMConfigSetup.COMMON.toolEnergyCapacity.get();
        int rate = MMConfigSetup.COMMON.toolEnergyReceiveRate.get();
        ToolEnergy energy = new ToolEnergy(tool, capacity);

        // a single receive is capped at the configured rate, not the full request
        int received = energy.receiveEnergy(2_000_000, false);
        check(helper, received == rate, "expected " + rate + " received (rate-capped), got " + received);
        check(helper, energy.getEnergyStored() == rate, "expected the store to hold " + rate + ", got " + energy.getEnergyStored());

        // repeated receives still cap out at the tool's total capacity
        for (int i = 0; i < capacity / rate + 5; i++) {
            energy.receiveEnergy(2_000_000, false);
        }
        check(helper, energy.getEnergyStored() == capacity, "expected repeated receives to cap at capacity " + capacity + ", got " + energy.getEnergyStored());

        int extracted = energy.extractEnergy(50, false);
        check(helper, extracted == 50, "expected 50 extracted, got " + extracted);
        check(helper, energy.getEnergyStored() == capacity - 50, "expected stored energy to decrease by 50, got " + energy.getEnergyStored());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void menuQuickMoveMerges(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        ItemStack tool = MMRegisters.MULTIBLOCK_TOOL.get().getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        MultiblockToolMenu menu = new MultiblockToolMenu(0, player.getInventory(), InteractionHand.MAIN_HAND);

        // 3x64 stone in main inventory slots 9-11 (container indices right after the 54 store slots)
        for (int i = 0; i < 3; i++) {
            player.getInventory().setItem(9 + i, new ItemStack(Blocks.STONE, 64));
        }
        // driven through clicked(..., QUICK_MOVE, ...) rather than quickMoveStack directly, since
        // that's the real vanilla shift-click entry point (and it loops calling quickMoveStack for as
        // long as it keeps getting a non-empty result back for the same slot)
        for (int i = 0; i < 3; i++) {
            menu.clicked(MultiblockToolMenu.STORE_SLOTS + i, 0, ClickType.QUICK_MOVE, player);
        }
        check(helper, menu.getStore().getStackInSlot(0).getCount() == 192,
                "expected 192 merged into one store slot, got " + menu.getStore().getStackInSlot(0).getCount());

        // one shift-click on the store slot must hand out exactly one stack (64), not drain it all
        menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
        check(helper, menu.getStore().getStackInSlot(0).getCount() == 128,
                "expected 128 left in the store slot after taking one stack back, got " + menu.getStore().getStackInSlot(0).getCount());
        check(helper, player.getInventory().countItem(Blocks.STONE.asItem()) == 64,
                "expected the player to receive exactly one stack (64) back, got " + player.getInventory().countItem(Blocks.STONE.asItem()));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void pickupSwapRefusedForOversizedStoreSlot(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        ItemStack tool = MMRegisters.MULTIBLOCK_TOOL.get().getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        MultiblockToolMenu menu = new MultiblockToolMenu(0, player.getInventory(), InteractionHand.MAIN_HAND);
        menu.getStore().insertItem(0, new ItemStack(Blocks.STONE, 512), false);

        // cursor holds a different item; vanilla's raw swap would otherwise hand the cursor 512 stone
        menu.setCarried(new ItemStack(Items.DIRT, 1));
        menu.clicked(0, 0, ClickType.PICKUP, player);

        check(helper, menu.getCarried().getItem() == Items.DIRT && menu.getCarried().getCount() == 1,
                "the cursor should still hold the original dirt untouched, got " + menu.getCarried());
        ItemStack storeSlot = menu.getStore().getStackInSlot(0);
        check(helper, storeSlot.getItem() == Blocks.STONE.asItem() && storeSlot.getCount() == 512,
                "the store slot should be untouched, got " + storeSlot);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void swapMovesAtMostOneStackFromStore(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        ItemStack tool = MMRegisters.MULTIBLOCK_TOOL.get().getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        MultiblockToolMenu menu = new MultiblockToolMenu(0, player.getInventory(), InteractionHand.MAIN_HAND);
        menu.getStore().insertItem(0, new ItemStack(Blocks.STONE, 512), false);

        // hotbar slot 1 is a plain, empty, unlocked hotbar slot (the tool sits in slot 0)
        menu.clicked(0, 1, ClickType.SWAP, player);

        ItemStack storeSlot = menu.getStore().getStackInSlot(0);
        ItemStack hotbarSlot = player.getInventory().getItem(1);
        check(helper, hotbarSlot.getCount() <= hotbarSlot.getMaxStackSize(),
                "the hotbar slot must never exceed its own max stack size, got " + hotbarSlot.getCount());
        check(helper, hotbarSlot.getCount() == 64, "expected exactly one stack (64) moved by the number-key swap, got " + hotbarSlot.getCount());
        check(helper, storeSlot.getCount() == 448, "expected 448 left in the store slot, got " + storeSlot.getCount());
        check(helper, storeSlot.getCount() + hotbarSlot.getCount() == 512, "total stone must be conserved across the swap");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void swapRefusesLockedDestination(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        ItemStack tool = MMRegisters.MULTIBLOCK_TOOL.get().getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        MultiblockToolMenu menu = new MultiblockToolMenu(0, player.getInventory(), InteractionHand.MAIN_HAND);
        menu.getStore().insertItem(0, new ItemStack(Blocks.STONE, 100), false);

        // button 0 is the hotbar slot currently holding the tool (selected defaults to 0)
        menu.clicked(0, 0, ClickType.SWAP, player);

        check(helper, menu.getStore().getStackInSlot(0).getCount() == 100, "the store slot should be untouched by a swap targeting the locked slot");
        check(helper, player.getItemInHand(InteractionHand.MAIN_HAND) == tool, "the held tool should be unaffected");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void menuInvalidAfterToolDropped(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        ItemStack tool = MMRegisters.MULTIBLOCK_TOOL.get().getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        MultiblockToolMenu menu = new MultiblockToolMenu(0, player.getInventory(), InteractionHand.MAIN_HAND);
        menu.getStore().insertItem(0, new ItemStack(Blocks.STONE, 512), false);

        // simulate a dropped-item split (ServerboundPlayerActionPacket DROP_ITEM/DROP_ALL_ITEMS):
        // the dropped copy carries the full store NBT, while the ORIGINAL instance - still the exact
        // object the menu and the player's hand reference - shrinks to empty count in place, keeping
        // its identity. Identity-only staleness checks would miss this entirely.
        ItemStack dropped = tool.split(tool.getCount());
        check(helper, tool.isEmpty(), "the original held-stack instance should now be empty");
        check(helper, dropped.getTagElement("ToolStore") != null, "the dropped copy should carry the store's NBT (this is the dupe vector)");
        check(helper, player.getItemInHand(InteractionHand.MAIN_HAND) == tool, "the hand should still reference the same (now empty) instance");

        check(helper, !menu.stillValid(player), "the menu must be invalid once the held tool is emptied, even though the instance is unchanged");

        menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
        check(helper, menu.getStore().getStackInSlot(0).getCount() == 512,
                "nothing should be extractable once the tool is gone, got " + menu.getStore().getStackInSlot(0).getCount());
        ItemStack pickupAttempt = menu.quickMoveStack(player, 0);
        check(helper, pickupAttempt.isEmpty(), "quickMoveStack should also refuse once the tool is gone");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cannotStoreTool(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        ItemStack heldTool = MMRegisters.MULTIBLOCK_TOOL.get().getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, heldTool);
        MultiblockToolMenu menu = new MultiblockToolMenu(0, player.getInventory(), InteractionHand.MAIN_HAND);

        // direct capability insert is refused
        ItemStack secondTool = MMRegisters.MULTIBLOCK_TOOL.get().getDefaultInstance();
        ItemStack leftover = menu.getStore().insertItem(0, secondTool.copy(), false);
        check(helper, leftover.getCount() == secondTool.getCount(), "the store should fully refuse a multiblock tool, got leftover " + leftover.getCount());
        check(helper, menu.getStore().getStackInSlot(0).isEmpty(), "the store slot should remain empty");

        // quick-moving a second tool from the player inventory into the store is refused too
        player.getInventory().setItem(9, secondTool);
        ItemStack moved = menu.quickMoveStack(player, MultiblockToolMenu.STORE_SLOTS);
        check(helper, moved.isEmpty(), "quick-moving a multiblock tool into the store should do nothing");
        check(helper, player.getInventory().getItem(9).getItem() instanceof MultiblockToolItem,
                "the second tool should remain in the player's inventory");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void lockedToolSlotCannotBeMoved(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        ItemStack tool = MMRegisters.MULTIBLOCK_TOOL.get().getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        MultiblockToolMenu menu = new MultiblockToolMenu(0, player.getInventory(), InteractionHand.MAIN_HAND);

        ItemStack moved = menu.quickMoveStack(player, menu.getLockedSlotIndex());
        check(helper, moved.isEmpty(), "quick-moving the held tool's own slot should do nothing");
        check(helper, player.getItemInHand(InteractionHand.MAIN_HAND) == tool, "the tool should remain in the player's hand");
        helper.succeed();
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
