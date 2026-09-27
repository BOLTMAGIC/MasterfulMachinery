package io.ticticboom.mods.mm.builder.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads a vanilla structure template ({@code size}, {@code palette} or {@code palettes}, {@code blocks[pos, state]})
 * into a {@link BuildableStructure}. Block entity NBT and entities are ignored; air and structure voids are skipped.
 */
public final class BuildableStructureParser {
    private BuildableStructureParser() {
    }

    /** Either a structure or the reason the file was skipped. */
    public record Result(@Nullable BuildableStructure structure, @Nullable String problem) {
        static Result ok(BuildableStructure structure) {
            return new Result(structure, null);
        }

        static Result skip(String problem) {
            return new Result(null, problem);
        }
    }

    public static Result parse(ResourceLocation id, CompoundTag nbt) {
        ListTag palette;
        if (nbt.contains("palette", Tag.TAG_LIST)) {
            palette = nbt.getList("palette", Tag.TAG_COMPOUND);
        } else if (nbt.contains("palettes", Tag.TAG_LIST) && !nbt.getList("palettes", Tag.TAG_LIST).isEmpty()) {
            palette = nbt.getList("palettes", Tag.TAG_LIST).getList(0);
        } else {
            return Result.skip("no palette");
        }

        // every palette entry must exist, used or not (a missing mod means the structure is not for this pack)
        List<BlockState> states = new ArrayList<>(palette.size());
        for (int i = 0; i < palette.size(); i++) {
            CompoundTag entry = palette.getCompound(i);
            String name = entry.getString("Name");
            ResourceLocation blockId = ResourceLocation.tryParse(name);
            if (blockId == null || !ForgeRegistries.BLOCKS.containsKey(blockId)) {
                return Result.skip("unknown block " + name);
            }
            states.add(readState(ForgeRegistries.BLOCKS.getValue(blockId), entry.getCompound("Properties")));
        }

        Map<BlockPos, BlockState> placed = new LinkedHashMap<>();
        ListTag blocks = nbt.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag block = blocks.getCompound(i);
            ListTag pos = block.getList("pos", Tag.TAG_INT);
            int index = block.getInt("state");
            if (pos.size() != 3 || index < 0 || index >= states.size()) {
                return Result.skip("malformed block entry " + i);
            }
            BlockState state = states.get(index);
            if (state.isAir() || state.is(Blocks.STRUCTURE_VOID)) {
                continue;
            }
            placed.put(new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2)), state);
        }
        if (placed.isEmpty()) {
            return Result.skip("no blocks");
        }

        ListTag sizeTag = nbt.getList("size", Tag.TAG_INT);
        int sx = sizeTag.size() == 3 ? sizeTag.getInt(0) : 0;
        int sy = sizeTag.size() == 3 ? sizeTag.getInt(1) : 0;
        int sz = sizeTag.size() == 3 ? sizeTag.getInt(2) : 0;
        List<BuildableStructure.Placement> placements = new ArrayList<>(placed.size());
        for (Map.Entry<BlockPos, BlockState> entry : placed.entrySet()) {
            BlockPos pos = entry.getKey();
            if (pos.getX() < 0 || pos.getY() < 0 || pos.getZ() < 0) {
                return Result.skip("negative block position " + pos.toShortString());
            }
            // a size smaller than the blocks it holds is grown to fit them
            sx = Math.max(sx, pos.getX() + 1);
            sy = Math.max(sy, pos.getY() + 1);
            sz = Math.max(sz, pos.getZ() + 1);
            placements.add(new BuildableStructure.Placement(pos, entry.getValue()));
        }
        return Result.ok(BuildableStructure.of(id, new Vec3i(sx, sy, sz), placements));
    }

    /** The block's default state with every known property that has a valid value applied; the rest are ignored. */
    static BlockState readState(Block block, CompoundTag properties) {
        BlockState state = block.defaultBlockState();
        for (String key : properties.getAllKeys()) {
            Property<?> property = block.getStateDefinition().getProperty(key);
            if (property != null) {
                state = withValue(state, property, properties.getString(key));
            }
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState withValue(BlockState state, Property<T> property, String value) {
        Optional<T> parsed = property.getValue(value);
        return parsed.isPresent() ? state.setValue(property, parsed.get()) : state;
    }
}
