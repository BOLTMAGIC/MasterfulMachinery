package io.ticticboom.mods.mm.builder;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.tool.ToolDismantles;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Runs assemblies and dismantles on the server tick; one job of either kind per player. */
@Mod.EventBusSubscriber(modid = Ref.ID)
public final class AssemblyJobs {
    private static final double MAX_DISTANCE_SQR = 64 * 64;
    private static final int MISSING_SHOWN = 2;
    private static final Map<UUID, Running> JOBS = new HashMap<>();

    private AssemblyJobs() {
    }

    /** A job of either kind, ticked each server tick. */
    private interface Running {
        ServerLevel level();

        BlockPos center();

        /** A message to stop with before ticking, or null to go on. */
        @Nullable
        Component stopReason();

        /** @return true when done */
        boolean tick(ServerPlayer player, int budget);

        Component summary();
    }

    /** An assembly paired with the source it draws blocks from. */
    private record Build(AssemblyJob job, MaterialSource source) implements Running {
        public ServerLevel level() {
            return job.level;
        }

        public BlockPos center() {
            return job.controllerPos;
        }

        public @Nullable Component stopReason() {
            return job.controllerPresent() ? null : Component.translatable("message.mm.assemble.controller_gone");
        }

        public boolean tick(ServerPlayer player, int budget) {
            return tickBuild(player, job, source, budget);
        }

        public Component summary() {
            return AssemblyJobs.summary(job);
        }
    }

    /** A dismantle paired with the sink its blocks go into. */
    private record Dismantle(DismantleJob job, ItemSink sink) implements Running {
        public ServerLevel level() {
            return job.level;
        }

        public BlockPos center() {
            return job.controllerPos;
        }

        public @Nullable Component stopReason() {
            return null; // the job removes the controller itself
        }

        public boolean tick(ServerPlayer player, int budget) {
            return tickDismantle(player, job, sink, budget);
        }

        public Component summary() {
            return AssemblyJobs.summary(job);
        }
    }

    /**
     * One server tick of a build: waits while the source is not {@link MaterialSource#ready ready} (e.g. the tool's
     * store is open), else lets it re-read its state and places up to budget blocks.
     *
     * @return true when done
     */
    public static boolean tickBuild(Player player, AssemblyJob job, MaterialSource source, int budget) {
        if (!source.ready()) {
            return false;
        }
        source.beginTick();
        return job.tick(player, source, budget);
    }

    /** One server tick of a dismantle, paused and refreshed like {@link #tickBuild}. */
    public static boolean tickDismantle(Player player, DismantleJob job, ItemSink sink, int budget) {
        if (!sink.ready()) {
            return false;
        }
        sink.beginTick();
        return job.tick(player, sink, budget);
    }

    /** True while the player has a job of either kind running. */
    public static boolean busy(ServerPlayer player) {
        return JOBS.containsKey(player.getUUID());
    }

    /** The controller's own Assemble: draws from the player's inventory, 0 FE per block. */
    public static boolean start(ServerPlayer player, BlockPos controllerPos, AssemblyPlanner.Plan plan) {
        return start(player, controllerPos, plan, new PlayerMaterialSource(player), 0);
    }

    public static boolean start(ServerPlayer player, BlockPos controllerPos, AssemblyPlanner.Plan plan, MaterialSource source, int perBlockFe) {
        if (JOBS.containsKey(player.getUUID())) {
            return false;
        }
        AssemblyJob job = AssemblyJob.create(player.serverLevel(), controllerPos, plan, perBlockFe);
        JOBS.put(player.getUUID(), new Build(job, source));
        return true;
    }

    /** Starts a dismantle; false when the player already runs a job. */
    public static boolean startDismantle(ServerPlayer player, DismantleJob job, ItemSink sink) {
        if (JOBS.containsKey(player.getUUID())) {
            return false;
        }
        JOBS.put(player.getUUID(), new Dismantle(job, sink));
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || JOBS.isEmpty()) {
            return;
        }
        int budget = MMConfigSetup.COMMON.assemblyBlocksPerTick.get();
        Iterator<Map.Entry<UUID, Running>> it = JOBS.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            Running job = entry.getValue();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.level() != job.level()
                    || player.distanceToSqr(job.center().getCenter()) > MAX_DISTANCE_SQR) {
                if (player != null) {
                    player.displayClientMessage(Component.translatable("message.mm.assemble.stopped"), true);
                }
                it.remove();
                continue;
            }
            Component stop = job.stopReason();
            if (stop != null) {
                player.displayClientMessage(stop, true);
                it.remove();
                continue;
            }
            if (job.tick(player, budget)) {
                player.displayClientMessage(job.summary(), true);
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        JOBS.clear();
        ToolDismantles.clear();
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

    private static Component summary(AssemblyJob job) {
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
        return text;
    }
}
