package io.ticticboom.mods.mmtest;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.energy.EnergyPortStorage;
import io.ticticboom.mods.mm.port.fluid.FluidPortStorage;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Bolt's six retained seeds / max_types 3 scenario, without optional mods: six wool kinds stand in for
 * seeds, carpets for essences, and water for ether gas. Uses the same flex/consume entries, 400 ticks,
 * 1,000,000 FE, 100 mB and 64 output per kind, with the controller's parallel limit set to 3.
 * Mystical Agriculture tier filtering and the KubeJS script itself still need testing in the pack.
 */
@GameTestHolder
public class FlexBatchGameTests {
    private static final BlockPos CONTROLLER = new BlockPos(3, 1, 3);
    private static final BlockPos INPUT = CONTROLLER.offset(-1, 0, 0);
    private static final BlockPos OUTPUT = CONTROLLER.offset(1, 0, 0);
    private static final BlockPos ENERGY = CONTROLLER.offset(0, 0, 1);
    private static final BlockPos FLUID = CONTROLLER.offset(1, 0, 1);
    private static final Item[] KINDS = { Items.WHITE_WOOL, Items.ORANGE_WOOL, Items.MAGENTA_WOOL,
            Items.LIGHT_BLUE_WOOL, Items.YELLOW_WOOL, Items.LIME_WOOL };
    private static final Item[] OUTPUTS = { Items.WHITE_CARPET, Items.ORANGE_CARPET, Items.MAGENTA_CARPET,
            Items.LIGHT_BLUE_CARPET, Items.YELLOW_CARPET, Items.LIME_CARPET };

    @GameTest(template = "empty", timeoutTicks = 1100)
    public static void sixRetainedKindsRunInTwoBatchesOfThree(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, BuiltInRegistries.BLOCK.getValue(Ref.id("flex_batch_test"))
                .defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        place(helper, INPUT, "test_port_flex_batch_item_input");
        place(helper, OUTPUT, "test_port_flex_batch_item_output");
        place(helper, ENERGY, "test_port_flex_batch_energy_input");
        place(helper, FLUID, "test_port_flex_batch_fluid_input");
        long[] startedAt = { 0 };
        helper.startSequence()
                .thenWaitUntil(() -> check(helper, controller(helper).isFormed()
                        && controller(helper).getPortStorages() != null, "batch machine not formed"))
                .thenExecute(() -> {
                    check(helper, controller(helper).getDisplayedParallelLimit() == 3, "controller parallel limit must match the report");
                    for (Item kind : KINDS) items(helper, INPUT).insert(new ItemStack(kind), 1);
                    check(helper, energy(helper).internalInsert(6000000, false) == 6000000, "fill energy for two batches");
                    check(helper, fluid(helper).getHandler().fill(new FluidStack(Fluids.WATER, 600), IFluidHandler.FluidAction.EXECUTE) == 600,
                            "fill fluid for two batches");
                })
                .thenWaitUntil(() -> check(helper, controller(helper).getRecipeState() != null, "first batch should start"))
                .thenExecute(() -> {
                    startedAt[0] = controller(helper).saveWithFullMetadata(helper.getLevel().registryAccess()).getLongOr("lastRecipeStartTime", 0L);
                    picked(helper, 0);
                    check(helper, controller(helper).getActiveRecipeCount() == 1, "max_types must not duplicate the same recipe");
                    check(helper, energy(helper).getStoredEnergy() == 3000000, "linear cost: three kinds use 3,000,000 FE");
                    check(helper, fluidAmount(helper) == 300, "linear cost: three kinds use 300 mB");
                })
                .thenWaitUntil(() -> check(helper, count(items(helper, OUTPUT), OUTPUTS[0]) == 64, "first batch should make its outputs"))
                .thenExecute(() -> {
                    check(helper, helper.getLevel().getGameTime() - startedAt[0] + 1 >= 400, "batch must take 400 ticks");
                    for (int i = 0; i < 6; i++) {
                        check(helper, count(items(helper, OUTPUT), OUTPUTS[i]) == (i < 3 ? 64 : 0), "first batch must contain only the first three kinds");
                    }
                })
                .thenWaitUntil(() -> check(helper, controller(helper).getRecipeState() != null, "second batch should start"))
                .thenExecute(() -> {
                    picked(helper, 3);
                    check(helper, energy(helper).getStoredEnergy() == 0 && fluidAmount(helper) == 0, "second batch must pay its own costs");
                })
                .thenWaitUntil(() -> check(helper, count(items(helper, OUTPUT), OUTPUTS[5]) == 64, "second batch should finish"))
                .thenExecute(() -> {
                    for (int i = 0; i < 6; i++) {
                        check(helper, count(items(helper, OUTPUT), OUTPUTS[i]) == 64, "every kind must produce exactly once");
                        check(helper, count(items(helper, INPUT), KINDS[i]) == 1, "chance 0 must keep every input");
                    }
                })
                .thenSucceed();
    }

    private static void picked(GameTestHelper helper, int offset) {
        var picked = controller(helper).getRecipeState().getCaptured("seed");
        check(helper, picked.size() == 3, "max_types must select three kinds");
        for (int i = 0; i < 3; i++) check(helper, picked.get(i).is(KINDS[offset + i]), "wrong kind in batch");
    }

    private static void place(GameTestHelper helper, BlockPos pos, String id) {
        helper.setBlock(pos, BuiltInRegistries.BLOCK.getValue(Ref.id(id)));
    }

    private static MachineControllerBlockEntity controller(GameTestHelper helper) {
        return (MachineControllerBlockEntity) helper.getBlockEntity(CONTROLLER, BlockEntity.class);
    }

    private static ItemPortStorage items(GameTestHelper helper, BlockPos pos) {
        return (ItemPortStorage) ((AbstractPortBlockEntity) helper.getBlockEntity(pos, BlockEntity.class)).getStorage();
    }

    private static EnergyPortStorage energy(GameTestHelper helper) {
        return (EnergyPortStorage) ((AbstractPortBlockEntity) helper.getBlockEntity(ENERGY, BlockEntity.class)).getStorage();
    }

    private static FluidPortStorage fluid(GameTestHelper helper) {
        return (FluidPortStorage) ((AbstractPortBlockEntity) helper.getBlockEntity(FLUID, BlockEntity.class)).getStorage();
    }

    private static int fluidAmount(GameTestHelper helper) {
        return fluid(helper).getHandler().getFluidInTank(0).getAmount();
    }

    private static int count(ItemPortStorage storage, Item item) {
        var handler = storage.getHandler();
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (handler.getStackInSlot(slot).is(item)) total += handler.getActualCount(slot);
        }
        return total;
    }

    private static void check(GameTestHelper helper, boolean ok, String message) {
        if (!ok) helper.fail(message);
    }
}
