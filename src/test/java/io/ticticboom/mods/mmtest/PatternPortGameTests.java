package io.ticticboom.mods.mmtest;

import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.pattern.PatternRun;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.setup.MMRegisters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * AE2 pushing patterns into a Pattern Port (registered by MeGameTests only with AE2). mmtest:pattern_push_test is
 * "ICO", iron in -> gold out, 40 ticks, 2 recipes at once; the Pattern Port touches the input port and a chest stands
 * where the pattern provider would be.
 */
@PrefixGameTestTemplate(false)
public class PatternPortGameTests {
    static final String TEMPLATE = "empty";
    static final BlockPos CONTROLLER = new BlockPos(3, 1, 3);
    static final BlockPos INPUT = CONTROLLER.offset(-1, 0, 0);
    static final BlockPos OUTPUT = CONTROLLER.offset(1, 0, 0);
    static final BlockPos PORT = INPUT.offset(0, 0, -1);
    /** The side of the port the "provider" (a chest) is on. */
    static final Direction PROVIDER_SIDE = Direction.NORTH;
    static final BlockPos PROVIDER = PORT.relative(PROVIDER_SIDE);

    @GameTest(templateNamespace = "mmtest", template = TEMPLATE, timeoutTicks = 200)
    public static void pushedPatternRunsWithoutInputPorts(GameTestHelper helper) {
        var controller = build(helper);

        helper.startSequence()
                .thenWaitUntil(() -> formed(helper, controller))
                .thenExecute(() -> check(helper, push(helper, ironToGold("iron")), "the pattern port should take the pattern"))
                .thenExecute(() -> check(helper, count(helper, INPUT, Items.IRON_INGOT) == 0, "the input port isn't used"))
                .thenWaitUntil(() -> check(helper, finishedWith(controller, Items.GOLD_INGOT), "the pushed craft hasn't made its gold"))
                .thenSucceed();
    }

    @GameTest(templateNamespace = "mmtest", template = TEMPLATE, timeoutTicks = 100)
    public static void unknownPatternIsRefused(GameTestHelper helper) {
        var controller = build(helper);
        var nothing = TestPattern.of("nothing", new GenericStack[]{new GenericStack(AEItemKey.of(Items.DIRT), 1)},
                new GenericStack(AEItemKey.of(Items.DIAMOND), 1));

        helper.startSequence()
                .thenWaitUntil(() -> formed(helper, controller))
                .thenExecute(() -> {
                    check(helper, !push(helper, nothing), "no recipe makes a diamond from dirt: the port should refuse");
                    check(helper, controller.getPatternRuns().isEmpty(), "nothing should run");
                })
                .thenSucceed();
    }

    @GameTest(templateNamespace = "mmtest", template = TEMPLATE, timeoutTicks = 100)
    public static void samePatternRunsUpToTheParallelLimit(GameTestHelper helper) {
        var controller = build(helper);
        var pattern = ironToGold("iron");

        helper.startSequence()
                .thenWaitUntil(() -> formed(helper, controller))
                .thenExecute(() -> {
                    check(helper, push(helper, pattern), "the first push should run");
                    check(helper, push(helper, pattern), "the second push should run next to it (limit 2)");
                    check(helper, !push(helper, pattern), "the third push should wait for a free slot");
                    check(helper, controller.getPatternRuns().size() == 2, "two crafts should run, there are " + controller.getPatternRuns().size());
                })
                .thenSucceed();
    }

    static TestPattern ironToGold(String name) {
        return TestPattern.of(name, new GenericStack[]{new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1)},
                new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 1));
    }

    static boolean push(GameTestHelper helper, TestPattern pattern) {
        var machine = ICraftingMachine.of(helper.getBlockEntity(PORT), PROVIDER_SIDE);
        check(helper, machine != null, "the pattern port should be an AE2 crafting machine");
        return machine.acceptsPlans() && machine.pushPattern(pattern, pattern.inputs(), PROVIDER_SIDE);
    }

    /** One craft that has finished and holds the item to give back. */
    static boolean finishedWith(MachineControllerBlockEntity controller, Item item) {
        for (PatternRun run : controller.getPatternRuns()) {
            if (run.isReturning() && run.getBuffer().leftovers().stream()
                    .anyMatch(left -> left.id().equals(ForgeRegistries.ITEMS.getKey(item)))) {
                return true;
            }
        }
        return false;
    }

    static MachineControllerBlockEntity build(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, block("pattern_push_test").defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        helper.setBlock(INPUT, block("test_wait_item_input"));
        helper.setBlock(OUTPUT, block("test_wait_item_output"));
        helper.setBlock(PORT, MMRegisters.PATTERN_PORT.get());
        helper.setBlock(PROVIDER, Blocks.CHEST);
        return (MachineControllerBlockEntity) helper.getBlockEntity(CONTROLLER);
    }

    static void formed(GameTestHelper helper, MachineControllerBlockEntity controller) {
        ResourceLocation id = ResourceLocation.tryBuild("mmtest", "pattern_push_test");
        check(helper, controller.getStructure() != null && controller.getStructure().id().equals(id)
                && !"not_formed".equals(controller.statusKey()), "pattern_push_test not formed yet");
    }

    static int count(GameTestHelper helper, BlockPos port, Item item) {
        if (!(helper.getBlockEntity(port) instanceof AbstractPortBlockEntity be) || !(be.getStorage() instanceof ItemPortStorage items)) {
            throw new IllegalStateException("no item port at " + port);
        }
        int total = 0;
        for (int slot = 0; slot < items.getHandler().getSlots(); slot++) {
            if (items.getHandler().getStackInSlot(slot).is(item)) {
                total += items.getHandler().getActualCount(slot);
            }
        }
        return total;
    }

    static Block block(String id) {
        Block block = ForgeRegistries.BLOCKS.getValue(Ref.id(id));
        if (block == null || block == Blocks.AIR) {
            throw new IllegalStateException("mm:" + id + " is not registered");
        }
        return block;
    }

    static void check(GameTestHelper helper, boolean ok, String message) {
        if (!ok) {
            helper.fail(message);
        }
    }
}
