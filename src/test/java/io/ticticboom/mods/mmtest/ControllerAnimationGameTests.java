package io.ticticboom.mods.mmtest;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
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
 * The controller screen's Animation switch: kept in the controller's data (saved, sent to clients in the update tag),
 * so the machine's model stays still after a reload and for every player.
 */
@GameTestHolder("mmtest")
@PrefixGameTestTemplate(false)
public class ControllerAnimationGameTests {
    private static final String TEMPLATE = "empty";
    private static final BlockPos CONTROLLER = new BlockPos(3, 1, 3);

    @GameTest(template = TEMPLATE)
    public static void hiddenAnimationsAreSavedAndSynced(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, block("flex_test").defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        var controller = (MachineControllerBlockEntity) helper.getBlockEntity(CONTROLLER);
        check(helper, !controller.isAnimationsHidden(), "animations should be on by default");
        check(helper, !controller.getUpdateTag().contains("AnimationsHidden"), "nothing to save while they are on");

        controller.setAnimationsHidden(true);
        check(helper, controller.isAnimationsHidden(), "animations should be hidden");
        check(helper, controller.getUpdateTag().getBoolean("AnimationsHidden"), "clients get the switch in the update tag");

        // a chunk reload loads the controller from its saved data
        BlockPos pos = helper.absolutePos(CONTROLLER);
        var reloaded = BlockEntity.loadStatic(pos, helper.getLevel().getBlockState(pos), controller.saveWithFullMetadata());
        check(helper, reloaded instanceof MachineControllerBlockEntity be && be.isAnimationsHidden(), "hidden animations should survive a reload");

        controller.setAnimationsHidden(false);
        check(helper, !controller.getUpdateTag().contains("AnimationsHidden"), "switched back on, nothing left in the data");
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
