package io.ticticboom.mods.mm.command;

import com.mojang.brigadier.context.CommandContext;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = Ref.ID)
public class MMCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("mm")
                .then(Commands.literal("reform")
                    .requires(cs -> cs.hasPermission(2)) // permission level 2: ops/admins
                    .executes(MMCommands::reformAll)
                )
        );
    }

    private static int reformAll(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        MinecraftServer server = level.getServer();

        // Step 1: Collect chunk coordinates synchronously (command executes on server thread)
        List<ChunkPos> chunksToProcess = new ArrayList<>();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        int viewDistance = server.getPlayerList().getViewDistance();
        for (ServerPlayer player : players) {
            int pcx = player.getBlockX() >> 4;
            int pcz = player.getBlockZ() >> 4;
            for (int dx = -viewDistance; dx <= viewDistance; dx++) {
                for (int dz = -viewDistance; dz <= viewDistance; dz++) {
                    int cx = pcx + dx;
                    int cz = pcz + dz;
                    ChunkPos pos = new ChunkPos(cx, cz);
                    if (!chunksToProcess.contains(pos)) {
                        chunksToProcess.add(pos);
                    }
                }
            }
        }

        // Start an async background executor to schedule chunk processing tasks on the main thread.
        ExecutorService bg = Executors.newSingleThreadExecutor(r -> new Thread(r, "MM-Reform-Worker"));
        ConcurrentLinkedQueue<ChunkPos> queue = new ConcurrentLinkedQueue<>(chunksToProcess);

        // Send initial message
        server.execute(() -> source.sendSuccess(() -> Component.literal("Started reform job for " + queue.size() + " chunks."), false));

        bg.submit(() -> {
            AtomicInteger processedChunks = new AtomicInteger(0);
            AtomicInteger reformedCount = new AtomicInteger(0);
            int total = queue.size();
            int reportInterval = Math.max(1, Math.min(50, Math.max(1, total / 10)));

            while (!queue.isEmpty()) {
                ChunkPos chunkPos = queue.poll();
                if (chunkPos == null) break;

                CompletableFuture<Void> fut = new CompletableFuture<>();
                server.execute(() -> {
                    try {
                        ChunkAccess chunk = level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, false);
                        if (chunk instanceof LevelChunk lc) {
                            for (BlockEntity be : lc.getBlockEntities().values()) {
                                if (be instanceof MachineControllerBlockEntity mcbe) {
                                    BlockPos pos = mcbe.getBlockPos();
                                    StructureModel matched = null;
                                    for (StructureModel sm : StructureManager.STRUCTURES.values()) {
                                        if (!sm.controllerIds().contains(Ref.id(mcbe.getModel().id()))) continue;
                                        if (sm.formed(level, pos)) {
                                            matched = sm;
                                            break;
                                        }
                                    }
                                    if (matched != null) {
                                        mcbe.reformTo(matched);
                                        reformedCount.incrementAndGet();
                                    }
                                }
                            }
                        }
                    } catch (Throwable t) {
                        Ref.LOG.error("Error while processing chunk {}:{} - {}", chunkPos.x, chunkPos.z, t.toString());
                    } finally {
                        int processed = processedChunks.incrementAndGet();
                        // Periodically send progress update
                        if (processed % reportInterval == 0) {
                            int reformed = reformedCount.get();
                            source.sendSuccess(() -> Component.literal("Reform progress: processed " + processed + " / " + total + " chunks, reformed " + reformed + " controllers."), false);
                        }
                        fut.complete(null);
                    }
                });

                // Wait for chunk processing to finish before continuing (keeps server-thread-safe ordering)
                try {
                    fut.get();
                } catch (Exception e) {
                    Ref.LOG.error("Interrupted while waiting for chunk processing: {}", e.toString());
                }
            }

            // Final report
            server.execute(() -> {
                source.sendSuccess(() -> Component.literal("Reform job finished. Reformed " + reformedCount.get() + " controllers."), true);
            });
            bg.shutdown();
        });

        // Return immediately with 1 to indicate command accepted; final result will be messaged to sender
        return 1;
    }
}
