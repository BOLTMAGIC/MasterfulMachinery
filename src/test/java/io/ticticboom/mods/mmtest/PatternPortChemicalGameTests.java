package io.ticticboom.mods.mmtest;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.gas.GasStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
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
}
