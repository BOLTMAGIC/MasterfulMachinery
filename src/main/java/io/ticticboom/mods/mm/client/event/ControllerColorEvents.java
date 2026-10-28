package io.ticticboom.mods.mm.client.event;

import org.jetbrains.annotations.Nullable;
import java.util.List;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.block.BlockTintSource;
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
    public static void onBlockColors(RegisterColorHandlersEvent.BlockTintSources event) {
        Block[] blocks = MMControllerRegistry.CONTROLLERS.stream()
                .map(holder -> holder.getBlock().get())
                .toArray(Block[]::new);
        // tint index 0 is the controller's screen
        event.register(List.of(new BlockTintSource() {
            @Override
            public int color(BlockState state) {
                return screenTint(state, ControllerState.IDLE);
            }

            @Override
            public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
                return screenTint(state, state.hasProperty(ControllerState.PROPERTY) ? state.getValue(ControllerState.PROPERTY) : ControllerState.IDLE);
            }
        }), blocks);
    }

    private static int screenTint(BlockState state, ControllerState machineState) {
        return state.getBlock() instanceof MachineControllerBlock block ? 0xFF000000 | screenColor(block, machineState) : -1;
    }

    /** Item tints are data driven since 1.21.4: controller item definitions name mm:controller_screen. */
    @SubscribeEvent
    public static void onItemTints(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(Ref.id("controller_screen"), ScreenTint.CODEC);
    }

    public record ScreenTint() implements ItemTintSource {
        public static final MapCodec<ScreenTint> CODEC = MapCodec.unit(new ScreenTint());

        @Override
        public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
            return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof MachineControllerBlock block
                    ? 0xFF000000 | screenColor(block, ControllerState.IDLE) : -1;
        }

        @Override
        public MapCodec<ScreenTint> type() {
            return CODEC;
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
