package io.ticticboom.mods.mm.recipe;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class RecipeStateModel {
    private boolean canProcess = false;
    private int tickProgress = 0;
    private double tickPercentage = 0;
    private boolean canFinish = false;
    /** Flex recipes: the item each "mm:input/flex" entry picked, by its "as" name (count 1). */
    private final Map<String, ItemStack> captures = new HashMap<>();
    /** "mm:input/count" entries: how many matching items were in the input ports, by their "as" name. */
    private final Map<String, Integer> counts = new HashMap<>();

    public void proceedTick() {
        tickProgress++;
    }

    public ItemStack getCapture(String name) {
        return captures.getOrDefault(name, ItemStack.EMPTY);
    }

    public void setCapture(String name, ItemStack stack) {
        if (stack.isEmpty()) {
            captures.remove(name);
        } else {
            captures.put(name, stack.copyWithCount(1));
        }
    }

    /** @return the count stored under this name, 1 if there is none */
    public int getCount(String name) {
        return counts.getOrDefault(name, 1);
    }

    public void setCount(String name, int count) {
        counts.put(name, count);
    }

    public void reset() {
        canProcess = false;
        tickProgress = 0;
        tickPercentage = 0;
        canFinish = false;
        captures.clear();
        counts.clear();
    }

    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("canProcess", canProcess);
        tag.putInt("tickProgress", tickProgress);
        tag.putDouble("tickPercentage", tickPercentage);
        tag.putBoolean("canFinish", canFinish);
        if (!captures.isEmpty()) {
            var capturesTag = new CompoundTag();
            captures.forEach((name, stack) -> capturesTag.put(name, stack.save(new CompoundTag())));
            tag.put("captures", capturesTag);
        }
        if (!counts.isEmpty()) {
            var countsTag = new CompoundTag();
            counts.forEach(countsTag::putInt);
            tag.put("counts", countsTag);
        }
        return tag;
    }

    public static RecipeStateModel load(CompoundTag tag) {
        RecipeStateModel model = new RecipeStateModel();
        model.setCanProcess(tag.getBoolean("canProcess"));
        model.setTickProgress(tag.getInt("tickProgress"));
        model.setTickPercentage(tag.getDouble("tickPercentage"));
        model.setCanFinish(tag.getBoolean("canFinish"));
        var capturesTag = tag.getCompound("captures");
        for (String name : capturesTag.getAllKeys()) {
            model.setCapture(name, ItemStack.of(capturesTag.getCompound(name)));
        }
        var countsTag = tag.getCompound("counts");
        for (String name : countsTag.getAllKeys()) {
            model.setCount(name, countsTag.getInt(name));
        }
        return model;
    }

}
