package io.ticticboom.mods.mmtest;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.builder.AssemblyJobs;
import io.ticticboom.mods.mm.builder.DismantleJob;
import io.ticticboom.mods.mm.builder.DismantlePlanner;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.tool.ToolData;
import io.ticticboom.mods.mm.tool.ToolDismantles;
import io.ticticboom.mods.mm.tool.ToolEnergy;
import io.ticticboom.mods.mm.tool.ToolStore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dismantling a formed assembly_test ("FCGS") with the Multiblock Tool: blocks go into the tool's store, port
 * contents drop like normal breaking, build rights and claims are respected, energy is paid per block.
 */
@GameTestHolder("mmtest")
@PrefixGameTestTemplate(false)
public class DismantleGameTests {
    private static final String TEMPLATE = "empty";
    private static final ResourceLocation STRUCTURE = ResourceLocation.tryBuild("mmtest", "assembly_test");
    private static final ResourceLocation PREVIEW_STRUCTURE = ResourceLocation.tryBuild("mmtest", "preview_test");
    private static final int START_FE = 1000;
    private static final BlockPos CONTROLLER = new BlockPos(3, 1, 3);
    private static final BlockPos FLEX = new BlockPos(-1, 0, 0);
    private static final BlockPos GLASS = new BlockPos(1, 0, 0);
    private static final BlockPos STRICT = new BlockPos(2, 0, 0);

    @GameTest(template = TEMPLATE)
    public static void dismantleReturnsBlocks(GameTestHelper helper) {
        BlockPos controller = buildMachine(helper);
        helper.startSequence().thenWaitUntil(() -> expectFormed(helper, controller)).thenExecute(() -> {
            Player player = helper.makeMockPlayer();
            ItemStack tool = tool(player);
            // aiming at the glass finds the machine it belongs to
            ToolDismantles.Prepared prepared = prepare(helper, player, tool, controller.offset(GLASS));
            check(helper, prepared.controllerPos().equals(controller), "the glass should resolve to the controller");
            List<BlockPos> positions = prepared.positions();
            check(helper, positions.size() == 4, "expected 4 positions, got " + positions);
            check(helper, positions.get(positions.size() - 1).equals(controller), "the controller must come last: " + positions);

            DismantleJob job = run(helper, player, prepared);

            int fe = MMConfigSetup.COMMON.toolEnergyPerDismantledBlock.get();
            check(helper, job.removed() == 4, "expected 4 removed, got " + job.removed());
            for (BlockPos pos : positions) {
                check(helper, helper.getLevel().getBlockState(pos).isAir(), "expected air at " + pos);
            }
            check(helper, storeCount(tool, controllerBlock().asItem()) == 1, "the controller should be in the store");
            check(helper, storeCount(tool, port("s").asItem()) == 1, "the flexible port should be in the store");
            check(helper, storeCount(tool, port("l").asItem()) == 1, "the strict port should be in the store");
            check(helper, storeCount(tool, Items.GLASS) == 1, "the glass should be in the store");
            check(helper, energy(tool) == START_FE - 4 * fe, "expected " + (START_FE - 4 * fe) + " FE, got " + energy(tool));
        }).thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void dismantlePortDropsContents(GameTestHelper helper) {
        BlockPos controller = buildMachine(helper);
        BlockPos portPos = controller.offset(FLEX);
        helper.startSequence().thenWaitUntil(() -> expectFormed(helper, controller)).thenExecute(() -> {
            var port = (IPortBlockEntity) helper.getLevel().getBlockEntity(portPos);
            var handler = ((ItemPortStorage) port.getStorage()).getHandler();
            ItemStack left = handler.insertItem(0, new ItemStack(Items.DIAMOND, 5), false);
            check(helper, left.isEmpty(), "the port should take the diamonds, left " + left);
            Player player = helper.makeMockPlayer();
            ItemStack tool = tool(player);

            DismantleJob job = run(helper, player, prepare(helper, player, tool, controller));

            check(helper, job.removed() == 4, "expected 4 removed, got " + job.removed());
            int diamonds = 0;
            for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(portPos).inflate(3))) {
                if (item.getItem().is(Items.DIAMOND)) {
                    diamonds += item.getItem().getCount();
                }
            }
            check(helper, diamonds == 5, "the port's 5 diamonds should drop on the ground, found " + diamonds);
            check(helper, storeCount(tool, Items.DIAMOND) == 0, "port contents must not go into the store");
            check(helper, storeCount(tool, port("s").asItem()) == 1, "the port block itself should be in the store");
        }).thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void dismantleRespectsBuildRights(GameTestHelper helper) {
        BlockPos controller = buildMachine(helper);
        helper.startSequence().thenWaitUntil(() -> expectFormed(helper, controller)).thenExecute(() -> {
            Player player = helper.makeMockPlayer();
            ItemStack tool = tool(player);
            player.getAbilities().mayBuild = false; // adventure mode / protected area

            ToolDismantles.Result result = ToolDismantles.prepare(helper.getLevel(), player, tool, controller);
            expectError(helper, result, "message.mm.tool.dismantle.protected");

            // rights lost after the job started: every block is skipped
            player.getAbilities().mayBuild = true;
            ToolDismantles.Prepared prepared = prepare(helper, player, tool, controller);
            player.getAbilities().mayBuild = false;
            DismantleJob job = run(helper, player, prepared);

            check(helper, job.removed() == 0, "nothing may be removed, got " + job.removed());
            check(helper, job.blocked() == 4, "expected 4 blocked, got " + job.blocked());
            expectMachineIntact(helper, controller, tool);
        }).thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void dismantleRespectsClaims(GameTestHelper helper) {
        BlockPos controller = buildMachine(helper);
        helper.startSequence().thenWaitUntil(() -> expectFormed(helper, controller)).thenExecute(() -> {
            Player player = helper.makeMockPlayer();
            ItemStack tool = tool(player);
            ToolDismantles.Prepared prepared = prepare(helper, player, tool, controller);
            // a claim mod refusing only the glass
            BlockPos glass = controller.offset(GLASS);
            Consumer<BlockEvent.BreakEvent> claims = event -> {
                if (event.getPlayer() == player && event.getPos().equals(glass)) {
                    event.setCanceled(true);
                }
            };
            MinecraftForge.EVENT_BUS.addListener(claims);
            DismantleJob job;
            try {
                job = run(helper, player, prepared);
            } finally {
                MinecraftForge.EVENT_BUS.unregister(claims);
            }

            int fe = MMConfigSetup.COMMON.toolEnergyPerDismantledBlock.get();
            check(helper, job.removed() == 3 && job.blocked() == 1, "expected 3 removed / 1 blocked, got " + job.removed() + " / " + job.blocked());
            check(helper, helper.getLevel().getBlockState(glass).is(Blocks.GLASS), "the claimed glass must stay");
            check(helper, storeCount(tool, Items.GLASS) == 0, "the claimed glass must not be collected");
            check(helper, energy(tool) == START_FE - 3 * fe, "only removed blocks cost energy, have " + energy(tool));
        }).thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void dismantleStopsOutOfEnergy(GameTestHelper helper) {
        BlockPos controller = buildMachine(helper);
        helper.startSequence().thenWaitUntil(() -> expectFormed(helper, controller)).thenExecute(() -> {
            Player player = helper.makeMockPlayer();
            ItemStack tool = tool(player);
            int fe = MMConfigSetup.COMMON.toolEnergyPerDismantledBlock.get();
            var energy = new ToolEnergy(tool, MMConfigSetup.COMMON.toolEnergyCapacity.get());
            energy.extractEnergy(START_FE - 2 * fe, false); // enough for two blocks

            DismantleJob job = run(helper, player, prepare(helper, player, tool, controller));

            check(helper, job.outOfEnergy(), "the job should stop out of energy");
            check(helper, job.removed() == 2, "expected 2 removed, got " + job.removed());
            check(helper, energy(tool) == 0, "all energy should be used, have " + energy(tool));
            check(helper, helper.getLevel().getBlockState(controller).is(controllerBlock()), "the controller (last) must stay");

            ToolDismantles.Result again = ToolDismantles.prepare(helper.getLevel(), player, tool, controller);
            expectError(helper, again, "message.mm.assemble.out_of_energy");
        }).thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void unformedOnlyDismantlesTargetedController(GameTestHelper helper) {
        BlockPos controller = helper.absolutePos(CONTROLLER);
        helper.getLevel().setBlockAndUpdate(controller, controllerBlock().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        helper.getLevel().setBlockAndUpdate(controller.offset(GLASS), Blocks.GLASS.defaultBlockState());
        Player player = helper.makeMockPlayer();
        ItemStack tool = tool(player);

        expectError(helper, ToolDismantles.prepare(helper.getLevel(), player, tool, controller.offset(GLASS)), "message.mm.tool.dismantle.no_machine");
        ToolDismantles.Prepared prepared = prepare(helper, player, tool, controller);
        check(helper, prepared.positions().equals(List.of(controller)), "only the controller should be dismantled, got " + prepared.positions());
        helper.succeed();
    }

    /**
     * The client's outline uses a resolver that never runs the structure's formed checks (they cast to ServerLevel);
     * it must find the same machine and blocks as the server's. Only the logic can be checked here: a real
     * ClientLevel cannot be created headless.
     */
    @GameTest(template = TEMPLATE)
    public static void previewPositionsMatchServer(GameTestHelper helper) {
        BlockPos controller = buildMachine(helper);
        var level = helper.getLevel();
        BlockPos stray = controller.offset(0, 0, 3);
        level.setBlockAndUpdate(stray, Blocks.GLASS.defaultBlockState());
        helper.startSequence().thenWaitUntil(() -> expectFormed(helper, controller)).thenExecute(() -> {
            for (BlockPos target : List.of(controller, controller.offset(GLASS), controller.offset(FLEX), controller.offset(STRICT))) {
                MachineControllerBlockEntity resolved = DismantlePlanner.resolve(level, target);
                check(helper, resolved != null, "the server should resolve " + target);
                List<BlockPos> server = DismantlePlanner.positions(level, resolved);
                List<BlockPos> preview = DismantlePlanner.previewPositions(level, target);
                check(helper, new HashSet<>(preview).equals(new HashSet<>(server)) && preview.size() == server.size(),
                        "preview " + preview + " differs from server " + server + " for " + target);
                check(helper, preview.get(preview.size() - 1).equals(controller), "the controller must come last: " + preview);
            }
            check(helper, DismantlePlanner.previewPositions(level, stray).isEmpty(), "a block of no machine has no preview");

            // broken: like the server, the controller alone and nothing for its former pieces
            level.removeBlock(controller.offset(GLASS), false);
            check(helper, DismantlePlanner.previewPositions(level, controller).equals(List.of(controller)),
                    "an unformed controller previews only itself");
            check(helper, DismantlePlanner.previewPositions(level, controller.offset(STRICT)).isEmpty(),
                    "a piece of an unformed machine has no preview");
        }).thenSucceed();
    }

    /**
     * Same parity for a machine the server only accepts through its looser checks: each anywhere port sits at the
     * other one's position, and an input gateway stands in for the glass.
     */
    @GameTest(template = TEMPLATE)
    public static void previewPositionsMatchServerWithAnywherePortsAndGateway(GameTestHelper helper) {
        BlockPos controller = helper.absolutePos(CONTROLLER);
        var level = helper.getLevel();
        // its own controller, so assembly_test's candidate list stays as other tests expect
        level.setBlockAndUpdate(controller, registered("preview_test").defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        level.setBlockAndUpdate(controller.offset(FLEX), port("l").defaultBlockState()); // A wants tier 1
        level.setBlockAndUpdate(controller.offset(GLASS), port("s").defaultBlockState()); // B wants tier 3
        level.setBlockAndUpdate(controller.offset(STRICT), MMRegisters.INPUT_GATEWAY.get().defaultBlockState());
        helper.startSequence().thenWaitUntil(() -> check(helper, level.getBlockEntity(controller) instanceof MachineControllerBlockEntity be
                && be.getStructure() != null && be.getStructure().id().equals(PREVIEW_STRUCTURE) && be.isFormed(), "preview_test not formed yet")).thenExecute(() -> {
            for (BlockPos target : List.of(controller, controller.offset(FLEX), controller.offset(GLASS), controller.offset(STRICT))) {
                MachineControllerBlockEntity resolved = DismantlePlanner.resolve(level, target);
                check(helper, resolved != null, "the server should resolve " + target);
                List<BlockPos> server = DismantlePlanner.positions(level, resolved);
                List<BlockPos> preview = DismantlePlanner.previewPositions(level, target);
                check(helper, server.size() == 4, "the server should dismantle 4 blocks, got " + server);
                check(helper, new HashSet<>(preview).equals(new HashSet<>(server)) && preview.size() == server.size(),
                        "preview " + preview + " differs from server " + server + " for " + target);
                check(helper, preview.get(preview.size() - 1).equals(controller), "the controller must come last: " + preview);
            }
        }).thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void dismantleConfirmWithinThreeSeconds(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        BlockPos controller = helper.absolutePos(CONTROLLER);
        BlockPos other = controller.east(5);
        var dimension = helper.getLevel().dimension();

        check(helper, !ToolDismantles.confirm(player, dimension, controller, 100), "the first click only asks");
        check(helper, ToolDismantles.confirm(player, dimension, controller, 100 + ToolDismantles.CONFIRM_TICKS), "a second click within 3 s confirms");
        check(helper, !ToolDismantles.confirm(player, dimension, controller, 200), "a confirmed click is used up");
        check(helper, !ToolDismantles.confirm(player, dimension, controller, 201 + ToolDismantles.CONFIRM_TICKS), "too late: asks again");
        check(helper, !ToolDismantles.confirm(player, dimension, other, 300 + ToolDismantles.CONFIRM_TICKS), "another machine asks again");
        check(helper, ToolDismantles.confirm(player, dimension, other, 310 + ToolDismantles.CONFIRM_TICKS), "same machine again confirms");
        helper.succeed();
    }

    /** The assembly_test machine (lowest tiers) placed directly, controller facing north; returns its position. */
    private static BlockPos buildMachine(GameTestHelper helper) {
        BlockPos controller = helper.absolutePos(CONTROLLER);
        var level = helper.getLevel();
        level.setBlockAndUpdate(controller, controllerBlock().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        level.setBlockAndUpdate(controller.offset(FLEX), port("s").defaultBlockState());
        level.setBlockAndUpdate(controller.offset(GLASS), Blocks.GLASS.defaultBlockState());
        level.setBlockAndUpdate(controller.offset(STRICT), port("l").defaultBlockState());
        return controller;
    }

    private static void expectFormed(GameTestHelper helper, BlockPos controller) {
        check(helper, helper.getLevel().getBlockEntity(controller) instanceof MachineControllerBlockEntity be
                && be.getStructure() != null && be.getStructure().id().equals(STRUCTURE), "controller not formed yet");
    }

    private static void expectMachineIntact(GameTestHelper helper, BlockPos controller, ItemStack tool) {
        check(helper, helper.getLevel().getBlockState(controller).is(controllerBlock()), "the controller must stay");
        check(helper, helper.getLevel().getBlockState(controller.offset(FLEX)).is(port("s")), "the flexible port must stay");
        check(helper, helper.getLevel().getBlockState(controller.offset(GLASS)).is(Blocks.GLASS), "the glass must stay");
        check(helper, helper.getLevel().getBlockState(controller.offset(STRICT)).is(port("l")), "the strict port must stay");
        check(helper, energy(tool) == START_FE, "no energy should be spent, have " + energy(tool));
    }

    private static void expectError(GameTestHelper helper, ToolDismantles.Result result, String key) {
        check(helper, result.prepared() == null && result.error() != null, "the dismantle should be refused");
        check(helper, result.error().getContents() instanceof TranslatableContents contents && contents.getKey().equals(key),
                "expected message " + key + ", got " + result.error().getString());
    }

    private static ToolDismantles.Prepared prepare(GameTestHelper helper, Player player, ItemStack tool, BlockPos target) {
        ToolDismantles.Result result = ToolDismantles.prepare(helper.getLevel(), player, tool, target);
        if (result.prepared() == null) {
            helper.fail("dismantle refused: " + (result.error() == null ? "?" : result.error().getString()));
        }
        return result.prepared();
    }

    private static DismantleJob run(GameTestHelper helper, Player player, ToolDismantles.Prepared prepared) {
        DismantleJob job = prepared.job(helper.getLevel());
        int ticks = 0;
        while (!AssemblyJobs.tickDismantle(player, job, prepared.sink(), 1)) {
            if (++ticks > 100) {
                helper.fail("dismantle did not finish");
            }
        }
        return job;
    }

    /** A tool selecting assembly_test with {@value #START_FE} FE, held in the main hand. */
    private static ItemStack tool(Player player) {
        ItemStack tool = MMRegisters.MULTIBLOCK_TOOL.get().getDefaultInstance();
        ToolData.setStructure(tool, STRUCTURE);
        new ToolEnergy(tool, MMConfigSetup.COMMON.toolEnergyCapacity.get()).restore(START_FE);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        return tool;
    }

    private static int storeCount(ItemStack tool, Item item) {
        var store = new ToolStore(tool);
        int total = 0;
        for (int i = 0; i < store.getSlots(); i++) {
            if (store.getStackInSlot(i).is(item)) {
                total += store.getStackInSlot(i).getCount();
            }
        }
        return total;
    }

    private static int energy(ItemStack tool) {
        return new ToolEnergy(tool, MMConfigSetup.COMMON.toolEnergyCapacity.get()).getEnergyStored();
    }

    private static Block controllerBlock() {
        return registered("assembly_test");
    }

    private static Block port(String size) {
        return registered("test_item_" + size + "_input");
    }

    private static Block registered(String id) {
        Block block = ForgeRegistries.BLOCKS.getValue(Ref.id(id));
        if (block == null || block == Blocks.AIR) {
            throw new IllegalStateException("block mm:" + id + " is not registered");
        }
        return block;
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }
}
