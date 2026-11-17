package io.ticticboom.mods.mmtest;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * Port slots: a key with {@code "allowPort": true} takes a port of the structure in place of its block, the position
 * the port left takes that block, and a port key's {@code "minPorts"} / {@code "maxPorts"} say how many ports of it
 * the structure takes (left out: as many as the layout draws).
 * mmtest:port_slot_test is "ICOGGX" (G = glass port slot, X = stone, no slot); mmtest:port_range_test is "IICOGG"
 * with one to three input ports; mmtest:port_replace_test is "IICOG" with glass replacing left-out input ports.
 */
@GameTestHolder("mmtest")
@PrefixGameTestTemplate(false)
public class PortSlotGameTests {
    private static final String TEMPLATE = "empty";
    private static final BlockPos CONTROLLER = new BlockPos(3, 1, 3);
    private static final BlockPos INPUT = CONTROLLER.offset(-1, 0, 0);
    private static final BlockPos FAR_INPUT = CONTROLLER.offset(-2, 0, 0);
    private static final BlockPos OUTPUT = CONTROLLER.offset(1, 0, 0);
    private static final BlockPos FIRST_SLOT = CONTROLLER.offset(2, 0, 0);
    private static final BlockPos SECOND_SLOT = CONTROLLER.offset(3, 0, 0);
    private static final BlockPos PLAIN = CONTROLLER.offset(4, 0, 0);
    /** Long enough for a controller to have checked its structure several times. */
    private static final int SETTLE_TICKS = 40;

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void aPortMovesToASlotAndItsPositionTakesTheBlock(GameTestHelper helper) {
        String id = "port_slot_test";
        controller(helper, id);
        helper.setBlock(INPUT, block("test_wait_item_input"));
        helper.setBlock(FIRST_SLOT, block("test_wait_item_output"));
        helper.setBlock(SECOND_SLOT, Blocks.GLASS);
        helper.setBlock(PLAIN, Blocks.STONE);

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> check(helper, !formed(helper, id), "the position the output port left is still empty"))
                .thenExecute(() -> helper.setBlock(OUTPUT, Blocks.GLASS))
                .thenWaitUntil(() -> check(helper, formed(helper, id), "the machine should form with its output port on a slot"))
                .thenExecute(() -> {
                    List<BlockPos> ports = ports(helper);
                    check(helper, ports.size() == 2 && ports.contains(helper.absolutePos(FIRST_SLOT)),
                            "the moved port should be one of the machine's ports, got " + ports);
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void aPortBeyondTheLayoutsCountDoesNotForm(GameTestHelper helper) {
        String id = "port_slot_test";
        controller(helper, id);
        helper.setBlock(INPUT, block("test_wait_item_input"));
        helper.setBlock(OUTPUT, block("test_wait_item_output"));
        helper.setBlock(FIRST_SLOT, block("test_wait_item_output"));
        helper.setBlock(SECOND_SLOT, Blocks.GLASS);
        helper.setBlock(PLAIN, Blocks.STONE);

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> check(helper, !formed(helper, id), "two output ports must not form a machine that draws one"))
                .thenExecute(() -> helper.setBlock(FIRST_SLOT, Blocks.GLASS))
                .thenWaitUntil(() -> check(helper, formed(helper, id), "the machine should form as its layout draws it"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void aPortCannotReplaceAnUnmarkedBlock(GameTestHelper helper) {
        String id = "port_slot_test";
        controller(helper, id);
        helper.setBlock(INPUT, block("test_wait_item_input"));
        helper.setBlock(OUTPUT, Blocks.GLASS);
        helper.setBlock(FIRST_SLOT, Blocks.GLASS);
        helper.setBlock(SECOND_SLOT, Blocks.GLASS);
        helper.setBlock(PLAIN, block("test_wait_item_output"));

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> check(helper, !formed(helper, id), "a port must not stand in for a block whose key does not allow it"))
                .thenExecute(() -> {
                    helper.setBlock(PLAIN, Blocks.STONE);
                    helper.setBlock(SECOND_SLOT, block("test_wait_item_output"));
                })
                .thenWaitUntil(() -> check(helper, formed(helper, id), "the machine should form with the port on a slot"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void fewerPortsThanDrawnFormDownToMinPorts(GameTestHelper helper) {
        String id = "port_range_test";
        controller(helper, id);
        helper.setBlock(FAR_INPUT, Blocks.GLASS);
        helper.setBlock(INPUT, block("test_wait_item_input"));
        helper.setBlock(OUTPUT, block("test_wait_item_output"));
        helper.setBlock(FIRST_SLOT, Blocks.GLASS);
        helper.setBlock(SECOND_SLOT, Blocks.GLASS);

        helper.startSequence()
                .thenWaitUntil(() -> check(helper, formed(helper, id), "one input port is within minPorts"))
                .thenExecute(() -> helper.setBlock(INPUT, Blocks.GLASS))
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> check(helper, !formed(helper, id), "no input port is below minPorts"))
                .thenExecute(() -> {
                    var controller = (MachineControllerBlockEntity) helper.getBlockEntity(CONTROLLER);
                    var diagnosis = controller.diagnoseStructure();
                    check(helper, diagnosis.total() == 1, "one missing input port should be reported, got " + diagnosis.total());
                    BlockPos pos = diagnosis.missing().get(0).pos();
                    check(helper, pos.equals(helper.absolutePos(INPUT)) || pos.equals(helper.absolutePos(FAR_INPUT)),
                            "the report should point at a position the layout draws an input port on, got " + pos);
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void morePortsThanDrawnFormUpToMaxPorts(GameTestHelper helper) {
        String id = "port_range_test";
        controller(helper, id);
        helper.setBlock(FAR_INPUT, block("test_wait_item_input"));
        helper.setBlock(INPUT, block("test_wait_item_input"));
        helper.setBlock(OUTPUT, block("test_wait_item_output"));
        helper.setBlock(FIRST_SLOT, block("test_wait_item_input"));
        helper.setBlock(SECOND_SLOT, Blocks.GLASS);

        helper.startSequence()
                .thenWaitUntil(() -> check(helper, formed(helper, id), "three input ports are within maxPorts"))
                .thenExecute(() -> check(helper, ports(helper).size() == 4, "the machine should use all four ports, got " + ports(helper)))
                .thenExecute(() -> helper.setBlock(SECOND_SLOT, block("test_wait_item_input")))
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> check(helper, !formed(helper, id), "four input ports are above maxPorts"))
                .thenSucceed();
    }

    /**
     * mmtest:port_replace_test is "IICOG": I (item input) names G (glass) as its "replaceWith" and has no port slot
     * to move to, so it takes zero to two input ports and glass where one is left out.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void aPortWithAReplacementMayBeLeftOut(GameTestHelper helper) {
        String id = "port_replace_test";
        controller(helper, id);
        helper.setBlock(FAR_INPUT, Blocks.GLASS);
        helper.setBlock(INPUT, block("test_wait_item_input"));
        helper.setBlock(OUTPUT, block("test_wait_item_output"));
        helper.setBlock(FIRST_SLOT, Blocks.GLASS);

        helper.startSequence()
                .thenWaitUntil(() -> check(helper, formed(helper, id), "one input port and glass in place of the other should form"))
                .thenExecute(() -> check(helper, ports(helper).size() == 2, "the machine should use its two ports, got " + ports(helper)))
                .thenExecute(() -> helper.setBlock(INPUT, Blocks.GLASS))
                .thenWaitUntil(() -> check(helper, formed(helper, id), "a key with a replacement defaults to minPorts 0"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void onlyTheReplacementStandsInForALeftOutPort(GameTestHelper helper) {
        String id = "port_replace_test";
        controller(helper, id);
        helper.setBlock(FAR_INPUT, Blocks.STONE);
        helper.setBlock(INPUT, block("test_wait_item_input"));
        helper.setBlock(OUTPUT, block("test_wait_item_output"));
        helper.setBlock(FIRST_SLOT, Blocks.GLASS);

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> check(helper, !formed(helper, id), "stone is not the replacement of an input port"))
                .thenExecute(() -> {
                    var diagnosis = ((MachineControllerBlockEntity) helper.getBlockEntity(CONTROLLER)).diagnoseStructure();
                    check(helper, diagnosis.total() == 1 && diagnosis.missing().get(0).pos().equals(helper.absolutePos(FAR_INPUT)),
                            "the report should point at the stone, got " + diagnosis.total());
                    check(helper, diagnosis.missing().get(0).icon().is(Blocks.GLASS.asItem()), "the report should ask for glass");
                })
                .thenExecute(() -> {
                    // without a port slot a port may not take the glass position either
                    helper.setBlock(FAR_INPUT, Blocks.GLASS);
                    helper.setBlock(FIRST_SLOT, block("test_wait_item_input"));
                })
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> check(helper, !formed(helper, id), "a port must not stand on a block that is no port slot"))
                .thenExecute(() -> helper.setBlock(FIRST_SLOT, Blocks.GLASS))
                .thenWaitUntil(() -> check(helper, formed(helper, id), "the machine should form once the glass is back"))
                .thenSucceed();
    }

    private static void controller(GameTestHelper helper, String id) {
        helper.setBlock(CONTROLLER, block(id).defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
    }

    private static List<BlockPos> ports(GameTestHelper helper) {
        var controller = (MachineControllerBlockEntity) helper.getBlockEntity(CONTROLLER);
        return controller.getStructure().getPortPositions(helper.getLevel(), helper.absolutePos(CONTROLLER));
    }

    private static boolean formed(GameTestHelper helper, String structure) {
        var controller = (MachineControllerBlockEntity) helper.getBlockEntity(CONTROLLER);
        ResourceLocation id = ResourceLocation.tryBuild("mmtest", structure);
        return controller.getStructure() != null && controller.getStructure().id().equals(id)
                && !"not_formed".equals(controller.statusKey());
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
