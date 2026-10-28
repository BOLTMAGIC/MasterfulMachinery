package io.ticticboom.mods.mm.compat.kjs.event;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.world.item.ItemStack;

/**
 * MMEvents.flexOutput: picks what a flex recipe's "mm:output/flex" entry makes from an input item, e.g.
 * <pre>{@code
 * MMEvents.flexOutput('mm:essence_forge', event => {
 *     if (event.input.id == 'mysticalagriculture:inferium_seeds') event.setOutput('mysticalagriculture:inferium_essence')
 *     else if (event.input.id == 'mysticalagriculture:cursed_seeds') event.reject()
 * })
 * }</pre>
 * {@code event.output} is what the recipe's id template made (empty if nothing). The result is remembered per
 * item until the next reload, so the event runs once per kind of item, not every tick. Server side only: JEI shows
 * the template results.
 */
public class FlexOutputEventJS implements KubeEvent {
    private final String recipeId;
    private final String from;
    private final ItemStack input;
    private final ItemStack templateOutput;
    private ItemStack result;
    private boolean changed;

    public FlexOutputEventJS(String recipeId, String from, ItemStack input, ItemStack templateOutput) {
        this.recipeId = recipeId;
        this.from = from;
        this.input = input;
        this.templateOutput = templateOutput;
        this.result = templateOutput;
    }

    public String getRecipeId() {
        return recipeId;
    }

    /** The "as" name of the flex input the item came from. */
    public String getFrom() {
        return from;
    }

    /** The consumed item (count 1). */
    public ItemStack getInput() {
        return input.copy();
    }

    /** What the id template made, empty if it made nothing. */
    public ItemStack getOutput() {
        return templateOutput.copy();
    }

    /** Use this item (its count is ignored; the recipe's "count" is used). */
    public void setOutput(ItemStack output) {
        result = output == null ? ItemStack.EMPTY : output.copy();
        changed = true;
    }

    /** Don't accept this input: it stays in the port. */
    public void reject() {
        result = ItemStack.EMPTY;
        changed = true;
    }

    public boolean isChanged() {
        return changed;
    }

    public ItemStack getResult() {
        return result;
    }
}
