package io.ticticboom.mods.mm.client.event;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.config.MMClientConfig;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import io.ticticboom.mods.mm.controller.MMControllerRegistry;
import io.ticticboom.mods.mm.controller.machine.register.ControllerState;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlock;
import io.ticticboom.mods.mm.client.render.PortStatusLightRenderer;
import io.ticticboom.mods.mm.port.MMPortRegistry;
import net.minecraft.world.level.block.entity.BlockEntity;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Tints the controller's screen (tintindex 0 of its model) by machine state. Port lights: PortStatusLightRenderer.
 */
@Mod.EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ControllerColorEvents {
    private static final int NO_TINT = 0xFFFFFF;
    // the screen texture is grayscale, so this is the controller's original green
    private static final int ORIGINAL_SCREEN_COLOR = 0x5CFF89;

    private ControllerColorEvents() {
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (RegistryGroupHolder holder : MMPortRegistry.PORTS) {
            // only ports with a status light
            if (holder.getBlock().get().defaultBlockState().hasProperty(ControllerState.PROPERTY)) {
                event.registerBlockEntityRenderer((BlockEntityType<BlockEntity>) holder.getBe().get(), ctx -> new PortStatusLightRenderer());
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
            // outside the world (JEI, blueprint preview) show the normal look instead of "not built"
            ControllerState machineState = level == null || pos == null
                    ? ControllerState.IDLE
                    : state.getValue(ControllerState.PROPERTY);
            return screenColor(block, machineState);
        }, blocks);
    }

    @SubscribeEvent
    public static void onItemColors(RegisterColorHandlersEvent.Item event) {
        for (RegistryGroupHolder holder : MMControllerRegistry.CONTROLLERS) {
            if (holder.getBlock().get() instanceof MachineControllerBlock block) {
                event.register((stack, tintIndex) -> tintIndex == 0 ? screenColor(block, ControllerState.IDLE) : NO_TINT,
                        holder.getItem().get());
            }
        }
    }

    private static int screenColor(MachineControllerBlock block, ControllerState state) {
        var config = MMConfigSetup.CLIENT;
        if (!config.tintControllerScreen.get()) {
            return ORIGINAL_SCREEN_COLOR;
        }
        Integer override = MMClientConfig.parseColor(block.getModel().screenColor(state.getSerializedName()));
        if (override != null) {
            return override;
        }
        Integer configured = MMClientConfig.parseColor(configValue(config, state).get());
        return configured != null ? configured : ORIGINAL_SCREEN_COLOR;
    }

    private static ForgeConfigSpec.ConfigValue<String> configValue(MMClientConfig config, ControllerState state) {
        return switch (state) {
            case UNFORMED -> config.controllerUnformedColor;
            case IDLE -> config.controllerIdleColor;
            case WORKING -> config.controllerWorkingColor;
        };
    }
}
