package io.ticticboom.mods.mm.compat.oritech;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.structure.layout.PositionedLayoutPiece;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = Ref.ID)
public final class BlackHoleShield {

    public static final boolean ENABLED = ModList.get().isLoaded("oritech");
    public static final int CONTAINED_WAIT_TICKS = 100;
    private static final int REACH = 64;

    private record Shielded(StructureModel structure, Rotation rotation, long[] positions) {
    }

    private static final Map<ResourceKey<Level>, Map<BlockPos, Shielded>> BY_CONTROLLER = new HashMap<>();
    private static final Map<ResourceKey<Level>, Long2IntOpenHashMap> COUNTS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Set<MachineControllerBlockEntity>> CONTROLLERS = new HashMap<>();

    private BlackHoleShield() {
    }

    public static void shield(Level level, BlockPos controllerPos, StructureModel structure, Rotation rotation) {
        if (!ENABLED || level == null || level.isClientSide()) {
            return;
        }
        var controllers = BY_CONTROLLER.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        var existing = controllers.get(controllerPos);
        if (existing != null && existing.structure() == structure && existing.rotation() == rotation) {
            return;
        }
        var counts = COUNTS.computeIfAbsent(level.dimension(), k -> new Long2IntOpenHashMap());
        if (existing != null) {
            release(counts, existing);
        }
        var pieces = structure.layout().getRotatedPositionedPieces().get(rotation);
        var positions = new long[pieces.size() + 1];
        positions[0] = controllerPos.asLong();
        for (int i = 0; i < pieces.size(); i++) {
            PositionedLayoutPiece piece = pieces.get(i);
            positions[i + 1] = piece.findAbsolutePos(controllerPos).asLong();
        }
        for (long pos : positions) {
            counts.addTo(pos, 1);
        }
        controllers.put(controllerPos.immutable(), new Shielded(structure, rotation, positions));
    }

    public static void unshield(Level level, BlockPos controllerPos) {
        if (!ENABLED || level == null || level.isClientSide()) {
            return;
        }
        var controllers = BY_CONTROLLER.get(level.dimension());
        if (controllers == null) {
            return;
        }
        var existing = controllers.remove(controllerPos);
        if (existing != null) {
            release(COUNTS.get(level.dimension()), existing);
        }
    }

    public static void watch(Level level, MachineControllerBlockEntity controller) {
        if (ENABLED && level != null && !level.isClientSide()) {
            CONTROLLERS.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(controller);
        }
    }

    public static void unwatch(Level level, MachineControllerBlockEntity controller) {
        if (ENABLED && level != null && !level.isClientSide()) {
            var controllers = CONTROLLERS.get(level.dimension());
            if (controllers != null) {
                controllers.remove(controller);
            }
        }
    }

    public static void validateNearby(Level level, BlockPos blackHole) {
        var controllers = CONTROLLERS.get(level.dimension());
        if (controllers == null || controllers.isEmpty()) {
            return;
        }
        for (var controller : new ArrayList<>(controllers)) {
            var pos = controller.getBlockPos();
            if (Math.abs(pos.getX() - blackHole.getX()) <= REACH
                    && Math.abs(pos.getY() - blackHole.getY()) <= REACH
                    && Math.abs(pos.getZ() - blackHole.getZ()) <= REACH) {
                controller.validateNow();
            }
        }
    }

    public static boolean isShielded(Level level, BlockPos pos) {
        var counts = COUNTS.get(level.dimension());
        return counts != null && counts.get(pos.asLong()) > 0;
    }

    private static void release(Long2IntOpenHashMap counts, Shielded shielded) {
        for (long pos : shielded.positions()) {
            if (counts.addTo(pos, -1) <= 1) {
                counts.remove(pos);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        BY_CONTROLLER.clear();
        CONTROLLERS.clear();
        COUNTS.clear();
    }
}
