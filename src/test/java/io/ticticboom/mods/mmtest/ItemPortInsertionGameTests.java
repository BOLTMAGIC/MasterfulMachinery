package io.ticticboom.mods.mmtest;

import io.ticticboom.mods.mm.port.item.ItemPortHandler;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.ItemHandlerHelper;

@GameTestHolder("mmtest")
@PrefixGameTestTemplate(false)
public class ItemPortInsertionGameTests {
    @GameTest(template = "empty")
    public static void indexedInsertionMatchesForge(GameTestHelper helper) {
        ItemPortHandler indexed = new ItemPortHandler(24, 128, () -> {});
        ItemPortHandler reference = new ItemPortHandler(24, 128, () -> {});
        ItemStack tagged = new ItemStack(Items.DIAMOND, 20);
        tagged.getOrCreateTag().putString("variant", "one");
        ItemStack otherTag = tagged.copy();
        otherTag.getOrCreateTag().putString("variant", "two");
        ItemStack[] inputs = {
                new ItemStack(Items.STONE, 64), tagged, new ItemStack(Items.STONE, 64),
                otherTag, new ItemStack(Items.DIAMOND, 64), new ItemStack(Items.SHEARS, 2),
                new ItemStack(Items.STONE, 64), tagged
        };
        for (ItemStack input : inputs) {
            compare(helper, indexed, reference, input, true);
            compare(helper, indexed, reference, input, false);
        }
        indexed.extractItem(0, 50, false);
        reference.extractItem(0, 50, false);
        compare(helper, indexed, reference, new ItemStack(Items.STONE, 64), false);

        ItemPortHandler restored = new ItemPortHandler(24, 128, () -> {});
        restored.deserializeStacks(indexed.serializeStacks());
        compare(helper, restored, reference, otherTag, false);
        helper.succeed();
    }

    private static void compare(GameTestHelper helper, ItemPortHandler indexed,
                                ItemPortHandler reference, ItemStack input, boolean simulate) {
        ItemStack actualRemainder = indexed.insertStackFast(input, simulate);
        ItemStack expectedRemainder = ItemHandlerHelper.insertItemStacked(reference, input, simulate);
        check(helper, ItemStack.isSameItemSameTags(actualRemainder, expectedRemainder)
                && actualRemainder.getCount() == expectedRemainder.getCount(), "different insertion remainder");
        for (int slot = 0; slot < indexed.getSlots(); slot++) {
            check(helper, indexed.getActualCount(slot) == reference.getActualCount(slot), "different slot count at " + slot);
            check(helper, ItemStack.isSameItemSameTags(indexed.getStackInSlot(slot), reference.getStackInSlot(slot)),
                    "different stack at " + slot);
        }
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
