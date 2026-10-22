package io.ticticboom.mods.mmtest;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.model.RecipeSelectionMode;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("mmtest")
@PrefixGameTestTemplate(false)
public class FlexRoundRobinGameTests {
    private static final BlockPos CONTROLLER = new BlockPos(3, 1, 3);
    private static final BlockPos INPUT = CONTROLLER.offset(-1, 0, 0);
    private static final BlockPos OUTPUT = CONTROLLER.offset(1, 0, 0);

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void catalystsRotateAndKeepTheirTurnAfterReload(GameTestHelper helper) {
        build(helper, RecipeSelectionMode.ROUND_ROBIN_INPUT_ITEM);
        helper.startSequence()
                .thenWaitUntil(() -> formed(helper))
                .thenExecute(() -> {
                    insert(helper, Items.WHITE_WOOL, 1);
                    insert(helper, Items.RED_WOOL, 1);
                })
                .thenWaitUntil(() -> check(helper, count(helper, Items.WHITE_CARPET) == 1, "first slot should run first"))
                .thenExecute(() -> reload(helper))
                .thenWaitUntil(() -> check(helper, count(helper, Items.RED_CARPET) == 1, "second kind must get the next turn after reload"))
                .thenExecute(() -> check(helper, count(helper, Items.WHITE_CARPET) == 1, "first kind must not run twice before the second"))
                .thenWaitUntil(() -> check(helper, count(helper, Items.WHITE_CARPET) == 2, "rotation must return to the first kind"))
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void defaultModeKeepsSlotOrder(GameTestHelper helper) {
        build(helper, RecipeSelectionMode.DEFAULT);
        helper.startSequence().thenWaitUntil(() -> formed(helper)).thenExecute(() -> {
                    insert(helper, Items.WHITE_WOOL, 1);
                    insert(helper, Items.RED_WOOL, 1);
                })
                .thenWaitUntil(() -> check(helper, count(helper, Items.WHITE_CARPET) >= 2, "first kind should repeat in default mode"))
                .thenExecute(() -> check(helper, count(helper, Items.RED_CARPET) == 0, "default must not rotate"))
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void maxTypesRotatesAllPickedKinds(GameTestHelper helper) {
        build(helper, RecipeSelectionMode.ROUND_ROBIN_INPUT_ITEM);
        helper.startSequence().thenWaitUntil(() -> formed(helper)).thenExecute(() -> {
                    insert(helper, Items.OAK_LOG, 1);
                    insert(helper, Items.BIRCH_LOG, 1);
                    insert(helper, Items.SPRUCE_LOG, 1);
                })
                .thenWaitUntil(() -> check(helper, count(helper, Items.OAK_PLANKS) == 1, "first batch should finish"))
                .thenExecute(() -> {
                    check(helper, count(helper, Items.BIRCH_PLANKS) == 1, "max_types 2 must pick two kinds");
                    check(helper, count(helper, Items.SPRUCE_PLANKS) == 0, "third kind waits for next batch");
                })
                .thenWaitUntil(() -> check(helper, count(helper, Items.SPRUCE_PLANKS) == 1, "third kind must get into the second batch"))
                .thenExecute(() -> check(helper, count(helper, Items.BIRCH_PLANKS) == 1, "both kinds in the first batch must be recorded as used"))
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void onlyAffordableKindsLoseTheirTurn(GameTestHelper helper) {
        build(helper, RecipeSelectionMode.ROUND_ROBIN_INPUT_ITEM);
        helper.startSequence().thenWaitUntil(() -> formed(helper)).thenExecute(() -> {
                    insert(helper, Items.OAK_SAPLING, 1);
                    insert(helper, Items.BIRCH_SAPLING, 1);
                    insert(helper, Items.BONE_MEAL, 1);
                })
                .thenWaitUntil(() -> check(helper, count(helper, Items.OAK_LEAVES) == 1, "one bone meal pays for the first kind"))
                .thenExecute(() -> {
                    check(helper, count(helper, Items.BIRCH_LEAVES) == 0, "unaffordable kind must wait");
                    insert(helper, Items.BONE_MEAL, 1);
                })
                .thenWaitUntil(() -> check(helper, count(helper, Items.BIRCH_LEAVES) == 1, "dropped kind must keep its turn"))
                .thenExecute(() -> check(helper, count(helper, Items.OAK_LEAVES) == 1, "first kind must wait for the second"))
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void nbtVariantsHaveSeparateTurns(GameTestHelper helper) {
        build(helper, RecipeSelectionMode.ROUND_ROBIN_INPUT_ITEM);
        helper.startSequence().thenWaitUntil(() -> formed(helper)).thenExecute(() -> {
                    for (int variant = 1; variant <= 2; variant++) {
                        var stack = new ItemStack(Items.WHITE_WOOL);
                        stack.getOrCreateTag().putInt("variant", variant);
                        storage(helper, INPUT).insert(stack, 1);
                    }
                })
                .thenWaitUntil(() -> check(helper, pickedVariant(helper) == 1, "first NBT variant should start"))
                .thenWaitUntil(() -> check(helper, pickedVariant(helper) == 2, "same item with different NBT needs its own turn"))
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void flexRecipesShareTheRoundRobinScheduler(GameTestHelper helper) {
        build(helper, RecipeSelectionMode.ROUND_ROBIN_INPUT_ITEM);
        helper.startSequence().thenWaitUntil(() -> formed(helper)).thenExecute(() -> {
                    insert(helper, Items.WHITE_WOOL, 1);
                    insert(helper, Items.OAK_LOG, 1);
                })
                .thenWaitUntil(() -> check(helper, count(helper, Items.WHITE_CARPET) > 0 && count(helper, Items.OAK_PLANKS) > 0,
                        "one flex recipe must not starve another"))
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void blockedOutputKindsKeepTheirTurn(GameTestHelper helper) {
        build(helper, RecipeSelectionMode.ROUND_ROBIN_INPUT_ITEM);
        helper.startSequence().thenWaitUntil(() -> formed(helper)).thenExecute(() -> {
                    insert(helper, Items.WHITE_WOOL, 1);
                    insert(helper, Items.RED_WOOL, 1);
                    var output = storage(helper, OUTPUT);
                    output.insert(new ItemStack(Items.STONE), 64);
                    output.insert(new ItemStack(Items.DIRT), 64);
                    output.insert(new ItemStack(Items.COBBLESTONE), 64);
                    output.insert(new ItemStack(Items.RED_CARPET), 1);
                })
                .thenWaitUntil(() -> check(helper, count(helper, Items.RED_CARPET) >= 2, "red output should fit while white cannot"))
                .thenExecute(() -> {
                    check(helper, count(helper, Items.WHITE_CARPET) == 0, "blocked kind must not start");
                    storage(helper, OUTPUT).getHandler().setStackInSlot(0, ItemStack.EMPTY);
                })
                .thenWaitUntil(() -> check(helper, count(helper, Items.WHITE_CARPET) == 1, "blocked kind must run when room becomes available"))
                .thenSucceed();
    }

    private static int pickedVariant(GameTestHelper helper) {
        var state = controller(helper).getRecipeState();
        if (state == null || state.getCaptured("kind").isEmpty()) return 0;
        CompoundTag tag = state.getCaptured("kind").get(0).getTag();
        return tag == null ? 0 : tag.getInt("variant");
    }

    private static void build(GameTestHelper helper, RecipeSelectionMode mode) {
        helper.setBlock(CONTROLLER, ForgeRegistries.BLOCKS.getValue(Ref.id("flex_round_robin_test"))
                .defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        helper.setBlock(INPUT, ForgeRegistries.BLOCKS.getValue(Ref.id("test_port_flex_round_robin_input")));
        helper.setBlock(OUTPUT, ForgeRegistries.BLOCKS.getValue(Ref.id("test_port_flex_round_robin_output")));
        controller(helper).setRecipeSelectionMode(mode);
    }

    private static MachineControllerBlockEntity controller(GameTestHelper helper) {
        return (MachineControllerBlockEntity) helper.getBlockEntity(CONTROLLER);
    }

    private static void formed(GameTestHelper helper) {
        check(helper, controller(helper).isFormed() && controller(helper).getCachedPortStorages() != null, "machine not formed yet");
    }

    private static void insert(GameTestHelper helper, Item item, int count) {
        storage(helper, INPUT).insert(new ItemStack(item), count);
    }

    private static int count(GameTestHelper helper, Item item) {
        var handler = storage(helper, OUTPUT).getHandler();
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (handler.getStackInSlot(slot).is(item)) total += handler.getActualCount(slot);
        }
        return total;
    }

    private static ItemPortStorage storage(GameTestHelper helper, BlockPos pos) {
        return (ItemPortStorage) ((AbstractPortBlockEntity) helper.getBlockEntity(pos)).getStorage();
    }

    private static void reload(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(CONTROLLER);
        var tag = controller(helper).saveWithFullMetadata();
        level.removeBlockEntity(pos);
        BlockEntity fresh = BlockEntity.loadStatic(pos, level.getBlockState(pos), tag);
        check(helper, fresh != null, "controller must reload");
        level.setBlockEntity(fresh);
    }
    private static void check(GameTestHelper helper, boolean ok, String message) {
        if (!ok) helper.fail(message);
    }
}
