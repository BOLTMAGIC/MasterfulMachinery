package io.ticticboom.mods.mmtest;

import io.ticticboom.mods.mm.builder.AssemblyJob;
import io.ticticboom.mods.mm.builder.AssemblyJobs;
import io.ticticboom.mods.mm.builder.AssemblyPlanner;
import io.ticticboom.mods.mm.builder.PlayerMaterialSource;
import io.ticticboom.mods.mm.net.packet.ToolBuildPkt;
import io.ticticboom.mods.mm.net.packet.ToolAnchorPkt;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.tool.ToolBuildMode;
import io.ticticboom.mods.mm.tool.ToolBuilds;
import io.ticticboom.mods.mm.tool.ToolData;
import io.ticticboom.mods.mm.tool.ToolTarget;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder("mmtest")
@PrefixGameTestTemplate(false)
public class ToolPlacementGameTests {
    private static final ResourceLocation TOWER = ResourceLocation.tryBuild("mmtest", "test_tower");

    @GameTest(template = "empty")
    public static void longRangeTargetUsesTheFirstLoadedBlockOnTheRay(GameTestHelper helper) {
        var player = helper.makeMockPlayer();
        BlockPos target = helper.absolutePos(new BlockPos(3, 2, 3));
        // The reused GameTest world may contain blocks from previous tall test structures above the template.
        for (int height = 1; height <= 45; height++) helper.getLevel().setBlockAndUpdate(target.above(height), Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(target, Blocks.STONE.defaultBlockState());
        player.setPos(target.getX() + 0.5D, target.getY() + 40, target.getZ() + 0.5D);
        player.setXRot(90);
        var shortTarget = ToolTarget.trace(player, 5);
        check(helper, shortTarget == null, "ordinary short reach cannot hit a block 40 blocks away: " + shortTarget
                + ", eye=" + player.getEyePosition() + ", view=" + player.getViewVector(1.0F));
        var distant = ToolTarget.trace(player, 64);
        check(helper, distant != null && distant.pos().equals(target) && distant.face() == Direction.UP,
                "extended targeting must resolve the distant block and its actual face");
        BlockPos nearer = target.above(2);
        helper.getLevel().setBlockAndUpdate(nearer, Blocks.DIRT.defaultBlockState());
        var nearest = ToolTarget.trace(player, 64);
        check(helper, nearest != null && nearest.pos().equals(nearer), "the ray must stop at the nearer obstruction");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void anchorKeepsThePlanWhenPlayerMovesAndTurns(GameTestHelper helper) {
        var player = helper.makeMockPlayer();
        player.getAbilities().instabuild = true;
        ItemStack tool = MMRegisters.MULTIBLOCK_TOOL.get().getDefaultInstance();
        ToolData.setBuilderStructure(tool, TOWER);
        BlockPos clicked = helper.absolutePos(new BlockPos(3, 0, 3));
        var anchor = new ToolData.Anchor(helper.getLevel().dimension().location(), clicked, Direction.UP, Direction.NORTH);
        ToolData.setAnchor(tool, anchor);
        ItemStack saved = tool.copy();
        check(helper, anchor.equals(ToolData.anchor(saved)), "anchor must survive item serialization/copy");
        player.setPos(clicked.getX(), clicked.getY() + 2, clicked.getZ());
        player.setYRot(180);
        var first = ToolBuilds.prepare(helper.getLevel(), player, saved, clicked, Direction.UP);
        check(helper, first.prepared() != null, "anchored tower should be buildable");
        player.setPos(clicked.getX() + 5, clicked.getY() + 2, clicked.getZ() + 5);
        player.setYRot(90);
        var target = ToolTarget.resolve(player, saved, 64);
        check(helper, target != null && target.pos().equals(clicked) && target.facing() == Direction.NORTH,
                "anchor target/facing must ignore the player's new position and rotation");
        var second = ToolBuilds.prepare(helper.getLevel(), player, saved, target.pos(), target.face());
        check(helper, second.prepared() != null && first.prepared().plan().steps().equals(second.prepared().plan().steps()),
                "hologram/placement plan must not move when walking around an anchor");
        ToolData.setExtraTurns(saved, 1);
        check(helper, ToolData.anchor(saved).equals(anchor), "extra rotation must keep the anchor");
        player.setPos(clicked.getX() + 80, clicked.getY() + 2, clicked.getZ());
        check(helper, ToolTarget.resolve(player, saved, 64) == null, "anchor must respect the server's range");
        ToolData.setAnchor(saved, new ToolData.Anchor(ResourceLocation.tryBuild("minecraft", "the_nether"), clicked, Direction.UP, Direction.NORTH));
        check(helper, ToolTarget.resolve(player, saved, 128) == null, "anchor from another dimension must be refused");
        ToolData.setBuilderStructure(saved, TOWER);
        check(helper, ToolData.anchor(saved) == null, "selecting a structure clears its old anchor");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void layerModeFinishesTheLowerLayerBeforeTheNextTick(GameTestHelper helper) {
        var player = helper.makeMockPlayer();
        player.getInventory().add(new ItemStack(Blocks.STONE, 3));
        BlockPos lowA = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos lowB = helper.absolutePos(new BlockPos(3, 1, 2));
        BlockPos high = helper.absolutePos(new BlockPos(2, 2, 2));
        var plan = new AssemblyPlanner.Plan(List.of(step(high), step(lowB), step(lowA)), 0);
        var source = new PlayerMaterialSource(player);
        var job = AssemblyJob.createTool(helper.getLevel(), lowA, plan, 0, false, ToolBuildMode.LAYER_BY_LAYER);
        check(helper, !AssemblyJobs.tickBuild(player, job, source, 100), "first tick must stop at the layer boundary");
        check(helper, helper.getLevel().getBlockState(lowA).is(Blocks.STONE) && helper.getLevel().getBlockState(lowB).is(Blocks.STONE),
                "lower layer should be finished first");
        check(helper, helper.getLevel().getBlockState(high).isAir(), "upper layer must wait for the next tick");
        check(helper, AssemblyJobs.tickBuild(player, job, source, 100) && job.placed() == 3, "second tick finishes the upper layer");
        check(helper, player.getInventory().countItem(Blocks.STONE.asItem()) == 0, "layer mode must pay normal material costs");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void instantAndSequentialModesKeepCostsAndObstructionChecks(GameTestHelper helper) {
        var player = helper.makeMockPlayer();
        player.getInventory().add(new ItemStack(Blocks.STONE, 3));
        BlockPos first = helper.absolutePos(new BlockPos(2, 1, 2));
        BlockPos blocked = helper.absolutePos(new BlockPos(3, 1, 2));
        BlockPos last = helper.absolutePos(new BlockPos(4, 1, 2));
        helper.getLevel().setBlockAndUpdate(blocked, Blocks.DIRT.defaultBlockState());
        var plan = new AssemblyPlanner.Plan(List.of(step(first), step(blocked), step(last)), 0);
        var source = new PlayerMaterialSource(player);
        var sequential = AssemblyJob.createTool(helper.getLevel(), first, plan, 0, false, ToolBuildMode.SEQUENTIAL);
        check(helper, !AssemblyJobs.tickBuild(player, sequential, source, ToolBuildMode.SEQUENTIAL.budget(1)), "one block/tick must not finish the job");
        check(helper, helper.getLevel().getBlockState(last).isAir(), "sequential mode must respect the budget");
        var instant = AssemblyJob.createTool(helper.getLevel(), first, plan, 0, false, ToolBuildMode.INSTANT);
        check(helper, AssemblyJobs.tickBuild(player, instant, source, ToolBuildMode.INSTANT.budget(1)), "instant finishes in one tick");
        check(helper, instant.blocked() == 1 && helper.getLevel().getBlockState(blocked).is(Blocks.DIRT), "instant must never replace an obstruction");
        check(helper, player.getInventory().countItem(Blocks.STONE.asItem()) == 1, "only the two placed blocks may be consumed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void layerOrderKeepsTheControllerFirstAndPacketsKeepTheHand(GameTestHelper helper) {
        check(helper, ToolBuildMode.SEQUENTIAL.forTool(true) == ToolBuildMode.INSTANT,
                "the existing instant preference must remain effective in sequential mode");
        check(helper, ToolBuildMode.LAYER_BY_LAYER.forTool(true) == ToolBuildMode.LAYER_BY_LAYER,
                "the explicit server layer mode must override a tool's instant preference");
        check(helper, ToolBuildMode.INSTANT.forTool(false) == ToolBuildMode.INSTANT,
                "the explicit server instant mode must override a tool's sequential preference");
        BlockPos controller = new BlockPos(3, 8, 3);
        var plan = new AssemblyPlanner.Plan(List.of(step(controller), step(new BlockPos(3, 2, 3)), step(new BlockPos(3, 1, 3))), 7);
        var sorted = ToolBuildMode.LAYER_BY_LAYER.order(plan, controller, true);
        check(helper, sorted.steps().get(0).pos().equals(controller) && sorted.steps().get(1).pos().getY() == 1
                && sorted.unavailable() == 7, "MM controller must remain first, even above the lower layers");
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ToolBuildPkt.encode(new ToolBuildPkt(InteractionHand.OFF_HAND), buf);
            check(helper, ToolBuildPkt.decode(buf).hand() == InteractionHand.OFF_HAND, "build packet must preserve offhand");
            buf.clear();
            ToolAnchorPkt.encode(new ToolAnchorPkt(InteractionHand.MAIN_HAND), buf);
            check(helper, ToolAnchorPkt.decode(buf).hand() == InteractionHand.MAIN_HAND, "anchor packet must preserve main hand");
        } finally { buf.release(); }
        helper.succeed();
    }

    private static AssemblyPlanner.Planned step(BlockPos pos) {
        return new AssemblyPlanner.Planned(pos, Blocks.STONE.defaultBlockState(), List.of(Blocks.STONE));
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
