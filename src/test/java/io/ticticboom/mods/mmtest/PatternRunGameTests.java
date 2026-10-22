package io.ticticboom.mods.mmtest;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.pattern.PatternBuffer;
import io.ticticboom.mods.mm.pattern.PatternPortBlockEntity;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.pattern.PatternIngredient;
import io.ticticboom.mods.mm.pattern.PatternIngredient.Kind;
import io.ticticboom.mods.mm.pattern.PatternRun;
import io.ticticboom.mods.mm.pattern.PatternSpec;
import io.ticticboom.mods.mm.pattern.PatternStartResult;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.recipe.MachineRecipeManager;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * Pattern runs in the controller: they take their inputs from their own buffer (never the input ports), count
 * towards the parallel limit, pause with the machine and survive a reload. mmtest:pattern_run_test is "ICO",
 * iron in -> gold out, 60 ticks, at most 1 recipe at a time.
 */
@GameTestHolder("mmtest")
@PrefixGameTestTemplate(false)
public class PatternRunGameTests {
    private static final String TEMPLATE = "empty";
    private static final BlockPos CONTROLLER = new BlockPos(3, 1, 3);
    private static final BlockPos INPUT = CONTROLLER.offset(-1, 0, 0);
    private static final BlockPos OUTPUT = CONTROLLER.offset(1, 0, 0);
    // where a pattern provider would be; empty unless a test puts a chest there
    private static final BlockPos RETURN = CONTROLLER.offset(0, 0, 2);
    private static final ResourceLocation RECIPE = ResourceLocation.tryBuild("mmtest", "pattern_run_test");

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void patternRunUsesTheBufferNotThePorts(GameTestHelper helper) {
        var controller = build(helper);

        helper.startSequence()
                .thenWaitUntil(() -> formed(helper, controller))
                .thenExecute(() -> expectStarted(helper, controller, 1, "the pattern run should start"))
                .thenExecute(() -> check(helper, items(helper, INPUT).getHandler().getStackInSlot(0).isEmpty()
                        && !items(helper, OUTPUT).getHandler().getStackInSlot(0).is(Items.GOLD_INGOT), "the ports aren't used"))
                .thenWaitUntil(() -> check(helper, controller.getPatternRuns().size() == 1 && controller.getPatternRuns().get(0).isReturning(),
                        "the pattern run hasn't finished"))
                .thenExecute(() -> {
                    var left = controller.getPatternRuns().get(0).getBuffer().leftovers();
                    check(helper, left.size() == 1 && left.get(0).sameResource(item("gold_ingot", 1)) && left.get(0).amount() == 1,
                            "the buffer should hold just the gold, it has " + left);
                    check(helper, items(helper, OUTPUT).getHandler().getStackInSlot(0).isEmpty(), "the gold shouldn't be in the output port");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void controllerScreenShowsPatternRunProgress(GameTestHelper helper) {
        var controller = build(helper);

        helper.startSequence()
                .thenWaitUntil(() -> formed(helper, controller))
                .thenExecute(() -> expectStarted(helper, controller, 1, "the pattern run should start"))
                .thenIdle(10)
                .thenExecute(() -> {
                    var state = controller.getRecipeState();
                    check(helper, state != null && state.getTickProgress() > 0,
                            "the controller screen should show the running pattern's progress, state " + state);
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void parallelLimitCountsPatternRuns(GameTestHelper helper) {
        var controller = build(helper);

        helper.startSequence()
                .thenWaitUntil(() -> formed(helper, controller))
                .thenExecute(() -> {
                    expectStarted(helper, controller, 1, "the first pattern run should start");
                    var second = start(helper, controller, 1);
                    check(helper, second == PatternStartResult.PARALLEL_FULL, "the machine runs 1 recipe at a time, got " + second);
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void patternRunPausesWithTheMachine(GameTestHelper helper) {
        var controller = build(helper);

        helper.startSequence()
                .thenWaitUntil(() -> formed(helper, controller))
                .thenExecute(() -> expectStarted(helper, controller, 1, "the pattern run should start"))
                .thenExecute(() -> helper.setBlock(OUTPUT, Blocks.AIR))
                // longer than the controller's stall timeout and the recipe
                .thenIdle(260)
                .thenExecute(() -> check(helper, controller.getPatternRuns().size() == 1 && !controller.getPatternRuns().get(0).isReturning(),
                        "the pattern run should wait while the machine is broken"))
                .thenExecute(() -> helper.setBlock(OUTPUT, block("test_wait_item_output")))
                .thenWaitUntil(() -> check(helper, controller.getPatternRuns().size() == 1 && controller.getPatternRuns().get(0).isReturning(),
                        "the pattern run should finish once the machine is whole again"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void patternRunSurvivesReload(GameTestHelper helper) {
        var controller = build(helper);

        helper.startSequence()
                .thenWaitUntil(() -> formed(helper, controller))
                .thenExecute(() -> expectStarted(helper, controller, 1, "the pattern run should start"))
                .thenIdle(10)
                .thenExecute(() -> reload(helper, tag -> { }))
                .thenWaitUntil(() -> {
                    var runs = live(helper).getPatternRuns();
                    check(helper, runs.size() == 1 && runs.get(0).isReturning(), "the reloaded pattern run hasn't finished");
                    var left = runs.get(0).getBuffer().leftovers();
                    check(helper, left.size() == 1 && left.get(0).sameResource(item("gold_ingot", 1)), "the reloaded run should make the gold, has " + left);
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void aRemovedRecipeReturnsTheBuffer(GameTestHelper helper) {
        var controller = build(helper);
        helper.setBlock(RETURN, Blocks.CHEST);

        helper.startSequence()
                .thenWaitUntil(() -> formed(helper, controller))
                // 2 iron in the buffer, the recipe takes 1
                .thenExecute(() -> expectStarted(helper, controller, 2, "the pattern run should start"))
                .thenExecute(() -> reload(helper, tag -> {
                    var runs = tag.getList("PatternRuns", Tag.TAG_COMPOUND);
                    runs.getCompound(0).putString("Recipe", "mmtest:no_such_recipe");
                }))
                .thenWaitUntil(() -> {
                    check(helper, live(helper).getPatternRuns().isEmpty(), "a run whose recipe is gone should give back what it holds and end");
                    var chest = helper.getBlockEntity(RETURN).getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(IllegalStateException::new);
                    check(helper, chest.getStackInSlot(0).is(Items.IRON_INGOT) && chest.getStackInSlot(0).getCount() == 1,
                            "the unused iron should be in the chest");
                })
                .thenSucceed();
    }

    private static void expectStarted(GameTestHelper helper, MachineControllerBlockEntity controller, int iron, String message) {
        var result = start(helper, controller, iron);
        check(helper, result == PatternStartResult.STARTED, message + ", got " + result);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void patternPortFindsItsMachine(GameTestHelper helper) {
        var controller = build(helper);
        // touching the input port, and far from the machine
        BlockPos touching = INPUT.offset(0, 0, -1);
        BlockPos away = new BlockPos(6, 1, 0);
        helper.setBlock(touching, MMRegisters.PATTERN_PORT.get());
        helper.setBlock(away, MMRegisters.PATTERN_PORT.get());

        helper.startSequence()
                .thenWaitUntil(() -> formed(helper, controller))
                .thenExecute(() -> {
                    check(helper, ((PatternPortBlockEntity) helper.getBlockEntity(touching)).machine() == controller,
                            "a pattern port touching an input port belongs to that machine");
                    check(helper, ((PatternPortBlockEntity) helper.getBlockEntity(away)).machine() == null,
                            "a pattern port away from any machine belongs to none");
                })
                .thenSucceed();
    }

    private static PatternStartResult start(GameTestHelper helper, MachineControllerBlockEntity controller, int iron) {
        RecipeModel recipe = MachineRecipeManager.RECIPES.get(RECIPE);
        var spec = new PatternSpec(List.of(item("iron_ingot", 1)), item("gold_ingot", 1));
        var buffer = PatternBuffer.forRun(spec, recipe);
        check(helper, buffer.insert(item("iron_ingot", iron)), "the buffer should take the iron");
        return controller.startPatternRun(recipe, buffer, helper.absolutePos(RETURN), Direction.NORTH);
    }

    /** Swaps the controller's block entity for one loaded from its (edited) data, like a chunk reload. */
    private static void reload(GameTestHelper helper, java.util.function.Consumer<CompoundTag> edit) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(CONTROLLER);
        BlockEntity old = level.getBlockEntity(pos);
        var tag = old.saveWithFullMetadata();
        edit.accept(tag);
        level.removeBlockEntity(pos);
        BlockEntity fresh = BlockEntity.loadStatic(pos, level.getBlockState(pos), tag);
        check(helper, fresh != null, "could not load the controller again");
        level.setBlockEntity(fresh);
    }

    private static MachineControllerBlockEntity live(GameTestHelper helper) {
        return (MachineControllerBlockEntity) helper.getBlockEntity(CONTROLLER);
    }

    private static MachineControllerBlockEntity build(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, block("pattern_run_test").defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        helper.setBlock(INPUT, block("test_wait_item_input"));
        helper.setBlock(OUTPUT, block("test_wait_item_output"));
        return live(helper);
    }

    private static void formed(GameTestHelper helper, MachineControllerBlockEntity controller) {
        check(helper, controller.getStructure() != null && controller.getStructure().id().equals(RECIPE)
                && !"not_formed".equals(controller.statusKey()), "pattern_run_test not formed yet");
    }

    private static PatternIngredient item(String path, long amount) {
        return new PatternIngredient(Kind.ITEM, ResourceLocation.tryBuild("minecraft", path), amount, null);
    }

    private static ItemPortStorage items(GameTestHelper helper, BlockPos relative) {
        if (helper.getBlockEntity(relative) instanceof AbstractPortBlockEntity port && port.getStorage() instanceof ItemPortStorage items) {
            return items;
        }
        throw new IllegalStateException("no item port at " + relative);
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
