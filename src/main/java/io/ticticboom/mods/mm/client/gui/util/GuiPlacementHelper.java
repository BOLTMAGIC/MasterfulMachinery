package io.ticticboom.mods.mm.client.gui.util;

import lombok.Getter;
import org.joml.Vector2i;

public class GuiPlacementHelper implements IGuiPlacementHelper {
    @Getter
    private final int containerWidth;
    @Getter
    private final int containerHeight;
    @Getter
    private final int containerPadding;
    @Getter
    private final int guiTop;
    @Getter
    private final int guiBottom;
    @Getter
    private final int guiLeft;
    @Getter
    private final int guiRight;
    @Getter
    private final int guiWidth;
    @Getter
    private final int guiHeight;

    public GuiPlacementHelper(int containerWidth, int containerHeight, int containerPadding) {
        this(GuiPos.of(0,
                        0,
                        containerWidth,
                        containerHeight),
                containerPadding);
    }
    public GuiPlacementHelper(GuiPos pos, int padding) {
        this.containerWidth = pos.w();
        this.containerHeight = pos.h();
        this.containerPadding = padding;
        this.guiTop = pos.y() + padding;
        this.guiLeft = pos.x() + padding;
        this.guiHeight = containerHeight - (containerPadding * 2);
        this.guiWidth = containerWidth - (containerPadding * 2);
        this.guiBottom = guiTop + guiHeight;
        this.guiRight = guiLeft + guiWidth;
    }

    @Override
    public int getGuiCenterX() {
        return this.guiLeft + (this.guiWidth / 2);
    }

    @Override
    public int getGuiCenterY() {
        return this.guiTop + (this.guiHeight / 2);
    }

    @Override
    public GuiBounds getGuiBounds() {
        return new GuiBounds(guiLeft, guiTop, guiRight, guiBottom, guiWidth, guiHeight);
    }

    @Override
    public GuiPos getGuiPos() {
        return GuiPos.of(GuiCoord.of(guiLeft, guiTop), GuiSize.of(guiWidth, guiHeight));
    }

    @Override
    public Vector2i offset(GuiAlignment alignment, Vector2i offset) {
        return switch (alignment) {
            case LEFT_TOP -> new Vector2i(this.getGuiLeft() + offset.x, this.getGuiTop() + offset.y);
            case RIGHT_TOP -> new Vector2i(this.getGuiRight() - offset.x, this.getGuiTop() + offset.y);
            case LEFT_BOTTOM -> new Vector2i(this.getGuiLeft() + offset.x, this.getGuiBottom() - offset.y);
            case RIGHT_BOTTOM -> new Vector2i(this.getGuiRight() - offset.x, this.getGuiBottom() - offset.y);
            case CENTER_TOP -> new Vector2i(this.getGuiCenterX() + offset.x, this.getGuiTop() + offset.y);
            case CENTER_BOTTOM -> new Vector2i(this.getGuiCenterX() + offset.x, this.getGuiBottom() - offset.y);
            case LEFT_CENTER -> new Vector2i(this.getGuiLeft() + offset.x, this.getGuiCenterY() + offset.y);
            case RIGHT_CENTER -> new Vector2i(this.getGuiRight() - offset.x, this.getGuiCenterY() + offset.y);
        };
    }

    /// offset from alignment of gui. Also uses Size of GuiPos to correct overflow when bottom or right aligned.
    @Override
    public GuiPos offset(GuiAlignment alignment, GuiPos pos) {
        var coord = offset(alignment, pos.coord());
        var size = pos.size();

        return switch (alignment) {
            case RIGHT_TOP, RIGHT_CENTER -> createPos(coord.x() - size.w(), coord.y(), size);
            case LEFT_BOTTOM, CENTER_BOTTOM -> createPos(coord.x(), coord.y() - size.h(), size);
            case RIGHT_BOTTOM -> createPos(coord.x() - size.w(), coord.y() - size.h(), size);
            default -> createPos(coord.x(), coord.y(), size);
        };
    }

    private GuiPos createPos(int x, int y, GuiSize size) {
        return GuiPos.of(GuiCoord.of(x, y), size);
    }
}
