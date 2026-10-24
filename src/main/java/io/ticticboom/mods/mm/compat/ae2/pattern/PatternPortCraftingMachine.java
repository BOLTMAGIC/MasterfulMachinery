package io.ticticboom.mods.mm.compat.ae2.pattern;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.capabilities.Capabilities;
import io.ticticboom.mods.mm.pattern.PatternBuffer;
import io.ticticboom.mods.mm.pattern.PatternIngredient;
import io.ticticboom.mods.mm.pattern.PatternPortBlockEntity;
import io.ticticboom.mods.mm.pattern.PatternSpec;
import io.ticticboom.mods.mm.pattern.PatternStartResult;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.setup.MMRegisters;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import java.util.List;

/**
 * The Pattern Port as AE2 sees it: a crafting machine a Pattern Provider pushes patterns into. A push starts a pattern
 * run on the port's machine right away, or is refused (AE2 keeps the inputs and tries again later).
 */
public final class PatternPortCraftingMachine implements ICraftingMachine {
    private final PatternPortBlockEntity port;

    private PatternPortCraftingMachine(PatternPortBlockEntity port) {
        this.port = port;
    }

    public static LazyOptional<ICraftingMachine> capabilityFor(PatternPortBlockEntity port) {
        return LazyOptional.of(() -> new PatternPortCraftingMachine(port));
    }

    public static boolean isCraftingMachine(Capability<?> cap) {
        return cap == Capabilities.CRAFTING_MACHINE;
    }

    @Override
    public PatternContainerGroup getCraftingMachineInfo() {
        return new PatternContainerGroup(AEItemKey.of(MMRegisters.PATTERN_PORT_ITEM.get()), Component.translatable("block.mm.pattern_port"), List.of());
    }

    @Override
    public boolean pushPattern(IPatternDetails details, KeyCounter[] inputs, Direction ejectionDirection) {
        var machine = port.machine();
        if (machine == null) {
            port.setLastResult(PatternStartResult.NOT_FORMED);
            return false;
        }
        PatternSpec spec = Ae2PatternSpecs.spec(details);
        RecipeModel recipe = spec == null ? null : port.recipeFor(details.getDefinition(), machine, spec);
        if (recipe == null) {
            port.setNoRecipe(spec == null ? Ae2PatternSpecs.unsupported(details) : port.explainNoRecipe(machine, spec));
            return false;
        }
        var buffer = PatternBuffer.forRun(spec, recipe);
        for (KeyCounter counter : inputs) {
            for (Object2LongMap.Entry<AEKey> entry : counter) {
                PatternIngredient input = Ae2PatternSpecs.ingredient(entry.getKey(), entry.getLongValue());
                if (input == null || !buffer.insert(input)) {
                    port.setLastResult(PatternStartResult.MISSING_INPUTS);
                    return false;
                }
            }
        }
        PatternStartResult result = machine.startPatternRun(recipe, buffer, port.getBlockPos().relative(ejectionDirection), ejectionDirection.getOpposite());
        port.setLastResult(result);
        return result == PatternStartResult.STARTED;
    }

    @Override
    public boolean acceptsPlans() {
        return port.machine() != null;
    }
}
