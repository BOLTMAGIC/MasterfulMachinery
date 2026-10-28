package io.ticticboom.mods.mmtest;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.cap.MMCapabilities;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.setup.MMRegisters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Checks for what the 26.1 port changed underneath MM: registry ids on block and item properties, the transfer API
 * adapters that expose MM's handlers to other mods, and the ValueOutput/ValueInput bridge for block entity data.
 */
@GameTestHolder
public class RegistrySmokeGameTests {
    private static final BlockPos PORT = new BlockPos(1, 1, 1);

    @GameTest
    public static void everyMmBlockAndItemHasItsRegistryId(GameTestHelper helper) {
        for (var entry : MMRegisters.BLOCKS.getEntries()) {
            if (!entry.isBound()) continue;
            var block = entry.get();
            helper.assertTrue(BuiltInRegistries.BLOCK.getKey(block).equals(entry.getId()), "block id mismatch: " + entry.getId());
            helper.assertTrue(block.defaultBlockState() != null, "no default state: " + entry.getId());
        }
        for (var entry : MMRegisters.ITEMS.getEntries()) {
            if (!entry.isBound()) continue;
            var stack = entry.get().getDefaultInstance();
            helper.assertTrue(!stack.isEmpty(), "empty default stack: " + entry.getId());
            helper.assertTrue(BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(entry.getId()), "item id mismatch: " + entry.getId());
        }
        helper.succeed();
    }

    @GameTest
    public static void itemPortTransferAdapterRollsBackAbortedTransactions(GameTestHelper helper) {
        var block = BuiltInRegistries.BLOCK.getValue(Ref.id("test_item_s_input"));
        helper.setBlock(PORT, block);
        var pos = helper.absolutePos(PORT);
        var level = helper.getLevel();

        var handler = level.getCapability(Capabilities.Item.BLOCK, pos, Direction.UP);
        helper.assertTrue(handler != null, "no transfer API item handler on the port");
        var own = level.getCapability(MMCapabilities.ITEM, pos, Direction.UP);
        helper.assertTrue(own != null, "no MM item handler on the port");

        var iron = ItemResource.of(Items.IRON_INGOT);
        try (var tx = Transaction.openRoot()) {
            int inserted = handler.insert(iron, 10, tx);
            helper.assertTrue(inserted == 10, "aborted insert accepted " + inserted);
            helper.assertTrue(own.getStackInSlot(0).getCount() == 10, "insert was not applied inside the transaction");
        }
        helper.assertTrue(own.getStackInSlot(0).isEmpty(), "aborted transaction left " + own.getStackInSlot(0));

        try (var tx = Transaction.openRoot()) {
            handler.insert(iron, 7, tx);
            tx.commit();
        }
        ItemStack after = own.getStackInSlot(0);
        helper.assertTrue(after.is(Items.IRON_INGOT) && after.getCount() == 7, "committed insert left " + after);

        // legacy view of the same handler, as other MM code and mods querying through IItemHandler see it
        var legacy = MMCapabilities.item(level, pos, Direction.UP);
        helper.assertTrue(legacy != null && legacy.getStackInSlot(0).getCount() == 7, "MMCapabilities.item does not see the port");
        helper.succeed();
    }

    @GameTest
    public static void portContentsSurviveTheValueIoBridge(GameTestHelper helper) {
        var block = BuiltInRegistries.BLOCK.getValue(Ref.id("test_item_s_input"));
        helper.setBlock(PORT, block);
        var pos = helper.absolutePos(PORT);
        var level = helper.getLevel();
        var own = level.getCapability(MMCapabilities.ITEM, pos, null);
        helper.assertTrue(own != null, "no MM item handler on the port");
        own.insertItem(0, new ItemStack(Items.DIAMOND, 5), false);

        var be = level.getBlockEntity(pos);
        helper.assertTrue(be instanceof AbstractPortBlockEntity, "not a port block entity: " + be);
        var saved = be.saveCustomOnly(level.registryAccess());
        helper.assertTrue(saved.contains(Ref.NBT_STORAGE_KEY), "saved data lost the 1.21.1 storage key: " + saved.keySet());

        // a fresh block entity loads what the old one wrote
        helper.setBlock(PORT, net.minecraft.world.level.block.Blocks.AIR);
        helper.setBlock(PORT, block);
        var fresh = level.getBlockEntity(pos);
        fresh.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), saved));
        var reloaded = level.getCapability(MMCapabilities.ITEM, pos, null);
        var stack = reloaded.getStackInSlot(0);
        helper.assertTrue(stack.is(Items.DIAMOND) && stack.getCount() == 5, "reloaded port holds " + stack);
        helper.succeed();
    }
}
