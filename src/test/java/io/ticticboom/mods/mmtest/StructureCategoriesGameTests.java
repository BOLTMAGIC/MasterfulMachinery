package io.ticticboom.mods.mmtest;

import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.tool.StructureCategories;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("mmtest")
@PrefixGameTestTemplate(false)
public class StructureCategoriesGameTests {
    @GameTest(template = "empty")
    public static void assignmentsAreValidatedAndSaved(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.tryBuild("mmtest", "assembly_test");
        check(helper, StructureManager.STRUCTURES.containsKey(id), "test structure not loaded");
        StructureCategories data = new StructureCategories();
        String key = StructureCategories.key(id, false);
        check(helper, data.apply(StructureCategories.Action.CREATE, "", "Factory"), "category creation failed");
        check(helper, !data.apply(StructureCategories.Action.CREATE, "", "factory"), "duplicate category accepted");
        check(helper, !data.apply(StructureCategories.Action.ASSIGN, "mm|mmtest:missing", "Factory"), "unknown structure accepted");
        check(helper, data.apply(StructureCategories.Action.ASSIGN, key, "Factory"), "assignment failed");
        check(helper, data.snapshot().category(id, false).equals("Factory"), "assignment not visible");
        check(helper, data.apply(StructureCategories.Action.RENAME, "Factory", "Assembly"), "rename failed");
        check(helper, data.snapshot().category(id, false).equals("Assembly"), "rename did not update assignment");
        CompoundTag saved = data.save(new CompoundTag());
        check(helper, saved.getList("Assignments", 10).size() == 1, "assignment not saved");
        check(helper, data.apply(StructureCategories.Action.DELETE, "Assembly", ""), "delete failed");
        check(helper, data.snapshot().category(id, false).equals(StructureCategories.DEFAULT), "delete did not clear assignment");
        helper.succeed();
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
