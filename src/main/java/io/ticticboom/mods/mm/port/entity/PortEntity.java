package io.ticticboom.mods.mm.port.entity;

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
            tag.putUUID("Uuid", uuid);
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
        var type = Identifier.tryParse(tag.getString("Type"));
        if (type == null) {
            return null;
        }
        var uuid = tag.hasUUID("Uuid") ? tag.getUUID("Uuid") : null;
        var data = tag.contains("Data") ? tag.getCompound("Data") : null;
        return new PortEntity(type, uuid, data,
                !tag.contains("HadAi") || tag.getBoolean("HadAi"),
                tag.getBoolean("WasInvulnerable"),
                tag.getBoolean("WasSilent"));
    }
}
