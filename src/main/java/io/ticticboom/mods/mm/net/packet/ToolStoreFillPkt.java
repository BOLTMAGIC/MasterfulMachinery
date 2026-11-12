package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.tool.MultiblockToolMenu;
import io.ticticboom.mods.mm.tool.ToolStoreFill;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * JEI's "+" on a structure page with the Structure Builder's store open: fill it from the player's inventory. Only
 * the open menu's own store is filled, and only from the sender's inventory.
 */
public record ToolStoreFillPkt(List<ToolStoreFill.Need> needs) {
    public static void encode(ToolStoreFillPkt packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.needs.size());
        packet.needs.forEach(need -> need.write(buf));
    }

    public static ToolStoreFillPkt decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > ToolStoreFill.MAX_NEEDS) {
            throw new IllegalArgumentException("too many needs: " + size);
        }
        var needs = new ArrayList<ToolStoreFill.Need>(size);
        for (int i = 0; i < size; i++) {
            needs.add(ToolStoreFill.Need.read(buf));
        }
        return new ToolStoreFillPkt(List.copyOf(needs));
    }

    public static void handle(ToolStoreFillPkt packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player == null || !player.isAlive() || player.isSpectator()
                    || !(player.containerMenu instanceof MultiblockToolMenu menu) || !menu.stillValid(player)) return;
            ToolStoreFill.fill(player, menu.store(), packet.needs);
            menu.broadcastChanges();
        });
        ctx.get().setPacketHandled(true);
    }
}
