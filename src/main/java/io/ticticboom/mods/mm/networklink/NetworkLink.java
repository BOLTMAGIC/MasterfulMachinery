package io.ticticboom.mods.mm.networklink;

import io.ticticboom.mods.mm.compat.ae2.Ae2NetworkLink;
import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

/**
 * Network linking: a multiblock linked to an owner (and their FTB team) and an AE2 network.
 * Linked machines send their outputs to the network, a removed port's contents go to the network
 * instead of dropping, and only the owner's team may open or break the machine.
 * <p>
 * Everything here is safe to load without AE2; AE2 code lives in {@code compat.ae2} and is only
 * touched when {@link #AVAILABLE} is true.
 */
public final class NetworkLink {
    public static final boolean AVAILABLE = ModList.get().isLoaded("ae2");

    /** The linker item, or null when AE2 isn't installed. */
    @Nullable
    public static RegistryObject<Item> LINKER;

    private NetworkLink() {
    }

    /** Called during mod construction, before the registers are attached to the bus. */
    public static void init() {
        if (AVAILABLE) {
            LINKER = Ae2NetworkLink.init();
        }
    }

    public static boolean isLinker(net.minecraft.world.item.ItemStack stack) {
        return LINKER != null && stack.is(LINKER.get());
    }

    /** Controller tick: periodically sends the outputs of a linked machine to its network. */
    public static void tickController(Level level, MachineControllerBlockEntity controller) {
        if (!AVAILABLE || !(level instanceof ServerLevel serverLevel) || controller.getNetworkLink() == null) {
            return;
        }
        if (level.getGameTime() % MMConfig.NETWORK_LINK_OUTPUT_INTERVAL == 0) {
            Ae2NetworkLink.exportOutputs(serverLevel, controller, controller.getNetworkLink());
        }
    }

    /**
     * Port block removal (player, explosion, commands...): send the port's contents to the linked
     * network before the block drops them.
     */
    public static void beforePortRemoved(Level level, BlockPos pos) {
        if (!AVAILABLE || !MMConfig.NETWORK_LINK_SEND_ON_REMOVE || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IPortBlockEntity port) {
            Ae2NetworkLink.sendPortContents(serverLevel, pos, port);
        }
    }
}
