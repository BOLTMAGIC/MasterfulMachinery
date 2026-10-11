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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * A port's block entity is replaced when its chunk unloads and loads again (the block stays). A machine spread over
 * two chunks must then work with the new block entity, not keep using the old one's storage (which voids outputs
 * and makes recipes stall). Uses mmtest:chunk_reload_test: "ICO", iron in -> gold out, 1 tick.
 */
@GameTestHolder("mmtest")
@PrefixGameTestTemplate(false)
public class ChunkReloadGameTests {
    private static final String TEMPLATE = "empty";
    private static final ResourceLocation STRUCTURE = ResourceLocation.tryBuild("mmtest", "chunk_reload_test");
    private static final BlockPos CONTROLLER = new BlockPos(3, 1, 3);
    private static final BlockPos INPUT = CONTROLLER.offset(-1, 0, 0);
    private static final BlockPos OUTPUT = CONTROLLER.offset(1, 0, 0);

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void outputsReachAReloadedPort(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, block("chunk_reload_test").defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        helper.setBlock(INPUT, block("test_reload_item_input"));
        helper.setBlock(OUTPUT, block("test_reload_item_output"));
        var controller = (MachineControllerBlockEntity) helper.getBlockEntity(CONTROLLER);

        helper.startSequence()
                .thenWaitUntil(() -> check(helper, controller.getStructure() != null && controller.getStructure().id().equals(STRUCTURE),
                        "chunk_reload_test not formed yet"))
                // one run, so the controller has picked up (and cached) the ports' storages
                .thenExecute(() -> storage(helper, INPUT).insert(new ItemStack(Items.IRON_INGOT), 1))
                .thenWaitUntil(() -> check(helper, gold(helper) == 1, "first gold not made yet, have " + gold(helper)))
                // the output port's chunk "reloads": same block, new block entity with the same contents
                .thenExecute(() -> reloadBlockEntity(helper, OUTPUT))
                .thenExecute(() -> storage(helper, INPUT).insert(new ItemStack(Items.IRON_INGOT), 1))
                .thenWaitUntil(() -> check(helper, gold(helper) == 2,
                        "the second gold should be in the reloaded output port, it has " + gold(helper)))
                .thenSucceed();
    }

    /** Swaps the block entity for a new one loaded from its data, like a chunk unload + load does. */
    private static void reloadBlockEntity(GameTestHelper helper, BlockPos relative) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(relative);
        BlockEntity old = level.getBlockEntity(pos);
        check(helper, old != null, "no block entity at " + pos);
        var tag = old.saveWithFullMetadata();
        level.removeBlockEntity(pos);
        check(helper, old.isRemoved(), "the old block entity should be removed");
        BlockEntity fresh = BlockEntity.loadStatic(pos, level.getBlockState(pos), tag);
        check(helper, fresh != null && fresh != old, "could not load a new block entity");
        level.setBlockEntity(fresh);
        check(helper, level.getBlockEntity(pos) == fresh, "the new block entity is not in the level");
    }

    private static int gold(GameTestHelper helper) {
        var handler = storage(helper, OUTPUT).getHandler();
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (handler.getStackInSlot(slot).is(Items.GOLD_INGOT)) {
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
