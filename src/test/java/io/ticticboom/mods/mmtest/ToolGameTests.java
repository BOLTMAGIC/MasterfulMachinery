package io.ticticboom.mods.mmtest;

import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.tool.ToolEnergy;
import io.ticticboom.mods.mm.tool.ToolStore;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
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
        ToolEnergy energy = new ToolEnergy(tool, capacity);

        int received = energy.receiveEnergy(2_000_000, false);
        check(helper, received == capacity, "expected " + capacity + " received, got " + received);
        check(helper, energy.getEnergyStored() == capacity, "expected the store to be capped at capacity, got " + energy.getEnergyStored());

        int extracted = energy.extractEnergy(50, false);
        check(helper, extracted == 50, "expected 50 extracted, got " + extracted);
        check(helper, energy.getEnergyStored() == capacity - 50, "expected stored energy to decrease by 50, got " + energy.getEnergyStored());
        helper.succeed();
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
