package io.ticticboom.mods.mmtest;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.controller.machine.register.ModelDisplay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The controller screen's model switch (On / Off / Hidden): kept in the controller's data (saved, sent to clients in
 * the update tag), so it stays after a reload and applies for every player.
 */
@GameTestHolder("mmtest")
@PrefixGameTestTemplate(false)
public class ControllerAnimationGameTests {
    private static final String TEMPLATE = "empty";
    private static final BlockPos CONTROLLER = new BlockPos(3, 1, 3);

    @GameTest(template = TEMPLATE)
    public static void modelDisplayIsSavedAndSynced(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, block("flex_test").defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        var controller = (MachineControllerBlockEntity) helper.getBlockEntity(CONTROLLER);
        check(helper, controller.getModelDisplay() == ModelDisplay.ON, "the model should be on by default");
        check(helper, !controller.getUpdateTag().contains("ModelDisplay"), "nothing to save while it is on");

        for (ModelDisplay display : new ModelDisplay[]{ModelDisplay.OFF, ModelDisplay.HIDDEN}) {
            controller.setModelDisplay(display);
            check(helper, controller.getUpdateTag().getString("ModelDisplay").equals(display.serializedName()),
                    "clients get " + display + " in the update tag");
            // a chunk reload loads the controller from its saved data
            BlockPos pos = helper.absolutePos(CONTROLLER);
            var reloaded = BlockEntity.loadStatic(pos, helper.getLevel().getBlockState(pos), controller.saveWithFullMetadata());
            check(helper, reloaded instanceof MachineControllerBlockEntity be && be.getModelDisplay() == display,
                    display + " should survive a reload");
        }

        controller.setModelDisplay(ModelDisplay.ON);
        check(helper, !controller.getUpdateTag().contains("ModelDisplay"), "switched back on, nothing left in the data");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void modelDisplayCyclesAndSkipsOffWithoutAnimations(GameTestHelper helper) {
        check(helper, ModelDisplay.ON.next(true) == ModelDisplay.OFF, "On -> Off");
        check(helper, ModelDisplay.OFF.next(true) == ModelDisplay.HIDDEN, "Off -> Hidden");
        check(helper, ModelDisplay.HIDDEN.next(true) == ModelDisplay.ON, "Hidden -> On");
        // a model without animations: Off would change nothing
        check(helper, ModelDisplay.ON.next(false) == ModelDisplay.HIDDEN, "On -> Hidden without animations");
        check(helper, ModelDisplay.HIDDEN.next(false) == ModelDisplay.ON, "Hidden -> On without animations");
        check(helper, ModelDisplay.parse("hidden") == ModelDisplay.HIDDEN && ModelDisplay.parse("bogus") == null, "parse");
        helper.succeed();
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
