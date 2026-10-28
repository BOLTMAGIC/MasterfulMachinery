package io.ticticboom.mods.mm.networklink;

import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.server.level.ServerPlayer;
import io.ticticboom.mods.mm.compat.ae2.Ae2ToolBinding;
import io.ticticboom.mods.mm.compat.ae2.linker.Ae2NetworkLink;
import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

public final class NetworkLink {
    public static final boolean AVAILABLE = ModList.get().isLoaded("ae2");

    @Nullable
    public static DeferredHolder<Item, Item> LINKER;

    private NetworkLink() {
    }

    public static void init(IEventBus modBus) {
        if (AVAILABLE) {
            LINKER = Ae2NetworkLink.init(modBus);
        }
    }

    public static boolean isLinker(ItemStack stack) {
        return LINKER != null && stack.is(LINKER.get());
    }

    public static boolean bindTool(ServerPlayer player, UseOnContext context) {
        return AVAILABLE && Ae2ToolBinding.tryBind(player, context);
    }

    @Nullable
    public static MachineControllerBlockEntity controllerAt(Level level, BlockPos pos) {
        var be = level.getBlockEntity(pos);
        if (be instanceof MachineControllerBlockEntity controller) {
            return controller;
        }
        if (be instanceof AbstractPortBlockEntity port && port.getControllerPos() != null
                && level.isLoaded(port.getControllerPos())
                && level.getBlockEntity(port.getControllerPos()) instanceof MachineControllerBlockEntity controller) {
            return controller;
        }
        return null;
    }

    @Nullable
    public static LinkData linkAt(Level level, BlockPos pos) {
        var controller = controllerAt(level, pos);
        return controller == null ? null : controller.getNetworkLink();
    }

    public static void tickController(ServerLevel level, MachineControllerBlockEntity controller) {
        if (!AVAILABLE || controller.getNetworkLink() == null) {
            return;
        }
        boolean finished = controller.takeLinkExportRequest();
        if (finished || level.getGameTime() % MMConfig.NETWORK_LINK_OUTPUT_INTERVAL == 0) {
            if (Ae2NetworkLink.exportOutputs(level, controller, controller.getNetworkLink())) {
                controller.markLinkExported(level.getGameTime());
            }
        }
    }

    public static void beforePortRemoved(Level level, BlockPos pos) {
        if (!AVAILABLE || !MMConfig.NETWORK_LINK_SEND_ON_REMOVE || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        var controller = controllerAt(level, pos);
        if (controller != null && controller.getNetworkLink() != null && level.getBlockEntity(pos) instanceof AbstractPortBlockEntity port) {
            Ae2NetworkLink.sendPortContents(serverLevel, pos, port, controller.getNetworkLink());
        }
    }
}
