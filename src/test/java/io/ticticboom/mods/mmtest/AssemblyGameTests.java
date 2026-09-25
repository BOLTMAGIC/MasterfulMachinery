package io.ticticboom.mods.mmtest;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.builder.AssemblyJob;
import io.ticticboom.mods.mm.builder.AssemblyPlanner;
import io.ticticboom.mods.mm.builder.PlayerMaterials;
import io.ticticboom.mods.mm.builder.PortTiers;
import io.ticticboom.mods.mm.builder.TierPrefs;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;

/**
 * Assemble end to end on a real server. Uses the "assembly_test" controller and the three item ports
 * (tiers 1-3) from src/test/gameTestConfig, and the mmtest:assembly_test structure: "FCGS" where F takes
 * tiers 1-3, G is glass and S takes only tier 3.
 */
@GameTestHolder("mmtest")
@PrefixGameTestTemplate(false)
public class AssemblyGameTests {
    private static final String TEMPLATE = "empty";
    private static final ResourceLocation STRUCTURE = ResourceLocation.tryBuild("mmtest", "assembly_test");
    private static final BlockPos CONTROLLER = new BlockPos(3, 1, 3);
    // offsets from the controller in the unrotated layout
    private static final BlockPos FLEX = new BlockPos(-1, 0, 0);
    private static final BlockPos GLASS = new BlockPos(1, 0, 0);
    private static final BlockPos STRICT = new BlockPos(2, 0, 0);

    @GameTest(template = TEMPLATE)
    public static void prefsPickChosenTier(GameTestHelper helper) {
        var prefs = new TierPrefs();
        prefs.set(PortTiers.key(Ref.Ports.ITEM, true), 2);
        BlockPos controller = helper.absolutePos(CONTROLLER);
        var plan = AssemblyPlanner.plan(structure(helper), controller, Rotation.NONE, prefs, b -> true);

        check(helper, plan.size() == 3, "plan should have 3 steps, got " + plan.size());
        expectPlanned(helper, plan, controller.offset(FLEX), port("m"));
        expectPlanned(helper, plan, controller.offset(STRICT), port("l"));
        expectPlanned(helper, plan, controller.offset(GLASS), Blocks.GLASS);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void lowestByDefault(GameTestHelper helper) {
        BlockPos controller = helper.absolutePos(CONTROLLER);
        var plan = AssemblyPlanner.plan(structure(helper), controller, Rotation.NONE, new TierPrefs(), b -> true);

        expectPlanned(helper, plan, controller.offset(FLEX), port("s"));
        expectPlanned(helper, plan, controller.offset(STRICT), port("l"));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void assemblesAndForms(GameTestHelper helper) {
        BlockPos controller = placeController(helper, Direction.NORTH);
        StructureModel structure = structure(helper);
        check(helper, !structure.formed(helper.getLevel(), controller), "structure formed before assembly");

        Player player = helper.makeMockPlayer();
        player.getAbilities().instabuild = true;
        AssemblyJob job = assemble(helper, structure, controller, Direction.NORTH, new TierPrefs(), player);

        check(helper, job.placed() == 3, "expected 3 placed, got " + job.placed());
        check(helper, job.blocked() == 0 && job.missing().isEmpty(), "creative assembly should not block or miss");
        expectBlock(helper, controller.offset(FLEX), port("s"));
        expectBlock(helper, controller.offset(STRICT), port("l"));
        check(helper, structure.formed(helper.getLevel(), controller), "structure not formed after assembly");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void rotatedControllerForms(GameTestHelper helper) {
        BlockPos controller = placeController(helper, Direction.EAST);
        StructureModel structure = structure(helper);
        Rotation rotation = AssemblyPlanner.bestRotation(helper.getLevel(), structure, controller, Direction.EAST);
        check(helper, rotation == Rotation.CLOCKWISE_90, "an empty site should follow the facing, got " + rotation);

        Player player = helper.makeMockPlayer();
        player.getAbilities().instabuild = true;
        var prefs = new TierPrefs();
        prefs.set(PortTiers.key(Ref.Ports.ITEM, true), 2);
        assemble(helper, structure, controller, Direction.EAST, prefs, player);

        expectBlock(helper, controller.offset(GLASS.rotate(Rotation.CLOCKWISE_90)), Blocks.GLASS);
        expectBlock(helper, controller.offset(FLEX.rotate(Rotation.CLOCKWISE_90)), port("m"));
        expectBlock(helper, controller.offset(STRICT.rotate(Rotation.CLOCKWISE_90)), port("l"));
        check(helper, structure.formed(helper.getLevel(), controller), "rotated structure not formed after assembly");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void survivalTakesFromInventory(GameTestHelper helper) {
        BlockPos controller = placeController(helper, Direction.NORTH);
        StructureModel structure = structure(helper);
        // something the player built where the glass belongs
        helper.getLevel().setBlockAndUpdate(controller.offset(GLASS), Blocks.STONE.defaultBlockState());

        Player player = helper.makeMockPlayer();
        check(helper, !player.getAbilities().instabuild, "mock player should not be instabuild");
        Block small = port("s");
        Block large = port("l");
        player.getInventory().add(new ItemStack(small, 2));
        player.getInventory().add(new ItemStack(Blocks.GLASS, 5));
        check(helper, !PlayerMaterials.has(player, large), "player should not have the tier 3 port");

        AssemblyJob job = assemble(helper, structure, controller, Direction.NORTH, new TierPrefs(), player);

        check(helper, job.placed() == 1, "expected 1 placed, got " + job.placed());
        check(helper, job.blocked() == 1, "expected 1 blocked, got " + job.blocked());
        check(helper, job.missing().equals(Map.of(large, 1)), "expected one tier 3 port missing, got " + job.missing());
        check(helper, count(player, small.asItem()) == 1, "one tier 1 port should be taken, left " + count(player, small.asItem()));
        check(helper, count(player, Blocks.GLASS.asItem()) == 5, "glass should not be taken, left " + count(player, Blocks.GLASS.asItem()));
        expectBlock(helper, controller.offset(FLEX), small);
        expectBlock(helper, controller.offset(GLASS), Blocks.STONE);
        expectBlock(helper, controller.offset(STRICT), Blocks.AIR);
        check(helper, !structure.formed(helper.getLevel(), controller), "structure should not form with parts missing");
        helper.succeed();
    }

    private static AssemblyJob assemble(GameTestHelper helper, StructureModel structure, BlockPos controller, Direction facing, TierPrefs prefs, Player player) {
        Rotation rotation = AssemblyPlanner.bestRotation(helper.getLevel(), structure, controller, facing);
        var plan = AssemblyPlanner.plan(structure, controller, rotation, prefs, b -> PlayerMaterials.has(player, b));
        AssemblyJob job = AssemblyJob.create(helper.getLevel(), controller, plan);
        int ticks = 0;
        // a small budget so the job really runs over several ticks
        while (!job.tick(player, 1)) {
            if (++ticks > 100) {
                helper.fail("assembly job did not finish");
            }
        }
        return job;
    }

    private static BlockPos placeController(GameTestHelper helper, Direction facing) {
        Block block = ForgeRegistries.BLOCKS.getValue(Ref.id("assembly_test"));
        check(helper, block != null && block != Blocks.AIR, "controller mm:assembly_test is not registered");
        helper.setBlock(CONTROLLER, block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing));
        return helper.absolutePos(CONTROLLER);
    }

    private static StructureModel structure(GameTestHelper helper) {
        StructureModel model = StructureManager.STRUCTURES.get(STRUCTURE);
        check(helper, model != null, "structure " + STRUCTURE + " is not loaded");
        return model;
    }

    private static Block port(String size) {
        Block block = ForgeRegistries.BLOCKS.getValue(Ref.id("test_item_" + size + "_input"));
        if (block == null || block == Blocks.AIR) {
            throw new IllegalStateException("port mm:test_item_" + size + "_input is not registered");
        }
        return block;
    }

    private static void expectPlanned(GameTestHelper helper, List<AssemblyPlanner.Planned> plan, BlockPos pos, Block expected) {
        for (AssemblyPlanner.Planned planned : plan) {
            if (planned.pos().equals(pos)) {
                check(helper, planned.state().is(expected), "planned " + planned.state() + " at " + pos + ", expected " + expected);
                return;
            }
        }
        helper.fail("nothing planned at " + pos + " in " + plan);
    }

    private static void expectBlock(GameTestHelper helper, BlockPos absolute, Block expected) {
        var state = helper.getLevel().getBlockState(absolute);
        check(helper, state.is(expected), "found " + state + " at " + absolute + ", expected " + expected);
    }

    private static int count(Player player, Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
