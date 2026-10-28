package io.ticticboom.mods.mm.networklink;

import io.ticticboom.mods.mm.util.LegacyNbt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record LinkData(UUID owner, String ownerName, NetworkPos network) {

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        LegacyNbt.putUuid(tag, "Owner", owner);
        tag.putString("OwnerName", ownerName);
        tag.put("Network", network.save());
        return tag;
    }

    @Nullable
    public static LinkData load(CompoundTag tag) {
        NetworkPos network = NetworkPos.load(tag.getCompoundOrEmpty("Network"));
        UUID owner = LegacyNbt.getUuid(tag, "Owner");
        if (owner == null || network == null) {
            return null;
        }
        return new LinkData(owner, tag.getStringOr("OwnerName", ""), network);
    }

    public record NetworkPos(ResourceKey<Level> dimension, BlockPos pos, @Nullable Direction face) {

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Dim", dimension.identifier().toString());
            tag.putLong("Pos", pos.asLong());
            tag.putInt("Face", face == null ? -1 : face.get3DDataValue());
            return tag;
        }

        @Nullable
        public static NetworkPos load(CompoundTag tag) {
            Identifier dim = Identifier.tryParse(tag.getStringOr("Dim", ""));
            if (dim == null || !tag.contains("Pos")) {
                return null;
            }
            int face = tag.getIntOr("Face", 0);
            return new NetworkPos(ResourceKey.create(Registries.DIMENSION, dim), BlockPos.of(tag.getLongOr("Pos", 0L)),
                    face < 0 ? null : Direction.from3DDataValue(face));
        }
    }
}
