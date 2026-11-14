package io.ticticboom.mods.mm.compat.kjs.builder;

import com.google.gson.JsonObject;
import dev.latvian.mods.rhino.util.HideFromJS;
import io.ticticboom.mods.mm.structure.layout.StructureCharacterGrid;
import io.ticticboom.mods.mm.structure.layout.StructureKeyChar;
import io.ticticboom.mods.mm.structure.layout.StructureLayout;
import io.ticticboom.mods.mm.structure.layout.StructureLayoutPiece;
import lombok.Getter;

import java.util.*;

@Getter
public class StructureLayoutBuilderJS {
    private final List<List<String>> grid = new ArrayList<>();
    private final Map<StructureKeyChar, StructureLayoutPiece> pieces = new HashMap<>();
    private final StructureBuilderJS root;
    private boolean portsAnywhere = false;
    private int maxHelpers = -1;

    public StructureLayoutBuilderJS(StructureBuilderJS root) {
        this.root = root;
    }

    public StructureLayoutBuilderJS layer(List<String> layer) {
        grid.add(0, layer);
        return this;
    }

    public StructureLayoutBuilderJS key(String chr, JsonObject key) {
        pieces.put(new StructureKeyChar(chr.charAt(0)), StructureLayoutPiece.parse(key, root.getId(), chr));
        return this;
    }

    public StructureLayoutBuilderJS portsAnywhere(boolean v) {
        this.portsAnywhere = v;
        return this;
    }

    /**
     * How many Input Gateways / Pattern Ports may stand in for blocks of this structure at once. Which blocks they
     * may replace is set per key with {@code allowHelper: true}.
     */
    public StructureLayoutBuilderJS maxHelpers(int max) {
        this.maxHelpers = max;
        return this;
    }

    @HideFromJS
    public StructureLayout build() {
        Collections.reverse(grid);
        return new StructureLayout(new StructureCharacterGrid(grid), pieces, portsAnywhere, maxHelpers);
    }
}
