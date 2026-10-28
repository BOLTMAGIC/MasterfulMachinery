package io.ticticboom.mods.mm.recipe;

import io.ticticboom.mods.mm.util.LegacyNbt;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Getter
@Setter
@NoArgsConstructor
public class RecipeStateModel {
    private static final Random ROLL_RANDOM = new Random();
    private static final String ROLL_TOKENS_KEY = "rollTokens";

    private boolean canProcess = false;
    private int tickProgress = 0;
    private double tickPercentage = 0;
    private boolean canFinish = false;
    private double progressCarry = 0;

    private final Map<String, Double> rollTokens = new HashMap<>();
    /** Flex recipes: the kinds of item each "mm:input/flex" entry took this run, by its "as" name (count 1 each). */
    private final Map<String, List<ItemStack>> captures = new HashMap<>();
    /** "mm:input/count" and "mm:input/flex" entries: how many items / kinds they found, by their "as" name. */
    private final Map<String, Integer> counts = new HashMap<>();
    /** Flex recipes: what this run's other consumed inputs are multiplied by (see FlexCost), 1 otherwise. */
    private double costMultiplier = 1;
    /** Only used when probing a new run; the controller's saved history supplies this order. */
    @Nullable
    private transient Comparator<ItemStack> flexInputOrder;

    public void proceedTick() {
        proceedTick(1.0);
    }

    public void proceedTick(double speed) {
        if (speed <= 1.0) {
            tickProgress++;
            return;
        }
        progressCarry += speed;
        int steps = (int) progressCarry;
        progressCarry -= steps;
        tickProgress += Math.max(1, steps);
    }

    public double getRollToken(String key) {
        var existing = rollTokens.get(key);
        if (existing != null) {
            return existing;
        }
        double token = ROLL_RANDOM.nextDouble();
        rollTokens.put(key, token);
        return token;
    }

    public List<ItemStack> getCaptured(String name) {
        return captures.getOrDefault(name, List.of());
    }

    public void setCaptured(String name, List<ItemStack> stacks) {
        if (stacks.isEmpty()) {
            captures.remove(name);
        } else {
            captures.put(name, stacks.stream().map(s -> s.copyWithCount(1)).toList());
        }
    }

    /** @return the count stored under this name, 1 if there is none */
    public int getCount(String name) {
        return counts.getOrDefault(name, 1);
    }

    public void setCount(String name, int count) {
        counts.put(name, count);
    }

    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("canProcess", canProcess);
        tag.putInt("tickProgress", tickProgress);
        tag.putDouble("tickPercentage", tickPercentage);
        tag.putBoolean("canFinish", canFinish);
        tag.putDouble("progressCarry", progressCarry);
        if (!rollTokens.isEmpty()) {
            var rolls = new CompoundTag();
            rollTokens.forEach(rolls::putDouble);
            tag.put(ROLL_TOKENS_KEY, rolls);
        }
        if (!captures.isEmpty()) {
            var capturesTag = new CompoundTag();
            captures.forEach((name, stacks) -> {
                var list = new ListTag();
                stacks.forEach(stack -> list.add(LegacyNbt.saveItem(stack, registries)));
                capturesTag.put(name, list);
            });
            tag.put("captures", capturesTag);
        }
        if (costMultiplier != 1) {
            tag.putDouble("costMultiplier", costMultiplier);
        }
        if (!counts.isEmpty()) {
            var countsTag = new CompoundTag();
            counts.forEach(countsTag::putInt);
            tag.put("counts", countsTag);
        }
        return tag;
    }

    public static RecipeStateModel load(CompoundTag tag, HolderLookup.Provider registries) {
        RecipeStateModel model = new RecipeStateModel();
        model.setCanProcess(tag.getBooleanOr("canProcess", false));
        model.setTickProgress(tag.getIntOr("tickProgress", 0));
        model.setTickPercentage(tag.getDoubleOr("tickPercentage", 0));
        model.setCanFinish(tag.getBooleanOr("canFinish", false));
        model.setProgressCarry(tag.getDoubleOr("progressCarry", 0));
        if (tag.contains(ROLL_TOKENS_KEY)) {
            var rolls = tag.getCompoundOrEmpty(ROLL_TOKENS_KEY);
            for (String key : rolls.keySet()) {
                model.rollTokens.put(key, rolls.getDoubleOr(key, 0));
            }
        }
        var capturesTag = tag.getCompoundOrEmpty("captures");
        for (String name : capturesTag.keySet()) {
            var list = capturesTag.getListOrEmpty(name);
            var stacks = new ArrayList<ItemStack>();
            for (int i = 0; i < list.size(); i++) {
                ItemStack stack = LegacyNbt.parseItem(registries, list.getCompoundOrEmpty(i));
                if (!stack.isEmpty()) {
                    stacks.add(stack);
                }
            }
            model.setCaptured(name, stacks);
        }
        if (tag.contains("costMultiplier")) {
            model.setCostMultiplier(tag.getDoubleOr("costMultiplier", 0));
        }
        var countsTag = tag.getCompoundOrEmpty("counts");
        for (String name : countsTag.keySet()) {
            model.setCount(name, countsTag.getIntOr(name, 0));
        }
        return model;
    }

}
