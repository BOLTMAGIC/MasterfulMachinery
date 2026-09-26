package io.ticticboom.mods.mmtest;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.StorageCell;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.me.helpers.IGridConnectedBlockEntity;
import com.mojang.authlib.GameProfile;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.builder.AssemblyJob;
import io.ticticboom.mods.mm.builder.AssemblyJobs;
import io.ticticboom.mods.mm.builder.me.MeAccessFactory;
import io.ticticboom.mods.mm.compat.ae2.NetworkAccess;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.networklink.LinkData;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.tool.ToolBuildPlan;
import io.ticticboom.mods.mm.tool.ToolBuilds;
import io.ticticboom.mods.mm.tool.ToolData;
import io.ticticboom.mods.mm.tool.ToolEnergy;
import io.ticticboom.mods.mm.tool.ToolStore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The Multiblock Tool on a real AE2 network: a creative energy cell powering an ME drive that holds a 1k cell filled
 * through AE2's cell API (an ad-hoc network, no controller). The tool is bound with Shift+right-click and builds
 * mmtest:assembly_test from the network only, or is refused it.
 */
@GameTestHolder("mmtest")
@PrefixGameTestTemplate(false)
public class MeToolGameTests {
    private static final String TEMPLATE = "empty";
    private static final ResourceLocation STRUCTURE = ResourceLocation.tryBuild("mmtest", "assembly_test");
    private static final int START_FE = 1000;
    /** Clicked on its top face, so the anchor is one block above; the structure runs along z = 3. */
    private static final BlockPos CLICKED = new BlockPos(3, 0, 3);
    /** Out of the structure's way. */
    private static final BlockPos ENERGY_CELL = new BlockPos(0, 1, 6);
    private static final BlockPos DRIVE = new BlockPos(1, 1, 6);
    private static final int EXTRA_GLASS = 4;
    /** AE2 creates grid nodes a tick after placement, then boots and assigns channels. */
    private static final int BOOT_TIMEOUT = 300;

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT)
    public static void toolBindsAndBuildsFromMeNetwork(GameTestHelper helper) {
        BlockPos drive = network(helper);
        TestPlayer player = player(helper);
        ItemStack tool = tool(player);

        helper.startSequence()
                .thenWaitUntil(() -> awaitStock(helper, drive))
                .thenExecute(() -> {
                    bind(player, tool, drive);
                    LinkData.NetworkPos bound = ToolData.network(tool);
                    check(helper, bound != null && bound.pos().equals(drive) && bound.dimension() == helper.getLevel().dimension(),
                            "Shift+right-click on the drive should bind the tool to it, got " + bound);
                    check(helper, player.lastKey().equals("message.mm.tool.me_bound"), "expected the bound message, got " + player.messages);

                    ToolBuilds.Result result = ToolBuilds.prepare(helper.getLevel(), player, tool, helper.absolutePos(CLICKED), Direction.UP);
                    check(helper, result.prepared() != null, "the build should start from the ME network, got "
                            + (result.error() == null ? "?" : result.error().getString()));
                    check(helper, result.notice() == null, "a reachable network needs no notice, got " + result.notice());
                    ToolBuilds.Prepared build = result.prepared();
                    AssemblyJob job = run(helper, player, build);

                    int fe = MMConfigSetup.COMMON.toolEnergyPerPlacedBlock.get();
                    check(helper, job.placed() == 4, "expected 4 placed (controller + 3), got " + job.placed());
                    check(helper, job.missing().isEmpty(), "nothing should be missing: " + job.missing());
                    check(helper, structure(helper).formed(helper.getLevel(), build.controllerPos()), "structure not formed from the ME network");
                    check(helper, energy(tool) == START_FE - 4 * fe, "the tool should pay for 4 blocks, has " + energy(tool));
                    KeyCounter left = liveStock(helper, drive);
                    check(helper, left.get(AEItemKey.of(controllerBlock())) == 0, "the controller should come out of ME");
                    check(helper, left.get(AEItemKey.of(port("s"))) == 0 && left.get(AEItemKey.of(port("l"))) == 0, "the ports should come out of ME");
                    check(helper, left.get(AEItemKey.of(Blocks.GLASS)) == EXTRA_GLASS,
                            "exactly one glass should be taken from ME, " + left.get(AEItemKey.of(Blocks.GLASS)) + " left");
                })
                .thenSucceed();
    }

    /** The bound block is gone: the build still goes on from the store, says why ME was not used, and ME is untouched. */
    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT)
    public static void toolFallsBackWhenMeUnreachable(GameTestHelper helper) {
        BlockPos drive = network(helper);
        BlockPos energyCell = helper.absolutePos(ENERGY_CELL);
        TestPlayer player = player(helper);
        ItemStack tool = tool(player);
        var store = new ToolStore(tool);
        store.insertItem(0, new ItemStack(controllerBlock()), false);
        store.insertItem(1, new ItemStack(port("s")), false);
        store.insertItem(2, new ItemStack(Blocks.GLASS), false);
        store.insertItem(3, new ItemStack(port("l")), false);

        helper.startSequence()
                .thenWaitUntil(() -> awaitStock(helper, drive))
                .thenExecute(() -> {
                    bind(player, tool, energyCell);
                    check(helper, ToolData.network(tool) != null && ToolData.network(tool).pos().equals(energyCell), "the tool should be bound to the energy cell");
                    // the bound block is removed: the drive loses its power as well
                    helper.getLevel().removeBlock(energyCell, false);

                    ToolBuilds.Result result = ToolBuilds.prepare(helper.getLevel(), player, tool, helper.absolutePos(CLICKED), Direction.UP);
                    check(helper, result.prepared() != null, "the build should fall back to the store, got "
                            + (result.error() == null ? "?" : result.error().getString()));
                    check(helper, hasKey(result.notice(), "message.mm.tool.me_unreachable"), "expected the unreachable notice, got " + result.notice());

                    AssemblyJob job = run(helper, player, result.prepared());

                    check(helper, job.placed() == 4, "expected 4 placed, got " + job.placed());
                    check(helper, structure(helper).formed(helper.getLevel(), result.prepared().controllerPos()), "structure not formed from the store");
                    check(helper, storeCount(tool) == 0, "every block should come out of the store, " + storeCount(tool) + " left");
                    KeyCounter cell = cellContents(helper, drive);
                    check(helper, cell.get(AEItemKey.of(Blocks.GLASS)) == 1 + EXTRA_GLASS && cell.get(AEItemKey.of(controllerBlock())) == 1,
                            "the ME cell must be untouched, holds " + cell);
                })
                .thenSucceed();
    }

    /**
     * AE2 15 has no security of its own: a network placed by one player can't be bound by another, and a tool bound by
     * the owner does nothing for them either.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT)
    public static void nonOwnerCannotBindOrUseNetwork(GameTestHelper helper) {
        BlockPos drive = network(helper);
        TestPlayer owner = player(helper);
        TestPlayer stranger = player(helper);
        own(helper, helper.absolutePos(ENERGY_CELL), owner);
        own(helper, drive, owner);
        ItemStack ownerTool = tool(owner);
        ItemStack strangerTool = tool(stranger);

        helper.startSequence()
                .thenWaitUntil(() -> awaitStock(helper, drive))
                .thenExecute(() -> {
                    bind(stranger, strangerTool, drive);
                    check(helper, ToolData.network(strangerTool) == null, "a stranger must not bind to the owner's network");
                    check(helper, stranger.lastKey().equals("message.mm.tool.me_no_access"), "expected the no access message, got " + stranger.messages);

                    bind(owner, ownerTool, drive);
                    check(helper, ToolData.network(ownerTool) != null, "the owner should bind to their own network");
                    check(helper, MeAccessFactory.forTool(owner, ownerTool) != null, "the owner's tool should reach the network");

                    // the owner's bound tool in the stranger's hands
                    ItemStack handed = ownerTool.copy();
                    stranger.setItemInHand(InteractionHand.MAIN_HAND, handed);
                    check(helper, MeAccessFactory.forTool(stranger, handed) == null, "a stranger must get no ME access through a bound tool");
                    ToolBuilds.Result result = ToolBuilds.prepare(helper.getLevel(), stranger, handed, helper.absolutePos(CLICKED), Direction.UP);
                    check(helper, hasKey(result.notice(), "message.mm.tool.me_denied"), "expected the no access notice, got " + result.notice());
                    check(helper, result.prepared() == null, "with nothing but the network to build from, the stranger's build must be refused");
                    check(helper, ToolBuildPlan.source(stranger, handed).take(controllerBlock()).isEmpty(), "nothing may be extracted for a stranger");
                    KeyCounter left = liveStock(helper, drive);
                    check(helper, left.get(AEItemKey.of(controllerBlock())) == 1 && left.get(AEItemKey.of(Blocks.GLASS)) == 1 + EXTRA_GLASS,
                            "the network must be untouched, holds " + left);
                })
                .thenSucceed();
    }

    /** A block taken from ME whose placement is cancelled goes back to the tool's store, not to ME, and is taken once. */
    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT)
    public static void cancelledMePlacementRefundedToStore(GameTestHelper helper) {
        BlockPos drive = network(helper);
        TestPlayer player = player(helper);
        ItemStack tool = tool(player);

        helper.startSequence()
                .thenWaitUntil(() -> awaitStock(helper, drive))
                .thenExecute(() -> {
                    bind(player, tool, drive);
                    ToolBuilds.Result result = ToolBuilds.prepare(helper.getLevel(), player, tool, helper.absolutePos(CLICKED), Direction.UP);
                    check(helper, result.prepared() != null, "the build should start from the ME network");
                    // a claim mod refusing the glass
                    Consumer<BlockEvent.EntityPlaceEvent> claims = event -> {
                        if (event.getEntity() == player && event.getPlacedBlock().is(Blocks.GLASS)) {
                            event.setCanceled(true);
                        }
                    };
                    MinecraftForge.EVENT_BUS.addListener(claims);
                    AssemblyJob job;
                    try {
                        job = run(helper, player, result.prepared());
                    } finally {
                        MinecraftForge.EVENT_BUS.unregister(claims);
                    }

                    check(helper, job.placed() == 3 && job.blocked() == 1, "expected 3 placed and 1 blocked, got " + job.placed() + "/" + job.blocked());
                    check(helper, storeCount(tool) == 1 && new ToolStore(tool).getStackInSlot(0).is(Blocks.GLASS.asItem()),
                            "the refused glass should be refunded into the store");
                    check(helper, liveStock(helper, drive).get(AEItemKey.of(Blocks.GLASS)) == EXTRA_GLASS,
                            "exactly one glass should have left ME, " + liveStock(helper, drive).get(AEItemKey.of(Blocks.GLASS)) + " left");
                })
                .thenSucceed();
    }

    /** The network goes away between two job ticks: the job goes on from the store alone and its summary says so. */
    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT)
    public static void networkLostMidJobContinuesFromStore(GameTestHelper helper) {
        BlockPos drive = network(helper);
        BlockPos energyCell = helper.absolutePos(ENERGY_CELL);
        TestPlayer player = player(helper);
        ItemStack tool = tool(player);
        var store = new ToolStore(tool);
        store.insertItem(0, new ItemStack(Blocks.GLASS), false);
        store.insertItem(1, new ItemStack(port("l")), false);

        helper.startSequence()
                .thenWaitUntil(() -> awaitStock(helper, drive))
                .thenExecute(() -> {
                    bind(player, tool, energyCell);
                    ToolBuilds.Result result = ToolBuilds.prepare(helper.getLevel(), player, tool, helper.absolutePos(CLICKED), Direction.UP);
                    check(helper, result.prepared() != null, "the build should start with the network");
                    ToolBuilds.Prepared build = result.prepared();
                    AssemblyJob job = AssemblyJob.create(helper.getLevel(), build.controllerPos(), build.plan(), build.perBlockFe());
                    // the first tick places the controller, which only ME has
                    AssemblyJobs.tickBuild(player, job, build.source(), 1);
                    check(helper, job.placed() == 1, "the controller should come from ME, placed " + job.placed());

                    helper.getLevel().removeBlock(energyCell, false);
                    int ticks = 0;
                    while (!AssemblyJobs.tickBuild(player, job, build.source(), 1)) {
                        if (++ticks > 100) {
                            helper.fail("tool build did not finish");
                        }
                    }

                    check(helper, job.placed() == 3, "controller, glass and large port expected, placed " + job.placed());
                    check(helper, job.missing().size() == 1 && job.missing().get(port("s")) == 1,
                            "the small port (only in ME) should be missing, got " + job.missing());
                    check(helper, storeCount(tool) == 0, "the store should be used, " + storeCount(tool) + " left");
                    check(helper, hasKey(AssemblyJobs.summary(job, build.source()), "message.mm.tool.me_lost"),
                            "the summary should say the network was lost: " + AssemblyJobs.summary(job, build.source()));
                    KeyCounter cell = cellContents(helper, drive);
                    check(helper, cell.get(AEItemKey.of(port("s"))) == 1 && cell.get(AEItemKey.of(port("l"))) == 1
                                    && cell.get(AEItemKey.of(Blocks.GLASS)) == 1 + EXTRA_GLASS,
                            "nothing more may leave the lost network, the cell holds " + cell);
                })
                .thenSucceed();
    }

    /** Whether component or any part of it (siblings, arguments) is the translation key. */
    private static boolean hasKey(@Nullable Component component, String key) {
        if (component == null) {
            return false;
        }
        if (component.getContents() instanceof TranslatableContents contents) {
            if (contents.getKey().equals(key)) {
                return true;
            }
            for (Object arg : contents.getArgs()) {
                if (arg instanceof Component part && hasKey(part, key)) {
                    return true;
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            if (hasKey(sibling, key)) {
                return true;
            }
        }
        return false;
    }

    /** The AE2 block at pos was placed by player (what AE2 records when a player places it). */
    private static void own(GameTestHelper helper, BlockPos pos, TestPlayer player) {
        ((IGridConnectedBlockEntity) helper.getLevel().getBlockEntity(pos)).getMainNode().setOwningPlayer(player);
    }

    /** Creative energy cell + drive with a filled 1k cell; returns the drive's absolute position. */
    private static BlockPos network(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos drive = helper.absolutePos(DRIVE);
        level.setBlockAndUpdate(helper.absolutePos(ENERGY_CELL), AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        level.setBlockAndUpdate(drive, AEBlocks.DRIVE.block().defaultBlockState());

        ItemStack cellStack = AEItems.ITEM_CELL_1K.stack();
        StorageCell cell = StorageCells.getCellInventory(cellStack, null);
        check(helper, cell != null, "no cell inventory for the 1k cell");
        for (ItemLike item : List.of(controllerBlock(), port("s"), port("l"))) {
            fill(helper, cell, item, 1);
        }
        fill(helper, cell, Blocks.GLASS, 1 + EXTRA_GLASS);
        cell.persist();
        ((DriveBlockEntity) level.getBlockEntity(drive)).getInternalInventory().setItemDirect(0, cellStack);
        return drive;
    }

    private static void fill(GameTestHelper helper, StorageCell cell, ItemLike item, int amount) {
        long inserted = cell.insert(AEItemKey.of(item), amount, Actionable.MODULATE, IActionSource.empty());
        check(helper, inserted == amount, "could only put " + inserted + " of " + item + " into the cell");
    }

    /** Fails (so the sequence waits) until the drive's network is up and its cached stock shows the cell's items. */
    private static void awaitStock(GameTestHelper helper, BlockPos drive) {
        IGrid grid = NetworkAccess.grid(helper.getLevel().getServer(), networkPos(helper, drive));
        check(helper, grid != null, "the network is not up yet");
        check(helper, grid.getStorageService().getCachedInventory().get(AEItemKey.of(Blocks.GLASS)) > 0, "the cell is not visible yet");
    }

    /** The network's stock read now (not the per-tick cache). */
    private static KeyCounter liveStock(GameTestHelper helper, BlockPos drive) {
        IGrid grid = NetworkAccess.grid(helper.getLevel().getServer(), networkPos(helper, drive));
        check(helper, grid != null, "the network should still be up");
        return grid.getStorageService().getInventory().getAvailableStacks();
    }

    /** What the drive's cell item holds, read from the item itself (works without power). */
    private static KeyCounter cellContents(GameTestHelper helper, BlockPos drive) {
        var be = (DriveBlockEntity) helper.getLevel().getBlockEntity(drive);
        check(helper, be != null, "the drive should still be there");
        StorageCell cell = StorageCells.getCellInventory(be.getInternalInventory().getStackInSlot(0), null);
        check(helper, cell != null, "the drive should still hold the cell");
        return cell.getAvailableStacks();
    }

    private static LinkData.NetworkPos networkPos(GameTestHelper helper, BlockPos pos) {
        return new LinkData.NetworkPos(helper.getLevel().dimension(), pos, Direction.UP);
    }

    /** Shift+right-click on the top face of pos with the tool, as the server sees it. */
    private static void bind(TestPlayer player, ItemStack tool, BlockPos pos) {
        player.setShiftKeyDown(true);
        try {
            tool.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
        } finally {
            player.setShiftKeyDown(false);
        }
    }

    /** Ticks the job like the server does (one block per tick, the source refreshed each tick) until it is done. */
    private static AssemblyJob run(GameTestHelper helper, TestPlayer player, ToolBuilds.Prepared build) {
        AssemblyJob job = AssemblyJob.create(helper.getLevel(), build.controllerPos(), build.plan(), build.perBlockFe());
        int ticks = 0;
        while (!AssemblyJobs.tickBuild(player, job, build.source(), 1)) {
            if (++ticks > 100) {
                helper.fail("tool build did not finish");
            }
        }
        return job;
    }

    /** A server player (AE2 extracts as a player) facing north that records its action bar and chat messages. */
    private static TestPlayer player(GameTestHelper helper) {
        TestPlayer player = new TestPlayer(helper.getLevel());
        BlockPos at = helper.absolutePos(new BlockPos(3, 1, 5));
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180, 0);
        check(helper, player.getDirection() == Direction.NORTH, "the test player should face north, faces " + player.getDirection());
        check(helper, !player.getAbilities().instabuild, "the test player should not be instabuild");
        return player;
    }

    private static final class TestPlayer extends FakePlayer {
        final List<Component> messages = new ArrayList<>();

        TestPlayer(ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "me-tool-test"));
        }

        @Override
        public void displayClientMessage(Component message, boolean actionBar) {
            messages.add(message);
        }

        String lastKey() {
            if (messages.isEmpty()) {
                return "";
            }
            return messages.get(messages.size() - 1).getContents() instanceof TranslatableContents contents ? contents.getKey() : "";
        }
    }

    /** A tool selecting assembly_test with {@value #START_FE} FE, held in the main hand. */
    private static ItemStack tool(TestPlayer player) {
        ItemStack tool = MMRegisters.MULTIBLOCK_TOOL.get().getDefaultInstance();
        ToolData.setStructure(tool, STRUCTURE);
        new ToolEnergy(tool, MMConfigSetup.COMMON.toolEnergyCapacity.get()).restore(START_FE);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        return tool;
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
