package io.ticticboom.mods.mm.client.blueprint.widgets;

import io.ticticboom.mods.mm.client.blueprint.event.YSliceChangeEvent;
import io.ticticboom.mods.mm.client.gui.GuiEventHandler;
import io.ticticboom.mods.mm.client.gui.event.ArrowOptionSelectChangeEvent;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import io.ticticboom.mods.mm.client.gui.widgets.ArrowOptionSelectWidget;
import io.ticticboom.mods.mm.client.structure.GuiStructureRenderer;
import io.ticticboom.mods.mm.structure.StructureModel;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class BlueprintViewYSliceWidget extends ArrowOptionSelectWidget {

    public static final String offStateOption = "Y Slice: All";
    private static final int offStateIndex = 0;
    public final GuiEventHandler<YSliceChangeEvent> changeEmitter = new GuiEventHandler<>();
    private final StructureModel model;

    public BlueprintViewYSliceWidget(GuiPos pos, StructureModel model) {
        super(pos, getOptions(model));
        this.model = model;
        super.changeEmitter.addListener(this::onChange);
    }

    private void onChange(ArrowOptionSelectChangeEvent event) {
        if (event.index() == offStateIndex) {
            changeEmitter.fireEvent(new YSliceChangeEvent(0, false));
            return;
        }
        GuiStructureRenderer guiRenderer = model.getGuiRenderer();
        var min = guiRenderer.getMinBound();
        int ySlice = min.y() + event.index() - 1;
        changeEmitter.fireEvent(new YSliceChangeEvent(ySlice, true));
    }

    private static List<String> getOptions(StructureModel model) {
        GuiStructureRenderer guiRenderer = model.getGuiRenderer();
        guiRenderer.init();
        Vector3i structureSize = guiRenderer.getStructureSize();
        int sliceCountMax = structureSize.y;
        var result = new ArrayList<String>();
        if (sliceCountMax > 0) {
            var layers = IntStream.rangeClosed(0, sliceCountMax).boxed().map(x -> "Y Slice: " + x).toList();
            result.addAll(layers);
        }
        result.add(offStateIndex, offStateOption);
        return result;
    }
}
