package io.ticticboom.mods.mm.builder;

import io.ticticboom.mods.mm.tool.ToolDismantles;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = Ref.ID)
public final class AssemblyJobs {
    private static final double MAX_DISTANCE_SQR = 64 * 64;
    private static final int MISSING_SHOWN = 2;
    private static final Map<UUID, Running> JOBS = new HashMap<>();

    private AssemblyJobs() {
    }

    interface Running {
        ServerLevel level();

        BlockPos center();

        @Nullable
        Component stopReason();

        boolean tick(ServerPlayer player, int budget);

        Component summary();

        Component tooFar();

        Component busy();
    }

    /** An assembly paired with the source it draws blocks from. */
    private record Build(AssemblyJob job, MaterialSource source, boolean instant, boolean tool, BlockPos distancePos,
                         io.ticticboom.mods.mm.tool.ToolBuildMode mode) implements Running {
        public ServerLevel level() {
            return job.level;
        }

        public BlockPos center() {
            return distancePos;
        }

        public @Nullable Component stopReason() {
            return job.controllerPresent() ? null : Component.translatable("message.mm.assemble.controller_gone");
        }

        public boolean tick(ServerPlayer player, int budget) {
            if (tool) budget = mode.budget(MMConfigSetup.COMMON.toolBuildBlocksPerTick.get());
            return tickBuild(player, job, source, budget);
        }

        public Component summary() {
            return AssemblyJobs.summary(job, source);
        }

        public Component tooFar() {
            return Component.translatable("message.mm.assemble.stopped");
        }

        public Component busy() {
            return Component.translatable("message.mm.assemble.busy");
        }
    }

    private record Dismantle(DismantleJob job, ItemSink sink) implements Running {
        public ServerLevel level() {
            return job.level;
        }

        public BlockPos center() {
            return job.controllerPos;
        }

        public @Nullable Component stopReason() {
            return null;
        }

        public boolean tick(ServerPlayer player, int budget) {
            return tickDismantle(player, job, sink, budget);
        }

        public Component summary() {
            return AssemblyJobs.summary(job);
        }

        public Component tooFar() {
            return Component.translatable("message.mm.tool.dismantle.stopped");
        }

        public Component busy() {
            return Component.translatable("message.mm.tool.dismantle.busy");
        }
    }

    public static boolean tickDismantle(Player player, DismantleJob job, ItemSink sink, int budget) {
        if (!sink.ready()) {
            return false;
        }
        sink.beginTick();
        return job.tick(player, sink, budget);
    }

    public static boolean startDismantle(ServerPlayer player, DismantleJob job, ItemSink sink) {
        return startRunning(player, new Dismantle(job, sink));
    }

    private static Component summary(DismantleJob job) {
        MutableComponent text = Component.translatable("message.mm.tool.dismantle.done", job.removed());
        if (job.blocked() > 0) {
            text.append(Component.literal(" · ")).append(Component.translatable("message.mm.assemble.blocked", job.blocked()));
        }
        if (job.sinkGone()) {
            text.append(Component.literal(" · ")).append(Component.translatable("message.mm.tool.not_carried"));
        }
        if (job.outOfEnergy()) {
            text.append(Component.literal(" · ")).append(Component.translatable("message.mm.assemble.out_of_energy", job.removed(), job.total()));
        }
        return text;
    }

    public static boolean tickBuild(Player player, AssemblyJob job, MaterialSource source, int budget) {
        if (!source.ready()) {
            return false;
        }
        source.beginTick();
        return job.tick(player, source, budget);
    }

    public static @Nullable Component busyMessage(ServerPlayer player) {
        Running running = JOBS.get(player.getUUID());
        return running == null ? null : running.busy();
    }

    public static boolean start(ServerPlayer player, BlockPos controllerPos, AssemblyPlanner.Plan plan) {
        return start(player, controllerPos, plan, new PlayerMaterialSource(player), 0);
    }

    public static boolean start(ServerPlayer player, BlockPos controllerPos, AssemblyPlanner.Plan plan, MaterialSource source, int perBlockFe) {
        return start(player, controllerPos, plan, source, perBlockFe, true);
    }

    public static boolean start(ServerPlayer player, BlockPos controllerPos, AssemblyPlanner.Plan plan, MaterialSource source, int perBlockFe,
                                boolean requiresController) {
        return start(player, controllerPos, plan, source, perBlockFe, requiresController, false);
    }

    public static boolean start(ServerPlayer player, BlockPos controllerPos, AssemblyPlanner.Plan plan, MaterialSource source, int perBlockFe,
                                boolean requiresController, boolean instant) {
        if (JOBS.containsKey(player.getUUID())) {
            return false;
        }
        AssemblyJob job = requiresController
                ? AssemblyJob.create(player.level(), controllerPos, plan, perBlockFe)
                : AssemblyJob.createWithoutController(player.level(), controllerPos, plan, perBlockFe);
        JOBS.put(player.getUUID(), new Build(job, source, instant, false, controllerPos,
                io.ticticboom.mods.mm.tool.ToolBuildMode.SEQUENTIAL));
        return true;
    }

    public static boolean startTool(ServerPlayer player, BlockPos controllerPos, AssemblyPlanner.Plan plan, MaterialSource source,
                                    int perBlockFe, boolean requiresController, BlockPos targetPos, boolean instantPreference) {
        if (JOBS.containsKey(player.getUUID())) return false;
        var mode = MMConfigSetup.COMMON.toolBuildMode.get().forTool(instantPreference);
        AssemblyJob job = AssemblyJob.createTool(player.level(), controllerPos, plan, perBlockFe, requiresController, mode);
        JOBS.put(player.getUUID(), new Build(job, source, false, true, targetPos.immutable(), mode));
        return true;
    }

    static boolean startRunning(ServerPlayer player, Running running) {
        if (JOBS.containsKey(player.getUUID())) {
            return false;
        }
        JOBS.put(player.getUUID(), running);
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (JOBS.isEmpty()) {
            return;
        }
        int budget = MMConfig.ASSEMBLY_BLOCKS_PER_TICK;
        Iterator<Map.Entry<UUID, Running>> it = JOBS.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            Running job = entry.getValue();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            double distanceSqr = job instanceof Build build && build.tool()
                    ? Math.pow(MMConfigSetup.COMMON.toolBuildRange.get() + 1.0D, 2) : MAX_DISTANCE_SQR;
            if (player == null || player.level() != job.level()
                    || (job instanceof Build build && build.tool()
                        ? player.getEyePosition().distanceToSqr(job.center().getCenter())
                        : player.distanceToSqr(job.center().getCenter())) > distanceSqr) {
                if (player != null) {
                    player.sendOverlayMessage(job.tooFar());
                }
                it.remove();
                continue;
            }
            Component stop = job.stopReason();
            if (stop != null) {
                player.sendOverlayMessage(stop);
                it.remove();
                continue;
            }
            int jobBudget = job instanceof Build build && build.instant() ? Integer.MAX_VALUE : budget;
            if (job.tick(player, jobBudget)) {
                player.sendOverlayMessage(job.summary());
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        JOBS.clear();
        ToolDismantles.clear();
    }

    public static Component summary(AssemblyJob job, MaterialSource source) {
        MutableComponent text = Component.translatable("message.mm.assemble.done", job.placed);
        if (!job.missing.isEmpty()) {
            MutableComponent list = Component.empty();
            int shown = 0;
            for (Map.Entry<Block, Integer> e : job.missing.entrySet()) {
                if (shown == MISSING_SHOWN) {
                    list.append(Component.literal(" +" + (job.missing.size() - MISSING_SHOWN)));
                    break;
                }
                if (shown > 0) list.append(Component.literal(", "));
                list.append(e.getKey().getName()).append(Component.literal(" ×" + e.getValue()));
                shown++;
            }
            text.append(Component.literal(" · ")).append(Component.translatable("message.mm.assemble.missing", list));
        }
        if (job.blocked > 0) {
            text.append(Component.literal(" · ")).append(Component.translatable("message.mm.assemble.blocked", job.blocked));
        }
        if (job.unavailable > 0) {
            text.append(Component.literal(" · ")).append(Component.translatable("message.mm.assemble.unavailable", job.unavailable));
        }
        if (job.controllerNotPlaced()) {
            text.append(Component.literal(" · ")).append(Component.translatable("message.mm.assemble.controller_not_placed"));
        }
        if (job.sourceGone()) {
            text.append(Component.literal(" · ")).append(Component.translatable("message.mm.tool.not_carried"));
        }
        if (job.outOfEnergy()) {
            text.append(Component.literal(" · ")).append(Component.translatable("message.mm.assemble.out_of_energy", job.placed(), job.total()));
        }
        Component note = source.summaryNote();
        if (note != null) {
            text.append(Component.literal(" · ")).append(note);
        }
        return text;
    }
}
