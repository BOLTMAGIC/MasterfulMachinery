package io.ticticboom.mods.mmtest;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * A pattern carrying a Mekanism chemical (Applied Mekanistics). Registered by MeGameTests only with AE2 and Applied
 * Mekanistics. Uses mmtest:pattern_push_gas: 100 mB hydrogen -> 1 emerald.
 */
@PrefixGameTestTemplate(false)
public class PatternPortChemicalGameTests {

    @GameTest(templateNamespace = "mmtest", template = PatternPortGameTests.TEMPLATE, timeoutTicks = 200)
    public static void chemicalPatternRuns(GameTestHelper helper) {
        var controller = PatternPortGameTests.build(helper);
        var hydrogen = MekanismAPI.gasRegistry().getValue(ResourceLocation.tryBuild("mekanism", "hydrogen"));
        var pattern = TestPattern.of("hydrogen", new GenericStack[]{new GenericStack(MekanismKey.of(new GasStack(hydrogen, 100)), 100)},
                new GenericStack(AEItemKey.of(Items.EMERALD), 1));

        helper.startSequence()
                .thenWaitUntil(() -> PatternPortGameTests.formed(helper, controller))
                .thenExecute(() -> PatternPortGameTests.check(helper, PatternPortGameTests.push(helper, pattern),
                        "the pattern port should take a hydrogen pattern"))
                .thenWaitUntil(() -> PatternPortGameTests.check(helper, PatternPortGameTests.finishedWith(controller, Items.EMERALD),
                        "the hydrogen craft hasn't made its emerald"))
                .thenSucceed();
    }

    /** A pattern made with an older MM had 1000 mB for every chemical from JEI: Jade says what differs. */
    @GameTest(templateNamespace = "mmtest", template = PatternPortGameTests.TEMPLATE, timeoutTicks = 100)
    public static void jadeShowsAWrongChemicalAmount(GameTestHelper helper) {
        var controller = PatternPortGameTests.build(helper);
        var pattern = TestPattern.of("bucket", new GenericStack[]{new GenericStack(gas("hydrogen"), 1000)},
                new GenericStack(AEItemKey.of(Items.EMERALD), 1));

        helper.startSequence()
                .thenWaitUntil(() -> PatternPortGameTests.formed(helper, controller))
                .thenExecute(() -> {
                    PatternPortGameTests.check(helper, !PatternPortGameTests.push(helper, pattern), "1000 mB isn't the recipe's 100 mB");
                    var why = PatternPortGameTests.jadeData(helper).getCompound("PatternWhy");
                    PatternPortGameTests.check(helper, "amount".equals(why.getString("Reason")) && "mekanism:hydrogen".equals(why.getString("What"))
                            && why.getLong("Pattern") == 1000 && why.getLong("Recipe") == 100, "Jade should say the hydrogen amount differs, got " + why);
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "mmtest", template = PatternPortGameTests.TEMPLATE, timeoutTicks = PatternPortMeGameTests.TIMEOUT)
    public static void providerRunsAChemicalPattern(GameTestHelper helper) {
        var controller = PatternPortGameTests.build(helper);
        ItemStack cell = chemicalCell(helper);
        PatternPortMeGameTests.insert(helper, cell, gas("hydrogen"), 100);
        PatternPortMeGameTests.network(helper, PatternPortMeGameTests.processing(new GenericStack[]{new GenericStack(gas("hydrogen"), 100)},
                new GenericStack(AEItemKey.of(Items.EMERALD), 1)), cell, AEItems.ITEM_CELL_1K.stack());

        PatternPortMeGameTests.craft(helper.startSequence().thenWaitUntil(() -> PatternPortGameTests.formed(helper, controller)),
                        helper, gas("hydrogen"), AEItemKey.of(Items.EMERALD), 1)
                .thenWaitUntil(() -> PatternPortGameTests.check(helper, PatternPortMeGameTests.stock(helper, AEItemKey.of(Items.EMERALD)) == 1
                        && PatternPortMeGameTests.idle(helper), "the emerald should come back into the network; port " + PatternPortGameTests.jadeData(helper) + ", network "
                        + PatternPortMeGameTests.grid(helper).getStorageService().getInventory().getAvailableStacks().keySet()))
                .thenExecute(() -> PatternPortGameTests.check(helper, PatternPortMeGameTests.stock(helper, gas("hydrogen")) == 0,
                        "the hydrogen should be used"))
                .thenSucceed();
    }

    /** The shape of the Infinity Crucible's chaos shard recipe: 25 mB gas, 16 + 1 items -> 24 items (mmtest:pattern_push_mixed). */
    @GameTest(templateNamespace = "mmtest", template = PatternPortGameTests.TEMPLATE, timeoutTicks = PatternPortMeGameTests.TIMEOUT)
    public static void providerRunsAGasAndItemsPattern(GameTestHelper helper) {
        var controller = PatternPortGameTests.build(helper);
        ItemStack chemicals = chemicalCell(helper);
        PatternPortMeGameTests.insert(helper, chemicals, gas("hydrogen"), 25);
        ItemStack items = PatternPortMeGameTests.itemCell(helper, AEItemKey.of(Items.IRON_NUGGET), 16);
        PatternPortMeGameTests.insert(helper, items, AEItemKey.of(Items.COPPER_INGOT), 1);
        PatternPortMeGameTests.network(helper, PatternPortMeGameTests.processing(new GenericStack[]{new GenericStack(gas("hydrogen"), 25),
                new GenericStack(AEItemKey.of(Items.IRON_NUGGET), 16), new GenericStack(AEItemKey.of(Items.COPPER_INGOT), 1)},
                new GenericStack(AEItemKey.of(Items.GOLD_NUGGET), 24)), chemicals, items);

        PatternPortMeGameTests.craft(helper.startSequence().thenWaitUntil(() -> PatternPortGameTests.formed(helper, controller)),
                        helper, gas("hydrogen"), AEItemKey.of(Items.GOLD_NUGGET), 24)
                .thenWaitUntil(() -> PatternPortGameTests.check(helper, PatternPortMeGameTests.stock(helper, AEItemKey.of(Items.GOLD_NUGGET)) == 24
                        && PatternPortMeGameTests.idle(helper), "the 24 gold nuggets should come back into the network; port "
                        + PatternPortGameTests.jadeData(helper)))
                .thenExecute(() -> PatternPortGameTests.check(helper, PatternPortMeGameTests.stock(helper, gas("hydrogen")) == 0
                        && PatternPortMeGameTests.stock(helper, AEItemKey.of(Items.IRON_NUGGET)) == 0
                        && PatternPortMeGameTests.stock(helper, AEItemKey.of(Items.COPPER_INGOT)) == 0, "every input should be used"))
                .thenSucceed();
    }

    @GameTest(templateNamespace = "mmtest", template = PatternPortGameTests.TEMPLATE, timeoutTicks = PatternPortMeGameTests.TIMEOUT)
    public static void chemicalOutputGoesBackIntoTheNetwork(GameTestHelper helper) {
        var controller = PatternPortGameTests.build(helper);
        ItemStack items = PatternPortMeGameTests.itemCell(helper, AEItemKey.of(Items.COAL), 1);
        PatternPortMeGameTests.network(helper, PatternPortMeGameTests.processing(new GenericStack[]{new GenericStack(AEItemKey.of(Items.COAL), 1)},
                new GenericStack(gas("oxygen"), 100)), items, chemicalCell(helper));

        PatternPortMeGameTests.craft(helper.startSequence().thenWaitUntil(() -> PatternPortGameTests.formed(helper, controller)),
                        helper, AEItemKey.of(Items.COAL), gas("oxygen"), 100)
                .thenWaitUntil(() -> PatternPortGameTests.check(helper, PatternPortMeGameTests.stock(helper, gas("oxygen")) == 100
                        && PatternPortMeGameTests.idle(helper), "the oxygen should come back into the network, it holds "
                        + PatternPortMeGameTests.grid(helper).getStorageService().getInventory().getAvailableStacks()))
                .thenSucceed();
    }

    static MekanismKey gas(String name) {
        Gas gas = MekanismAPI.gasRegistry().getValue(ResourceLocation.tryBuild("mekanism", name));
        return MekanismKey.of(new GasStack(gas, 1));
    }

    static ItemStack chemicalCell(GameTestHelper helper) {
        var item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryBuild("appmek", "chemical_storage_cell_1k"));
        PatternPortGameTests.check(helper, item != null && item != Items.AIR, "appmek:chemical_storage_cell_1k is not registered");
        return new ItemStack(item);
    }
}
