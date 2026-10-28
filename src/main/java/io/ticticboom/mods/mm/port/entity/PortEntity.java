package io.ticticboom.mods.mm.port.entity;

import io.ticticboom.mods.mm.util.LegacyNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record PortEntity(
        Identifier type,
        UUID uuid,
        CompoundTag data,
        boolean hadAi,
        boolean wasInvulnerable,
        boolean wasSilent
) {

    public static PortEntity stored(Identifier type, UUID uuid, CompoundTag data) {
        return new PortEntity(type, uuid, data, true, false, false);
    }

    public static PortEntity pinned(Identifier type, UUID uuid, boolean hadAi, boolean wasInvulnerable, boolean wasSilent) {
        return new PortEntity(type, uuid, null, hadAi, wasInvulnerable, wasSilent);
    }

    public boolean isStored() {
        return data != null;
    }

    public Component displayName() {
        return EntityTypes.displayName(type);
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putString("Type", type.toString());
        if (uuid != null) {
            LegacyNbt.putUuid(tag, "Uuid", uuid);
        }
        if (data != null) {
            tag.put("Data", data);
        }
        tag.putBoolean("HadAi", hadAi);
        tag.putBoolean("WasInvulnerable", wasInvulnerable);
        tag.putBoolean("WasSilent", wasSilent);
        return tag;
    }

    public static PortEntity load(CompoundTag tag) {
        var type = Identifier.tryParse(tag.getStringOr("Type", ""));
        if (type == null) {
            return null;
        }
        var uuid = LegacyNbt.getUuid(tag, "Uuid");
        var data = tag.contains("Data") ? tag.getCompoundOrEmpty("Data") : null;
        return new PortEntity(type, uuid, data,
                !tag.contains("HadAi") || tag.getBooleanOr("HadAi", false),
                tag.getBooleanOr("WasInvulnerable", false),
                tag.getBooleanOr("WasSilent", false));
    }
}
