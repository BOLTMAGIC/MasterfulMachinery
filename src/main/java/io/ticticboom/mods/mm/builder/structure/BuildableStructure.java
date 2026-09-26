package io.ticticboom.mods.mm.builder.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * A multiblock from another mod that the Multiblock Tool can build: a fixed list of block states at positions relative
 * to the structure's corner, loaded from a vanilla structure {@code .nbt} file. Name, group and buildability are derived
 * from the id and the blocks, so the client computes the same values from the synced data.
 */
public final class BuildableStructure {
    /** One block to place. */
    public record Placement(BlockPos pos, BlockState state) {
    }

    private final ResourceLocation id;
    private final Vec3i size;
    private final List<Placement> blocks;
    private final String group;
    @Nullable
    private final Block unbuildableBlock;

    private BuildableStructure(ResourceLocation id, Vec3i size, List<Placement> blocks) {
        this.id = id;
        this.size = size;
        this.blocks = List.copyOf(blocks);
        this.group = groupOf(id, this.blocks);
        this.unbuildableBlock = firstUnbuildable(this.blocks);
    }

    public static BuildableStructure of(ResourceLocation id, Vec3i size, List<Placement> blocks) {
        return new BuildableStructure(id, size, blocks);
    }

    public ResourceLocation id() {
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

    /** The lang key {@code structure.<ns>.<path>} if a resource pack defines it, else the title-cased file name. */
    public Component displayName() {
        return Component.translatableWithFallback(langKey(id), fallbackName(id));
    }

    /** The namespace of the mod that owns most of the non-vanilla blocks, or the file's namespace if all are vanilla. */
    public String group() {
        return group;
    }

    /** The group's mod display name, or the namespace itself when no such mod is loaded. */
    public String groupName() {
        return ModList.get().getModContainerById(group)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(group);
    }

    public boolean buildable() {
        return unbuildableBlock == null;
    }

    /** The first block without an item form (it can't be placed by the tool), or null if every block has one. */
    @Nullable
    public Block unbuildableBlock() {
        return unbuildableBlock;
    }

    public static String langKey(ResourceLocation id) {
        return "structure." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    /** "woot/woot_tier_1_copper" -> "Woot Tier 1 Copper". */
    public static String fallbackName(ResourceLocation id) {
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

    private static String groupOf(ResourceLocation id, List<Placement> blocks) {
        Map<String, Integer> counts = new HashMap<>();
        for (Placement placement : blocks) {
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(placement.state().getBlock());
            if (blockId != null && !blockId.getNamespace().equals("minecraft")) {
                counts.merge(blockId.getNamespace(), 1, Integer::sum);
            }
        }
        String best = null;
        int bestCount = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            // ties go to the alphabetically first namespace so the group never depends on map order
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
