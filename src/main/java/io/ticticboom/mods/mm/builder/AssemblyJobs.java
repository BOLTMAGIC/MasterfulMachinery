package io.ticticboom.mods.mm.builder;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Runs assemblies on the server tick; one per player. */
@Mod.EventBusSubscriber(modid = Ref.ID)
public final class AssemblyJobs {
    private static final double MAX_DISTANCE_SQR = 64 * 64;
    private static final int MISSING_SHOWN = 2;
    private static final Map<UUID, Running> JOBS = new HashMap<>();

    private AssemblyJobs() {
    }

    /** A job paired with the source it draws blocks from, ticked together each server tick. */
    private record Running(AssemblyJob job, MaterialSource source) {
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
        JOBS.put(player.getUUID(), new Running(job, source));
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
            AssemblyJob job = entry.getValue().job();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.level() != job.level
                    || player.distanceToSqr(job.controllerPos.getCenter()) > MAX_DISTANCE_SQR) {
                if (player != null) {
                    player.displayClientMessage(Component.translatable("message.mm.assemble.stopped"), true);
                }
                it.remove();
                continue;
            }
            if (!job.controllerPresent()) {
                player.displayClientMessage(Component.translatable("message.mm.assemble.controller_gone"), true);
                it.remove();
                continue;
            }
            if (job.tick(player, entry.getValue().source(), budget)) {
                player.displayClientMessage(summary(job), true);
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        JOBS.clear();
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
        if (job.outOfEnergy()) {
            text.append(Component.literal(" · ")).append(Component.translatable("message.mm.assemble.out_of_energy", job.placed(), job.total()));
        }
        return text;
    }
}
