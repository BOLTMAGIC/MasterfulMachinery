package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class SelectRecipePkt implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SelectRecipePkt> TYPE =
            new CustomPacketPayload.Type<>(Ref.id("select_recipe"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectRecipePkt> STREAM_CODEC =
            StreamCodec.of(SelectRecipePkt::encode, SelectRecipePkt::decode);

    private static final double MAX_DISTANCE_SQR = 64 * 64;

    private final BlockPos pos;
    private final String recipeId;

    public SelectRecipePkt(BlockPos pos, String recipeId) {
        this.pos = pos;
        this.recipeId = recipeId;
    }

    public static void encode(RegistryFriendlyByteBuf buf, SelectRecipePkt pkt) {
        buf.writeBlockPos(pkt.pos);
        buf.writeUtf(pkt.recipeId);
    }

    public static SelectRecipePkt decode(RegistryFriendlyByteBuf buf) {
        return new SelectRecipePkt(buf.readBlockPos(), buf.readUtf());
    }

    public static void handle(SelectRecipePkt pkt, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) {
                return;
            }
            var level = sender.level();
            if (!level.isLoaded(pkt.pos)) {
                return;
            }
            if (sender.distanceToSqr(pkt.pos.getX() + 0.5, pkt.pos.getY() + 0.5, pkt.pos.getZ() + 0.5) > MAX_DISTANCE_SQR) {
                return;
            }
            if (level.getBlockEntity(pkt.pos) instanceof MachineControllerBlockEntity controller) {
                controller.selectRecipe(pkt.recipeId.isEmpty() ? null : Identifier.tryParse(pkt.recipeId));
            }
        });
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
