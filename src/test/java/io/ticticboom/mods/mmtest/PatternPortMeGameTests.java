package io.ticticboom.mods.mmtest;

import net.minecraft.world.level.block.entity.BlockEntity;
import appeng.api.config.Actionable;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.StorageCell;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.me.helpers.IGridConnectedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Future;

import static io.ticticboom.mods.mmtest.PatternPortGameTests.PROVIDER;
import static io.ticticboom.mods.mmtest.PatternPortGameTests.check;

/**
 * The whole way, as players use it: a real ME network (creative energy cell, drive, 1k crafting storage) with a
 * Pattern Provider holding an encoded processing pattern, next to the Pattern Port of mmtest:pattern_push_test. A
 * craft is asked from the network; the CPU pushes the pattern through the provider and the outputs must come back
 * into the network. Registered by MeGameTests only with AE2.
 */
public class PatternPortMeGameTests {
    static final BlockPos DRIVE = PROVIDER.west();
    static final BlockPos ENERGY = DRIVE.west();
    static final BlockPos CPU = PROVIDER.east();
    static final int TIMEOUT = 600;

    @GameTest(templateNamespace = "mmtest", template = PatternPortGameTests.TEMPLATE, timeoutTicks = TIMEOUT)
    public static void providerRunsAProcessingPattern(GameTestHelper helper) {
        var controller = PatternPortGameTests.build(helper);
        ItemStack cell = itemCell(helper, AEItemKey.of(Items.IRON_INGOT), 1);
        network(helper, processing(new GenericStack[]{new GenericStack(AEItemKey.of(Items.IRON_INGOT), 1)},
                new GenericStack(AEItemKey.of(Items.GOLD_INGOT), 1)), cell);

        craft(helper.startSequence().thenWaitUntil(() -> PatternPortGameTests.formed(helper, controller)), helper, AEItemKey.of(Items.IRON_INGOT), AEItemKey.of(Items.GOLD_INGOT), 1)
                .thenWaitUntil(() -> check(helper, stock(helper, AEItemKey.of(Items.GOLD_INGOT)) == 1 && idle(helper),
                        "the gold should come back into the network, it holds " + grid(helper).getStorageService().getInventory().getAvailableStacks()))
                .thenExecute(() -> check(helper, stock(helper, AEItemKey.of(Items.IRON_INGOT)) == 0, "the iron should be used"))
                .thenSucceed();
    }

    /** A pattern made from JEI also carries the recipe's catalyst: it runs, and the catalyst comes back. */
    @GameTest(templateNamespace = "mmtest", template = PatternPortGameTests.TEMPLATE, timeoutTicks = TIMEOUT)
    public static void providerRunsAPatternWithItsCatalyst(GameTestHelper helper) {
        var controller = PatternPortGameTests.build(helper);
        ItemStack cell = itemCell(helper, AEItemKey.of(Items.REDSTONE), 1);
        insert(helper, cell, AEItemKey.of(Items.BLAZE_ROD), 1);
        network(helper, processing(new GenericStack[]{new GenericStack(AEItemKey.of(Items.REDSTONE), 1), new GenericStack(AEItemKey.of(Items.BLAZE_ROD), 1)},
                new GenericStack(AEItemKey.of(Items.LAPIS_LAZULI), 1)), cell);

        craft(helper.startSequence().thenWaitUntil(() -> PatternPortGameTests.formed(helper, controller)), helper, AEItemKey.of(Items.REDSTONE), AEItemKey.of(Items.LAPIS_LAZULI), 1)
                .thenWaitUntil(() -> check(helper, stock(helper, AEItemKey.of(Items.LAPIS_LAZULI)) == 1
                                && stock(helper, AEItemKey.of(Items.BLAZE_ROD)) == 1 && idle(helper),
                        "the lapis and the blaze rod should come back, the network holds " + grid(helper).getStorageService().getInventory().getAvailableStacks()))
                .thenExecute(() -> check(helper, stock(helper, AEItemKey.of(Items.REDSTONE)) == 0, "the redstone should be used"))
                .thenSucceed();
    }

    /**
     * Asks the network for amount of what, once it knows the pattern and shows the input it needs, and waits until the
     * job is submitted.
     */
    static GameTestSequence craft(GameTestSequence sequence, GameTestHelper helper, AEKey input, AEKey what, long amount) {
        @SuppressWarnings("unchecked")
        Future<ICraftingPlan>[] plan = new Future[1];
        return sequence
                .thenWaitUntil(() -> {
                    IGrid grid = grid(helper);
                    check(helper, grid != null, "the network is not up yet");
                    check(helper, grid.getCraftingService().isCraftable(what), "the pattern is not known yet");
                    check(helper, !grid.getCraftingService().getCpus().isEmpty(), "the crafting CPU is not formed yet");
                    check(helper, grid.getStorageService().getCachedInventory().get(input) > 0, "the cells are not visible yet");
                })
                .thenExecute(() -> {
                    IGrid grid = grid(helper);
                    // the calculation only looks for patterns on the requester's grid: without a node it finds none
                    IGridNode node = grid.getPivot();
                    ICraftingSimulationRequester requester = new ICraftingSimulationRequester() {
                        @Override
                        public IActionSource getActionSource() {
                            return IActionSource.empty();
                        }

                        @Override
                        public IGridNode getGridNode() {
                            return node;
                        }
                    };
                    plan[0] = grid.getCraftingService().beginCraftingCalculation(helper.getLevel(), requester, what, amount,
                            CalculationStrategy.REPORT_MISSING_ITEMS);
                })
                .thenWaitUntil(() -> check(helper, plan[0].isDone(), "the crafting plan is not ready yet"))
                .thenExecute(() -> {
                    ICraftingPlan done;
                    try {
                        done = plan[0].get();
                    } catch (Exception e) {
                        throw new IllegalStateException("the crafting calculation failed", e);
                    }
                    check(helper, !done.simulation(), "the network should have everything, missing " + done.missingItems().keySet());
                    var result = grid(helper).getCraftingService().submitJob(done, null, null, false, IActionSource.empty());
                    check(helper, result.successful(), "the job should be submitted: " + result.errorCode());
                });
    }

    /** Replaces the chest with a Pattern Provider holding pattern, and builds the network around it with the cells. */
    static void network(GameTestHelper helper, ItemStack pattern, ItemStack... cells) {
        helper.setBlock(ENERGY, AEBlocks.CREATIVE_ENERGY_CELL.block());
        helper.setBlock(DRIVE, AEBlocks.DRIVE.block());
        helper.setBlock(CPU, AEBlocks.CRAFTING_STORAGE_1K.block());
        helper.setBlock(PROVIDER, AEBlocks.PATTERN_PROVIDER.block());
        var drive = (DriveBlockEntity) helper.getBlockEntity(DRIVE, BlockEntity.class);
        for (int slot = 0; slot < cells.length; slot++) {
            drive.getInternalInventory().setItemDirect(slot, cells[slot]);
        }
        ItemStack rest = ((PatternProviderBlockEntity) helper.getBlockEntity(PROVIDER, BlockEntity.class)).getLogic().getPatternInv().addItems(pattern);
        check(helper, rest.isEmpty(), "the pattern provider did not take the pattern");
    }

    static ItemStack processing(GenericStack[] inputs, GenericStack... outputs) {
        return PatternDetailsHelper.encodeProcessingPattern(java.util.List.of(inputs), java.util.List.of(outputs));
    }

    static ItemStack itemCell(GameTestHelper helper, AEKey what, long amount) {
        ItemStack cell = AEItems.ITEM_CELL_1K.stack();
        insert(helper, cell, what, amount);
        return cell;
    }

    /** Puts amount of what into the storage cell item. */
    static void insert(GameTestHelper helper, ItemStack cellStack, AEKey what, long amount) {
        StorageCell cell = StorageCells.getCellInventory(cellStack, null);
        check(helper, cell != null, "no cell inventory for " + cellStack);
        long inserted = cell.insert(what, amount, Actionable.MODULATE, IActionSource.empty());
        check(helper, inserted == amount, "could only put " + inserted + " of " + what + " into the cell");
        cell.persist();
    }

    @Nullable
    static IGrid grid(GameTestHelper helper) {
        return ((IGridConnectedBlockEntity) helper.getBlockEntity(DRIVE, BlockEntity.class)).getMainNode().getGrid();
    }

    /** How much of what the network holds, read now. */
    static long stock(GameTestHelper helper, AEKey what) {
        IGrid grid = grid(helper);
        return grid == null ? 0 : grid.getStorageService().getInventory().getAvailableStacks().get(what);
    }

    /** No crafting job is running any more. */
    static boolean idle(GameTestHelper helper) {
        return grid(helper).getCraftingService().getCpus().stream().noneMatch(ICraftingCPU::isBusy);
    }
}
