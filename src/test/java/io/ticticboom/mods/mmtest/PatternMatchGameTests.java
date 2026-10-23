package io.ticticboom.mods.mmtest;

import io.ticticboom.mods.mm.pattern.PatternIngredient;
import io.ticticboom.mods.mm.pattern.PatternIngredient.Kind;
import io.ticticboom.mods.mm.pattern.PatternRecipeMatcher;
import io.ticticboom.mods.mm.pattern.PatternSpec;
import io.ticticboom.mods.mm.recipe.MachineRecipeManager;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Matching an AE2 pattern (as MM sees it) to one of a machine's recipes. The recipes are in
 * data/mmtest/mm/processes/pattern/, all for the structure mmtest:pattern_match (no machine forms it).
 */
@GameTestHolder("mmtest")
@PrefixGameTestTemplate(false)
public class PatternMatchGameTests {
    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public static void plainPatternMatchesItsRecipe(GameTestHelper helper) {
        expect(helper, spec(List.of(item("iron_ingot", 1)), item("gold_ingot", 1)), "pattern/iron");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void amountsMustMatch(GameTestHelper helper) {
        expect(helper, spec(List.of(item("iron_ingot", 2)), item("gold_ingot", 1)), "pattern/two_iron");
        // two single iron inputs are one input of two
        expect(helper, spec(List.of(item("iron_ingot", 1), item("iron_ingot", 1)), item("gold_ingot", 1)), "pattern/two_iron");
        expect(helper, spec(List.of(item("iron_ingot", 3)), item("gold_ingot", 1)), null);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void nbtInputsMatchOnlyTheirRecipe(GameTestHelper helper) {
        CompoundTag named;
        try {
            named = TagParser.parseTag("{display:{Name:'{\"text\":\"Key\"}'}}");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        var namedIron = new PatternIngredient(Kind.ITEM, mc("iron_ingot"), 1, named);
        expect(helper, spec(List.of(namedIron), item("diamond", 1)), "pattern/iron_named");
        expect(helper, spec(List.of(item("iron_ingot", 1)), item("diamond", 1)), null);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void catalystsComeFromPorts(GameTestHelper helper) {
        // the bucket (chance 0) isn't in the pattern: the machine's ports provide it
        expect(helper, spec(List.of(item("iron_ingot", 1)), item("emerald", 1)), "pattern/catalyst");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void flexRecipesAreSkipped(GameTestHelper helper) {
        expect(helper, spec(List.of(item("oak_log", 1)), item("oak_planks", 2)), null);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void primaryOutputMustBeMade(GameTestHelper helper) {
        // 2 gold asked, the recipe makes 1
        expect(helper, spec(List.of(item("iron_ingot", 1)), item("gold_ingot", 2)), null);
        expect(helper, spec(List.of(item("iron_ingot", 1)), item("copper_ingot", 1)), null);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void ambiguousPatternPicksTheSmallestRecipeId(GameTestHelper helper) {
        // pattern/iron and pattern/iron_copy both make gold from iron: the choice mustn't depend on the list's order
        var spec = spec(List.of(item("iron_ingot", 1)), item("gold_ingot", 1));
        var forward = new ArrayList<>(candidates());
        forward.sort(java.util.Comparator.comparing(r -> r.id().toString()));
        var backward = new ArrayList<>(forward);
        java.util.Collections.reverse(backward);
        for (var list : List.of(forward, backward)) {
            RecipeModel found = PatternRecipeMatcher.match(list, spec);
            if (found == null || !found.id().equals(ResourceLocation.tryBuild("mmtest", "pattern/iron"))) {
                helper.fail("the smallest matching id should win, got " + (found == null ? null : found.id()));
            }
        }
        helper.succeed();
    }

    private static void expect(GameTestHelper helper, PatternSpec spec, String recipePath) {
        RecipeModel found = PatternRecipeMatcher.match(candidates(), spec);
        ResourceLocation got = found == null ? null : found.id();
        ResourceLocation want = recipePath == null ? null : ResourceLocation.tryBuild("mmtest", recipePath);
        if (want == null ? got != null : !want.equals(got)) {
            helper.fail("pattern " + spec + " should match " + want + ", matched " + got);
        }
    }

    private static List<RecipeModel> candidates() {
        return new ArrayList<>(MachineRecipeManager.getRecipesByStrucutreId(ResourceLocation.tryBuild("mmtest", "pattern_match")));
    }

    private static PatternSpec spec(List<PatternIngredient> inputs, PatternIngredient output) {
        return new PatternSpec(inputs, output);
    }

    private static PatternIngredient item(String path, long amount) {
        return new PatternIngredient(Kind.ITEM, mc(path), amount, null);
    }

    private static ResourceLocation mc(String path) {
        return ResourceLocation.tryBuild("minecraft", path);
    }
}
