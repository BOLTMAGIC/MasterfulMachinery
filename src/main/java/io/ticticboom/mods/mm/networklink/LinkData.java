package io.ticticboom.mods.mm.networklink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Owner and AE2 network of a linked multiblock, saved with its controller.
 */
public record LinkData(UUID owner, String ownerName, NetworkPos network) {

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Owner", owner);
        tag.putString("OwnerName", ownerName);
        tag.put("Network", network.save());
        return tag;
    }

    @Nullable
    public static LinkData load(CompoundTag tag) {
        NetworkPos network = NetworkPos.load(tag.getCompound("Network"));
        if (!tag.hasUUID("Owner") || network == null) {
            return null;
        }
        return new LinkData(tag.getUUID("Owner"), tag.getString("OwnerName"), network);
    }

    /**
     * Location of an AE2 grid node host. The face is null when the node isn't sided.
     */
    public record NetworkPos(ResourceKey<Level> dimension, BlockPos pos, @Nullable Direction face) {

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Dim", dimension.location().toString());
            tag.putLong("Pos", pos.asLong());
            tag.putInt("Face", face == null ? -1 : face.get3DDataValue());
            return tag;
        }

        @Nullable
        public static NetworkPos load(CompoundTag tag) {
            ResourceLocation dim = ResourceLocation.tryParse(tag.getString("Dim"));
            if (dim == null || !tag.contains("Pos")) {
                return null;
            }
            int face = tag.getInt("Face");
            return new NetworkPos(ResourceKey.create(Registries.DIMENSION, dim), BlockPos.of(tag.getLong("Pos")),
                    face < 0 ? null : Direction.from3DDataValue(face));
        }
    }
}
