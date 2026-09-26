package io.ticticboom.mods.mmtest.client;

import com.mojang.logging.LogUtils;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.builder.DismantlePlanner;
import io.ticticboom.mods.mm.client.builder.AssemblyScreen;
import io.ticticboom.mods.mm.client.tool.MultiblockToolScreen;
import io.ticticboom.mods.mm.client.tool.ToolHologramRenderer;
import io.ticticboom.mods.mm.client.tool.ToolKeys;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerScreen;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.tool.ToolData;
import io.ticticboom.mods.mm.tool.ToolEnergy;
import io.ticticboom.mods.mm.tool.ToolStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Real-client smoke test of the multiblock tool, active only with {@code -Dmmtest.clientSmoke=true} (Gradle run
 * {@code runClientSmoke}). From the title screen it creates a superflat world, then drives the tool's client code the
 * way a player would: hologram, tool screen (tabs, search), build by right-click, the controller's Assemble screen,
 * V-hold dismantle and Shift+scroll. Every step logs {@code [MM-SMOKE] step <name> OK|FAIL}, the run ends with
 * {@code [MM-SMOKE] RESULT PASS|FAIL} and the client stops itself. Screenshots go to {@code <run dir>/screenshots}.
 * <p>
 * The mod's client state it asserts on (hologram plan, dismantle outline, screen internals) is private, so it is read
 * by reflection instead of widening the mod's API for a test.
 */
@Mod.EventBusSubscriber(modid = "mmtest", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientSmokeTest {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean("mmtest.clientSmoke");
    private static final String TAG = "[MM-SMOKE] ";
    private static final String WORLD = "mm_client_smoke";
    private static final ResourceLocation STRUCTURE = ResourceLocation.tryBuild("mmtest", "assembly_test");
    /** Whole test, counted in client ticks from world creation on: 4 minutes. */
    private static final int TOTAL_TICKS = 4 * 60 * 20;
    /** Wall-clock safety net (loading screens do not tick): the JVM is halted after this. */
    private static final long WATCHDOG_MS = 15 * 60 * 1000L;
    private static final int HOLD_TICKS = 25;

    private record Step(String name, int timeoutTicks, Body body) {
    }

    @FunctionalInterface
    private interface Body {
        /** @return true when the step is done; throws when it failed */
        boolean run(int tick) throws Exception;
    }

    private static final List<Step> STEPS = List.of(
            new Step("title_screen", 20 * 60 * 5, ClientSmokeTest::waitTitle),
            new Step("create_world", 20 * 60, ClientSmokeTest::createWorld),
            new Step("world_ready", 20 * 30, ClientSmokeTest::worldReady),
            new Step("server_setup", 20 * 10, ClientSmokeTest::serverSetup),
            new Step("aim_floor", 20 * 15, ClientSmokeTest::aimFloor),
            new Step("hologram", 20 * 5, ClientSmokeTest::hologram),
            new Step("open_tool_screen", 20 * 10, ClientSmokeTest::openToolScreen),
            new Step("tool_screen_settings_tab", 20 * 5, ClientSmokeTest::settingsTab),
            new Step("tool_screen_search", 20 * 5, ClientSmokeTest::search),
            new Step("close_tool_screen", 20 * 5, ClientSmokeTest::closeScreen),
            new Step("build", 20 * 20, ClientSmokeTest::build),
            new Step("built_screenshot", 20 * 5, ClientSmokeTest::builtScreenshot),
            new Step("open_controller_screen", 20 * 10, ClientSmokeTest::openControllerScreen),
            new Step("assemble_screen", 20 * 5, ClientSmokeTest::assembleScreen),
            new Step("close_controller_screen", 20 * 5, ClientSmokeTest::closeScreen),
            new Step("aim_machine", 20 * 5, ClientSmokeTest::aimMachine),
            new Step("dismantle_hold", 20 * 5, ClientSmokeTest::dismantleHold),
            new Step("dismantle_server", 20 * 20, ClientSmokeTest::dismantleServer),
            new Step("shift_scroll", 20 * 5, ClientSmokeTest::shiftScroll));

    private static int stepIndex;
    private static int stepTick;
    private static int totalTicks = -1;
    private static boolean finished;
    private static final Map<String, CompletableFuture<?>> futures = new HashMap<>();

    // found along the way
    private static BlockPos floor;
    private static BlockPos controller;
    private static List<BlockPos> machine = List.of();
    private static BlockPos aimed;
    private static boolean highlightSeen;
    private static int turnsBefore;
    // tick a step's condition was first met (-1: not yet)
    private static int aimedTick = -1;

    private ClientSmokeTest() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // START: keys set here are seen by the game's own tick and the mod's END-phase handlers of the same tick
        if (!ENABLED || finished || event.phase != TickEvent.Phase.START) {
            return;
        }
        if (stepIndex == 0 && stepTick == 0) {
            startWatchdog();
            LOGGER.info(TAG + "started");
        }
        Step step = STEPS.get(stepIndex);
        if (Minecraft.getInstance().level != null) {
            // chat from other mods (KubeJS, ProbeJS) would cover the screenshots
            Minecraft.getInstance().gui.getChat().clearMessages(false);
        }
        try {
            if (totalTicks >= 0 && ++totalTicks > TOTAL_TICKS) {
                throw new IllegalStateException("whole test exceeded " + TOTAL_TICKS + " client ticks");
            }
            if (stepTick > step.timeoutTicks()) {
                throw new IllegalStateException("timed out after " + step.timeoutTicks() + " ticks");
            }
            if (step.body().run(stepTick++)) {
                LOGGER.info(TAG + "step {} OK ({} ticks)", step.name(), stepTick);
                futures.clear();
                stepTick = 0;
                if (++stepIndex == STEPS.size()) {
                    finish(true);
                }
            }
        } catch (Throwable e) {
            LOGGER.error(TAG + "step {} FAIL {}", step.name(), e.toString(), e);
            finish(false);
        }
    }

    private static void finish(boolean pass) {
        finished = true;
        Minecraft mc = Minecraft.getInstance();
        ToolKeys.DISMANTLE.setDown(false);
        mc.options.keyShift.setDown(false);
        LOGGER.info(TAG + "RESULT {}", pass ? "PASS" : "FAIL");
        mc.stop();
    }

    private static void startWatchdog() {
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(WATCHDOG_MS);
            } catch (InterruptedException e) {
                return;
            }
            if (!finished) {
                LOGGER.error(TAG + "watchdog: no result after {} ms", WATCHDOG_MS);
                LOGGER.error(TAG + "RESULT FAIL");
                Runtime.getRuntime().halt(2);
            }
        }, "mm-smoke-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    // ---- steps ----

    /** Loading done: no overlay, a screen (title or first-launch onboarding) shown for a moment. */
    private static boolean waitTitle(int tick) {
        Minecraft mc = Minecraft.getInstance();
        return mc.getOverlay() == null && mc.screen != null && mc.level == null && tick >= 20;
    }

    private static boolean createWorld(int tick) throws IOException {
        Minecraft mc = Minecraft.getInstance();
        if (tick == 0) {
            totalTicks = 0;
            // the window may lose focus while the test runs
            mc.options.pauseOnLostFocus = false;
            mc.options.renderDistance().set(6);
            // no "move with WASD" toasts over the screenshots
            mc.getTutorial().setStep(TutorialSteps.NONE);
            deleteRecursively(mc.getLevelSource().getBaseDir().resolve(WORLD));
            deleteOldScreenshots(mc);
            GameRules rules = new GameRules();
            rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
            rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
            rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
            LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT);
            WorldOptions options = new WorldOptions(20260926L, false, false);
            // outside the tick: world loading runs its own nested frames
            mc.tell(() -> mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, options,
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions()));
            return false;
        }
        return mc.level != null && mc.player != null && mc.getSingleplayerServer() != null;
    }

    private static boolean worldReady(int tick) {
        Minecraft mc = Minecraft.getInstance();
        return tick >= 40 && mc.screen == null && mc.level.getChunkSource().hasChunk(0, 0) && mc.level.getChunkSource().hasChunk(0, -1);
    }

    /**
     * Survival, a tool with assembly_test, full FE and exactly one assembly_test worth of blocks in its store, at
     * (0.5, floor, 0.5) facing north.
     */
    private static boolean serverSetup(int tick) {
        Optional<Integer> floorY = server("setup", player -> {
            MinecraftServer server = player.getServer();
            var level = player.serverLevel();
            level.setDayTime(6000);
            level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
            player.setGameMode(GameType.SURVIVAL);
            player.getInventory().clearContent();
            ItemStack tool = MMRegisters.MULTIBLOCK_TOOL.get().getDefaultInstance();
            ToolData.setStructure(tool, STRUCTURE);
            int capacity = MMConfigSetup.COMMON.toolEnergyCapacity.get();
            new ToolEnergy(tool, capacity).restore(capacity);
            var store = new ToolStore(tool);
            store.insertItem(0, new ItemStack(controllerBlock()), false);
            store.insertItem(1, new ItemStack(port("s")), false);
            store.insertItem(2, new ItemStack(Blocks.GLASS), false);
            store.insertItem(3, new ItemStack(port("l")), false);
            player.getInventory().setItem(0, tool);
            player.getInventory().selected = 0;
            player.connection.send(new ClientboundSetCarriedItemPacket(0));
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, -3) - 1;
            player.teleportTo(level, 0.5, y + 1, 0.5, 180.0F, 0.0F);
            player.inventoryMenu.broadcastChanges();
            return y;
        });
        if (floorY.isEmpty()) {
            return false;
        }
        floor = new BlockPos(0, floorY.get(), -3);
        Minecraft.getInstance().player.getInventory().selected = 0;
        return true;
    }

    /** Looks at the top of the floor block two blocks north; waits for the tool, survival and rendered chunks. */
    private static boolean aimFloor(int tick) {
        Minecraft mc = Minecraft.getInstance();
        lookAt(new Vec3(floor.getX() + 0.5, floor.getY() + 0.99, floor.getZ() + 0.5));
        if (tick < 40) {
            return false;
        }
        ItemStack held = mc.player.getMainHandItem();
        return held.is(MMRegisters.MULTIBLOCK_TOOL.get()) && STRUCTURE.equals(ToolData.structure(held))
                && mc.gameMode.getPlayerMode() == GameType.SURVIVAL && hitting(floor, Direction.UP)
                && mc.player.getDirection() == Direction.NORTH;
    }

    /** The hologram's cached plan: 4 ghosts (nothing is built yet) and an outline. */
    private static boolean hologram(int tick) throws Exception {
        if (tick == 10) {
            List<?> ghosts = (List<?>) staticField(ToolHologramRenderer.class, "ghosts");
            AABB bounds = (AABB) staticField(ToolHologramRenderer.class, "bounds");
            check(bounds != null, "the hologram has no outline");
            check(ghosts.size() == 4, "expected 4 ghost blocks, got " + ghosts.size());
            screenshot("smoke_hologram.png");
        }
        return tick >= 14;
    }

    /** Shift+right-click on a plain block opens the tool's screen (the item's own useOn path). */
    private static boolean openToolScreen(int tick) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        if (tick <= 3) {
            // sneaking lowers the eyes: aim again
            lookAt(new Vec3(floor.getX() + 0.5, floor.getY() + 0.99, floor.getZ() + 0.5));
        }
        if (tick == 0) {
            mc.options.keyShift.setDown(true);
        } else if (tick == 3) {
            check(mc.player.isShiftKeyDown(), "the player should sneak");
            check(hitting(floor, Direction.UP), "the floor should still be aimed at");
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, (BlockHitResult) mc.hitResult);
        } else if (tick > 3 && mc.screen instanceof MultiblockToolScreen) {
            mc.options.keyShift.setDown(false);
            if (aimedTick < 0) {
                aimedTick = tick;
            }
            if (tick == aimedTick + 5) {
                screenshot("smoke_tool_screen.png");
            }
            if (tick >= aimedTick + 8) {
                aimedTick = -1;
                return true;
            }
        }
        return false;
    }

    private static boolean settingsTab(int tick) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        MultiblockToolScreen screen = toolScreen(mc);
        if (tick == 0) {
            Rect2i area = screen.getTabAreas().get(1);
            screen.mouseClicked(area.getX() + area.getWidth() / 2.0, area.getY() + area.getHeight() / 2.0, 0);
            check(String.valueOf(staticField(MultiblockToolScreen.class, "tab")).equals("SETTINGS"), "the Settings tab should be active");
        } else if (tick == 5) {
            screenshot("smoke_tool_settings.png");
        }
        return tick >= 8;
    }

    private static boolean search(int tick) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        MultiblockToolScreen screen = toolScreen(mc);
        if (tick == 0) {
            Rect2i area = screen.getTabAreas().get(0);
            screen.mouseClicked(area.getX() + area.getWidth() / 2.0, area.getY() + area.getHeight() / 2.0, 0);
            check(String.valueOf(staticField(MultiblockToolScreen.class, "tab")).equals("STRUCTURES"), "the Structures tab should be active");
            EditBox box = searchBox(screen);
            int before = galleryLines(screen).size();
            screen.mouseClicked(box.getX() + 4, box.getY() + box.getHeight() / 2.0, 0);
            check(box.isFocused(), "clicking the search box should focus it");
            for (char c : "assem".toCharArray()) {
                screen.charTyped(c, 0);
            }
            check(box.getValue().equals("assem"), "the search box should read 'assem', reads '" + box.getValue() + "'");
            // the fixture has two structures named "Assembly ...", both mmtest's: its group header and those two
            List<?> lines = galleryLines(screen);
            int after = lines.size();
            check(after < before, "the search should hide lines: " + before + " -> " + after);
            check(after == 3, "expected 3 lines (mmtest header + 2 structures), got " + after);
            check(record(lines.get(0), "structure") == null, "the first line should be the group header");
            check("mmtest".equals(record(record(lines.get(0), "group"), "namespace")), "the header should be mmtest's group");
            var shown = new HashSet<ResourceLocation>();
            for (Object line : lines.subList(1, after)) {
                StructureModel structure = (StructureModel) record(line, "structure");
                check(structure != null, "a structure line expected after the header");
                check(structure.name().toLowerCase(Locale.ROOT).contains("assem"), "'" + structure.name() + "' does not match 'assem'");
                shown.add(structure.id());
            }
            check(shown.equals(Set.of(ResourceLocation.tryBuild("mmtest", "assembly_extra"), STRUCTURE)),
                    "expected assembly_extra and assembly_test, got " + shown);
            LOGGER.info(TAG + "gallery lines {} -> {} for 'assem'", before, after);
        } else if (tick == 5) {
            screenshot("smoke_tool_search.png");
        }
        return tick >= 8;
    }

    private static boolean closeScreen(int tick) {
        Minecraft mc = Minecraft.getInstance();
        if (tick == 0) {
            mc.player.closeContainer();
        }
        return mc.screen == null && tick >= 3;
    }

    /** Right-click with the tool on the floor builds the machine; waits for the server's job to form it. */
    private static boolean build(int tick) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        if (tick == 0) {
            lookAt(new Vec3(floor.getX() + 0.5, floor.getY() + 0.99, floor.getZ() + 0.5));
            return false;
        }
        if (tick == 2) {
            check(hitting(floor, Direction.UP), "the floor should be aimed at");
            check(!mc.player.isShiftKeyDown(), "the player should not sneak");
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, (BlockHitResult) mc.hitResult);
            return false;
        }
        if (tick < 2) {
            return false;
        }
        BlockPos anchor = floor.above();
        Optional<List<BlockPos>> formed = poll("formed", player -> {
            var level = player.serverLevel();
            if (!(level.getBlockEntity(anchor) instanceof MachineControllerBlockEntity be) || !be.isFormed()
                    || be.getStructure() == null || !be.getStructure().id().equals(STRUCTURE)) {
                return null;
            }
            List<BlockPos> positions = DismantlePlanner.positions(level, be);
            int stored = storeTotal(player.getInventory().getItem(0));
            if (stored != 0) {
                throw new IllegalStateException("every block should come out of the store, " + stored + " left");
            }
            return positions;
        });
        if (formed.isEmpty()) {
            return false;
        }
        controller = anchor;
        machine = formed.get();
        check(machine.size() == 4, "expected 4 machine positions, got " + machine);
        LOGGER.info(TAG + "built at {}: {}", controller, machine);
        return true;
    }

    private static boolean builtScreenshot(int tick) {
        if (tick == 10) {
            screenshot("smoke_built.png");
        }
        return tick >= 13;
    }

    /** Empty hand, right-click on the controller: its screen opens like for any player. */
    private static boolean openControllerScreen(int tick) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        if (tick == 0) {
            mc.player.getInventory().selected = 1;
            lookAt(Vec3.atCenterOf(controller).relative(Direction.SOUTH, 0.49));
            return false;
        }
        if (tick == 3) {
            check(mc.player.getMainHandItem().isEmpty(), "the main hand should be empty");
            check(hitting(controller, null), "the controller should be aimed at");
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, (BlockHitResult) mc.hitResult);
            return false;
        }
        return tick > 3 && mc.screen instanceof MachineControllerScreen && tick >= 8;
    }

    /** The Assemble (brick) button opens the Assemble screen. */
    private static boolean assembleScreen(int tick) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        if (tick == 0) {
            check(mc.screen instanceof MachineControllerScreen, "the controller screen should be open, is " + mc.screen);
            MachineControllerScreen screen = (MachineControllerScreen) mc.screen;
            int x = screen.getGuiLeft() + (int) field(screen, MachineControllerScreen.class, "assembleBtnX")
                    + (int) staticField(MachineControllerScreen.class, "PAGE_BTN") / 2;
            int y = screen.getGuiTop() + (int) staticField(MachineControllerScreen.class, "PAGE_BTN_Y")
                    + (int) staticField(MachineControllerScreen.class, "PAGE_BTN") / 2;
            screen.mouseClicked(x, y, 0);
            check(mc.screen instanceof AssemblyScreen, "the Assemble button should open the Assemble screen, got " + mc.screen);
        } else if (tick == 5) {
            check(mc.screen instanceof AssemblyScreen, "the Assemble screen should stay open, got " + mc.screen);
            screenshot("smoke_assemble_screen.png");
        } else if (tick == 8) {
            // back to the controller screen, which closeScreen then closes
            mc.screen.onClose();
            check(mc.screen instanceof MachineControllerScreen, "closing Assemble should return to the controller screen, got " + mc.screen);
            return true;
        }
        return false;
    }

    /** The tool again, aimed at the machine's glass. */
    private static boolean aimMachine(int tick) {
        Minecraft mc = Minecraft.getInstance();
        aimed = null;
        for (BlockPos pos : machine) {
            if (mc.level.getBlockState(pos).is(Blocks.GLASS)) {
                aimed = pos;
            }
        }
        if (aimed == null) {
            throw new IllegalStateException("no glass among " + machine);
        }
        mc.player.getInventory().selected = 0;
        lookAt(Vec3.atCenterOf(aimed));
        return tick >= 5 && hitting(aimed, null) && mc.player.getMainHandItem().is(MMRegisters.MULTIBLOCK_TOOL.get());
    }

    /** V held for {@value #HOLD_TICKS} ticks: the machine is outlined while held, then dismantled. */
    private static boolean dismantleHold(int tick) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        if (tick == 0) {
            highlightSeen = false;
        }
        if (tick < HOLD_TICKS) {
            ToolKeys.DISMANTLE.setDown(true);
            check(ToolKeys.DISMANTLE.isDown(), "the dismantle key should read as held");
        }
        if (tick >= 3 && tick <= 15) {
            Method highlight = ToolKeys.class.getDeclaredMethod("dismantleHighlight", net.minecraft.world.level.Level.class);
            highlight.setAccessible(true);
            List<?> outlined = (List<?>) highlight.invoke(null, mc.level);
            check(new HashSet<>(outlined).equals(new HashSet<>(machine)), "hold tick " + tick + ": outline " + outlined + " should be the machine " + machine);
            highlightSeen = true;
        }
        if (tick == 12) {
            screenshot("smoke_dismantle_hold.png");
        }
        if (tick == HOLD_TICKS) {
            ToolKeys.DISMANTLE.setDown(false);
            check(highlightSeen, "the outline was never checked");
            return true;
        }
        return false;
    }

    /** The server removes every block into the tool's store. */
    private static boolean dismantleServer(int tick) {
        Optional<Boolean> done = poll("dismantled", player -> {
            var level = player.serverLevel();
            for (BlockPos pos : machine) {
                if (!level.getBlockState(pos).isAir()) {
                    return null;
                }
            }
            ItemStack tool = player.getInventory().getItem(0);
            for (Item item : List.of(controllerBlock().asItem(), port("s").asItem(), Items.GLASS, port("l").asItem())) {
                int count = storeCount(tool, item);
                if (count != 1) {
                    throw new IllegalStateException("the store should hold one " + item + ", holds " + count);
                }
            }
            return true;
        });
        return done.isPresent();
    }

    /** Sneaking + one scroll notch through the Forge event turns the tool's build a quarter turn. */
    private static boolean shiftScroll(int tick) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        if (tick == 0) {
            Optional<Integer> turns = server("turns_before", player -> ToolData.extraTurns(player.getInventory().getItem(0)));
            if (turns.isEmpty()) {
                // run tick 0 again next tick
                stepTick = 0;
                return false;
            }
            turnsBefore = turns.get();
            mc.options.keyShift.setDown(true);
            return false;
        }
        if (tick == 3) {
            check(mc.player.isShiftKeyDown(), "the player should sneak");
            var event = new InputEvent.MouseScrollingEvent(1.0, false, false, false, mc.mouseHandler.xpos(), mc.mouseHandler.ypos());
            MinecraftForge.EVENT_BUS.post(event);
            check(event.isCanceled(), "Shift+scroll with the tool should be consumed (not change the hotbar slot)");
            return false;
        }
        if (tick < 3) {
            return false;
        }
        int expected = Math.floorMod(turnsBefore + 1, 4);
        Optional<Boolean> turned = poll("turned", player -> ToolData.extraTurns(player.getInventory().getItem(0)) == expected ? Boolean.TRUE : null);
        if (turned.isEmpty()) {
            return false;
        }
        mc.options.keyShift.setDown(false);
        LOGGER.info(TAG + "extra turns {} -> {}", turnsBefore, expected);
        return true;
    }

    // ---- helpers ----

    /** Runs a task once on the integrated server thread; empty until it has finished. */
    @SuppressWarnings("unchecked")
    private static <T> Optional<T> server(String key, Function<ServerPlayer, T> task) {
        CompletableFuture<T> future = (CompletableFuture<T>) futures.computeIfAbsent(key, k -> submit(task));
        if (!future.isDone()) {
            return Optional.empty();
        }
        return Optional.ofNullable(future.join());
    }

    /** Like {@link #server} but runs the task again every tick until it returns non-null. */
    private static <T> Optional<T> poll(String key, Function<ServerPlayer, T> task) {
        Optional<T> result = server(key, task);
        CompletableFuture<?> future = futures.get(key);
        if (result.isEmpty() && future.isDone()) {
            futures.remove(key);
        }
        return result;
    }

    private static <T> CompletableFuture<T> submit(Function<ServerPlayer, T> task) {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        UUID id = mc.player.getUUID();
        return server.submit(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) {
                throw new IllegalStateException("no server player");
            }
            return task.apply(player);
        });
    }

    private static void lookAt(Vec3 target) {
        Minecraft.getInstance().player.lookAt(EntityAnchorArgument.Anchor.EYES, target);
    }

    /** The crosshair is on this block (and this face, when given). */
    private static boolean hitting(BlockPos pos, Direction face) {
        HitResult hit = Minecraft.getInstance().hitResult;
        return hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK && block.getBlockPos().equals(pos)
                && (face == null || block.getDirection() == face);
    }

    private static MultiblockToolScreen toolScreen(Minecraft mc) {
        check(mc.screen instanceof MultiblockToolScreen, "the tool screen should be open, is " + mc.screen);
        return (MultiblockToolScreen) mc.screen;
    }

    private static EditBox searchBox(Screen screen) {
        for (var child : screen.children()) {
            if (child instanceof EditBox box) {
                return box;
            }
        }
        throw new IllegalStateException("the tool screen has no search box");
    }

    private static List<?> galleryLines(MultiblockToolScreen screen) throws Exception {
        Object gallery = field(screen, MultiblockToolScreen.class, "gallery");
        return List.copyOf((List<?>) field(gallery, gallery.getClass(), "lines"));
    }

    /** A component of a private record. */
    private static Object record(Object record, String component) throws Exception {
        Method accessor = record.getClass().getDeclaredMethod(component);
        accessor.setAccessible(true);
        return accessor.invoke(record);
    }

    private static void screenshot(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> LOGGER.info(TAG + "screenshot {}: {}", name, message.getString()));
    }

    private static void deleteOldScreenshots(Minecraft mc) throws IOException {
        File dir = new File(mc.gameDirectory, Screenshot.SCREENSHOT_DIR);
        File[] old = dir.listFiles((d, n) -> n.startsWith("smoke_") && n.endsWith(".png"));
        if (old != null) {
            for (File file : old) {
                Files.deleteIfExists(file.toPath());
            }
        }
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        List<Path> paths;
        try (Stream<Path> walk = Files.walk(dir)) {
            paths = new ArrayList<>(walk.sorted(Comparator.reverseOrder()).toList());
        }
        for (Path path : paths) {
            Files.delete(path);
        }
    }

    private static Object staticField(Class<?> owner, String name) throws Exception {
        return field(null, owner, name);
    }

    private static Object field(Object target, Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static int storeTotal(ItemStack tool) {
        var store = new ToolStore(tool);
        int total = 0;
        for (int i = 0; i < store.getSlots(); i++) {
            total += store.getStackInSlot(i).getCount();
        }
        return total;
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

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
