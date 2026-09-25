package io.ticticboom.mods.mm.client.structure;

import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.structure.layout.PositionedLayoutPiece;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.joml.*;
import org.lwjgl.glfw.GLFW;

import java.lang.Math;

@Getter
@Setter
public class AutoTransform {

    private static Minecraft mc = Minecraft.getInstance();

    private int lastX = 0;
    private int lastY = 0;

    private double xRotation;
    private double yRotation;

    private double scrollLastPos = 0;

    private Vector3i minBound = new Vector3i(Integer.MAX_VALUE);
    private Vector3i maxBound = new Vector3i(Integer.MIN_VALUE);
    private Vector3f pan;
    private Vector3f offset;
    // structure point the view rotates around, in block coordinates
    private Vector3f center = new Vector3f();
    private float scaleFactor;
    private static final float MIN_SCALE = 0.05f;
    private static final float MAX_SCALE = 20f;
    // mouse button of the drag in progress, -1 when none; buttons as last seen, to catch fresh presses
    private int dragButton = -1;
    private boolean wasLeft;
    private boolean wasRight;
    private boolean wasMiddle;

    public AutoTransform(StructureModel model) {
        for (PositionedLayoutPiece piece : model.layout().getPositionedPieces()) {
            BlockPos pos = piece.pos();
            // min
            minBound.x = Math.min(pos.getX(), minBound.x);
            minBound.y = Math.min(pos.getY(), minBound.y);
            minBound.z = Math.min(pos.getZ(), minBound.z);
            // max
            maxBound.x = Math.max(pos.getX(), maxBound.x);
            maxBound.y = Math.max(pos.getY(), maxBound.y);
            maxBound.z = Math.max(pos.getZ(), maxBound.z);
        }

        reset();
    }


    public void run(int mouseX, int mouseY) {
        run(mouseX, mouseY, true);
    }

    /**
     * Mouse controls: left or middle drag rotates, shift + left drag pans, right drag zooms.
     * A drag only starts when the button is pressed while the pointer is over the view ({@code hovered}),
     * so clicks elsewhere on the screen don't move the structure.
     */
    public void run(int mouseX, int mouseY, boolean hovered) {
        if (lastX == 0 && lastY == 0) {
            lastX = mouseX;
            lastY = mouseY;
        }
        long window = mc.getWindow().getWindow();
        boolean left = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean right = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean middle = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;

        if (dragButton == -1 && hovered) {
            if (left && !wasLeft) dragButton = GLFW.GLFW_MOUSE_BUTTON_LEFT;
            else if (right && !wasRight) dragButton = GLFW.GLFW_MOUSE_BUTTON_RIGHT;
            else if (middle && !wasMiddle) dragButton = GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
        }
        if ((dragButton == GLFW.GLFW_MOUSE_BUTTON_LEFT && !left)
                || (dragButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT && !right)
                || (dragButton == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && !middle)) {
            dragButton = -1;
        }
        wasLeft = left;
        wasRight = right;
        wasMiddle = middle;

        double dx = mouseX - lastX;
        double dy = mouseY - lastY;
        boolean shift = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (dragButton == GLFW.GLFW_MOUSE_BUTTON_LEFT && shift) {
            pan.add((float) dx * 0.08f, (float) -dy * 0.08f, 0);
        } else if (dragButton == GLFW.GLFW_MOUSE_BUTTON_LEFT || dragButton == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            xRotation += dx;
            yRotation += dy;
        } else if (dragButton == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            scaleFactor += (float) dy * 0.05f;
            scaleFactor = Math.max(MIN_SCALE, scaleFactor);
        }

        offset = new Vector3f(-0.5f, -0.5f, -0.5f);
        lastX = mouseX;
        lastY = mouseY;
    }

    /** Mouse wheel zoom: each notch scales the view by 10%. */
    public void zoom(double scrollDelta) {
        scaleFactor = (float) Math.min(MAX_SCALE, Math.max(MIN_SCALE, scaleFactor * Math.pow(1.1, scrollDelta)));
    }

    public Matrix4f getModelTransform() {
        var m = new Matrix4f().identity();
        m.translate(offset.x, offset.y, offset.z);
        m.scale(scaleFactor);
        m.translate(-center.x, -center.y, -center.z);
        return m;
    }

    public Matrix4f getViewTransform() {
        var m = new Matrix4f().identity();
        m.translate(pan.x, pan.y, pan.z);
        return m;
    }

    public void reset() {
        xRotation = -35;
        yRotation = 15;
        lastX = 0;
        lastY = 0;
        scaleFactor = 1.003f;
        pan = new Vector3f(0, 0, 0);
    }

}
