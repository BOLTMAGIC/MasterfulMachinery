package io.ticticboom.mods.mmtest;

import com.mojang.authlib.GameProfile;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.builder.AssemblyJob;
import io.ticticboom.mods.mm.builder.AssemblyJobs;
import io.ticticboom.mods.mm.builder.ChainedMaterialSource;
import io.ticticboom.mods.mm.builder.me.CraftHandle;
import io.ticticboom.mods.mm.builder.me.CraftTracker;
import io.ticticboom.mods.mm.builder.me.MeAccess;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.structure.StructureManager;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tool builds are all or nothing on materials, and ask the tool's ME network to craft what is missing: here with a fake
 * network ({@link FakeMe}) whose crafts the test moves on by hand. The build never starts on its own; a later
 * right-click (prepare) builds once everything is there. Uses mmtest:assembly_test ("FCGS", one of each block).
 */
@GameTestHolder("mmtest")
@PrefixGameTestTemplate(false)
public class ToolCraftGameTests {
    private static final String TEMPLATE = "empty";
    private static final ResourceLocation STRUCTURE = ResourceLocation.tryBuild("mmtest", "assembly_test");
    private static final int START_FE = 1000;
    private static final BlockPos CLICKED = new BlockPos(3, 0, 3);
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);

    /** Missing blocks are crafted where the network has a pattern; the rest is reported; nothing is built. */
    @GameTest(template = TEMPLATE)
    public static void craftsRequestedForCraftablesOnly(GameTestHelper helper) {
        TestPlayer player = player(helper);
        ItemStack tool = tool(player);
        var store = new ToolStore(tool);
        store.insertItem(0, new ItemStack(controllerBlock()), false);
        store.insertItem(1, new ItemStack(Blocks.GLASS), false);
        FakeMe me = new FakeMe();
        me.craftable.add(port("s").asItem());

        ToolBuilds.Result result = prepare(helper, player, tool, me);

        check(helper, result.prepared() == null, "the build must not start with blocks missing");
        check(helper, me.requests.size() == 1 && me.requests.get(0).item() == port("s").asItem() && me.requests.get(0).amount() == 1,
                "only the craftable small port should be requested, got " + me.requests);
        TranslatableContents crafting = find(result.error(), "message.mm.tool.crafting");
        check(helper, crafting != null && crafting.getArgs()[0].equals(1), "expected 'crafting 1 items', got " + text(result.error()));
        check(helper, find(result.error(), "message.mm.tool.craft.no_pattern") != null,
                "the large port should be reported as having no pattern: " + text(result.error()));
        check(helper, CraftTracker.active(player, port("s").asItem()) == 1, "the requested craft should be tracked");
        expectNothingBuilt(helper, tool, 2);
        CraftTracker.clear(player);
        helper.succeed();
    }

    /** Nothing craftable: every missing block is named ("first 2 +N"), no craft is requested. */
    @GameTest(template = TEMPLATE)
    public static void nonCraftableReported(GameTestHelper helper) {
        TestPlayer player = player(helper);
        ItemStack tool = tool(player);
        new ToolStore(tool).insertItem(0, new ItemStack(Blocks.GLASS), false);
        FakeMe me = new FakeMe();

        ToolBuilds.Result result = prepare(helper, player, tool, me);

        check(helper, result.prepared() == null, "the build must not start");
        check(helper, me.requests.isEmpty(), "nothing is craftable, nothing should be requested: " + me.requests);
        check(helper, find(result.error(), "message.mm.tool.crafting") == null, "nothing is being crafted: " + text(result.error()));
        String message = text(result.error());
        check(helper, find(result.error(), "message.mm.tool.craft.no_pattern") != null && message.endsWith(" +1"),
                "the controller and two ports lack patterns, two named and '+1': " + message);
        expectNothingBuilt(helper, tool, 1);
        helper.succeed();
    }

    /**
     * The old tool's bug: a block being crafted was reported missing. Right-clicking again while the crafts run (even
     * when the network momentarily reports no pattern) only waits, and asks for nothing twice.
     */
    @GameTest(template = TEMPLATE)
    public static void rightClickWhileCraftingStillWaits(GameTestHelper helper) {
        TestPlayer player = player(helper);
        ItemStack tool = tool(player);
        var store = new ToolStore(tool);
        store.insertItem(0, new ItemStack(controllerBlock()), false);
        store.insertItem(1, new ItemStack(Blocks.GLASS), false);
        FakeMe me = new FakeMe();
        me.craftable.add(port("s").asItem());
        me.craftable.add(port("l").asItem());
        prepare(helper, player, tool, me);
        check(helper, me.requests.size() == 2, "both ports should be requested, got " + me.requests);
        me.requests.get(0).state = CraftHandle.State.CALCULATING;
        me.craftable.clear();

        ToolBuilds.Result again = prepare(helper, player, tool, me);

        check(helper, again.prepared() == null, "still waiting for the crafts");
        TranslatableContents crafting = find(again.error(), "message.mm.tool.crafting");
        check(helper, crafting != null && crafting.getArgs()[0].equals(2), "both ports should still be 'crafting': " + text(again.error()));
        check(helper, find(again.error(), "message.mm.tool.craft.no_pattern") == null && find(again.error(), "message.mm.assemble.missing") == null,
                "a block being crafted must never be reported missing: " + text(again.error()));
        check(helper, me.requests.size() == 2, "nothing should be requested twice, got " + me.requests);
        expectNothingBuilt(helper, tool, 2);
        CraftTracker.clear(player);
        helper.succeed();
    }

    /** Even when the network can't be reached on the next right-click, a block being crafted is not called missing. */
    @GameTest(template = TEMPLATE)
    public static void craftingNotMissingWhileNetworkUnreachable(GameTestHelper helper) {
        TestPlayer player = player(helper);
        ItemStack tool = tool(player);
        var store = new ToolStore(tool);
        store.insertItem(0, new ItemStack(controllerBlock()), false);
        store.insertItem(1, new ItemStack(Blocks.GLASS), false);
        FakeMe me = new FakeMe();
        me.craftable.add(port("s").asItem());
        prepare(helper, player, tool, me);

        ChainedMaterialSource noNetwork = ToolBuildPlan.source(player, tool, null);
        ToolBuilds.Result again = ToolBuilds.prepare(helper.getLevel(), player, tool, helper.absolutePos(CLICKED), Direction.UP, noNetwork);

        TranslatableContents crafting = find(again.error(), "message.mm.tool.crafting");
        check(helper, crafting != null && crafting.getArgs()[0].equals(1), "the small port is still being crafted: " + text(again.error()));
        TranslatableContents missing = find(again.error(), "message.mm.assemble.missing");
        check(helper, missing != null && !text(again.error()).contains(port("s").getName().getString())
                        && text(again.error()).contains(port("l").getName().getString()),
                "only the large port (never requested) is missing: " + text(again.error()));
        CraftTracker.clear(player);
        helper.succeed();
    }

    /** The crafts finish: the player is told to right-click, and the next right-click builds from the network. */
    @GameTest(template = TEMPLATE)
    public static void craftsDoneThenRightClickBuilds(GameTestHelper helper) {
        TestPlayer player = player(helper);
        ItemStack tool = tool(player);
        var store = new ToolStore(tool);
        store.insertItem(0, new ItemStack(controllerBlock()), false);
        store.insertItem(1, new ItemStack(Blocks.GLASS), false);
        FakeMe me = new FakeMe();
        me.craftable.add(port("s").asItem());
        me.craftable.add(port("l").asItem());
        prepare(helper, player, tool, me);

        CraftTracker.update(player);
        check(helper, player.messages.isEmpty(), "nothing to tell while crafting, got " + player.messages);
        for (FakeCraft craft : me.requests) {
            craft.state = CraftHandle.State.DONE;
            me.stock.merge(craft.item(), (long) craft.amount(), Long::sum);
        }
        CraftTracker.update(player);
        check(helper, player.lastKey().equals("message.mm.tool.craft.ready"), "the player should be told to right-click, got " + player.messages);
        check(helper, helper.getLevel().getBlockState(helper.absolutePos(ANCHOR)).isAir(), "the build must not start on its own");

        ToolBuilds.Result result = prepare(helper, player, tool, me);
        check(helper, result.prepared() != null, "everything is there now, the build should start: " + text(result.error()));
        check(helper, CraftTracker.orders(player).isEmpty(), "a started build forgets the crafts");
        AssemblyJob job = run(helper, player, result.prepared());

        check(helper, job.placed() == 4 && job.missing().isEmpty(), "expected 4 placed, nothing missing: " + job.placed() + ", " + job.missing());
        check(helper, StructureManager.STRUCTURES.get(STRUCTURE).formed(helper.getLevel(), result.prepared().controllerPos()), "structure not formed");
        check(helper, me.stock.get(port("s").asItem()) == 0 && me.stock.get(port("l").asItem()) == 0, "the crafted ports should be taken from ME: " + me.stock);
        check(helper, me.requests.size() == 2, "nothing more should be requested, got " + me.requests);
        helper.succeed();
    }

    /** A plan that lacks ingredients is told once; the next right-click tries again. */
    @GameTest(template = TEMPLATE)
    public static void failedPlanReported(GameTestHelper helper) {
        TestPlayer player = player(helper);
        ItemStack tool = tool(player);
        var store = new ToolStore(tool);
        store.insertItem(0, new ItemStack(controllerBlock()), false);
        store.insertItem(1, new ItemStack(Blocks.GLASS), false);
        store.insertItem(2, new ItemStack(port("l")), false);
        FakeMe me = new FakeMe();
        me.craftable.add(port("s").asItem());
        prepare(helper, player, tool, me);
        check(helper, me.requests.size() == 1, "the small port should be requested, got " + me.requests);

        FakeCraft craft = me.requests.get(0);
        craft.state = CraftHandle.State.FAILED;
        craft.failure = Component.translatable("message.mm.tool.craft.missing_ingredients");
        CraftTracker.update(player);
        check(helper, player.messages.size() == 1, "the failure should be told once, got " + player.messages);
        Component told = player.messages.get(0);
        check(helper, find(told, "message.mm.tool.craft.entry") != null && find(told, "message.mm.tool.craft.missing_ingredients") != null,
                "expected 'Test Item S ×1: missing ingredients in ME', got " + text(told));
        CraftTracker.update(player);
        check(helper, player.messages.size() == 1, "a failure is told only once, got " + player.messages);
        check(helper, CraftTracker.active(player, port("s").asItem()) == 0, "a failed craft is not on its way");

        ToolBuilds.Result again = prepare(helper, player, tool, me);
        check(helper, again.prepared() == null && me.requests.size() == 2, "a new right-click should try the craft again, got " + me.requests);
        check(helper, CraftTracker.orders(player).size() == 1 && CraftTracker.orders(player).get(0).failed() == null,
                "the new attempt replaces the failed one in the same order");
        expectNothingBuilt(helper, tool, 3);
        CraftTracker.clear(player);
        helper.succeed();
    }

    /** Auto-craft turned off: the missing blocks are named, nothing is requested. */
    @GameTest(template = TEMPLATE)
    public static void autoCraftOffOnlyReports(GameTestHelper helper) {
        TestPlayer player = player(helper);
        ItemStack tool = tool(player);
        ToolData.setAutoCraft(tool, false);
        var store = new ToolStore(tool);
        store.insertItem(0, new ItemStack(controllerBlock()), false);
        store.insertItem(1, new ItemStack(Blocks.GLASS), false);
        FakeMe me = new FakeMe();
        me.craftable.add(port("s").asItem());
        me.craftable.add(port("l").asItem());

        ToolBuilds.Result result = prepare(helper, player, tool, me);

        check(helper, result.error() != null && result.error().getContents() instanceof TranslatableContents c
                && c.getKey().equals("message.mm.assemble.missing"), "expected the missing message, got " + text(result.error()));
        check(helper, me.requests.isEmpty(), "auto-craft is off, nothing should be requested: " + me.requests);
        expectNothingBuilt(helper, tool, 2);
        helper.succeed();
    }

    /** Counts matter, not just presence: part in the store, the rest in ME is enough; one short is not. */
    @GameTest(template = TEMPLATE)
    public static void storeAndMeCountedTogether(GameTestHelper helper) {
        TestPlayer player = player(helper);
        ItemStack tool = tool(player);
        var store = new ToolStore(tool);
        store.insertItem(0, new ItemStack(controllerBlock()), false);
        store.insertItem(1, new ItemStack(Blocks.GLASS), false);
        FakeMe me = new FakeMe();
        me.stock.put(port("s").asItem(), 1L);

        ToolBuilds.Result short1 = prepare(helper, player, tool, me);
        check(helper, short1.prepared() == null && text(short1.error()).contains(port("l").getName().getString())
                        && !text(short1.error()).contains(port("s").getName().getString()),
                "only the large port is missing: " + text(short1.error()));

        me.stock.put(port("l").asItem(), 1L);
        ToolBuilds.Result ready = prepare(helper, player, tool, me);
        check(helper, ready.prepared() != null, "store and ME together hold everything: " + text(ready.error()));
        helper.succeed();
    }

    // --- helpers ---

    private static ToolBuilds.Result prepare(GameTestHelper helper, TestPlayer player, ItemStack tool, FakeMe me) {
        ChainedMaterialSource source = ToolBuildPlan.source(player, tool, me);
        return ToolBuilds.prepare(helper.getLevel(), player, tool, helper.absolutePos(CLICKED), Direction.UP, source);
    }

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

    /** The first part of component (itself, siblings, arguments) with the translation key, or null. */
    private static @Nullable TranslatableContents find(@Nullable Component component, String key) {
        if (component == null) {
            return null;
        }
        if (component.getContents() instanceof TranslatableContents contents) {
            if (contents.getKey().equals(key)) {
                return contents;
            }
            for (Object arg : contents.getArgs()) {
                if (arg instanceof Component part) {
                    TranslatableContents found = find(part, key);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            TranslatableContents found = find(sibling, key);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** The message as the server's language renders it, for checks and failure text. */
    private static String text(@Nullable Component component) {
        return component == null ? "null" : component.getString();
    }

    private static void expectNothingBuilt(GameTestHelper helper, ItemStack tool, int stored) {
        BlockPos anchor = helper.absolutePos(ANCHOR);
        for (int dx = -1; dx <= 2; dx++) {
            check(helper, helper.getLevel().getBlockState(anchor.offset(dx, 0, 0)).isAir(), "nothing should be placed at " + anchor.offset(dx, 0, 0));
        }
        var store = new ToolStore(tool);
        int total = 0;
        for (int i = 0; i < store.getSlots(); i++) {
            total += store.getStackInSlot(i).getCount();
        }
        check(helper, total == stored, "the store should keep " + stored + ", has " + total);
        int energy = new ToolEnergy(tool, MMConfigSetup.COMMON.toolEnergyCapacity.get()).getEnergyStored();
        check(helper, energy == START_FE, "no energy should be spent, have " + energy);
    }

    /** A network with a settable stock and patterns; its crafts wait until the test moves them on. */
    private static final class FakeMe implements MeAccess {
        final Map<Item, Long> stock = new HashMap<>();
        final Set<Item> craftable = new HashSet<>();
        final List<FakeCraft> requests = new ArrayList<>();

        @Override
        public boolean reachable() {
            return true;
        }

        @Override
        public long stock(Item item) {
            return stock.getOrDefault(item, 0L);
        }

        @Override
        public ItemStack extract(Item item, int amount, boolean simulate) {
            int got = (int) Math.min(amount, stock(item));
            if (got <= 0) {
                return ItemStack.EMPTY;
            }
            if (!simulate) {
                stock.put(item, stock(item) - got);
            }
            return new ItemStack(item, got);
        }

        @Override
        public boolean isCraftable(Item item) {
            return craftable.contains(item);
        }

        @Override
        public CraftHandle requestCraft(Item item, int amount) {
            FakeCraft craft = new FakeCraft(item, amount);
            requests.add(craft);
            return craft;
        }
    }

    private static final class FakeCraft implements CraftHandle {
        private final Item item;
        private final int amount;
        State state = State.CRAFTING;
        @Nullable
        Component failure;

        FakeCraft(Item item, int amount) {
            this.item = item;
            this.amount = amount;
        }

        @Override
        public State state() {
            return state;
        }

        @Override
        public Item item() {
            return item;
        }

        @Override
        public int amount() {
            return amount;
        }

        @Override
        public @Nullable Component failure() {
            return failure;
        }

        @Override
        public String toString() {
            return item + " x" + amount + " " + state;
        }
    }

    /** A server player facing north that records its messages. */
    private static TestPlayer player(GameTestHelper helper) {
        TestPlayer player = new TestPlayer(helper.getLevel());
        BlockPos at = helper.absolutePos(new BlockPos(3, 1, 5));
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180, 0);
        check(helper, player.getDirection() == Direction.NORTH, "the test player should face north, faces " + player.getDirection());
        return player;
    }

    private static final class TestPlayer extends FakePlayer {
        final List<Component> messages = new ArrayList<>();

        TestPlayer(ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "tool-craft-test"));
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

    private static ItemStack tool(TestPlayer player) {
        ItemStack tool = MMRegisters.MULTIBLOCK_TOOL.get().getDefaultInstance();
        ToolData.setStructure(tool, STRUCTURE);
        new ToolEnergy(tool, MMConfigSetup.COMMON.toolEnergyCapacity.get()).restore(START_FE);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        return tool;
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
