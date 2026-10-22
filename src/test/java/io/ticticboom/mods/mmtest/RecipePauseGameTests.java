package io.ticticboom.mods.mmtest;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * A running recipe has already taken its inputs, so nothing may throw it away before its outputs are made.
 * mmtest:recipe_pause_test is "ICO", iron in -> gold out, 60 ticks; mmtest:output_fit_test is "ICO", iron in ->
 * gold + copper out, with a one-slot output port.
 */
@GameTestHolder("mmtest")
@PrefixGameTestTemplate(false)
public class RecipePauseGameTests {
    private static final String TEMPLATE = "empty";
    private static final BlockPos CONTROLLER = new BlockPos(3, 1, 3);
    private static final BlockPos INPUT = CONTROLLER.offset(-1, 0, 0);
    private static final BlockPos OUTPUT = CONTROLLER.offset(1, 0, 0);
    private static final BlockPos NEARBY = CONTROLLER.offset(0, 0, 2);

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void breakingABlockNearTheMachineKeepsTheRecipe(GameTestHelper helper) {
        var controller = build(helper, "recipe_pause_test");
        helper.setBlock(NEARBY, Blocks.DIRT);

        helper.startSequence()
                .thenWaitUntil(() -> formed(helper, controller, "recipe_pause_test"))
                .thenExecute(() -> storage(helper, INPUT).insert(new ItemStack(Items.IRON_INGOT), 1))
                .thenWaitUntil(() -> check(helper, count(helper, INPUT, Items.IRON_INGOT) == 0, "the recipe hasn't taken the iron yet"))
                // a player breaks a block that isn't part of the machine
                .thenExecute(() -> {
                    BlockPos pos = helper.absolutePos(NEARBY);
                    MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(helper.getLevel(), pos,
                            helper.getLevel().getBlockState(pos), helper.makeMockPlayer()));
                    helper.setBlock(NEARBY, Blocks.AIR);
                })
                .thenWaitUntil(() -> check(helper, count(helper, OUTPUT, Items.GOLD_INGOT) == 1,
                        "the iron was taken but no gold came out"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void aBrokenMachinePausesAndResumes(GameTestHelper helper) {
        var controller = build(helper, "recipe_pause_test");

        helper.startSequence()
                .thenWaitUntil(() -> formed(helper, controller, "recipe_pause_test"))
                .thenExecute(() -> storage(helper, INPUT).insert(new ItemStack(Items.IRON_INGOT), 1))
                .thenWaitUntil(() -> check(helper, count(helper, INPUT, Items.IRON_INGOT) == 0, "the recipe hasn't taken the iron yet"))
                .thenExecute(() -> helper.setBlock(OUTPUT, Blocks.AIR))
                // longer than the controller's 200 tick stall timeout
                .thenIdle(260)
                .thenExecute(() -> helper.setBlock(OUTPUT, block("test_wait_item_output")))
                .thenWaitUntil(() -> check(helper, count(helper, OUTPUT, Items.GOLD_INGOT) == 1,
                        "the paused recipe should finish once the machine is whole again"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void outputsThatDontFitTogetherWait(GameTestHelper helper) {
        var controller = build(helper, "output_fit_test");

        helper.startSequence()
                .thenWaitUntil(() -> formed(helper, controller, "output_fit_test"))
                .thenExecute(() -> storage(helper, INPUT).insert(new ItemStack(Items.IRON_INGOT), 1))
                .thenIdle(20)
                // gold and copper each fit in the one empty slot, but not both: the recipe mustn't start
                .thenExecute(() -> {
                    check(helper, count(helper, OUTPUT, Items.COPPER_INGOT) == 0 && count(helper, OUTPUT, Items.GOLD_INGOT) == 0,
                            "an output was made although both can't fit");
                    check(helper, count(helper, INPUT, Items.IRON_INGOT) == 1, "the iron was taken although the outputs can't fit");
                })
                .thenSucceed();
    }

    private static MachineControllerBlockEntity build(GameTestHelper helper, String controllerId) {
        helper.setBlock(CONTROLLER, block(controllerId).defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        helper.setBlock(INPUT, block("test_wait_item_input"));
        helper.setBlock(OUTPUT, block("test_wait_item_output"));
        return (MachineControllerBlockEntity) helper.getBlockEntity(CONTROLLER);
    }

    private static void formed(GameTestHelper helper, MachineControllerBlockEntity controller, String structure) {
        ResourceLocation id = ResourceLocation.tryBuild("mmtest", structure);
        check(helper, controller.getStructure() != null && controller.getStructure().id().equals(id), structure + " not formed yet");
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

    private static ItemPortStorage storage(GameTestHelper helper, BlockPos relative) {
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
