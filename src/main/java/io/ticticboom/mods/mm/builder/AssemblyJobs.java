package io.ticticboom.mods.mm.builder;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Runs assemblies on the server tick; one per player. */
@Mod.EventBusSubscriber(modid = Ref.ID)
public final class AssemblyJobs {
    private static final double MAX_DISTANCE_SQR = 64 * 64;
    private static final int MISSING_SHOWN = 2;
    private static final Map<UUID, AssemblyJob> JOBS = new HashMap<>();

    private AssemblyJobs() {
    }

    public static boolean start(ServerPlayer player, BlockPos controllerPos, List<AssemblyPlanner.Planned> plan) {
        if (JOBS.containsKey(player.getUUID())) {
            return false;
        }
        JOBS.put(player.getUUID(), new AssemblyJob(player.serverLevel(), controllerPos, plan));
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || JOBS.isEmpty()) {
            return;
        }
        int budget = MMConfigSetup.COMMON.assemblyBlocksPerTick.get();
        Iterator<Map.Entry<UUID, AssemblyJob>> it = JOBS.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            AssemblyJob job = entry.getValue();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.level() != job.level
                    || player.distanceToSqr(job.controllerPos.getCenter()) > MAX_DISTANCE_SQR) {
                if (player != null) {
                    player.displayClientMessage(Component.translatable("message.mm.assemble.stopped"), true);
                }
                it.remove();
                continue;
            }
            if (job.tick(player, budget)) {
                player.displayClientMessage(summary(job), true);
                it.remove();
            }
        }
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
        return text;
    }
}
