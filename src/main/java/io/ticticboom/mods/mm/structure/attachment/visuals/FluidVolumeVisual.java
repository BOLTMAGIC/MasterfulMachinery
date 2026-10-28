package io.ticticboom.mods.mm.structure.attachment.visuals;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.structure.layout.PositionedLayoutPiece;
import io.ticticboom.mods.mm.structure.layout.StructureLayout;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

@Getter
public class FluidVolumeVisual {
    private final @Nullable BlockPos from;
    private final @Nullable BlockPos to;
    private final @Nullable String area;
    private final @Nullable String port;
    @Getter(lombok.AccessLevel.NONE)
    private final Map<Rotation, long[]> cellCache = new EnumMap<>(Rotation.class);

    public FluidVolumeVisual(@Nullable BlockPos from, @Nullable BlockPos to, @Nullable String area, @Nullable String port) {
        this.from = from;
        this.to = to;
        this.area = area;
        this.port = port;
    }

    public static FluidVolumeVisual parse(JsonObject json) {
        String port = json.has("port") ? json.get("port").getAsString() : null;
        if (json.has("area")) {
            return new FluidVolumeVisual(null, null, json.get("area").getAsString(), port);
        }
        return new FluidVolumeVisual(parsePos(json.getAsJsonArray("from")), parsePos(json.getAsJsonArray("to")), null, port);
    }

    private static BlockPos parsePos(JsonArray array) {
        return new BlockPos(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt());
    }

    public BlockPos min() {
        return new BlockPos(Math.min(from.getX(), to.getX()), Math.min(from.getY(), to.getY()), Math.min(from.getZ(), to.getZ()));
    }

    public BlockPos max() {
        return new BlockPos(Math.max(from.getX(), to.getX()), Math.max(from.getY(), to.getY()), Math.max(from.getZ(), to.getZ()));
    }

    public long[] relativeCells(StructureLayout layout, Rotation rotation) {
        return cellCache.computeIfAbsent(rotation, r -> area != null ? areaCells(layout, r) : boxCells(layout, r));
    }

    private long[] areaCells(StructureLayout layout, Rotation rotation) {
        return layout.getRotatedPositionedPieces().get(rotation).stream()
                .filter(piece -> area.equals(piece.piece().valueId()))
                .map(PositionedLayoutPiece::pos)
                .mapToLong(BlockPos::asLong)
                .toArray();
    }

    private long[] boxCells(StructureLayout layout, Rotation rotation) {
        var origin = layout.getCharGrid().getControllerPos();
        var min = min();
        var max = max();
        var cells = new long[(max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1)];
        int i = 0;
        for (var pos : BlockPos.betweenClosed(min, max)) {
            cells[i++] = pos.subtract(origin).rotate(rotation).asLong();
        }
        return cells;
    }

    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("type", "fluid_volume");
        if (area != null) {
            json.addProperty("area", area);
        } else {
            json.add("from", posJson(from));
            json.add("to", posJson(to));
        }
        if (port != null) {
            json.addProperty("port", port);
        }
        return json;
    }

    private static JsonArray posJson(BlockPos pos) {
        var array = new JsonArray();
        array.add(pos.getX());
        array.add(pos.getY());
        array.add(pos.getZ());
        return array;
    }
}
