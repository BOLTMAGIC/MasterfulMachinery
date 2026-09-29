package io.ticticboom.mods.mmtest;

import io.ticticboom.mods.mm.port.item.ItemPortHandler;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.port.item.ItemPortStorageModel;
import io.ticticboom.mods.mm.port.item.SingleItemPortIngredient;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.List;

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

    @GameTest(template = "empty")
    public static void indexedExtractionKeepsTagsAndPartialFirst(GameTestHelper helper) {
        ItemPortHandler handler = new ItemPortHandler(3, 64, () -> {});
        ItemStack one = new ItemStack(Items.DIAMOND);
        one.getOrCreateTag().putString("variant", "one");
        ItemStack two = new ItemStack(Items.DIAMOND);
        two.getOrCreateTag().putString("variant", "two");
        handler.setActualCountAndDisplay(0, 64, one);
        handler.setActualCountAndDisplay(1, 20, one);
        handler.setActualCountAndDisplay(2, 10, two);
        check(helper, handler.itemTotals().getOrDefault(Items.DIAMOND, 0L) == 94L, "initial item total");
        var filter = (java.util.function.Predicate<ItemStack>) stack ->
                stack.hasTag() && "one".equals(stack.getTag().getString("variant"));

        check(helper, handler.extractMatching(Items.DIAMOND, filter, 70, true) == 0, "simulation remainder");
        check(helper, handler.getActualCount(1) == 20, "simulation changed a partial slot");
        check(helper, handler.extractMatching(Items.DIAMOND, filter, 70, false) == 0, "extraction remainder");
        check(helper, handler.getActualCount(0) == 14, "full slot was not used after partial slot");
        check(helper, handler.getActualCount(1) == 0, "partial slot was not emptied first");
        check(helper, handler.getActualCount(2) == 10, "different NBT variant was extracted");
        check(helper, handler.itemTotals().getOrDefault(Items.DIAMOND, 0L) == 24L, "item total after extraction");
        check(helper, handler.hasEmptySlots() && handler.hasItem(Items.DIAMOND), "slot index was not updated");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void controllerPortLookupTracksChanges(GameTestHelper helper) {
        var model = new ItemPortStorageModel(1, 2, () -> false, 64, 0);
        ItemPortStorage first = new ItemPortStorage(model, () -> {});
        ItemPortStorage second = new ItemPortStorage(model, () -> {});
        RecipeStorages storages = new RecipeStorages(List.of(first, second), List.of());
        first.getHandler().setActualCountAndDisplay(0, 5, new ItemStack(Items.STONE));
        check(helper, storages.getInputItemStorages(Items.STONE).equals(List.of(first)), "first port missing");
        first.getHandler().extractItem(0, 5, false);
        second.getHandler().setActualCountAndDisplay(1, 8, new ItemStack(Items.STONE));
        check(helper, storages.getInputItemStorages(Items.STONE).equals(List.of(second)), "stale item-to-port lookup");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void outputTopsUpMatchingPortBeforeEmptyPort(GameTestHelper helper) {
        var model = new ItemPortStorageModel(1, 2, () -> false, 64, 0);
        ItemPortStorage empty = new ItemPortStorage(model, () -> {});
        ItemPortStorage partial = new ItemPortStorage(model, () -> {});
        ItemStack tagged = new ItemStack(Items.DIAMOND);
        tagged.getOrCreateTag().putString("variant", "one");
        partial.getHandler().setActualCountAndDisplay(0, 10, tagged);
        RecipeStorages storages = new RecipeStorages(List.of(), List.of(empty, partial));
        var output = new SingleItemPortIngredient(ResourceLocation.tryParse("minecraft:diamond"), 20,
                tagged.getTag(), true);
        check(helper, output.canOutput(null, storages, new RecipeStateModel()), "output capacity was not found");
        output.output(null, storages, new RecipeStateModel());
        check(helper, partial.getHandler().getActualCount(0) == 30, "matching port was not topped up");
        check(helper, empty.getHandler().getActualCount(0) == 0, "empty port was filled first");
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
