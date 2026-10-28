package io.ticticboom.mods.mm.compat.jei.ingredient.matter;

import com.mojang.blaze3d.systems.RenderSystem;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MatterIngredientRenderer implements IIngredientRenderer<MatterIngredient> {
    private static final Identifier MATTER_SPRITE = Identifier.fromNamespaceAndPath("replication", "block/matter");
    private static final Map<Identifier, Optional<Identifier>> ICONS = new ConcurrentHashMap<>();

    @Override
    public void render(GuiGraphics gfx, @NotNull MatterIngredient ingredient) {
        int color = ingredient.color();
        gfx.fill(0, 0, 16, 16, 0xFF1B1B1B);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f, 1f);
        var icon = icon(ingredient.matter());
        if (icon.isPresent()) {
            gfx.blit(icon.get(), 0, 0, 0, 0, 16, 16, 16, 16);
        } else {
            var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(MATTER_SPRITE);
            gfx.blit(1, 1, 0, 14, 14, sprite);
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private static Optional<Identifier> icon(Identifier matter) {
        return ICONS.computeIfAbsent(matter, id -> {
            var texture = Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/gui/mattertypes/" + id.getPath() + ".png");
            return Minecraft.getInstance().getResourceManager().getResource(texture).map(resource -> texture);
        });
    }

    @SuppressWarnings("removal")
    @Override
    public @NotNull List<Component> getTooltip(MatterIngredient ingredient, @NotNull TooltipFlag tooltipFlag) {
        var result = new ArrayList<Component>();
        result.add(Component.translatable("jei.mm.ingredient.replication_matter.title", MatterIngredientNames.displayName(ingredient.matter())));
        result.add(Component.translatable("jei.mm.ingredient.replication_matter.amount", ingredient.amount()));
        return result;
    }
}
