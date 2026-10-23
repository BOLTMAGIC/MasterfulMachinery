package io.ticticboom.mods.mmtest;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.waila.PortPriorityDataProvider;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.item.PrioritySetterItem;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.fluid.FluidPortStorage;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.setup.MMRegisters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import snownee.jade.api.BlockAccessor;

import java.lang.reflect.Proxy;

/**
 * Output port priority: the priority setter, outputs going to the highest priority port first, and the value Jade
 * gets from the server. mmtest:priority_test is "ICOOFF": iron in -> 1 gold + 100 mB water out, two item and two
 * fluid output ports.
 */
@GameTestHolder("mmtest")
@PrefixGameTestTemplate(false)
public class PortPriorityGameTests {
    private static final String TEMPLATE = "empty";
    private static final BlockPos CONTROLLER = new BlockPos(1, 1, 3);
    private static final BlockPos INPUT = CONTROLLER.offset(-1, 0, 0);
    private static final BlockPos ITEM_A = CONTROLLER.offset(1, 0, 0);
    private static final BlockPos ITEM_B = CONTROLLER.offset(2, 0, 0);
    private static final BlockPos FLUID_A = CONTROLLER.offset(3, 0, 0);
    private static final BlockPos FLUID_B = CONTROLLER.offset(4, 0, 0);

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void outputsGoToTheHighestPriorityPort(GameTestHelper helper) {
        var controller = build(helper);

        helper.startSequence()
                .thenWaitUntil(() -> formed(helper, controller))
                // B first: whichever port the default order prefers, priority must win
                .thenExecute(() -> {
                    storage(helper, ITEM_B).setPriority(5);
                    storage(helper, FLUID_B).setPriority(5);
                    items(helper, INPUT).insert(new ItemStack(Items.IRON_INGOT), 1);
                })
                .thenWaitUntil(() -> check(helper, gold(helper, ITEM_A) + gold(helper, ITEM_B) == 1 && water(helper, FLUID_A) + water(helper, FLUID_B) == 100,
                        "the first run hasn't finished"))
                .thenExecute(() -> check(helper, gold(helper, ITEM_B) == 1 && water(helper, FLUID_B) == 100,
                        "outputs should go to the priority 5 ports (B), gold A/B " + gold(helper, ITEM_A) + "/" + gold(helper, ITEM_B)
                                + ", water A/B " + water(helper, FLUID_A) + "/" + water(helper, FLUID_B)))
                .thenExecute(() -> {
                    storage(helper, ITEM_A).setPriority(10);
                    storage(helper, FLUID_A).setPriority(10);
                    items(helper, INPUT).insert(new ItemStack(Items.IRON_INGOT), 1);
                })
                .thenWaitUntil(() -> check(helper, gold(helper, ITEM_A) + gold(helper, ITEM_B) == 2 && water(helper, FLUID_A) + water(helper, FLUID_B) == 200,
                        "the second run hasn't finished"))
                .thenExecute(() -> check(helper, gold(helper, ITEM_A) == 1 && water(helper, FLUID_A) == 100,
                        "outputs should go to the priority 10 ports (A), gold A/B " + gold(helper, ITEM_A) + "/" + gold(helper, ITEM_B)
                                + ", water A/B " + water(helper, FLUID_A) + "/" + water(helper, FLUID_B)))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void prioritySetterAppliesAndIsSaved(GameTestHelper helper) {
        build(helper);

        helper.startSequence()
                .thenExecute(() -> {
                    var player = FakePlayerFactory.getMinecraft(helper.getLevel());
                    player.setShiftKeyDown(true);
                    ItemStack setter = new ItemStack(MMRegisters.PRIORITY_SETTER.get());
                    PrioritySetterItem.setPriorityInItem(setter, 7);
                    player.setItemInHand(InteractionHand.MAIN_HAND, setter);
                    BlockPos pos = helper.absolutePos(ITEM_B);
                    var chunk = helper.getLevel().getChunkAt(pos);
                    chunk.setUnsaved(false);

                    setter.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));

                    check(helper, storage(helper, ITEM_B).getPriority() == 7, "the setter didn't set priority 7, the port has "
                            + storage(helper, ITEM_B).getPriority());
                    // otherwise the new priority is only written to disk if something else in the chunk changes
                    check(helper, chunk.isUnsaved(), "setting the priority should mark the port to be saved");
                    check(helper, savedPriority(helper, ITEM_B) == 7, "the port's saved data should have priority 7");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void jadeGetsTheOutputPriority(GameTestHelper helper) {
        build(helper);

        helper.startSequence()
                .thenExecute(() -> {
                    storage(helper, ITEM_B).setPriority(4);
                    storage(helper, FLUID_B).setPriority(6);
                    check(helper, jadePriority(helper, ITEM_B) == 4, "Jade should get priority 4 for the item output port");
                    check(helper, jadePriority(helper, FLUID_B) == 6, "Jade should get priority 6 for the fluid output port");
                    check(helper, jadePriority(helper, INPUT) == -1, "input ports have no priority in Jade");
                })
                .thenSucceed();
    }

    /** What PortPriorityDataProvider sends to the client for this port, -1 when nothing. */
    private static int jadePriority(GameTestHelper helper, BlockPos port) {
        BlockEntity be = helper.getBlockEntity(port);
        BlockPos pos = helper.absolutePos(port);
        BlockAccessor accessor = (BlockAccessor) Proxy.newProxyInstance(BlockAccessor.class.getClassLoader(),
                new Class<?>[]{BlockAccessor.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getBlockEntity" -> be;
                    case "getLevel" -> helper.getLevel();
                    case "getPosition" -> pos;
                    case "getBlockState" -> helper.getLevel().getBlockState(pos);
                    case "getBlock" -> helper.getLevel().getBlockState(pos).getBlock();
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                });
        var data = new CompoundTag();
        PortPriorityDataProvider.INSTANCE.appendServerData(data, accessor);
        return data.contains(PortPriorityDataProvider.KEY) ? data.getInt(PortPriorityDataProvider.KEY) : -1;
    }

    private static int savedPriority(GameTestHelper helper, BlockPos port) {
        return helper.getBlockEntity(port).saveWithoutMetadata().getCompound(Ref.NBT_STORAGE_KEY).getInt("Priority");
    }

    private static MachineControllerBlockEntity build(GameTestHelper helper) {
        helper.setBlock(CONTROLLER, block("priority_test").defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        helper.setBlock(INPUT, block("test_prio_item_input"));
        helper.setBlock(ITEM_A, block("test_prio_item_output"));
        helper.setBlock(ITEM_B, block("test_prio_item_output"));
        helper.setBlock(FLUID_A, block("test_prio_fluid_output"));
        helper.setBlock(FLUID_B, block("test_prio_fluid_output"));
        return (MachineControllerBlockEntity) helper.getBlockEntity(CONTROLLER);
    }

    private static void formed(GameTestHelper helper, MachineControllerBlockEntity controller) {
        ResourceLocation id = ResourceLocation.tryBuild("mmtest", "priority_test");
        check(helper, controller.getStructure() != null && controller.getStructure().id().equals(id), "priority_test not formed yet");
    }

    private static int gold(GameTestHelper helper, BlockPos port) {
        var handler = items(helper, port).getHandler();
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (handler.getStackInSlot(slot).is(Items.GOLD_INGOT)) {
                total += handler.getActualCount(slot);
            }
        }
        return total;
    }

    private static int water(GameTestHelper helper, BlockPos port) {
        var handler = ((FluidPortStorage) storage(helper, port)).getHandler();
        int total = 0;
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            total += handler.getFluidInTank(tank).getAmount();
        }
        return total;
    }

    private static ItemPortStorage items(GameTestHelper helper, BlockPos port) {
        return (ItemPortStorage) storage(helper, port);
    }

    private static IPortStorage storage(GameTestHelper helper, BlockPos relative) {
        if (helper.getBlockEntity(relative) instanceof AbstractPortBlockEntity port) {
            return port.getStorage();
        }
        throw new IllegalStateException("no port at " + relative);
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
