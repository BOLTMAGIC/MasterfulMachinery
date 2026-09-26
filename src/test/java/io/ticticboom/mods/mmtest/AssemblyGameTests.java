package io.ticticboom.mods.mmtest;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.builder.AssemblyJob;
import io.ticticboom.mods.mm.builder.AssemblyPlanner;
import io.ticticboom.mods.mm.builder.PlayerMaterials;
import io.ticticboom.mods.mm.builder.PortTiers;
import io.ticticboom.mods.mm.builder.TierPrefs;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
    /** "ACO": A is an anywhere port_type (input, tiers 2-3), O an output port_type (tiers 1-3). */
    private static final ResourceLocation EXTRA = ResourceLocation.tryBuild("mmtest", "assembly_extra");
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
        var plan = AssemblyPlanner.plan(structure(helper), controller, Rotation.NONE, prefs, b -> true).steps();

        check(helper, plan.size() == 3, "plan should have 3 steps, got " + plan.size());
        expectPlanned(helper, plan, controller.offset(FLEX), port("m"));
        expectPlanned(helper, plan, controller.offset(STRICT), port("l"));
        expectPlanned(helper, plan, controller.offset(GLASS), Blocks.GLASS);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void lowestByDefault(GameTestHelper helper) {
        BlockPos controller = helper.absolutePos(CONTROLLER);
        var plan = AssemblyPlanner.plan(structure(helper), controller, Rotation.NONE, new TierPrefs(), b -> true).steps();

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

    @GameTest(template = TEMPLATE)
    public static void otherAllowedTierIsNotBlocked(GameTestHelper helper) {
        BlockPos controller = placeController(helper, Direction.NORTH);
        StructureModel structure = structure(helper);
        // the player already put a tier 2 port where tiers 1-3 fit; the preference (lowest) would be tier 1
        helper.getLevel().setBlockAndUpdate(controller.offset(FLEX), port("m").defaultBlockState());

        Player player = helper.makeMockPlayer();
        player.getAbilities().instabuild = true;
        AssemblyJob job = assemble(helper, structure, controller, Direction.NORTH, new TierPrefs(), player);

        check(helper, job.blocked() == 0, "a valid tier should not count as blocked, got " + job.blocked());
        check(helper, job.placed() == 2, "expected 2 placed, got " + job.placed());
        expectBlock(helper, controller.offset(FLEX), port("m"));
        check(helper, structure.formed(helper.getLevel(), controller), "structure not formed after assembly");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void playerWhoMayNotBuildPlacesNothing(GameTestHelper helper) {
        BlockPos controller = placeController(helper, Direction.NORTH);
        StructureModel structure = structure(helper);
        Player player = helper.makeMockPlayer();
        player.getAbilities().mayBuild = false; // adventure mode
        player.getInventory().add(new ItemStack(port("s"), 2));
        player.getInventory().add(new ItemStack(port("l"), 1));
        player.getInventory().add(new ItemStack(Blocks.GLASS, 5));

        AssemblyJob job = assemble(helper, structure, controller, Direction.NORTH, new TierPrefs(), player);

        check(helper, job.placed() == 0, "expected nothing placed, got " + job.placed());
        check(helper, job.blocked() == 3, "expected 3 blocked, got " + job.blocked());
        check(helper, job.missing().isEmpty(), "nothing should be missing, got " + job.missing());
        expectInventory(helper, player, 2, 1, 5);
        expectUntouched(helper, controller);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void cancelledPlaceEventIsUndoneAndRefunded(GameTestHelper helper) {
        BlockPos controller = placeController(helper, Direction.NORTH);
        StructureModel structure = structure(helper);
        Player player = helper.makeMockPlayer();
        player.getInventory().add(new ItemStack(port("s"), 2));
        player.getInventory().add(new ItemStack(port("l"), 1));
        player.getInventory().add(new ItemStack(Blocks.GLASS, 5));

        // a claim mod refusing this player
        Consumer<BlockEvent.EntityPlaceEvent> claims = event -> {
            if (event.getEntity() == player) {
                event.setCanceled(true);
            }
        };
        MinecraftForge.EVENT_BUS.addListener(claims);
        AssemblyJob job;
        try {
            job = assemble(helper, structure, controller, Direction.NORTH, new TierPrefs(), player);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(claims);
        }

        check(helper, job.placed() == 0, "expected nothing placed, got " + job.placed());
        check(helper, job.blocked() == 3, "expected 3 blocked, got " + job.blocked());
        expectInventory(helper, player, 2, 1, 5);
        expectUntouched(helper, controller);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void plainStacksAreUsedBeforeRenamedOnes(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        ItemStack named = new ItemStack(Blocks.GLASS, 1);
        named.setHoverName(Component.literal("Keep me"));
        player.getInventory().add(named);
        player.getInventory().add(new ItemStack(Blocks.GLASS, 1));

        ItemStack taken = PlayerMaterials.take(player, Blocks.GLASS);

        check(helper, taken != null && !taken.hasTag(), "a plain glass should be taken, got " + taken);
        check(helper, count(player, Blocks.GLASS.asItem()) == 1, "one glass should be left");
        check(helper, player.getInventory().items.stream().anyMatch(stack -> stack.is(Items.GLASS) && stack.hasCustomHoverName()),
                "the renamed glass should be kept");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void anywhereAndOutputPositionsFollowTiers(GameTestHelper helper) {
        BlockPos controller = helper.absolutePos(CONTROLLER);
        StructureModel extra = structure(helper, EXTRA);

        var lowest = AssemblyPlanner.plan(extra, controller, Rotation.NONE, new TierPrefs(), b -> true);
        check(helper, lowest.steps().size() == 2 && lowest.unavailable() == 0, "expected 2 steps, got " + lowest);
        // the anywhere position takes tiers 2-3, the output position tiers 1-3
        expectPlanned(helper, lowest.steps(), controller.offset(FLEX), port("m"));
        expectPlanned(helper, lowest.steps(), controller.offset(GLASS), port("s", false));

        var prefs = new TierPrefs();
        prefs.set(PortTiers.key(Ref.Ports.ITEM, true), 3);
        prefs.set(PortTiers.key(Ref.Ports.ITEM, false), 2);
        var chosen = AssemblyPlanner.plan(extra, controller, Rotation.NONE, prefs, b -> true).steps();
        expectPlanned(helper, chosen, controller.offset(FLEX), port("l"));
        expectPlanned(helper, chosen, controller.offset(GLASS), port("m", false));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void controllerOnlyStoresValidChoices(GameTestHelper helper) {
        BlockPos pos = placeController(helper, Direction.NORTH);
        check(helper, helper.getLevel().getBlockEntity(pos) instanceof MachineControllerBlockEntity, "no controller block entity");
        var controller = (MachineControllerBlockEntity) helper.getLevel().getBlockEntity(pos);

        List<ResourceLocation> ids = controller.getAssemblyCandidates().stream().map(StructureModel::id).toList();
        check(helper, ids.equals(List.of(EXTRA, STRUCTURE)), "candidates should be sorted by id, got " + ids);

        String input = PortTiers.key(Ref.Ports.ITEM, true);
        controller.setAssemblyTier("mm:nonsense/input", 2);
        controller.setAssemblyTier(input, 99);
        controller.setAssemblyTier(PortTiers.key(Ref.Ports.ITEM, false), -5);
        var stored = controller.getAssemblyTiers().asMap();
        check(helper, stored.equals(Map.of(input, 3)), "expected only " + input + "=3, got " + stored);

        controller.setAssemblyStructureId(ResourceLocation.tryBuild("mmtest", "not_a_candidate"));
        check(helper, controller.getAssemblyStructureId() == null, "an unknown structure id should be ignored");
        controller.setAssemblyStructureId(STRUCTURE);
        check(helper, STRUCTURE.equals(controller.getAssemblyStructureId()), "a candidate id should be stored");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void assembleDefaultsToFormedStructure(GameTestHelper helper) {
        BlockPos pos = placeController(helper, Direction.NORTH);
        var controller = (MachineControllerBlockEntity) helper.getLevel().getBlockEntity(pos);
        check(helper, controller.getAssemblyStructureId() == null, "no structure should be chosen yet");
        check(helper, controller.getAssemblyStructure() != null && controller.getAssemblyStructure().id().equals(EXTRA),
                "an empty controller should default to the first candidate, got " + controller.getAssemblyStructure());
        // assembly_test (lowest tiers), which is not the first candidate
        helper.setBlock(CONTROLLER.offset(FLEX), port("s"));
        helper.setBlock(CONTROLLER.offset(GLASS), Blocks.GLASS);
        helper.setBlock(CONTROLLER.offset(STRICT), port("l"));
        helper.startSequence().thenWaitUntil(() -> check(helper, structure(helper).formed(helper.getLevel(), pos) && controller.getStructure() != null
                && controller.getStructure().id().equals(STRUCTURE), "assembly_test not formed yet")).thenExecute(() -> {
            check(helper, controller.getAssemblyStructureId() == null, "forming should not store a choice");
            StructureModel shown = controller.getAssemblyStructure();
            check(helper, shown != null && shown.id().equals(STRUCTURE), "Assemble should default to the formed assembly_test, got " + shown);
            // an explicit choice still wins
            controller.setAssemblyStructureId(EXTRA);
            check(helper, controller.getAssemblyStructure().id().equals(EXTRA), "a stored choice should win over the formed structure");
        }).thenSucceed();
    }

    private static AssemblyJob assemble(GameTestHelper helper, StructureModel structure, BlockPos controller, Direction facing, TierPrefs prefs, Player player) {
        Rotation rotation = AssemblyPlanner.bestRotation(helper.getLevel(), structure, controller, facing);
        var plan = AssemblyPlanner.plan(structure, controller, rotation, prefs, b -> PlayerMaterials.has(player, b));
        check(helper, plan.unavailable() == 0, "every position should have a part, " + plan.unavailable() + " have none");
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
        return structure(helper, STRUCTURE);
    }

    private static StructureModel structure(GameTestHelper helper, ResourceLocation id) {
        StructureModel model = StructureManager.STRUCTURES.get(id);
        check(helper, model != null, "structure " + id + " is not loaded");
        return model;
    }

    private static Block port(String size) {
        return port(size, true);
    }

    private static Block port(String size, boolean input) {
        String id = "test_item_" + size + (input ? "_input" : "_output");
        Block block = ForgeRegistries.BLOCKS.getValue(Ref.id(id));
        if (block == null || block == Blocks.AIR) {
            throw new IllegalStateException("port mm:" + id + " is not registered");
        }
        return block;
    }

    private static void expectInventory(GameTestHelper helper, Player player, int small, int large, int glass) {
        check(helper, count(player, port("s").asItem()) == small, "expected " + small + " tier 1 ports, have " + count(player, port("s").asItem()));
        check(helper, count(player, port("l").asItem()) == large, "expected " + large + " tier 3 ports, have " + count(player, port("l").asItem()));
        check(helper, count(player, Blocks.GLASS.asItem()) == glass, "expected " + glass + " glass, have " + count(player, Blocks.GLASS.asItem()));
    }

    private static void expectUntouched(GameTestHelper helper, BlockPos controller) {
        expectBlock(helper, controller.offset(FLEX), Blocks.AIR);
        expectBlock(helper, controller.offset(GLASS), Blocks.AIR);
        expectBlock(helper, controller.offset(STRICT), Blocks.AIR);
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
