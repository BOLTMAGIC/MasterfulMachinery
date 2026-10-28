package io.ticticboom.mods.mm.compat.jei.ingredient.matter;

import io.ticticboom.mods.mm.client.FluidSprites;
import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.blaze3d.systems.RenderSystem;
import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
    public void extractRenderState(GuiGraphicsExtractor gfx, @NotNull MatterIngredient ingredient) {
        int color = ingredient.color();
        gfx.fill(0, 0, 16, 16, 0xFF1B1B1B);
        int tint = 0xFF000000 | color;
        var icon = icon(ingredient.matter());
        if (icon.isPresent()) {
            gfx.blit(RenderPipelines.GUI_TEXTURED, icon.get(), 0, 0, 0, 0, 16, 16, 16, 16, tint);
        } else {
            gfx.blitSprite(RenderPipelines.GUI_TEXTURED, FluidSprites.block(MATTER_SPRITE), 1, 1, 14, 14, tint);
        }
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
