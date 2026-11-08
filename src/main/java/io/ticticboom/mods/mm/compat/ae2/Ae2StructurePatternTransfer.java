package io.ticticboom.mods.mm.compat.ae2;

import appeng.api.stacks.GenericStack;
import appeng.integration.modules.jeirei.EncodingHelper;
import appeng.menu.me.items.PatternEncodingTermMenu;
import io.ticticboom.mods.mm.compat.jei.category.MMStructureCategory;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.structure.StructureModel;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * "+" on a structure page with an AE2 Pattern Encoding Terminal open: a processing pattern whose inputs are the
 * structure's blocks (each with its count, every accepted variant offered like AE2 does for a recipe) and whose output
 * is the structure's blueprint, so requesting the blueprint from the network crafts every block at once. Goes through
 * the same encoding call as AE2's own (universal) JEI "+", but names the output itself instead of relying on the page's
 * invisible blueprint slot reaching it. Only registered when AE2 is loaded.
 */
public final class Ae2StructurePatternTransfer implements IRecipeTransferHandler<PatternEncodingTermMenu, StructureModel> {

    public static void register(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new Ae2StructurePatternTransfer(), MMStructureCategory.RECIPE_TYPE);
    }

    @Override
    public Class<? extends PatternEncodingTermMenu> getContainerClass() {
        return PatternEncodingTermMenu.class;
    }

    @Override
    public Optional<MenuType<PatternEncodingTermMenu>> getMenuType() {
        return Optional.of(PatternEncodingTermMenu.TYPE);
    }

    @Override
    public RecipeType<StructureModel> getRecipeType() {
        return MMStructureCategory.RECIPE_TYPE;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(PatternEncodingTermMenu menu, StructureModel structure, IRecipeSlotsView slots,
                                                         Player player, boolean maxTransfer, boolean doTransfer) {
        if (doTransfer) {
            EncodingHelper.encodeProcessingRecipe(menu, inputs(slots), outputs(structure));
        }
        return null;
    }

    /** Each block slot of the page: its accepted blocks with the count the structure takes. */
    static List<List<GenericStack>> inputs(IRecipeSlotsView slots) {
        var inputs = new ArrayList<List<GenericStack>>();
        for (IRecipeSlotView slot : slots.getSlotViews(RecipeIngredientRole.INPUT)) {
            List<GenericStack> options = slot.getIngredients(VanillaTypes.ITEM_STACK)
                    .filter(stack -> !stack.isEmpty())
                    .map(GenericStack::fromItemStack)
                    .filter(Objects::nonNull)
                    .toList();
            if (!options.isEmpty()) {
                inputs.add(options);
            }
        }
        return inputs;
    }

    static List<GenericStack> outputs(StructureModel structure) {
        ItemStack blueprint = MMRegisters.BLUEPRINT.get().getStructureInstance(structure.id());
        GenericStack stack = GenericStack.fromItemStack(blueprint);
        return stack == null ? List.of() : List.of(stack);
    }
}
