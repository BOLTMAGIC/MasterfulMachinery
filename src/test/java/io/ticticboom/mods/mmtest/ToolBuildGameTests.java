package io.ticticboom.mods.mmtest;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.builder.AssemblyJob;
import io.ticticboom.mods.mm.builder.AssemblyPlanner;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.tool.ToolBuilds;
import io.ticticboom.mods.mm.tool.ToolData;
import io.ticticboom.mods.mm.tool.ToolEnergy;
import io.ticticboom.mods.mm.tool.ToolStore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Consumer;

/**
 * Building with the Multiblock Tool: in the world at an anchor (controller included) and finishing a controller.
 * Uses the mmtest:assembly_test structure ("FCGS") like {@link AssemblyGameTests}.
 */
@GameTestHolder("mmtest")
@PrefixGameTestTemplate(false)
public class ToolBuildGameTests {
    private static final String TEMPLATE = "empty";
    private static final ResourceLocation STRUCTURE = ResourceLocation.tryBuild("mmtest", "assembly_test");
    private static final int START_FE = 1000;
    /** Clicked on its top face, so the anchor is one block above. */
    private static final BlockPos CLICKED = new BlockPos(3, 0, 3);
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    private static final BlockPos GLASS = new BlockPos(1, 0, 0);

    @GameTest(template = TEMPLATE)
    public static void toolBuildsFromStore(GameTestHelper helper) {
        Player player = player(helper, 180); // facing north
        ItemStack tool = tool(player);
        stockStore(tool);

        ToolBuilds.Prepared build = prepare(helper, player, tool);
        check(helper, build.controllerPos().equals(helper.absolutePos(ANCHOR)),
                "facing north, the controller (front middle of FCGS) should sit on the anchor, got " + build.controllerPos());
        AssemblyJob job = run(helper, player, build);

        int fe = MMConfigSetup.COMMON.toolEnergyPerPlacedBlock.get();
        check(helper, job.placed() == 4, "expected 4 placed (controller + 3), got " + job.placed());
        check(helper, job.missing().isEmpty() && job.blocked() == 0, "nothing should be missing or blocked: " + job.missing() + ", " + job.blocked());
        check(helper, structure(helper).formed(helper.getLevel(), build.controllerPos()), "structure not formed after the tool build");
        expectFacing(helper, build.controllerPos(), Direction.NORTH);
        check(helper, storeCount(tool) == 0, "every block should come out of the store, " + storeCount(tool) + " left");
        check(helper, energy(tool) == START_FE - 4 * fe, "expected " + (START_FE - 4 * fe) + " FE left, got " + energy(tool));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void toolBuildRefusedWhenObstructed(GameTestHelper helper) {
        Player player = player(helper, 180);
        ItemStack tool = tool(player);
        stockStore(tool);
        BlockPos anchor = helper.absolutePos(ANCHOR);
        helper.getLevel().setBlockAndUpdate(anchor.offset(GLASS), Blocks.STONE.defaultBlockState());

        ToolBuilds.Result result = ToolBuilds.prepare(helper.getLevel(), player, tool, helper.absolutePos(CLICKED), Direction.UP);

        check(helper, result.prepared() == null && result.error() != null, "an obstructed build must be refused");
        check(helper, helper.getLevel().getBlockState(anchor).isAir(), "nothing should be placed");
        check(helper, helper.getLevel().getBlockState(anchor.offset(GLASS)).is(Blocks.STONE), "the obstruction must not be broken");
        check(helper, storeCount(tool) == 4, "the store should be untouched, has " + storeCount(tool));
        check(helper, energy(tool) == START_FE, "no energy should be used, have " + energy(tool));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void toolCompletesController(GameTestHelper helper) {
        Player player = player(helper, 0);
        ItemStack tool = tool(player);
        var store = new ToolStore(tool);
        store.insertItem(0, new ItemStack(port("s")), false);
        store.insertItem(1, new ItemStack(Blocks.GLASS), false);
        store.insertItem(2, new ItemStack(port("l")), false);
        BlockPos controller = helper.absolutePos(ANCHOR);
        helper.getLevel().setBlockAndUpdate(controller, controllerBlock().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));

        ToolBuilds.Result result = ToolBuilds.prepare(helper.getLevel(), player, tool, controller, Direction.UP);
        check(helper, result.prepared() != null, "the tool should complete a matching controller, got " + result.error());
        check(helper, result.prepared().controllerPos().equals(controller), "the job should use the clicked controller");
        AssemblyJob job = run(helper, player, result.prepared());

        int fe = MMConfigSetup.COMMON.toolEnergyPerPlacedBlock.get();
        check(helper, job.placed() == 3, "expected 3 placed, got " + job.placed());
        check(helper, structure(helper).formed(helper.getLevel(), controller), "structure not formed after completing the controller");
        check(helper, helper.getLevel().getBlockState(controller.above()).isAir(), "nothing should be built at the anchor above the controller");
        check(helper, storeCount(tool) == 0, "the store should be used, " + storeCount(tool) + " left");
        check(helper, energy(tool) == START_FE - 3 * fe, "expected " + (START_FE - 3 * fe) + " FE left, got " + energy(tool));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void toolBuildRotatedForms(GameTestHelper helper) {
        Player player = player(helper, 90); // facing west
        check(helper, player.getDirection() == Direction.WEST, "mock player should face west, faces " + player.getDirection());
        ItemStack tool = tool(player);
        ToolData.setExtraTurns(tool, 1);
        stockStore(tool);

        ToolBuilds.Prepared build = prepare(helper, player, tool);
        // west is a quarter turn counter-clockwise; one extra clockwise turn brings it back to none
        Rotation expected = AssemblyPlanner.rotationFor(Direction.WEST).getRotated(Rotation.CLOCKWISE_90);
        // facing west the front is the east end of the row, so the row "FCGS" ends on the anchor
        check(helper, build.controllerPos().equals(helper.absolutePos(ANCHOR).offset(-2, 0, 0)),
                "controller expected two blocks west of the anchor, got " + build.controllerPos());
        AssemblyJob job = run(helper, player, build);

        check(helper, job.placed() == 4, "expected 4 placed, got " + job.placed());
        check(helper, structure(helper).formed(helper.getLevel(), build.controllerPos()), "rotated structure not formed");
        Direction facing = helper.getLevel().getBlockState(build.controllerPos()).getValue(HorizontalDirectionalBlock.FACING);
        check(helper, AssemblyPlanner.rotationFor(facing) == expected,
                "the controller should face so its own Assemble picks " + expected + ", faces " + facing);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void toolBuildTurnedFacesController(GameTestHelper helper) {
        Player player = player(helper, 180); // facing north
        ItemStack tool = tool(player);
        ToolData.setExtraTurns(tool, 1); // turned to east
        stockStore(tool);

        ToolBuilds.Prepared build = prepare(helper, player, tool);
        AssemblyJob job = run(helper, player, build);

        check(helper, job.placed() == 4, "expected 4 placed, got " + job.placed());
        expectFacing(helper, build.controllerPos(), Direction.EAST);
        BlockPos glass = build.controllerPos().offset(GLASS.rotate(Rotation.CLOCKWISE_90));
        check(helper, helper.getLevel().getBlockState(glass).is(Blocks.GLASS), "glass expected at " + glass);
        check(helper, structure(helper).formed(helper.getLevel(), build.controllerPos()), "turned structure not formed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void toolEnergyRefundedWhenPlacementCancelled(GameTestHelper helper) {
        Player player = player(helper, 180);
        ItemStack tool = tool(player);
        stockStore(tool);
        ToolBuilds.Prepared build = prepare(helper, player, tool);

        // a claim mod refusing this player
        Consumer<BlockEvent.EntityPlaceEvent> claims = event -> {
            if (event.getEntity() == player) {
                event.setCanceled(true);
            }
        };
        MinecraftForge.EVENT_BUS.addListener(claims);
        AssemblyJob job;
        try {
            job = run(helper, player, build);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(claims);
        }

        check(helper, job.placed() == 0, "expected nothing placed, got " + job.placed());
        check(helper, job.blocked() == 4, "expected 4 blocked, got " + job.blocked());
        check(helper, energy(tool) == START_FE, "paid energy should be refunded, have " + energy(tool));
        check(helper, storeCount(tool) == 4, "taken blocks should go back into the store, has " + storeCount(tool));
        check(helper, helper.getLevel().getBlockState(build.controllerPos()).isAir(), "the cancelled controller should be undone");
        helper.succeed();
    }

    private static ToolBuilds.Prepared prepare(GameTestHelper helper, Player player, ItemStack tool) {
        ToolBuilds.Result result = ToolBuilds.prepare(helper.getLevel(), player, tool, helper.absolutePos(CLICKED), Direction.UP);
        if (result.prepared() == null) {
            helper.fail("tool build refused: " + (result.error() == null ? "?" : result.error().getString()));
        }
        return result.prepared();
    }

    private static AssemblyJob run(GameTestHelper helper, Player player, ToolBuilds.Prepared build) {
        AssemblyJob job = AssemblyJob.create(helper.getLevel(), build.controllerPos(), build.plan(), build.perBlockFe());
        int ticks = 0;
        while (!job.tick(player, build.source(), 1)) {
            if (++ticks > 100) {
                helper.fail("tool build did not finish");
            }
        }
        return job;
    }

    private static Player player(GameTestHelper helper, float yRot) {
        Player player = helper.makeMockPlayer();
        player.setYRot(yRot);
        check(helper, !player.getAbilities().instabuild, "mock player should not be instabuild");
        return player;
    }

    /** A tool selecting assembly_test with {@value #START_FE} FE, held in the main hand. */
    private static ItemStack tool(Player player) {
        ItemStack tool = MMRegisters.MULTIBLOCK_TOOL.get().getDefaultInstance();
        ToolData.setStructure(tool, STRUCTURE);
        new ToolEnergy(tool, MMConfigSetup.COMMON.toolEnergyCapacity.get()).restore(START_FE);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        return tool;
    }

    /** Exactly the blocks of one assembly_test (lowest tiers), only in the tool's store. */
    private static void stockStore(ItemStack tool) {
        var store = new ToolStore(tool);
        store.insertItem(0, new ItemStack(controllerBlock()), false);
        store.insertItem(1, new ItemStack(port("s")), false);
        store.insertItem(2, new ItemStack(Blocks.GLASS), false);
        store.insertItem(3, new ItemStack(port("l")), false);
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

    private static void expectFacing(GameTestHelper helper, BlockPos pos, Direction facing) {
        BlockState state = helper.getLevel().getBlockState(pos);
        check(helper, state.is(controllerBlock()), "expected the controller at " + pos + ", found " + state);
        check(helper, state.getValue(HorizontalDirectionalBlock.FACING) == facing, "controller should face " + facing + ", faces " + state.getValue(HorizontalDirectionalBlock.FACING));
    }

    private static StructureModel structure(GameTestHelper helper) {
        StructureModel model = StructureManager.STRUCTURES.get(STRUCTURE);
        check(helper, model != null, "structure " + STRUCTURE + " is not loaded");
        return model;
    }

    private static Block controllerBlock() {
        return registered("assembly_test");
    }

    private static Block port(String size) {
        return registered("test_item_" + size + "_input");
    }

    private static Block registered(String id) {
        Block block = ForgeRegistries.BLOCKS.getValue(Ref.id(id));
        if (block == null || block == Blocks.AIR) {
            throw new IllegalStateException("block mm:" + id + " is not registered");
        }
        return block;
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
