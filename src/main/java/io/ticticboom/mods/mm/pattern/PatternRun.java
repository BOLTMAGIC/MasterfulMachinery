package io.ticticboom.mods.mm.pattern;

import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * One craft AE2 pushed into a Pattern Port, run by the controller next to its normal recipes. Once the recipe is
 * done (or can't run anymore) it is "returning": what's left in its buffer goes back to the block at returnPos
 * (the pattern provider), through its returnFace side.
 */
@Getter
public final class PatternRun {
    private final ResourceLocation recipeId;
    private final RecipeStateModel state;
    private final PatternBuffer buffer;
    private final BlockPos returnPos;
    private final Direction returnFace;
    private boolean returning;

    public PatternRun(ResourceLocation recipeId, RecipeStateModel state, PatternBuffer buffer, BlockPos returnPos, Direction returnFace) {
        this.recipeId = recipeId;
        this.state = state;
        this.buffer = buffer;
        this.returnPos = returnPos;
        this.returnFace = returnFace;
    }

    public void setReturning() {
        returning = true;
    }

    public CompoundTag save() {
        var tag = new CompoundTag();
        tag.putString("Recipe", recipeId.toString());
        tag.put("State", state.save(new CompoundTag()));
        tag.put("Buffer", buffer.save());
        tag.put("ReturnPos", NbtUtils.writeBlockPos(returnPos));
        tag.putString("ReturnFace", returnFace.getName());
        tag.putBoolean("Returning", returning);
        return tag;
    }

    /** @return null if the data is broken */
    @Nullable
    public static PatternRun load(CompoundTag tag) {
        ResourceLocation recipeId = ResourceLocation.tryParse(tag.getString("Recipe"));
        Direction face = Direction.byName(tag.getString("ReturnFace"));
        if (recipeId == null || face == null) {
            return null;
        }
        var run = new PatternRun(recipeId, RecipeStateModel.load(tag.getCompound("State")), PatternBuffer.load(tag.getCompound("Buffer")),
                NbtUtils.readBlockPos(tag.getCompound("ReturnPos")), face);
        run.returning = tag.getBoolean("Returning");
        return run;
    }
}
