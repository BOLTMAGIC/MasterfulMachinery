package io.ticticboom.mods.mm.client.blueprint.widgets;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.gui.AbstractWidget;
import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.util.GLScissor;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Supplier;

public class StructureRenderWidget extends AbstractWidget {
    private final Supplier<StructureModel> structureSupplier;

    public StructureRenderWidget(GuiPos pos, Supplier<StructureModel> structureSupplier) {
        super(pos);
        this.structureSupplier = structureSupplier;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.fill(position.x(), position.y(), position.x() + position.w(), position.y() + position.h(), 0x77777777);
        StructureModel model = structureSupplier.get();
        if (model == null) {
            Ref.LOG.fatal("Structure model is null in client renderer");
            return;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(position.x(), position.y(), 100);
        GLScissor.enable(position.x(), position.y(), position.w(), position.h());
        var guiRenderer = model.getGuiRenderer();
        guiRenderer.init();
        guiRenderer.render(guiGraphics, mouseX, mouseY);
        GLScissor.disable();
    }
}
