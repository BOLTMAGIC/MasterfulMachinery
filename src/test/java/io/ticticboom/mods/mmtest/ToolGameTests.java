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
        for (int i = 0; i < 3; i++) {
            menu.quickMoveStack(player, MultiblockToolMenu.STORE_SLOTS + i);
        }
        check(helper, menu.getStore().getStackInSlot(0).getCount() == 192,
                "expected 192 merged into one store slot, got " + menu.getStore().getStackInSlot(0).getCount());

        menu.quickMoveStack(player, 0);
        check(helper, menu.getStore().getStackInSlot(0).getCount() == 128,
                "expected 128 left in the store slot after taking one stack back, got " + menu.getStore().getStackInSlot(0).getCount());
        check(helper, player.getInventory().countItem(Blocks.STONE.asItem()) == 64,
                "expected the player to receive exactly one stack (64) back, got " + player.getInventory().countItem(Blocks.STONE.asItem()));
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
