package io.ticticboom.mods.mm.net.packet;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.ticticboom.mods.mm.recipe.MachineRecipeManager;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.util.ItemComponentUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import io.ticticboom.mods.mm.Ref;

import java.util.HashMap;
import java.util.Map;

public class ProcessesSyncPkt implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ProcessesSyncPkt> TYPE =
            new CustomPacketPayload.Type<>(Ref.id("processes_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProcessesSyncPkt> STREAM_CODEC =
            StreamCodec.of(ProcessesSyncPkt::encode, ProcessesSyncPkt::decode);

    public Map<Identifier, JsonElement> recipes;

    public ProcessesSyncPkt(Map<Identifier, RecipeModel> recipes) {
        this.recipes = new HashMap<>();
        for (Map.Entry<Identifier, RecipeModel> entry : recipes.entrySet()) {
            this.recipes.put(entry.getKey(), entry.getValue().config());
        }
    }

    protected ProcessesSyncPkt() {

    }


    public static void encode(RegistryFriendlyByteBuf buf, ProcessesSyncPkt packet) {
        buf.writeVarInt(packet.recipes.size());
        for (Map.Entry<Identifier, JsonElement> entry : packet.recipes.entrySet()) {
            buf.writeIdentifier(entry.getKey());
            buf.writeUtf(entry.getValue().toString());
        }
    }

    public static ProcessesSyncPkt decode(RegistryFriendlyByteBuf buf) {
        ProcessesSyncPkt packet = new ProcessesSyncPkt();
        packet.recipes = new HashMap<>();
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            packet.recipes.put(buf.readIdentifier(), JsonParser.parseString(buf.readUtf()));
        }
        return packet;
    }

    public static void handle(ProcessesSyncPkt packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ItemComponentUtil.setRegistries(context.player().registryAccess());
            handler(packet);
        });
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handler(ProcessesSyncPkt packet) {
        MachineRecipeManager.recieveRecipes(packet.recipes);
    }
}
