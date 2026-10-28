package io.ticticboom.mods.mm.builder.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BuildableStructureParser {
    private BuildableStructureParser() {
    }

    public record Result(@Nullable BuildableStructure structure, @Nullable String problem) {
        static Result ok(BuildableStructure structure) {
            return new Result(structure, null);
        }

        static Result skip(String problem) {
            return new Result(null, problem);
        }
    }

    public static Result parse(Identifier id, CompoundTag nbt) {
        ListTag palette;
        if (nbt.contains("palette")) {
            palette = nbt.getListOrEmpty("palette");
        } else if (nbt.contains("palettes") && !nbt.getListOrEmpty("palettes").isEmpty()) {
            palette = nbt.getListOrEmpty("palettes").getListOrEmpty(0);
        } else {
            return Result.skip("no palette");
        }

        List<BlockState> states = new ArrayList<>(palette.size());
        for (int i = 0; i < palette.size(); i++) {
            CompoundTag entry = palette.getCompoundOrEmpty(i);
            String name = entry.getStringOr("Name", "");
            Identifier blockId = Identifier.tryParse(name);
            if (blockId == null || !BuiltInRegistries.BLOCK.containsKey(blockId)) {
                return Result.skip("unknown block " + name);
            }
            states.add(readState(BuiltInRegistries.BLOCK.getValue(blockId), entry.getCompoundOrEmpty("Properties")));
        }

        Map<BlockPos, BlockState> placed = new LinkedHashMap<>();
        ListTag blocks = nbt.getListOrEmpty("blocks");
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag block = blocks.getCompoundOrEmpty(i);
            ListTag pos = block.getListOrEmpty("pos");
            int index = block.getIntOr("state", 0);
            if (pos.size() != 3) {
                return Result.skip("malformed block entry " + i);
            }
            if (index < 0 || index >= states.size()) {
                continue;
            }
            BlockState state = states.get(index);
            if (state.isAir() || state.is(Blocks.STRUCTURE_VOID)) {
                continue;
            }
            placed.put(new BlockPos(pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0)), state);
        }
        if (placed.isEmpty()) {
            return Result.skip("no blocks");
        }

        ListTag sizeTag = nbt.getListOrEmpty("size");
        int sx = sizeTag.size() == 3 ? sizeTag.getIntOr(0, 0) : 0;
        int sy = sizeTag.size() == 3 ? sizeTag.getIntOr(1, 0) : 0;
        int sz = sizeTag.size() == 3 ? sizeTag.getIntOr(2, 0) : 0;
        List<BuildableStructure.Placement> placements = new ArrayList<>(placed.size());
        for (Map.Entry<BlockPos, BlockState> entry : placed.entrySet()) {
            BlockPos pos = entry.getKey();
            if (pos.getX() < 0 || pos.getY() < 0 || pos.getZ() < 0) {
                return Result.skip("negative block position " + pos.toShortString());
            }
            sx = Math.max(sx, pos.getX() + 1);
            sy = Math.max(sy, pos.getY() + 1);
            sz = Math.max(sz, pos.getZ() + 1);
            placements.add(new BuildableStructure.Placement(pos, entry.getValue()));
        }
        return Result.ok(BuildableStructure.of(id, new Vec3i(sx, sy, sz), placements));
    }

    static BlockState readState(Block block, CompoundTag properties) {
        BlockState state = block.defaultBlockState();
        for (String key : properties.keySet()) {
            Property<?> property = block.getStateDefinition().getProperty(key);
            if (property != null) {
                state = withValue(state, property, properties.getStringOr(key, ""));
            }
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState withValue(BlockState state, Property<T> property, String value) {
        Optional<T> parsed = property.getValue(value);
        return parsed.isPresent() ? state.setValue(property, parsed.get()) : state;
    }
}
