package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.builder.PortTiers;
import io.ticticboom.mods.mm.builder.TierPrefs;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.tool.MultiblockToolItem;
import io.ticticboom.mods.mm.tool.MultiblockToolMenu;
import io.ticticboom.mods.mm.tool.ToolData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Multiblock tool screen -> server: select a structure ({@code key} = its id), set a preferred port tier
 * ({@code key} = a {@link PortTiers#key}, {@code value} = the tier), toggle the ME network options
 * ({@code value} nonzero = on) or forget the bound network, on the held tool.
 */
public record ToolSettingsPkt(Action action, String key, int value) {
    public enum Action { SELECT_STRUCTURE, SET_TIER, SET_USE_ME, SET_AUTOCRAFT, FORGET_NETWORK }

    public static void encode(ToolSettingsPkt pkt, FriendlyByteBuf buf) {
        buf.writeEnum(pkt.action);
        buf.writeUtf(pkt.key, 256);
        buf.writeVarInt(pkt.value);
    }

    public static ToolSettingsPkt decode(FriendlyByteBuf buf) {
        return new ToolSettingsPkt(buf.readEnum(Action.class), buf.readUtf(256), buf.readVarInt());
    }

    public static void handle(ToolSettingsPkt pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender == null) {
                return;
            }
            // the tool whose screen is open, else whichever tool is held
            ItemStack tool = sender.containerMenu instanceof MultiblockToolMenu menu
                    ? sender.getItemInHand(menu.getHand()) : ToolRotatePkt.heldTool(sender);
            apply(tool, pkt.action, pkt.key, pkt.value, StructureManager.STRUCTURES::containsKey, PortTiers.registeredMaxTiers());
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * Checks a request and writes it to the tool.
     *
     * @param structureExists which structure ids may be selected
     * @param maxTierByKey    the tier keys that may be set, with the highest tier each has
     * @return true when the request was valid and stored
     */
    public static boolean apply(@Nullable ItemStack tool, Action action, String key, int value,
                                Predicate<ResourceLocation> structureExists, Map<String, Integer> maxTierByKey) {
        if (tool == null || !(tool.getItem() instanceof MultiblockToolItem)) {
            return false;
        }
        switch (action) {
            case SELECT_STRUCTURE -> {
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id == null || !structureExists.test(id)) {
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
            case FORGET_NETWORK -> {
                ToolData.setNetwork(tool, null);
                return true;
            }
        }
        return false;
    }
}
