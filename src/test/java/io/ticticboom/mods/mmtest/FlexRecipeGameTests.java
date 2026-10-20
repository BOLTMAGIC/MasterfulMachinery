package io.ticticboom.mods.mmtest;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.recipe.MachineRecipeManager;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.input.flex.FlexRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.output.IRecipeOutputEntry;
import io.ticticboom.mods.mm.recipe.output.flex.FlexRecipeOutputEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * Flex recipes, count inputs and the per-kind cost on a real server, with vanilla items. Structure
 * mmtest:flex_test ("ICO", 4-slot ports "test_port_flex"; named to sort after test_item_* so Assemble tests still pick those) and its recipes in data/mmtest/mm/processes/flex:
 * logs -> 2 planks (consumed), wool -> carpet (chance 0), cobblestone counted -> stone, saplings -> leaves
 * with 1 bone meal per kind.
 */
@GameTestHolder("mmtest")
@PrefixGameTestTemplate(false)
public class FlexRecipeGameTests {
    private static final String TEMPLATE = "empty";
    private static final ResourceLocation STRUCTURE = ResourceLocation.tryBuild("mmtest", "flex_test");
    private static final BlockPos CONTROLLER = new BlockPos(3, 1, 3);
    private static final BlockPos INPUT = CONTROLLER.offset(-1, 0, 0);
    private static final BlockPos OUTPUT = CONTROLLER.offset(1, 0, 0);

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void mixedKindsGoInOneRun(GameTestHelper helper) {
        var controller = build(helper);
        helper.startSequence()
                .thenWaitUntil(() -> formed(helper, controller))
                .thenExecute(() -> {
                    insert(helper, Items.OAK_LOG, 1);
                    insert(helper, Items.BIRCH_LOG, 1);
                    // stripped_oak_planks doesn't exist: not accepted, stays
                    insert(helper, Items.STRIPPED_OAK_LOG, 1);
                })
                .thenWaitUntil(() -> check(helper, total(helper, OUTPUT) > 0, "no planks yet"))
                .thenExecute(() -> {
                    // the first outputs to show up already have both kinds: one run took both logs
                    check(helper, count(helper, OUTPUT, Items.OAK_PLANKS) == 2, "oak planks: " + count(helper, OUTPUT, Items.OAK_PLANKS));
                    check(helper, count(helper, OUTPUT, Items.BIRCH_PLANKS) == 2, "birch planks: " + count(helper, OUTPUT, Items.BIRCH_PLANKS));
                    check(helper, count(helper, INPUT, Items.OAK_LOG) == 0, "oak log should be used");
                    check(helper, count(helper, INPUT, Items.BIRCH_LOG) == 0, "birch log should be used");
                    check(helper, count(helper, INPUT, Items.STRIPPED_OAK_LOG) == 1, "stripped log should stay");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void flexOutputShowsOneEntryForJei(GameTestHelper helper) {
        // the flex recipes set no max_types: one entry for the controller screen and one cycling JEI slot, not
        // one per kind (the saplings recipe accepts many kinds)
        int flexOutputs = 0;
        for (RecipeModel recipe : MachineRecipeManager.getRecipesByStrucutreId(STRUCTURE)) {
            for (IRecipeOutputEntry output : recipe.outputs().outputs()) {
                if (output instanceof FlexRecipeOutputEntry) {
                    flexOutputs++;
                    int shown = output.displayedOutputs().size();
                    check(helper, shown == 1, recipe.id() + ": expected 1 displayed flex output, got " + shown);
                    int slots = output.jeiSlotCount(recipe);
                    check(helper, slots == 1, recipe.id() + ": expected 1 JEI slot for the flex output, got " + slots);
                }
            }
        }
        check(helper, flexOutputs > 0, "no flex output in the mmtest:flex_test recipes");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void maxTypesSetsTheJeiLanes(GameTestHelper helper) {
        RecipeModel saplings = null;
        FlexRecipeIngredientEntry flex = null;
        for (RecipeModel recipe : MachineRecipeManager.getRecipesByStrucutreId(STRUCTURE)) {
            for (var input : recipe.inputs().inputs()) {
                if (input instanceof FlexRecipeIngredientEntry entry && entry.getName().equals("sapling")) {
                    saplings = recipe;
                    flex = entry;
                }
            }
        }
        check(helper, flex != null, "no saplings flex recipe in mmtest:flex_test");
        int kinds = flex.acceptedExamples().size();
        check(helper, kinds >= 4, "expected at least 4 sapling kinds with leaves, got " + kinds);
        check(helper, flex.jeiLanes(saplings) == 1, "without max_types one slot cycles through all kinds");

        // the same ingredient with a max_types: one slot per kind a run takes, never more than the kinds or the cap
        var four = new FlexRecipeIngredientEntry("sapling", flex.getIngredient(), 4, 1, List.of(), null, null);
        check(helper, four.jeiLanes(saplings) == 4, "max_types 4 should give 4 slots, got " + four.jeiLanes(saplings));
        var many = new FlexRecipeIngredientEntry("sapling", flex.getIngredient(), 1000, 1, List.of(), null, null);
        // (not bound to an output, so it accepts every sapling, also those without leaves)
        int expected = Math.min(FlexRecipeIngredientEntry.JEI_MAX_LANES, many.acceptedExamples().size());
        check(helper, many.jeiLanes(saplings) == expected, "max_types 1000 should give " + expected + " slots, got " + many.jeiLanes(saplings));

        // lanes list the same kinds started later, so side by side they show different kinds while turning together
        check(helper, FlexRecipeIngredientEntry.forLane(List.of(1, 2, 3), 0).equals(List.of(1, 2, 3)), "lane 0 keeps the order");
        check(helper, FlexRecipeIngredientEntry.forLane(List.of(1, 2, 3), 1).equals(List.of(2, 3, 1)), "lane 1 starts at the 2nd kind");
        check(helper, FlexRecipeIngredientEntry.forLane(List.of(1, 2, 3), 4).equals(List.of(2, 3, 1)), "lanes wrap around");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void chanceZeroKeepsTheInput(GameTestHelper helper) {
        var controller = build(helper);
        helper.startSequence()
                .thenWaitUntil(() -> formed(helper, controller))
                .thenExecute(() -> insert(helper, Items.WHITE_WOOL, 1))
                .thenWaitUntil(() -> check(helper, count(helper, OUTPUT, Items.WHITE_CARPET) >= 1, "no carpet yet"))
                .thenExecute(() -> check(helper, count(helper, INPUT, Items.WHITE_WOOL) == 1, "the wool should stay"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void countScalesTheOutput(GameTestHelper helper) {
        var controller = build(helper);
        helper.startSequence()
                .thenWaitUntil(() -> formed(helper, controller))
                .thenExecute(() -> insert(helper, Items.COBBLESTONE, 3))
                .thenWaitUntil(() -> check(helper, count(helper, OUTPUT, Items.STONE) > 0, "no stone yet"))
                .thenExecute(() -> {
                    check(helper, count(helper, OUTPUT, Items.STONE) == 3, "3 cobblestone should give 3 stone, got " + count(helper, OUTPUT, Items.STONE));
                    check(helper, count(helper, INPUT, Items.COBBLESTONE) == 3, "cobblestone should not be used");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void costIsPaidPerKind(GameTestHelper helper) {
        var controller = build(helper);
        helper.startSequence()
                .thenWaitUntil(() -> formed(helper, controller))
                .thenExecute(() -> {
                    insert(helper, Items.OAK_SAPLING, 1);
                    insert(helper, Items.BIRCH_SAPLING, 1);
                    insert(helper, Items.BONE_MEAL, 2);
                })
                .thenWaitUntil(() -> check(helper, total(helper, OUTPUT) > 0, "no leaves yet"))
                .thenExecute(() -> {
                    check(helper, count(helper, OUTPUT, Items.OAK_LEAVES) == 1 && count(helper, OUTPUT, Items.BIRCH_LEAVES) == 1,
                            "both kinds in one run");
                    check(helper, count(helper, INPUT, Items.BONE_MEAL) == 0, "2 kinds should take 2 bone meal");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void fewerKindsWhenTheCostCantBePaid(GameTestHelper helper) {
        var controller = build(helper);
        helper.startSequence()
                .thenWaitUntil(() -> formed(helper, controller))
                .thenExecute(() -> {
                    insert(helper, Items.OAK_SAPLING, 1);
                    insert(helper, Items.BIRCH_SAPLING, 1);
                    insert(helper, Items.BONE_MEAL, 1);
                })
                .thenWaitUntil(() -> check(helper, total(helper, OUTPUT) > 0, "no leaves yet"))
                .thenExecute(() -> {
                    // 1 bone meal pays for one kind: the first one in the port runs, the other waits
                    check(helper, count(helper, OUTPUT, Items.OAK_LEAVES) == 1, "oak should run");
                    check(helper, count(helper, OUTPUT, Items.BIRCH_LEAVES) == 0, "birch should wait");
                    check(helper, count(helper, INPUT, Items.BIRCH_SAPLING) == 1, "the birch sapling should stay");
                    check(helper, count(helper, INPUT, Items.BONE_MEAL) == 0, "one bone meal used");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void runningKindsSurviveAReload(GameTestHelper helper) {
        var controller = build(helper);
        helper.startSequence()
                .thenWaitUntil(() -> formed(helper, controller))
                .thenExecute(() -> insert(helper, Items.OAK_LOG, 1))
                // the run has taken the log, nothing made yet
                .thenWaitUntil(() -> check(helper, count(helper, INPUT, Items.OAK_LOG) == 0, "log not taken yet"))
                .thenExecute(() -> {
                    check(helper, total(helper, OUTPUT) == 0, "the run should still be going");
                    // save + load the controller: the kind it took comes back from NBT
                    reloadBlockEntity(helper, CONTROLLER);
                })
                .thenWaitUntil(() -> check(helper, count(helper, OUTPUT, Items.OAK_PLANKS) == 2,
                        "oak planks after reload: " + count(helper, OUTPUT, Items.OAK_PLANKS)))
                .thenSucceed();
    }

    private static MachineControllerBlockEntity build(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, block("flex_test").defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        helper.setBlock(INPUT, block("test_port_flex_input"));
        helper.setBlock(OUTPUT, block("test_port_flex_output"));
        return (MachineControllerBlockEntity) helper.getBlockEntity(CONTROLLER);
    }

    private static void formed(GameTestHelper helper, MachineControllerBlockEntity ignored) {
        // read the live block entity: a test may have replaced it
        var controller = (MachineControllerBlockEntity) helper.getBlockEntity(CONTROLLER);
        check(helper, controller.getStructure() != null && controller.getStructure().id().equals(STRUCTURE), "flex_test not formed yet");
    }

    private static void insert(GameTestHelper helper, Item item, int count) {
        storage(helper, INPUT).insert(new ItemStack(item), count);
    }

    private static int count(GameTestHelper helper, BlockPos port, Item item) {
        var handler = storage(helper, port).getHandler();
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (handler.getStackInSlot(slot).is(item)) {
                total += handler.getActualCount(slot);
            }
        }
        return total;
    }

    private static int total(GameTestHelper helper, BlockPos port) {
        var handler = storage(helper, port).getHandler();
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (!handler.getStackInSlot(slot).isEmpty()) {
                total += handler.getActualCount(slot);
            }
        }
        return total;
    }

    private static ItemPortStorage storage(GameTestHelper helper, BlockPos relative) {
        if (helper.getBlockEntity(relative) instanceof AbstractPortBlockEntity port && port.getStorage() instanceof ItemPortStorage items) {
            return items;
        }
        throw new IllegalStateException("no item port at " + relative);
    }

    /** Swaps the block entity for a new one loaded from its data, like a chunk unload + load does. */
    private static void reloadBlockEntity(GameTestHelper helper, BlockPos relative) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(relative);
        BlockEntity old = level.getBlockEntity(pos);
        check(helper, old != null, "no block entity at " + pos);
        var tag = old.saveWithFullMetadata();
        level.removeBlockEntity(pos);
        BlockEntity fresh = BlockEntity.loadStatic(pos, level.getBlockState(pos), tag);
        check(helper, fresh != null && fresh != old, "could not load a new block entity");
        level.setBlockEntity(fresh);
    }

    private static Block block(String id) {
        Block block = ForgeRegistries.BLOCKS.getValue(Ref.id(id));
        if (block == null || block == Blocks.AIR) {
            throw new IllegalStateException("mm:" + id + " is not registered");
        }
        return block;
    }

    private static void check(GameTestHelper helper, boolean ok, String message) {
        if (!ok) {
            helper.fail(message);
        }
    }
}
