package io.ticticboom.mods.mm.net.packet;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.builder.PortTiers;
import io.ticticboom.mods.mm.builder.TierPrefs;
import io.ticticboom.mods.mm.builder.me.CraftTracker;
import io.ticticboom.mods.mm.builder.structure.BuildableStructureRegistry;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.tool.MultiblockToolItem;
import io.ticticboom.mods.mm.tool.MultiblockToolMenu;
import io.ticticboom.mods.mm.tool.ToolData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Predicate;

public record ToolSettingsPkt(Action action, String key, int value) implements CustomPacketPayload {
    public static final Type<ToolSettingsPkt> TYPE = new Type<>(Ref.id("tool_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToolSettingsPkt> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), ToolSettingsPkt::decode);

    public enum Action { SELECT_STRUCTURE, SET_TIER, SET_USE_ME, SET_AUTOCRAFT, FORGET_NETWORK, SET_INSTANT_BUILD }

    public static final int BUILDER_STRUCTURE = 1;

    public static void encode(ToolSettingsPkt pkt, RegistryFriendlyByteBuf buf) {
        buf.writeEnum(pkt.action);
        buf.writeUtf(pkt.key, 256);
        buf.writeVarInt(pkt.value);
    }

    public static ToolSettingsPkt decode(RegistryFriendlyByteBuf buf) {
        return new ToolSettingsPkt(buf.readEnum(Action.class), buf.readUtf(256), buf.readVarInt());
    }

    public static void handle(ToolSettingsPkt pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sender = (ctx.player() instanceof ServerPlayer player ? player : null);
            if (sender == null) {
                return;
            }
            ItemStack tool = sender.containerMenu instanceof MultiblockToolMenu menu
                    ? sender.getItemInHand(menu.getHand()) : ToolRotatePkt.heldTool(sender);
            apply(sender, tool, pkt.action, pkt.key, pkt.value);
        });
    }

    public static boolean apply(ServerPlayer player, @Nullable ItemStack tool, Action action, String key, int value) {
        if (!apply(tool, action, key, value, StructureManager.STRUCTURES::containsKey,
                id -> BuildableStructureRegistry.SERVER.get(id) != null, PortTiers.registeredMaxTiers())) {
            return false;
        }
        if (action == Action.FORGET_NETWORK || (action == Action.SET_AUTOCRAFT && value == 0)) {
            CraftTracker.clear(player);
        }
        return true;
    }

    public static boolean apply(@Nullable ItemStack tool, Action action, String key, int value,
                                Predicate<Identifier> structureExists, Map<String, Integer> maxTierByKey) {
        return apply(tool, action, key, value, structureExists, id -> false, maxTierByKey);
    }

    public static boolean apply(@Nullable ItemStack tool, Action action, String key, int value,
                                Predicate<Identifier> structureExists, Predicate<Identifier> builderStructureExists,
                                Map<String, Integer> maxTierByKey) {
        if (tool == null || !(tool.getItem() instanceof MultiblockToolItem)) {
            return false;
        }
        switch (action) {
            case SELECT_STRUCTURE -> {
                Identifier id = Identifier.tryParse(key);
                if (id == null) {
                    return false;
                }
                if (value == BUILDER_STRUCTURE) {
                    if (!builderStructureExists.test(id)) {
                        return false;
                    }
                    ToolData.setBuilderStructure(tool, id);
                    return true;
                }
                if (!structureExists.test(id)) {
                    return false;
                }
                ToolData.setStructure(tool, id);
                return true;
            }
            case SET_TIER -> {
                int tier = TierPrefs.validate(maxTierByKey, key, value);
                if (tier == TierPrefs.REJECTED) {
                    return false;
                }
                TierPrefs prefs = ToolData.tiers(tool);
                prefs.set(key, tier);
                ToolData.setTiers(tool, prefs);
                return true;
            }
            case SET_USE_ME -> {
                ToolData.setUseMe(tool, value != 0);
                return true;
            }
            case SET_AUTOCRAFT -> {
                ToolData.setAutoCraft(tool, value != 0);
                return true;
            }
            case SET_INSTANT_BUILD -> {
                ToolData.setInstantBuild(tool, value != 0);
                return true;
            }
            case FORGET_NETWORK -> {
                ToolData.setNetwork(tool, null);
                return true;
            }
        }
        return false;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
