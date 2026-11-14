package io.ticticboom.mods.mmtest;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.structure.HelperSlotOverrides;
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

/**
 * An Input Gateway or a Pattern Port stands in for a structure block only where the structure's key says
 * {@code "allowHelper": true}, and never more of them than {@code "maxHelpers"}.
 * mmtest:helper_denied_test is "ICOX" (X = glass, no allowHelper); mmtest:helper_limit_test is "ICOGG"
 * (G = glass with allowHelper, maxHelpers 1).
 */
@GameTestHolder("mmtest")
@PrefixGameTestTemplate(false)
public class HelperSlotGameTests {
    private static final String TEMPLATE = "empty";
    private static final BlockPos CONTROLLER = new BlockPos(3, 1, 3);
    private static final BlockPos INPUT = CONTROLLER.offset(-1, 0, 0);
    private static final BlockPos OUTPUT = CONTROLLER.offset(1, 0, 0);
    private static final BlockPos FIRST = CONTROLLER.offset(2, 0, 0);
    private static final BlockPos SECOND = CONTROLLER.offset(3, 0, 0);
    /** Long enough for an unformed controller to have tried forming several times. */
    private static final int SETTLE_TICKS = 40;

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void patternPortCannotReplaceAnUnmarkedBlock(GameTestHelper helper) {
        build(helper, "helper_denied_test");
        helper.setBlock(FIRST, MMRegisters.PATTERN_PORT.get());

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> check(helper, !formed(helper, "helper_denied_test"),
                        "a Pattern Port must not stand in for a block its key does not allow"))
                .thenExecute(() -> helper.setBlock(FIRST, Blocks.GLASS))
                .thenWaitUntil(() -> check(helper, formed(helper, "helper_denied_test"), "the machine should form with its glass"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void inputGatewayCannotReplaceAnUnmarkedBlock(GameTestHelper helper) {
        build(helper, "helper_denied_test");
        helper.setBlock(FIRST, MMRegisters.INPUT_GATEWAY.get());

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> check(helper, !formed(helper, "helper_denied_test"),
                        "an Input Gateway must not stand in for a block its key does not allow"))
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void helpersBeyondMaxHelpersDoNotForm(GameTestHelper helper) {
        build(helper, "helper_limit_test");
        helper.setBlock(FIRST, MMRegisters.PATTERN_PORT.get());
        helper.setBlock(SECOND, MMRegisters.INPUT_GATEWAY.get());

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> check(helper, !formed(helper, "helper_limit_test"),
                        "two helpers must not form a machine that allows one"))
                .thenExecute(() -> helper.setBlock(SECOND, Blocks.GLASS))
                .thenWaitUntil(() -> check(helper, formed(helper, "helper_limit_test"),
                        "one helper within maxHelpers should form the machine"))
                .thenSucceed();
    }

    /**
     * The Structure Builder's admin rule replaces the structure's own settings: mmtest:helper_admin_test is "ICOXG"
     * (G allows a helper, X does not). With the rule X does and G does not, at most 1; reset gives the JSON back.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void adminRuleReplacesTheStructureSettings(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.tryBuild("mmtest", "helper_admin_test");
        var overrides = HelperSlotOverrides.get(helper.getLevel().getServer());
        overrides.apply(HelperSlotOverrides.Action.RESET, id, ' ', 0);
        build(helper, "helper_admin_test");
        helper.setBlock(FIRST, MMRegisters.PATTERN_PORT.get()); // X
        helper.setBlock(SECOND, Blocks.GLASS);                   // G

        helper.startSequence()
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> check(helper, !formed(helper, "helper_admin_test"), "X does not allow a helper by its JSON"))
                .thenExecute(() -> {
                    check(helper, overrides.apply(HelperSlotOverrides.Action.TOGGLE_KEY, id, 'X', 0), "toggling X should apply");
                    check(helper, overrides.apply(HelperSlotOverrides.Action.TOGGLE_KEY, id, 'G', 0), "toggling G should apply");
                    check(helper, overrides.apply(HelperSlotOverrides.Action.SET_MAX, id, ' ', 1), "setting the limit should apply");
                    check(helper, !overrides.apply(HelperSlotOverrides.Action.TOGGLE_KEY, id, 'I', 0), "a port key must be refused");
                })
                .thenWaitUntil(() -> check(helper, formed(helper, "helper_admin_test"), "the admin rule lets X take a helper"))
                .thenExecute(() -> helper.setBlock(SECOND, MMRegisters.INPUT_GATEWAY.get()))
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> check(helper, !formed(helper, "helper_admin_test"), "the admin rule no longer lets G take a helper"))
                .thenExecute(() -> check(helper, overrides.apply(HelperSlotOverrides.Action.RESET, id, ' ', 0), "reset should apply"))
                .thenIdle(SETTLE_TICKS)
                .thenExecute(() -> check(helper, !formed(helper, "helper_admin_test"), "after reset X refuses a helper again"))
                .thenExecute(() -> helper.setBlock(FIRST, Blocks.GLASS))
                .thenWaitUntil(() -> check(helper, formed(helper, "helper_admin_test"), "after reset G takes a helper by its JSON"))
                .thenSucceed();
    }

    private static void build(GameTestHelper helper, String controller) {
        helper.setBlock(CONTROLLER, block(controller).defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        helper.setBlock(INPUT, block("test_wait_item_input"));
        helper.setBlock(OUTPUT, block("test_wait_item_output"));
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
