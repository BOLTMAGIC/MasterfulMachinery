package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerMenu;
import io.ticticboom.mods.mm.controller.machine.register.ModelDisplay;
import io.ticticboom.mods.mm.model.RecipeSelectionMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ControllerSettingsPkt(BlockPos pos, Setting setting, String value) implements CustomPacketPayload {

    public enum Setting {
        NAME,
        RECIPE_ORDER,
        SOUND_MUTED,
        MODEL_DISPLAY
    }

    public static final Type<ControllerSettingsPkt> TYPE = new Type<>(Ref.id("controller_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ControllerSettingsPkt> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeBlockPos(pkt.pos);
                buf.writeEnum(pkt.setting);
                buf.writeUtf(pkt.value, 256);
            },
            buf -> new ControllerSettingsPkt(buf.readBlockPos(), buf.readEnum(Setting.class), buf.readUtf(256)));

    public static void handle(ControllerSettingsPkt pkt, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) return;
            if (!(sender.containerMenu instanceof MachineControllerMenu menu) || !menu.stillValid(sender)) return;
            if (!(menu.getBe().getBlockEntity() instanceof MachineControllerBlockEntity controller) || !controller.getBlockPos().equals(pkt.pos)) return;
            switch (pkt.setting) {
                case NAME -> controller.setCustomName(pkt.value);
                case RECIPE_ORDER -> controller.setRecipeSelectionMode(RecipeSelectionMode.parse(pkt.value));
                case SOUND_MUTED -> controller.setSoundMuted(Boolean.parseBoolean(pkt.value));
                case MODEL_DISPLAY -> {
                    ModelDisplay display = ModelDisplay.parse(pkt.value);
                    if (display != null) controller.setModelDisplay(display);
                }
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
