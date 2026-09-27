package io.ticticboom.mods.mmtest.client;

import appeng.api.config.Actionable;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.StorageCell;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import io.ticticboom.mods.mm.compat.ae2.NetworkAccess;
import io.ticticboom.mods.mm.networklink.LinkData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.ItemLike;

import java.util.Arrays;

/**
 * The client smoke test's AE2 crafting network (the same as {@code MeToolGameTests}' craftingNetwork), built on the
 * integrated server: creative energy cell, drive with a 1k cell (the craft_test controller and one oak log), 1k crafting
 * storage and a pattern provider with the oak planks pattern. The molecular assembler comes later
 * ({@link #addAssembler}), so the craft stays in progress until the test lets it finish. Only loaded with AE2 present.
 */
final class SmokeAe2 {
    private SmokeAe2() {
    }

    private static BlockPos energy(BlockPos base) {
        return base.offset(0, 0, 1);
    }

    private static BlockPos cpu(BlockPos base) {
        return base.offset(1, 0, 1);
    }

    private static BlockPos provider(BlockPos base) {
        return base.offset(2, 0, 1);
    }

    private static BlockPos assembler(BlockPos base) {
        return base.offset(3, 0, 1);
    }

    /** Places the network with its drive at base; returns the drive. */
    static BlockPos build(ServerLevel level, BlockPos base, ItemLike controller) {
        level.setBlockAndUpdate(energy(base), AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        level.setBlockAndUpdate(base, AEBlocks.DRIVE.block().defaultBlockState());
        level.setBlockAndUpdate(cpu(base), AEBlocks.CRAFTING_STORAGE_1K.block().defaultBlockState());
        level.setBlockAndUpdate(provider(base), AEBlocks.PATTERN_PROVIDER.block().defaultBlockState());

        ItemStack cellStack = AEItems.ITEM_CELL_1K.stack();
        StorageCell cell = StorageCells.getCellInventory(cellStack, null);
        if (cell == null) {
            throw new IllegalStateException("no cell inventory for the 1k cell");
        }
        fill(cell, controller, 1);
        fill(cell, Items.OAK_LOG, 1);
        cell.persist();
        ((DriveBlockEntity) level.getBlockEntity(base)).getInternalInventory().setItemDirect(0, cellStack);

        CraftingRecipe recipe = (CraftingRecipe) level.getRecipeManager().byKey(ResourceLocation.tryBuild("minecraft", "oak_planks"))
                .orElseThrow(() -> new IllegalStateException("no oak_planks recipe"));
        ItemStack[] inputs = new ItemStack[9];
        Arrays.fill(inputs, ItemStack.EMPTY);
        inputs[0] = new ItemStack(Items.OAK_LOG);
        ItemStack pattern = PatternDetailsHelper.encodeCraftingPattern(recipe, inputs, recipe.getResultItem(level.registryAccess()), false, false);
        ItemStack rest = ((PatternProviderBlockEntity) level.getBlockEntity(provider(base))).getLogic().getPatternInv().addItems(pattern);
        if (!rest.isEmpty()) {
            throw new IllegalStateException("the pattern provider did not take the pattern");
        }
        return base;
    }

    /** The network is up: it sees the cell, knows the planks pattern and has its crafting CPU. */
    static boolean ready(ServerLevel level, BlockPos drive) {
        IGrid grid = NetworkAccess.grid(level.getServer(), new LinkData.NetworkPos(level.dimension(), drive, Direction.UP));
        return grid != null && grid.getEnergyService().isNetworkPowered()
                && grid.getStorageService().getCachedInventory().get(AEItemKey.of(Items.OAK_LOG)) > 0
                && grid.getCraftingService().isCraftable(AEItemKey.of(Items.OAK_PLANKS))
                && !grid.getCraftingService().getCpus().isEmpty();
    }

    /** Whether a crafting CPU is busy (the tool's job was submitted). */
    static boolean crafting(ServerLevel level, BlockPos drive) {
        IGrid grid = NetworkAccess.grid(level.getServer(), new LinkData.NetworkPos(level.dimension(), drive, Direction.UP));
        return grid != null && grid.getCraftingService().getCpus().stream().anyMatch(cpu -> cpu.isBusy());
    }

    /** The network's live stock of item (0 when it is down). */
    static long stock(ServerLevel level, BlockPos drive, ItemLike item) {
        IGrid grid = NetworkAccess.grid(level.getServer(), new LinkData.NetworkPos(level.dimension(), drive, Direction.UP));
        return grid == null ? 0 : grid.getStorageService().getInventory().getAvailableStacks().get(AEItemKey.of(item));
    }

    /** The molecular assembler beside the pattern provider: the waiting job can now be crafted. */
    static void addAssembler(ServerLevel level, BlockPos base) {
        level.setBlockAndUpdate(assembler(base), AEBlocks.MOLECULAR_ASSEMBLER.block().defaultBlockState());
    }

    private static void fill(StorageCell cell, ItemLike item, int amount) {
        long inserted = cell.insert(AEItemKey.of(item), amount, Actionable.MODULATE, IActionSource.empty());
        if (inserted != amount) {
            throw new IllegalStateException("could only put " + inserted + " of " + item + " into the cell");
        }
    }
}
