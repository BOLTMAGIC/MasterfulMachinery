package io.ticticboom.mods.mmtest;

import com.mojang.authlib.GameProfile;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.builder.AssemblyJob;
import io.ticticboom.mods.mm.builder.AssemblyJobs;
import io.ticticboom.mods.mm.builder.AssemblyPlanner;
import io.ticticboom.mods.mm.builder.MaterialSource;
import io.ticticboom.mods.mm.builder.PlayerMaterials;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.tool.MultiblockToolMenu;
import io.ticticboom.mods.mm.tool.ToolBuildPlan;
import io.ticticboom.mods.mm.tool.ToolBuilds;
import io.ticticboom.mods.mm.tool.ToolData;
import io.ticticboom.mods.mm.tool.ToolEnergy;
import io.ticticboom.mods.mm.tool.ToolStore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Building with the Multiblock Tool: in the world at an anchor (controller included) and finishing a controller.
 * Uses the mmtest:assembly_test structure ("FCGS") like {@link AssemblyGameTests}.
 */
@GameTestHolder("mmtest")
@PrefixGameTestTemplate(false)
public class ToolBuildGameTests {
    private static final String TEMPLATE = "empty";
    private static final ResourceLocation STRUCTURE = ResourceLocation.tryBuild("mmtest", "assembly_test");
    private static final int START_FE = 1000;
    /** Clicked on its top face, so the anchor is one block above. */
    private static final BlockPos CLICKED = new BlockPos(3, 0, 3);
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    private static final BlockPos GLASS = new BlockPos(1, 0, 0);

    @GameTest(template = TEMPLATE)
    public static void toolBuildsFromStore(GameTestHelper helper) {
        Player player = player(helper, 180); // facing north
        ItemStack tool = tool(player);
        stockStore(tool);

        ToolBuilds.Prepared build = prepare(helper, player, tool);
        check(helper, build.controllerPos().equals(helper.absolutePos(ANCHOR)),
                "facing north, the controller (front middle of FCGS) should sit on the anchor, got " + build.controllerPos());
        AssemblyJob job = run(helper, player, build);

        int fe = MMConfigSetup.COMMON.toolEnergyPerPlacedBlock.get();
        check(helper, job.placed() == 4, "expected 4 placed (controller + 3), got " + job.placed());
        check(helper, job.missing().isEmpty() && job.blocked() == 0, "nothing should be missing or blocked: " + job.missing() + ", " + job.blocked());
        check(helper, structure(helper).formed(helper.getLevel(), build.controllerPos()), "structure not formed after the tool build");
        expectFacing(helper, build.controllerPos(), Direction.NORTH);
        check(helper, storeCount(tool) == 0, "every block should come out of the store, " + storeCount(tool) + " left");
        check(helper, energy(tool) == START_FE - 4 * fe, "expected " + (START_FE - 4 * fe) + " FE left, got " + energy(tool));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void toolBuildRefusedWhenObstructed(GameTestHelper helper) {
        Player player = player(helper, 180);
        ItemStack tool = tool(player);
        stockStore(tool);
        BlockPos anchor = helper.absolutePos(ANCHOR);
        helper.getLevel().setBlockAndUpdate(anchor.offset(GLASS), Blocks.STONE.defaultBlockState());

        ToolBuilds.Result result = ToolBuilds.prepare(helper.getLevel(), player, tool, helper.absolutePos(CLICKED), Direction.UP);

        check(helper, result.prepared() == null && result.error() != null, "an obstructed build must be refused");
        check(helper, helper.getLevel().getBlockState(anchor).isAir(), "nothing should be placed");
        check(helper, helper.getLevel().getBlockState(anchor.offset(GLASS)).is(Blocks.STONE), "the obstruction must not be broken");
        check(helper, storeCount(tool) == 4, "the store should be untouched, has " + storeCount(tool));
        check(helper, energy(tool) == START_FE, "no energy should be used, have " + energy(tool));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void toolCompletesController(GameTestHelper helper) {
        Player player = player(helper, 0);
        ItemStack tool = tool(player);
        var store = new ToolStore(tool);
        store.insertItem(0, new ItemStack(port("s")), false);
        store.insertItem(1, new ItemStack(Blocks.GLASS), false);
        store.insertItem(2, new ItemStack(port("l")), false);
        BlockPos controller = helper.absolutePos(ANCHOR);
        helper.getLevel().setBlockAndUpdate(controller, controllerBlock().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));

        ToolBuilds.Result result = ToolBuilds.prepare(helper.getLevel(), player, tool, controller, Direction.UP);
        check(helper, result.prepared() != null, "the tool should complete a matching controller, got " + result.error());
        check(helper, result.prepared().controllerPos().equals(controller), "the job should use the clicked controller");
        AssemblyJob job = run(helper, player, result.prepared());

        int fe = MMConfigSetup.COMMON.toolEnergyPerPlacedBlock.get();
        check(helper, job.placed() == 3, "expected 3 placed, got " + job.placed());
        check(helper, structure(helper).formed(helper.getLevel(), controller), "structure not formed after completing the controller");
        check(helper, helper.getLevel().getBlockState(controller.above()).isAir(), "nothing should be built at the anchor above the controller");
        check(helper, storeCount(tool) == 0, "the store should be used, " + storeCount(tool) + " left");
        check(helper, energy(tool) == START_FE - 3 * fe, "expected " + (START_FE - 3 * fe) + " FE left, got " + energy(tool));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void toolBuildRotatedForms(GameTestHelper helper) {
        Player player = player(helper, 90); // facing west
        check(helper, player.getDirection() == Direction.WEST, "mock player should face west, faces " + player.getDirection());
        ItemStack tool = tool(player);
        ToolData.setExtraTurns(tool, 1);
        stockStore(tool);

        ToolBuilds.Prepared build = prepare(helper, player, tool);
        // west is a quarter turn counter-clockwise; one extra clockwise turn brings it back to none
        Rotation expected = AssemblyPlanner.rotationFor(Direction.WEST).getRotated(Rotation.CLOCKWISE_90);
        // facing west the front is the east end of the row, so the row "FCGS" ends on the anchor
        check(helper, build.controllerPos().equals(helper.absolutePos(ANCHOR).offset(-2, 0, 0)),
                "controller expected two blocks west of the anchor, got " + build.controllerPos());
        AssemblyJob job = run(helper, player, build);

        check(helper, job.placed() == 4, "expected 4 placed, got " + job.placed());
        check(helper, structure(helper).formed(helper.getLevel(), build.controllerPos()), "rotated structure not formed");
        Direction facing = helper.getLevel().getBlockState(build.controllerPos()).getValue(HorizontalDirectionalBlock.FACING);
        check(helper, AssemblyPlanner.rotationFor(facing) == expected,
                "the controller should face so its own Assemble picks " + expected + ", faces " + facing);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void toolBuildTurnedFacesController(GameTestHelper helper) {
        Player player = player(helper, 180); // facing north
        ItemStack tool = tool(player);
        ToolData.setExtraTurns(tool, 1); // turned to east
        stockStore(tool);

        ToolBuilds.Prepared build = prepare(helper, player, tool);
        AssemblyJob job = run(helper, player, build);

        check(helper, job.placed() == 4, "expected 4 placed, got " + job.placed());
        expectFacing(helper, build.controllerPos(), Direction.EAST);
        BlockPos glass = build.controllerPos().offset(GLASS.rotate(Rotation.CLOCKWISE_90));
        check(helper, helper.getLevel().getBlockState(glass).is(Blocks.GLASS), "glass expected at " + glass);
        check(helper, structure(helper).formed(helper.getLevel(), build.controllerPos()), "turned structure not formed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void toolEnergyRefundedWhenPlacementCancelled(GameTestHelper helper) {
        Player player = player(helper, 180);
        ItemStack tool = tool(player);
        stockStore(tool);
        ToolBuilds.Prepared build = prepare(helper, player, tool);

        // a claim mod refusing this player everywhere but the controller position
        Consumer<BlockEvent.EntityPlaceEvent> claims = event -> {
            if (event.getEntity() == player && !event.getPos().equals(build.controllerPos())) {
                event.setCanceled(true);
            }
        };
        MinecraftForge.EVENT_BUS.addListener(claims);
        AssemblyJob job;
        try {
            job = run(helper, player, build);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(claims);
        }

        int fe = MMConfigSetup.COMMON.toolEnergyPerPlacedBlock.get();
        check(helper, job.placed() == 1, "only the controller should be placed, got " + job.placed());
        check(helper, job.blocked() == 3, "expected 3 blocked, got " + job.blocked());
        check(helper, energy(tool) == START_FE - fe, "energy paid for cancelled blocks should be refunded, have " + energy(tool));
        check(helper, storeCount(tool) == 3, "taken blocks should go back into the store, has " + storeCount(tool));
        check(helper, helper.getLevel().getBlockState(build.controllerPos().offset(GLASS)).isAir(), "the cancelled glass should be undone");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void toolBuildRefusedWithoutController(GameTestHelper helper) {
        Player player = player(helper, 180);
        ItemStack tool = tool(player);
        var store = new ToolStore(tool);
        store.insertItem(0, new ItemStack(port("s")), false);
        store.insertItem(1, new ItemStack(Blocks.GLASS), false);
        store.insertItem(2, new ItemStack(port("l")), false);

        ToolBuilds.Result result = ToolBuilds.prepare(helper.getLevel(), player, tool, helper.absolutePos(CLICKED), Direction.UP);

        // all or nothing: the up-front check lists what is short, here only the controller, and the tool has no network
        expectError(helper, result, "message.mm.assemble.missing");
        String message = result.error().getString();
        check(helper, message.contains(controllerBlock().getName().getString()) && !message.contains(port("s").getName().getString())
                && !message.contains(port("l").getName().getString()), "only the controller should be named: " + message);
        check(helper, result.error().getSiblings().stream().anyMatch(part -> part.getContents() instanceof TranslatableContents c
                && c.getKey().equals("message.mm.tool.no_me")), "an unbound tool should say it has no ME network: " + message);
        expectNothingBuilt(helper, tool, 3);
        helper.succeed();
    }

    /**
     * All or nothing: with the controller at hand but one port missing, nothing is placed (before, the controller and
     * the other blocks were built and the port reported missing afterwards).
     */
    @GameTest(template = TEMPLATE)
    public static void toolBuildRefusedWhenAnyBlockMissing(GameTestHelper helper) {
        Player player = player(helper, 180);
        ItemStack tool = tool(player);
        var store = new ToolStore(tool);
        store.insertItem(0, new ItemStack(controllerBlock()), false);
        store.insertItem(1, new ItemStack(port("s")), false);
        store.insertItem(2, new ItemStack(Blocks.GLASS), false);

        ToolBuilds.Result result = ToolBuilds.prepare(helper.getLevel(), player, tool, helper.absolutePos(CLICKED), Direction.UP);

        expectError(helper, result, "message.mm.assemble.missing");
        check(helper, result.error().getString().contains(port("l").getName().getString()), "the large port should be named: " + result.error().getString());
        expectNothingBuilt(helper, tool, 3);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void toolBuildRefusedWhenControllerProtected(GameTestHelper helper) {
        Player player = player(helper, 180);
        player.getAbilities().mayBuild = false; // adventure mode / protected position
        ItemStack tool = tool(player);
        stockStore(tool);

        ToolBuilds.Result result = ToolBuilds.prepare(helper.getLevel(), player, tool, helper.absolutePos(CLICKED), Direction.UP);

        expectError(helper, result, "message.mm.tool.protected");
        expectNothingBuilt(helper, tool, 4);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void toolBuildStopsWhenControllerCancelled(GameTestHelper helper) {
        Player player = player(helper, 180);
        ItemStack tool = tool(player);
        stockStore(tool);
        ToolBuilds.Prepared build = prepare(helper, player, tool);

        // a claim mod refusing only the controller position
        Consumer<BlockEvent.EntityPlaceEvent> claims = event -> {
            if (event.getEntity() == player && event.getPos().equals(build.controllerPos())) {
                event.setCanceled(true);
            }
        };
        MinecraftForge.EVENT_BUS.addListener(claims);
        AssemblyJob job;
        try {
            // a big budget: nothing after the controller may be placed in the same tick either
            job = AssemblyJob.create(helper.getLevel(), build.controllerPos(), build.plan(), build.perBlockFe());
            AssemblyJobs.tickBuild(player, job, build.source(), 64);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(claims);
        }

        check(helper, job.controllerNotPlaced(), "the job should report the controller could not be placed");
        check(helper, job.placed() == 0, "nothing should be placed without the controller, got " + job.placed());
        expectNothingBuilt(helper, tool, 4);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void toolBuildStopsWhenToolNotCarried(GameTestHelper helper) {
        Player player = player(helper, 180);
        ItemStack tool = tool(player);
        stockStore(tool);
        ToolBuilds.Prepared build = prepare(helper, player, tool);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); // put away

        AssemblyJob job = run(helper, player, build);

        check(helper, job.sourceGone(), "the job should stop because the tool is gone");
        check(helper, !job.outOfEnergy(), "not carrying the tool is not out of energy");
        check(helper, job.placed() == 0, "nothing should be placed, got " + job.placed());
        expectNothingBuilt(helper, tool, 4);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void creativeToolBuildContinuesWhenToolNotCarried(GameTestHelper helper) {
        Player player = player(helper, 180);
        player.getAbilities().instabuild = true;
        ItemStack tool = tool(player);
        ToolBuilds.Prepared build = prepare(helper, player, tool);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); // put away

        AssemblyJob job = run(helper, player, build);

        check(helper, !job.sourceGone(), "a creative build needs nothing from the tool and should go on");
        check(helper, job.placed() == 4, "expected 4 placed, got " + job.placed());
        check(helper, structure(helper).formed(helper.getLevel(), build.controllerPos()), "structure not formed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void toolBuildRefusedWithoutEnergy(GameTestHelper helper) {
        Player player = player(helper, 180);
        ItemStack tool = tool(player);
        stockStore(tool);
        var energy = new ToolEnergy(tool, MMConfigSetup.COMMON.toolEnergyCapacity.get());
        energy.extractEnergy(START_FE - MMConfigSetup.COMMON.toolEnergyPerPlacedBlock.get() + 1, false);
        int left = energy.getEnergyStored();

        ToolBuilds.Result result = ToolBuilds.prepare(helper.getLevel(), player, tool, helper.absolutePos(CLICKED), Direction.UP);

        expectError(helper, result, "message.mm.assemble.out_of_energy");
        check(helper, energy(tool) == left, "no energy should be spent, have " + energy(tool));
        check(helper, storeCount(tool) == 4, "the store should be untouched, has " + storeCount(tool));
        check(helper, helper.getLevel().getBlockState(helper.absolutePos(ANCHOR)).isAir(), "nothing should be placed");
        helper.succeed();
    }

    /**
     * While the tool's store is open the job waits (the menu and the job would otherwise write over each other's
     * store); after closing it goes on and sees what the player moved meanwhile.
     */
    @GameTest(template = TEMPLATE)
    public static void toolBuildWaitsWhileStoreOpen(GameTestHelper helper) {
        Player player = player(helper, 180);
        ItemStack tool = tool(player);
        stockStore(tool);
        ToolBuilds.Prepared build = prepare(helper, player, tool);
        AssemblyJob job = AssemblyJob.create(helper.getLevel(), build.controllerPos(), build.plan(), build.perBlockFe());

        MultiblockToolMenu menu = new MultiblockToolMenu(0, player.getInventory(), InteractionHand.MAIN_HAND);
        player.containerMenu = menu;
        for (int i = 0; i < 5; i++) {
            check(helper, !AssemblyJobs.tickBuild(player, job, build.source(), 64), "the job must not finish while the store is open");
        }
        check(helper, job.placed() == 0, "nothing may be placed while the store is open, got " + job.placed());
        check(helper, helper.getLevel().getBlockState(build.controllerPos()).isAir(), "the controller must not be placed yet");
        // the player moves the glass out of the store meanwhile
        menu.quickMoveStack(player, 2);
        check(helper, player.getInventory().countItem(Items.GLASS) == 1, "the glass should be moved to the inventory");
        menu.removed(player);
        player.containerMenu = player.inventoryMenu;

        AssemblyJob done = run(helper, player, build, job);

        check(helper, done.placed() == 4, "expected 4 placed after closing, got " + done.placed());
        check(helper, structure(helper).formed(helper.getLevel(), build.controllerPos()), "structure not formed after closing");
        // the glass now came from the inventory; the store's old copy of it must not be used as well
        check(helper, storeCount(tool) == 0, "the store should be empty, has " + storeCount(tool));
        check(helper, player.getInventory().countItem(Items.GLASS) == 0, "the moved glass should be used, not duplicated");
        helper.succeed();
    }

    /** Stacks carrying block entity data are never used: the block is placed in its default state, losing the data. */
    @GameTest(template = TEMPLATE)
    public static void blockEntityDataStacksNotConsumed(GameTestHelper helper) {
        Player player = player(helper, 180);
        ItemStack tool = tool(player);
        ItemStack tagged = new ItemStack(Blocks.GLASS);
        tagged.getOrCreateTag().put(BlockItem.BLOCK_ENTITY_TAG, new CompoundTag());
        // stores filled before the store refused such stacks may still hold one
        new ToolStore(tool).setStackInSlot(0, tagged.copy());
        player.getInventory().setItem(9, tagged.copy());
        MaterialSource source = ToolBuildPlan.source(player, tool);

        check(helper, !source.has(Blocks.GLASS), "tagged glass must not count as available");
        check(helper, !ToolBuildPlan.availableSnapshot(player, tool).test(Blocks.GLASS), "tagged glass must not count for planning");
        check(helper, !PlayerMaterials.has(player, Blocks.GLASS), "the inventory's tagged glass must not count either");
        check(helper, source.take(Blocks.GLASS).isEmpty(), "nothing should be taken");
        check(helper, storeCount(tool) == 1 && player.getInventory().getItem(9).getCount() == 1, "both tagged stacks must stay");

        player.getInventory().setItem(10, new ItemStack(Blocks.GLASS));
        ItemStack taken = source.take(Blocks.GLASS);
        check(helper, !taken.isEmpty() && !taken.hasTag(), "the plain glass should be taken, got " + taken);
        check(helper, storeCount(tool) == 1 && player.getInventory().getItem(9).getCount() == 1, "the tagged stacks must still stay");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void chainedSourceUsesStoreThenInventory(GameTestHelper helper) {
        List<ItemStack> dropped = new ArrayList<>();
        Player player = new Player(helper.getLevel(), helper.absolutePos(ANCHOR), 0, new GameProfile(UUID.randomUUID(), "tool-test")) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }

            @Override
            public ItemEntity drop(ItemStack stack, boolean dropAround, boolean includeThrowerName) {
                dropped.add(stack.copy());
                return null;
            }
        };
        ItemStack tool = tool(player);
        new ToolStore(tool).insertItem(0, new ItemStack(Blocks.GLASS), false);
        player.getInventory().setItem(9, new ItemStack(Blocks.GLASS));
        MaterialSource source = ToolBuildPlan.source(player, tool);

        check(helper, !source.take(Blocks.GLASS).isEmpty(), "the first glass should be taken");
        check(helper, storeCount(tool) == 0 && player.getInventory().getItem(9).getCount() == 1, "the store should be used first");
        check(helper, !source.take(Blocks.GLASS).isEmpty(), "the second glass should come from the inventory");
        check(helper, player.getInventory().getItem(9).isEmpty(), "the inventory glass should be taken");
        check(helper, !source.has(Blocks.GLASS), "nothing should be left");

        source.refund(new ItemStack(Blocks.GLASS));
        check(helper, storeCount(tool) == 1, "a refund should go into the store first");

        // store full of something else -> the inventory
        var store = new ToolStore(tool);
        store.extractItem(0, 1, false);
        for (int i = 0; i < store.getSlots(); i++) {
            store.insertItem(i, new ItemStack(Blocks.DIRT, ToolStore.LIMIT), false);
        }
        source.beginTick(); // what the job runner does each tick: see the store as changed elsewhere
        source.refund(new ItemStack(Blocks.GLASS));
        check(helper, player.getInventory().countItem(Items.GLASS) == 1, "with the store full, a refund should go into the inventory");

        // inventory full too -> dropped at the player
        var items = player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).is(Items.GLASS)) {
                items.set(i, ItemStack.EMPTY);
            }
            if (items.get(i).isEmpty()) {
                items.set(i, new ItemStack(Blocks.STONE, 64));
            }
        }
        source.refund(new ItemStack(Blocks.GLASS));
        check(helper, dropped.size() == 1 && dropped.get(0).is(Items.GLASS), "with both full, the refund should be dropped, got " + dropped);
        helper.succeed();
    }

    private static void expectError(GameTestHelper helper, ToolBuilds.Result result, String key) {
        check(helper, result.prepared() == null && result.error() != null, "the build should be refused");
        check(helper, result.error().getContents() instanceof TranslatableContents contents && contents.getKey().equals(key),
                "expected message " + key + ", got " + result.error().getString());
    }

    /** Nothing placed at any assembly_test position around the anchor, store and energy untouched. */
    private static void expectNothingBuilt(GameTestHelper helper, ItemStack tool, int stored) {
        BlockPos anchor = helper.absolutePos(ANCHOR);
        for (int dx = -1; dx <= 2; dx++) {
            check(helper, helper.getLevel().getBlockState(anchor.offset(dx, 0, 0)).isAir(), "nothing should be placed at " + anchor.offset(dx, 0, 0));
        }
        check(helper, storeCount(tool) == stored, "the store should keep " + stored + ", has " + storeCount(tool));
        check(helper, energy(tool) == START_FE, "no energy should be spent, have " + energy(tool));
    }

    private static ToolBuilds.Prepared prepare(GameTestHelper helper, Player player, ItemStack tool) {
        ToolBuilds.Result result = ToolBuilds.prepare(helper.getLevel(), player, tool, helper.absolutePos(CLICKED), Direction.UP);
        if (result.prepared() == null) {
            helper.fail("tool build refused: " + (result.error() == null ? "?" : result.error().getString()));
        }
        return result.prepared();
    }

    private static AssemblyJob run(GameTestHelper helper, Player player, ToolBuilds.Prepared build) {
        return run(helper, player, build, AssemblyJob.create(helper.getLevel(), build.controllerPos(), build.plan(), build.perBlockFe()));
    }

    /** Ticks the job like the server does (one block per tick) until it is done. */
    private static AssemblyJob run(GameTestHelper helper, Player player, ToolBuilds.Prepared build, AssemblyJob job) {
        int ticks = 0;
        while (!AssemblyJobs.tickBuild(player, job, build.source(), 1)) {
            if (++ticks > 100) {
                helper.fail("tool build did not finish");
            }
        }
        return job;
    }

    private static Player player(GameTestHelper helper, float yRot) {
        Player player = helper.makeMockPlayer();
        player.setYRot(yRot);
        check(helper, !player.getAbilities().instabuild, "mock player should not be instabuild");
        return player;
    }

    /** A tool selecting assembly_test with {@value #START_FE} FE, held in the main hand. */
    private static ItemStack tool(Player player) {
        ItemStack tool = MMRegisters.MULTIBLOCK_TOOL.get().getDefaultInstance();
        ToolData.setStructure(tool, STRUCTURE);
        new ToolEnergy(tool, MMConfigSetup.COMMON.toolEnergyCapacity.get()).restore(START_FE);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        return tool;
    }

    /** Exactly the blocks of one assembly_test (lowest tiers), only in the tool's store. */
    private static void stockStore(ItemStack tool) {
        var store = new ToolStore(tool);
        store.insertItem(0, new ItemStack(controllerBlock()), false);
        store.insertItem(1, new ItemStack(port("s")), false);
        store.insertItem(2, new ItemStack(Blocks.GLASS), false);
        store.insertItem(3, new ItemStack(port("l")), false);
    }

    private static int storeCount(ItemStack tool) {
        var store = new ToolStore(tool);
        int total = 0;
        for (int i = 0; i < store.getSlots(); i++) {
            total += store.getStackInSlot(i).getCount();
        }
        return total;
    }

    private static int energy(ItemStack tool) {
        return new ToolEnergy(tool, MMConfigSetup.COMMON.toolEnergyCapacity.get()).getEnergyStored();
    }

    private static void expectFacing(GameTestHelper helper, BlockPos pos, Direction facing) {
        BlockState state = helper.getLevel().getBlockState(pos);
        check(helper, state.is(controllerBlock()), "expected the controller at " + pos + ", found " + state);
        check(helper, state.getValue(HorizontalDirectionalBlock.FACING) == facing, "controller should face " + facing + ", faces " + state.getValue(HorizontalDirectionalBlock.FACING));
    }

    private static StructureModel structure(GameTestHelper helper) {
        StructureModel model = StructureManager.STRUCTURES.get(STRUCTURE);
        check(helper, model != null, "structure " + STRUCTURE + " is not loaded");
        return model;
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
