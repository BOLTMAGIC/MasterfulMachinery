package io.ticticboom.mods.mm.builder.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class BuildableStructure {
    public record Placement(BlockPos pos, BlockState state) {
    }

    private final Identifier id;
    private final Vec3i size;
    private final List<Placement> blocks;
    private final String group;
    @Nullable
    private final Block unbuildableBlock;

    private BuildableStructure(Identifier id, Vec3i size, List<Placement> blocks) {
        this.id = id;
        this.size = size;
        this.blocks = List.copyOf(blocks);
        this.group = groupOf(id, this.blocks);
        this.unbuildableBlock = firstUnbuildable(this.blocks);
    }

    public static BuildableStructure of(Identifier id, Vec3i size, List<Placement> blocks) {
        return new BuildableStructure(id, size, blocks);
    }

    public Identifier id() {
        return id;
    }

    public Vec3i size() {
        return size;
    }

    public List<Placement> blocks() {
        return blocks;
    }

    public int blockCount() {
        return blocks.size();
    }

    public Component displayName() {
        return Component.translatableWithFallback(langKey(id), fallbackName(id));
    }

    public String group() {
        return group;
    }

    public String groupName() {
        return ModList.get().getModContainerById(group)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(group);
    }

    public boolean buildable() {
        return unbuildableBlock == null;
    }

    @Nullable
    public Block unbuildableBlock() {
        return unbuildableBlock;
    }

    public static String langKey(Identifier id) {
        return "structure." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    public static String fallbackName(Identifier id) {
        String file = id.getPath().substring(id.getPath().lastIndexOf('/') + 1);
        StringBuilder name = new StringBuilder();
        for (String word : file.split("[_\\-\\s]+")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return name.isEmpty() ? file : name.toString();
    }

    private static String groupOf(Identifier id, List<Placement> blocks) {
        Map<String, Integer> counts = new HashMap<>();
        for (Placement placement : blocks) {
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(placement.state().getBlock());
            if (blockId != null && !blockId.getNamespace().equals("minecraft")) {
                counts.merge(blockId.getNamespace(), 1, Integer::sum);
            }
        }
        String best = null;
        int bestCount = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount || (entry.getValue() == bestCount && entry.getKey().compareTo(best) < 0)) {
                best = entry.getKey();
                bestCount = entry.getValue();
            }
        }
        return best != null ? best : id.getNamespace();
    }

    @Nullable
    private static Block firstUnbuildable(List<Placement> blocks) {
        for (Placement placement : blocks) {
            Block block = placement.state().getBlock();
            if (block.asItem() == Items.AIR) {
                return block;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BuildableStructure other && id.equals(other.id) && size.equals(other.size)
                && blocks.equals(other.blocks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, size, blocks);
    }

    @Override
    public String toString() {
        return "BuildableStructure[" + id + ", " + blocks.size() + " blocks]";
    }
}
