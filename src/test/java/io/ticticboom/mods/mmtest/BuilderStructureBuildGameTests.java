package io.ticticboom.mods.mmtest;

import io.ticticboom.mods.mm.builder.AssemblyJob;
import io.ticticboom.mods.mm.builder.AssemblyJobs;
import io.ticticboom.mods.mm.builder.AssemblyPlanner;
import io.ticticboom.mods.mm.builder.DismantleJob;
import io.ticticboom.mods.mm.builder.DismantlePlanner;
import io.ticticboom.mods.mm.builder.structure.BuildableStructureRegistry;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.net.packet.ToolSettingsPkt;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.tool.ToolBuilds;
import io.ticticboom.mods.mm.tool.ToolData;
import io.ticticboom.mods.mm.tool.ToolDismantles;
import io.ticticboom.mods.mm.tool.ToolEnergy;
import io.ticticboom.mods.mm.tool.ToolStore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

/**
 * Building another mod's structure (a datapack .nbt) with the Multiblock Tool: the mmtest:test_tower file
 * (9 stone and 1 oak stairs, 3x2x3), no controller, all or nothing like MM builds.
 */
@GameTestHolder("mmtest")
@PrefixGameTestTemplate(false)
public class BuilderStructureBuildGameTests {
    private static final String TEMPLATE = "empty";
    private static final ResourceLocation TOWER = ResourceLocation.tryBuild("mmtest", "test_tower");
    private static final ResourceLocation UNBUILDABLE = ResourceLocation.tryBuild("mmtest", "unbuildable");
    private static final int START_FE = 1000;
    /** Clicked on its top face, so the anchor is one block above. */
    private static final BlockPos CLICKED = new BlockPos(3, 0, 3);

    @GameTest(template = TEMPLATE)
    public static void builderStructureBuildsFromStore(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack tool = tool(player, TOWER);
        stockStore(tool, 9, 1);

        ToolBuilds.Prepared build = prepare(helper, player, tool);
        check(helper, !build.requiresController(), "another mod's structure has no controller");
        check(helper, build.plan().steps().size() == 10, "expected 10 planned blocks, got " + build.plan().steps().size());
        AssemblyJob job = AssemblyJob.createWithoutController(helper.getLevel(), build.controllerPos(), build.plan(), build.perBlockFe());
        int ticks = 0;
        while (!AssemblyJobs.tickBuild(player, job, build.source(), 1)) {
            if (++ticks > 100) {
                helper.fail("the build did not finish");
            }
        }

        check(helper, job.placed() == 10, "expected 10 placed, got " + job.placed());
        check(helper, !job.controllerNotPlaced(), "a structure without controller never stops for one");
        for (AssemblyPlanner.Planned step : build.plan().steps()) {
            // the block, not the exact state: stairs may change their shape to fit their neighbours once placed
            check(helper, helper.getLevel().getBlockState(step.pos()).is(step.state().getBlock()),
                    "expected " + step.state() + " at " + step.pos() + ", found " + helper.getLevel().getBlockState(step.pos()));
        }
        int fe = MMConfigSetup.COMMON.toolEnergyPerPlacedBlock.get();
        check(helper, storeCount(tool) == 0, "every block should come out of the store, " + storeCount(tool) + " left");
        check(helper, energy(tool) == START_FE - 10 * fe, "expected " + (START_FE - 10 * fe) + " FE left, got " + energy(tool));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void instantBuilderBuildCompletesInOneTickWithNormalCosts(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack tool = tool(player, TOWER);
        stockStore(tool, 9, 1);
        ToolBuilds.Prepared build = prepare(helper, player, tool);
        AssemblyJob job = AssemblyJob.createWithoutController(helper.getLevel(), build.controllerPos(), build.plan(), build.perBlockFe());

        check(helper, AssemblyJobs.tickBuild(player, job, build.source(), Integer.MAX_VALUE), "instant build should finish in one tick");
        check(helper, job.placed() == 10 && job.blocked() == 0, "instant build should place every planned block");
        check(helper, storeCount(tool) == 0, "instant build should consume the blocks");
        check(helper, energy(tool) == START_FE - 10 * build.perBlockFe(), "instant build should charge normal FE per block");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void controllerFreeStructureCanBeDismantled(GameTestHelper helper) {
        Player player = player(helper);
        player.setYRot(90); // the matcher must recover the build's rotation without a controller
        ItemStack tool = tool(player, TOWER);
        stockStore(tool, 9, 1);
        ToolBuilds.Prepared build = prepare(helper, player, tool);
        AssemblyJob assembly = AssemblyJob.createWithoutController(helper.getLevel(), build.controllerPos(), build.plan(), build.perBlockFe());
        int ticks = 0;
        while (!AssemblyJobs.tickBuild(player, assembly, build.source(), 2)) {
            if (++ticks > 100) helper.fail("builder assembly did not finish");
        }
        BlockPos clicked = build.plan().steps().get(0).pos();
        DismantlePlanner.BuilderMatch match = DismantlePlanner.matchBuilder(helper.getLevel(), clicked, tool);
        check(helper, match != null && match.positions().size() == 10,
                "a complete controller-free structure should match from one of its blocks");
        ToolDismantles.Result result = ToolDismantles.prepare(helper.getLevel(), player, tool, clicked);
        check(helper, result.prepared() != null, "the selected builder structure should be dismantleable");
        ToolDismantles.Prepared prepared = result.prepared();
        DismantleJob job = prepared.job(helper.getLevel());
        ticks = 0;
        while (!AssemblyJobs.tickDismantle(player, job, prepared.sink(), 2)) {
            if (++ticks > 100) helper.fail("builder dismantle did not finish");
        }
        check(helper, job.removed() == 10, "expected all 10 blocks to be removed, got " + job.removed());
        for (AssemblyPlanner.Planned step : build.plan().steps()) {
            check(helper, helper.getLevel().getBlockState(step.pos()).isAir(), "left a block at " + step.pos());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void incompleteControllerFreeStructureIsNotDismantled(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack tool = tool(player, TOWER);
        stockStore(tool, 9, 1);
        ToolBuilds.Prepared build = prepare(helper, player, tool);
        AssemblyJob assembly = AssemblyJob.createWithoutController(helper.getLevel(), build.controllerPos(), build.plan(), build.perBlockFe());
        int ticks = 0;
        while (!AssemblyJobs.tickBuild(player, assembly, build.source(), 2)) {
            if (++ticks > 100) helper.fail("builder assembly did not finish");
        }
        BlockPos missing = build.plan().steps().stream().filter(step -> step.state().is(Blocks.OAK_STAIRS))
                .findFirst().orElseThrow().pos();
        helper.getLevel().removeBlock(missing, false);
        BlockPos clicked = build.plan().steps().get(0).pos();
        check(helper, DismantlePlanner.matchBuilder(helper.getLevel(), clicked, tool) == null,
                "an incomplete structure must not match");
        ToolDismantles.Result result = ToolDismantles.prepare(helper.getLevel(), player, tool, clicked);
        check(helper, result.prepared() == null, "an incomplete structure must not be dismantled");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void builderStructureStandsOnTheClickedFace(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack tool = tool(player, TOWER);
        stockStore(tool, 9, 1);

        ToolBuilds.Prepared build = prepare(helper, player, tool);
        int anchorY = helper.absolutePos(CLICKED).getY() + 1;
        int minY = build.plan().steps().stream().mapToInt(step -> step.pos().getY()).min().orElseThrow();
        check(helper, minY == anchorY, "the structure's bottom should be the block above the clicked face, got y " + minY + " for anchor " + anchorY);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void builderStructureRefusedWhenObstructed(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack tool = tool(player, TOWER);
        stockStore(tool, 9, 1);
        BlockPos blocked = ToolBuilds.prepare(helper.getLevel(), player, tool, helper.absolutePos(CLICKED), Direction.UP)
                .prepared().plan().steps().get(0).pos();
        helper.getLevel().setBlockAndUpdate(blocked, Blocks.DIRT.defaultBlockState());

        ToolBuilds.Result result = ToolBuilds.prepare(helper.getLevel(), player, tool, helper.absolutePos(CLICKED), Direction.UP);
        expectError(helper, result, "message.mm.tool.obstructed");
        check(helper, helper.getLevel().getBlockState(blocked).is(Blocks.DIRT), "the obstruction must not be broken");
        check(helper, storeCount(tool) == 10, "the store should be untouched, has " + storeCount(tool));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void builderStructureRefusedWhenAnyBlockMissing(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack tool = tool(player, TOWER);
        stockStore(tool, 9, 0); // no stairs

        ToolBuilds.Result result = ToolBuilds.prepare(helper.getLevel(), player, tool, helper.absolutePos(CLICKED), Direction.UP);
        check(helper, result.prepared() == null && result.error() != null, "all or nothing: a missing block refuses the build");
        check(helper, storeCount(tool) == 9, "the store should be untouched, has " + storeCount(tool));
        check(helper, energy(tool) == START_FE, "no energy should be spent, have " + energy(tool));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void unbuildableStructureRefused(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack tool = tool(player, UNBUILDABLE);

        ToolBuilds.Result result = ToolBuilds.prepare(helper.getLevel(), player, tool, helper.absolutePos(CLICKED), Direction.UP);
        expectError(helper, result, "message.mm.tool.unbuildable");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void selectingBuilderAndMmStructuresReplaceEachOther(GameTestHelper helper) {
        ItemStack tool = MMRegisters.MULTIBLOCK_TOOL.get().getDefaultInstance();
        ResourceLocation mmStructure = ResourceLocation.tryBuild("mmtest", "assembly_test");
        check(helper, ToolSettingsPkt.apply(tool, ToolSettingsPkt.Action.SELECT_STRUCTURE, TOWER.toString(), ToolSettingsPkt.BUILDER_STRUCTURE,
                id -> false, id -> BuildableStructureRegistry.SERVER.get(id) != null, Map.of()), "a loaded builder structure should be selectable");
        check(helper, TOWER.equals(ToolData.builderStructure(tool)) && ToolData.structure(tool) == null,
                "the builder structure is selected and no MM structure");
        check(helper, !ToolSettingsPkt.apply(tool, ToolSettingsPkt.Action.SELECT_STRUCTURE, "mmtest:no_such_file", ToolSettingsPkt.BUILDER_STRUCTURE,
                id -> false, id -> BuildableStructureRegistry.SERVER.get(id) != null, Map.of()), "an unknown builder structure is refused");
        // the old overload never selects a builder structure
        check(helper, !ToolSettingsPkt.apply(tool, ToolSettingsPkt.Action.SELECT_STRUCTURE, TOWER.toString(), ToolSettingsPkt.BUILDER_STRUCTURE,
                id -> true, Map.of()), "without a builder check a builder selection is refused");

        check(helper, ToolSettingsPkt.apply(tool, ToolSettingsPkt.Action.SELECT_STRUCTURE, mmStructure.toString(), 0,
                mmStructure::equals, Map.of()), "an MM structure should be selectable");
        check(helper, mmStructure.equals(ToolData.structure(tool)) && ToolData.builderStructure(tool) == null,
                "selecting an MM structure replaces the builder one");
        helper.succeed();
    }

    private static void expectError(GameTestHelper helper, ToolBuilds.Result result, String key) {
        check(helper, result.prepared() == null && result.error() != null, "the build should be refused");
        check(helper, result.error().getContents() instanceof TranslatableContents contents && contents.getKey().equals(key),
                "expected message " + key + ", got " + result.error().getString());
    }

    private static ToolBuilds.Prepared prepare(GameTestHelper helper, Player player, ItemStack tool) {
        ToolBuilds.Result result = ToolBuilds.prepare(helper.getLevel(), player, tool, helper.absolutePos(CLICKED), Direction.UP);
        if (result.prepared() == null) {
            helper.fail("tool build refused: " + (result.error() == null ? "?" : result.error().getString()));
        }
        return result.prepared();
    }

    private static Player player(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        player.setYRot(180); // facing north
        check(helper, !player.getAbilities().instabuild, "mock player should not be instabuild");
        return player;
    }

    private static ItemStack tool(Player player, ResourceLocation structure) {
        ItemStack tool = MMRegisters.MULTIBLOCK_TOOL.get().getDefaultInstance();
        ToolData.setBuilderStructure(tool, structure);
        new ToolEnergy(tool, MMConfigSetup.COMMON.toolEnergyCapacity.get()).restore(START_FE);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        return tool;
    }

    private static void stockStore(ItemStack tool, int stone, int stairs) {
        var store = new ToolStore(tool);
        if (stone > 0) {
            store.insertItem(0, new ItemStack(Blocks.STONE, stone), false);
        }
        if (stairs > 0) {
            store.insertItem(1, new ItemStack(Blocks.OAK_STAIRS, stairs), false);
        }
    }

    private static int storeCount(ItemStack tool) {
        var store = new ToolStore(tool);
        int total = 0;
        for (int i = 0; i < store.getSlots(); i++) {
            total += store.getStackInSlot(i).getCount();
        }
        return total;
    }

    private static int energy(ItemStack tool) {
        return new ToolEnergy(tool, MMConfigSetup.COMMON.toolEnergyCapacity.get()).getEnergyStored();
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
