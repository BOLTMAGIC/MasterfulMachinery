package io.ticticboom.mods.mm.client.blueprint.widgets;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.gui.AbstractWidget;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import io.ticticboom.mods.mm.structure.StructureModel;
import net.minecraft.client.gui.GuiGraphics;

public class StructureRenderWidget extends AbstractWidget {
    private final StructureModel model;

    public StructureRenderWidget(GuiPos pos, StructureModel model) {
        super(pos);
        this.model = model;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.fill(position.x(), position.y(), position.x() + position.w(), position.y() + position.h(), 0x77777777);
        if (model == null) {
            Ref.LOG.fatal("Structure model is null in client renderer");
            return;
        }

        renderStructure(model, guiGraphics, mouseX, mouseY);
    }

    private void renderStructure(StructureModel model, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().setIdentity();

        var guiRenderer = model.getGuiRenderer();
        guiRenderer.setViewport(position);
        guiRenderer.init();
        guiRenderer.render(guiGraphics, mouseX, mouseY);

        guiGraphics.pose().popPose();
    }
}
