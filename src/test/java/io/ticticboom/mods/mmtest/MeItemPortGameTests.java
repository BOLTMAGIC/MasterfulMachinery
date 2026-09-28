package io.ticticboom.mods.mmtest;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.capabilities.Capabilities;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.ae2.Ae2ItemPortStorage;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.port.item.ItemPortStorageModel;
import io.ticticboom.mods.mm.port.item.register.ItemPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Registered by MeGameTests only when AE2 is installed. */
@PrefixGameTestTemplate(false)
public class MeItemPortGameTests {
    @GameTest(templateNamespace = "mmtest", template = "empty")
    public static void itemPortsExposeDirectMeStorage(GameTestHelper helper) {
        Block block = ForgeRegistries.BLOCKS.getValue(Ref.id("test_item_s_input"));
        check(helper, block != null && block != Blocks.AIR, "test input port is not registered");
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        check(helper, helper.getLevel().getBlockEntity(pos) instanceof ItemPortBlockEntity,
                "input port block entity missing");
        var port = (ItemPortBlockEntity) helper.getLevel().getBlockEntity(pos);
        MEStorage direct = port.getCapability(Capabilities.STORAGE, Direction.UP).orElse(null);
        check(helper, direct != null, "AE2 storage capability missing from item port");
        AEItemKey key = AEItemKey.of(Items.STONE);
        IActionSource source = IActionSource.empty();
        check(helper, direct.insert(key, 100, Actionable.SIMULATE, source) == 64,
                "simulation must report the one-slot capacity");
        check(helper, direct.getAvailableStacks().get(key) == 0, "simulation changed the port");
        check(helper, direct.insert(key, 40, Actionable.MODULATE, source) == 40, "direct insertion failed");
        check(helper, direct.getAvailableStacks().get(key) == 40, "AE2 cannot see the port contents");
        check(helper, direct.extract(key, 10, Actionable.MODULATE, source) == 10, "direct extraction failed");
        check(helper, direct.getAvailableStacks().get(key) == 30, "extraction did not update the port");
        helper.succeed();
    }

    @GameTest(templateNamespace = "mmtest", template = "empty")
    public static void itemPortMeStorageSimulatesAndSplitsLargeInsertions(GameTestHelper helper) {
        ItemPortStorage port = new ItemPortStorage(new ItemPortStorageModel(1, 2, () -> false, 16384, 1), () -> {});
        MEStorage direct = new Ae2ItemPortStorage(port, Component.literal("test item port"));
        AEItemKey key = AEItemKey.of(Items.STONE);
        IActionSource source = IActionSource.empty();
        check(helper, direct.insert(key, 40000, Actionable.SIMULATE, source) == 32768,
                "simulation over-reported the two-slot capacity");
        check(helper, direct.getAvailableStacks().get(key) == 0, "simulation changed the port");
        check(helper, direct.insert(key, 30000, Actionable.MODULATE, source) == 30000,
                "large insertion failed to span chunks and slots");
        check(helper, direct.getAvailableStacks().get(key) == 30000, "large insertion lost items");
        check(helper, direct.extract(key, 100, Actionable.MODULATE, source) == 100,
                "large port must support direct extraction");
        helper.succeed();
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }
}
