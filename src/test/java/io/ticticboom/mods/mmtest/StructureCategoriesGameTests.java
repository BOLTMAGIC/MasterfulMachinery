package io.ticticboom.mods.mmtest;

import io.ticticboom.mods.mm.builder.structure.BuildableStructure;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.tool.StructureCategories;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

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

    @GameTest(template = "empty")
    public static void legacyFoldersAreImportedAndAdminChangesSurviveReload(GameTestHelper helper) {
        ResourceLocation machine = ResourceLocation.tryBuild("mmtest", "assembly_test");
        BuildableStructure reactor = BuildableStructure.of(ResourceLocation.tryBuild("mbtool", "reactor/nuclearcraft_fission_reactor"),
                new Vec3i(1, 1, 1), List.of(new BuildableStructure.Placement(BlockPos.ZERO, Blocks.STONE.defaultBlockState())));
        BuildableStructure nested = BuildableStructure.of(ResourceLocation.tryBuild("mbtool", "voidminer/solar/panel"),
                new Vec3i(1, 1, 1), reactor.blocks());
        StructureCategories data = new StructureCategories();
        data.importDefaults(List.of(machine), List.of(reactor, nested));
        check(helper, data.snapshot().category(machine, false).equals("Custom Multiblocks"), "MM machines not grouped");
        check(helper, data.snapshot().category(reactor.id(), true).equals("Reactor"), "legacy reactor folder not imported");
        check(helper, data.snapshot().category(nested.id(), true).equals("Void Miner"), "first legacy folder not used");
        check(helper, data.apply(StructureCategories.Action.RENAME, "Custom Multiblocks", "Factory"), "imported category not editable");
        check(helper, data.apply(StructureCategories.Action.DELETE, "Reactor", ""), "imported category not removable");
        check(helper, data.apply(StructureCategories.Action.ASSIGN, StructureCategories.key(machine, false), StructureCategories.DEFAULT),
                "explicit uncategorized assignment failed");
        StructureCategories restored = StructureCategories.load(data.save(new CompoundTag()));
        restored.importDefaults(List.of(machine), List.of(reactor, nested));
        check(helper, !restored.snapshot().categories().contains("Reactor"), "deleted category recreated on reload");
        check(helper, !restored.snapshot().categories().contains("Custom Multiblocks"), "renamed category recreated on reload");
        check(helper, restored.snapshot().category(machine, false).equals(StructureCategories.DEFAULT), "explicit assignment overwritten");
        check(helper, restored.snapshot().category(reactor.id(), true).equals(StructureCategories.DEFAULT), "deleted category assignment restored");
        check(helper, restored.snapshot().category(nested.id(), true).equals("Void Miner"), "imported assignment lost on save");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void importPreservesExistingCategoriesAndFindsNewStructures(GameTestHelper helper) {
        ResourceLocation machine = ResourceLocation.tryBuild("mmtest", "assembly_test");
        StructureCategories data = new StructureCategories();
        check(helper, data.apply(StructureCategories.Action.CREATE, "", "Factory"), "manual category creation failed");
        check(helper, data.apply(StructureCategories.Action.ASSIGN, StructureCategories.key(machine, false), "Factory"), "manual assignment failed");
        data.importDefaults(List.of(machine), List.of());
        check(helper, data.snapshot().category(machine, false).equals("Factory"), "manual assignment overwritten by import");
        BuildableStructure altar = BuildableStructure.of(ResourceLocation.tryBuild("mbtool", "blood_magic_altar/altar_2"),
                new Vec3i(1, 1, 1), List.of(new BuildableStructure.Placement(BlockPos.ZERO, Blocks.STONE.defaultBlockState())));
        data.importDefaults(List.of(machine), List.of(altar));
        check(helper, data.snapshot().category(altar.id(), true).equals("Blood Magic Altar"), "newly loaded structure not imported");
        data.importDefaults(List.of(machine), List.of(altar));
        check(helper, data.snapshot().categories().size() == 2, "repeat import created duplicate categories");
        helper.succeed();
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
