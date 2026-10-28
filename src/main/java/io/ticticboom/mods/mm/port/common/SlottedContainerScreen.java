package io.ticticboom.mods.mm.port.common;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import io.ticticboom.mods.mm.client.util.TextColors;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.client.gui.widgets.PortConfigPanel;
import io.ticticboom.mods.mm.port.IPortMenu;
import io.ticticboom.mods.mm.util.BlockUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;

public class SlottedContainerScreen<T extends AbstractContainerMenu & IPortMenu & IPagedPortMenu> extends AbstractContainerScreen<T> {

    private static final int PAGE_BAR_TOP = 112;
    private static final int PAGE_BUTTON_SIZE = 20;

    protected final T menu;
    protected final PortConfigPanel configPanel;
    protected final PortPager pager;
    protected final PortGrid grid;
    protected ArrayList<Vec2> slots = new ArrayList<>();
    private Button previousPage;
    private Button nextPage;

    public SlottedContainerScreen(T menu, Inventory inv, Component displayName) {
        super(menu, inv, displayName);
        this.menu = menu;
        this.imageHeight = 222;
        this.imageWidth = 174;
        configPanel = new PortConfigPanel(menu.getBlockEntity());
        pager = menu.getPager();
        grid = pager.grid();
        setupSlots();
    }

    protected int page() {
        return pager.page();
    }

    protected int firstSlotOnPage() {
        return grid.firstSlot(page());
    }

    private void setPage(int page) {
        pager.setPage(page);
        setupSlots();
        updatePageButtons();
    }

    private void setupSlots() {
        int count = grid.slotsOnPage(page());
        slots.clear();
        slots.ensureCapacity(count);

        // The 18x18 slot background has a 1px border around the 16x16 content area, so it is drawn
        // one pixel up and left of where the menu places the Slot itself.
        for (int i = 0; i < count; i++) {
            slots.add(new Vec2(grid.slotX(i) - 1, grid.slotY(i) - 1));
        }
    }

    @Override
    protected void init() {
        super.init();
        configPanel.setPosition(this.leftPos, this.topPos, this.imageWidth);
        if (grid.pages() <= 1) {
            return;
        }
        int top = this.topPos + PAGE_BAR_TOP;
        previousPage = addRenderableWidget(Button.builder(Component.literal("<"), b -> setPage(page() - 1))
                .bounds(this.leftPos + 8, top, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE).build());
        nextPage = addRenderableWidget(Button.builder(Component.literal(">"), b -> setPage(page() + 1))
                .bounds(this.leftPos + this.imageWidth - 8 - PAGE_BUTTON_SIZE, top, PAGE_BUTTON_SIZE, PAGE_BUTTON_SIZE).build());
        updatePageButtons();
    }

    private void updatePageButtons() {
        if (previousPage == null || nextPage == null) {
            return;
        }
        previousPage.active = page() > 0;
        nextPage.active = page() < grid.pages() - 1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (configPanel.mouseClicked(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        return super.hasClickedOutside(mouseX, mouseY, left, top) && !configPanel.isWithin(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (grid.pages() > 1 && scrollY != 0 && overPortArea(mouseX, mouseY)) {
            setPage(page() + (scrollY < 0 ? 1 : -1));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean overPortArea(double mouseX, double mouseY) {
        double x = mouseX - this.leftPos;
        double y = mouseY - this.topPos;
        return x >= 0 && x < this.imageWidth && y >= 0 && y < BlockUtils.PLAYER_INVENTORY_TOP;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(gfx, mouseX, mouseY, partialTicks);
        gfx.blit(RenderPipelines.GUI_TEXTURED, Ref.UiTextures.PORT_GUI, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        for (Vec2 slot : slots) {
            int x = this.leftPos + (int) slot.x;
            int y = this.topPos + (int) slot.y;
            if (x + 18 <= 0 || y + 18 <= 0 || x >= this.width || y >= this.height) continue;
            gfx.blit(RenderPipelines.GUI_TEXTURED, Ref.UiTextures.SLOT_PARTS, x, y, 0, 26, 18, 18, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
        PortConfigPanel.drawTitle(gfx, this.font, menu.getModel().displayName(), this.imageWidth);
        if (grid.pages() > 1) {
            var label = Component.literal((page() + 1) + " / " + grid.pages());
            gfx.text(this.font, label, (this.imageWidth - this.font.width(label)) / 2, PAGE_BAR_TOP + (PAGE_BUTTON_SIZE - 8) / 2, TextColors.argb(0x404040), false);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(gfx, mouseX, mouseY, partialTicks);
        configPanel.extractRenderState(gfx, this.font, mouseX, mouseY);
        if (configPanel.isWithin(mouseX, mouseY)) {
            configPanel.extractTooltip(gfx, this.font, mouseX, mouseY);
        } else {
            extractTooltip(gfx, mouseX, mouseY);
            renderExtraTooltip(gfx, mouseX, mouseY);
        }
    }

    protected void renderExtraTooltip(GuiGraphicsExtractor gfx, int mouseX, int mouseY) {
    }
}
