package io.ticticboom.mods.mm.client.structure;

import io.ticticboom.mods.mm.client.gui.util.GuiPos;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Where a structure preview goes on screen and how it is turned. 1.21.1 set up a perspective camera on the GUI's own
 * framebuffer; 26.1 draws previews into a picture-in-picture texture through an orthographic camera, so the
 * perspective framing (a fixed field of view) becomes a pixels-per-block scale that fits the same share of the view.
 */
public class GuiRenderEnvSetup {
    private static final float FOV_DEGREES = 85;
    private static final float FIT_MARGIN = 1.1f;
    /** how much of the view's half height the structure's bounding radius took with the old perspective camera */
    private static final float FIT = (float) (Math.cos(Math.toRadians(FOV_DEGREES / 2)) / FIT_MARGIN);

    private GuiPos viewportPos;

    public GuiPos getViewportPos() {
        return viewportPos;
    }

    public void setViewportPos(GuiPos viewportPos) {
        this.viewportPos = viewportPos;
    }

    /** GUI pixels per block for a structure of the given bounding radius. */
    public float scale(float radius) {
        return FIT * (viewportPos.h() / 2f) / Math.max(radius, 0.5f);
    }

    /** View pan, then the drag rotation, then the model's own transform. */
    public Matrix4f transform(float xRot, float yRot, Matrix4f viewMatrix, Matrix4f modelTransform) {
        var m = new Matrix4f(viewMatrix);
        m.rotate(new Quaternionf().rotateXYZ((float) Math.toRadians(xRot), (float) Math.toRadians(yRot), 0));
        m.mul(modelTransform);
        return m;
    }
}
