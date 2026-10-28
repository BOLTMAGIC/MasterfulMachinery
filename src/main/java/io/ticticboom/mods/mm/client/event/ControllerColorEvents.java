package io.ticticboom.mods.mm.client.event;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.render.ControllerVisualsRenderer;
import io.ticticboom.mods.mm.client.render.PortStatusLightRenderer;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.controller.MMControllerRegistry;
import io.ticticboom.mods.mm.controller.machine.register.ControllerState;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlock;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.port.MMPortRegistry;
import io.ticticboom.mods.mm.util.ColorUtil;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT)
public final class ControllerColorEvents {
    private static final int NO_TINT = 0xFFFFFF;
    private static final int ORIGINAL_SCREEN_COLOR = 0x5CFF89;

    private ControllerColorEvents() {
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (var holder : MMPortRegistry.PORTS) {
            var type = holder.getBe().get();
            if (type != null) {
                event.registerBlockEntityRenderer((BlockEntityType<BlockEntity>) type, ctx -> new PortStatusLightRenderer());
            }
        }
        for (var holder : MMControllerRegistry.CONTROLLERS) {
            var type = holder.getBe().get();
            if (type != null) {
                event.registerBlockEntityRenderer((BlockEntityType<MachineControllerBlockEntity>) type, ctx -> new ControllerVisualsRenderer());
            }
        }
    }

    @SubscribeEvent
    public static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        Block[] blocks = MMControllerRegistry.CONTROLLERS.stream()
                .map(holder -> holder.getBlock().get())
                .toArray(Block[]::new);
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0 || !(state.getBlock() instanceof MachineControllerBlock block)) {
                return NO_TINT;
            }
            ControllerState machineState = level == null || pos == null
                    ? ControllerState.IDLE
                    : state.getValue(ControllerState.PROPERTY);
            return screenColor(block, machineState);
        }, blocks);
    }

    @SubscribeEvent
    public static void onItemColors(RegisterColorHandlersEvent.Item event) {
        for (var holder : MMControllerRegistry.CONTROLLERS) {
            if (holder.getBlock().get() instanceof MachineControllerBlock block) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? 0xFF000000 | screenColor(block, ControllerState.IDLE) : -1,
                        holder.getItem().get());
            }
        }
    }

    private static int screenColor(MachineControllerBlock block, ControllerState state) {
        if (!MMConfigSetup.CLIENT.tintControllerScreen.get()) {
            return ORIGINAL_SCREEN_COLOR;
        }
        Integer override = ColorUtil.parse(block.getModel().screenColor(state.getSerializedName()));
        return override != null ? override : configColor(state);
    }

    public static int configColor(ControllerState state) {
        var config = MMConfigSetup.CLIENT;
        var value = switch (state) {
            case UNFORMED -> config.controllerUnformedColor;
            case IDLE -> config.controllerIdleColor;
            case WORKING -> config.controllerWorkingColor;
        };
        Integer parsed = ColorUtil.parse(value.get());
        return parsed != null ? parsed : ORIGINAL_SCREEN_COLOR;
    }
}
