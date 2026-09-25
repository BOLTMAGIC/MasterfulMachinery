package io.ticticboom.mods.mm.networklink;

import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class MultiblockLookup {

    private MultiblockLookup() {
    }

    /**
     * Finds the linked, formed multiblock a port storage belongs to.
     * Ports are matched by storage identity, so two adjacent machines can't be confused.
     * Only already loaded chunks are searched.
     */
    @Nullable
    public static MachineControllerBlockEntity findLinkedController(ServerLevel level, BlockPos portPos, IPortStorage portStorage) {
        int radius = MMConfig.NETWORK_LINK_SEARCH_RADIUS;
        int minCx = (portPos.getX() - radius) >> 4, maxCx = (portPos.getX() + radius) >> 4;
        int minCz = (portPos.getZ() - radius) >> 4, maxCz = (portPos.getZ() + radius) >> 4;

        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (var be : chunk.getBlockEntities().values()) {
                    if (be instanceof MachineControllerBlockEntity controller
                            && controller.getNetworkLink() != null
                            && be.getBlockPos().closerThan(portPos, radius + 1)) {
                        RecipeStorages storages = storagesOf(level, controller);
                        if (storages != null && (containsSame(storages.inputStorages(), portStorage)
                                || containsSame(storages.outputStorages(), portStorage))) {
                            return controller;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    public static RecipeStorages storagesOf(ServerLevel level, MachineControllerBlockEntity controller) {
        var structure = controller.getStructure();
        if (structure == null) {
            return null;
        }
        return structure.getStorages(level, controller.getBlockPos());
    }

    private static boolean containsSame(List<IPortStorage> storages, IPortStorage target) {
        for (IPortStorage storage : storages) {
            if (storage == target) {
                return true;
            }
        }
        return false;
    }
}
