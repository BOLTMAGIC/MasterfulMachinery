package io.ticticboom.mods.mmtest;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * A processing pattern for the Pattern Port GameTests, without a real encoded pattern item. Only touched when AE2 is
 * installed.
 */
record TestPattern(AEItemKey definition, GenericStack[] ins, GenericStack[] outs) implements IPatternDetails {

    /** @param name tells patterns apart (the port caches the recipe per pattern) */
    static TestPattern of(String name, GenericStack[] ins, GenericStack... outs) {
        var paper = new ItemStack(Items.PAPER);
        paper.setHoverName(Component.literal(name));
        return new TestPattern(AEItemKey.of(paper), ins, outs);
    }

    /** What a pattern provider hands over with the push: one counter per input. */
    KeyCounter[] inputs() {
        var counters = new KeyCounter[ins.length];
        for (int i = 0; i < ins.length; i++) {
            counters[i] = new KeyCounter();
            counters[i].add(ins[i].what(), ins[i].amount());
        }
        return counters;
    }

    @Override
    public AEItemKey getDefinition() {
        return definition;
    }

    @Override
    public IInput[] getInputs() {
        var inputs = new IInput[ins.length];
        for (int i = 0; i < ins.length; i++) {
            GenericStack stack = ins[i];
            inputs[i] = new IInput() {
                @Override
                public GenericStack[] getPossibleInputs() {
                    return new GenericStack[]{stack};
                }

                @Override
                public long getMultiplier() {
                    return 1;
                }

                @Override
                public boolean isValid(AEKey input, Level level) {
                    return input.equals(stack.what());
                }

                @Override
                public AEKey getRemainingKey(AEKey template) {
                    return null;
                }
            };
        }
        return inputs;
    }

    @Override
    public GenericStack[] getOutputs() {
        return outs;
    }
}
