package io.ticticboom.mods.mm.recipe.condition;

import io.ticticboom.mods.mm.structure.StructureModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Where a recipe would run: the controller's level and position, and its formed structure.
 * {@code pos} is null when there is no machine (e.g. a recipe checked on its own); conditions about the
 * machine's place then pass.
 */
public record RecipeConditionContext(Level level, @Nullable BlockPos pos, @Nullable StructureModel structure) {
}
