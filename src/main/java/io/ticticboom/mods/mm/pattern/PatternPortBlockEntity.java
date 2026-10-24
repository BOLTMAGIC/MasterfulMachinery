package io.ticticboom.mods.mm.pattern;

import io.ticticboom.mods.mm.compat.ae2.pattern.PatternPortCraftingMachine;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.networklink.NetworkLink;
import io.ticticboom.mods.mm.recipe.MachineRecipeManager;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.setup.MMRegisters;
import io.ticticboom.mods.mm.structure.MachineLinks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A Pattern Port: finds the machine it belongs to (like an Input Gateway) and remembers why the last push was
 * refused, for Jade. The pattern runs themselves live in the controller.
 */
public class PatternPortBlockEntity extends BlockEntity {
    /** How long the found machine is trusted before looking again, in ticks. */
    private static final int REFRESH_TICKS = 20;

    @Nullable
    private BlockPos controllerPos;
    private long refreshAt = Long.MIN_VALUE;
    @Nullable
    private PatternStartResult lastResult;
    private boolean noRecipe;
    // why the last pattern fit no recipe, when it didn't
    @Nullable
    private PatternMismatch noRecipeWhy;
    // recipe found per (pattern, structure), until recipes reload
    private final Map<List<Object>, Optional<ResourceLocation>> recipeByPattern = new HashMap<>();
    private int recipeGeneration = -1;
    // the AE2 crafting machine capability, made on first request; only with AE2
    @Nullable
    private LazyOptional<?> craftingMachine;

    public PatternPortBlockEntity(BlockPos pos, BlockState state) {
        super(MMRegisters.PATTERN_PORT_BE.get(), pos, state);
    }

    /** The formed machine this port belongs to, or null. Looked up again at most once a second. */
    @Nullable
    public MachineControllerBlockEntity machine() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        long now = serverLevel.getGameTime();
        if (now >= refreshAt) {
            refreshAt = now + REFRESH_TICKS;
            var found = MachineLinks.find(serverLevel, getBlockPos());
            controllerPos = found == null ? null : found.getBlockPos();
        }
        if (controllerPos != null && serverLevel.getExistingBlockEntity(controllerPos) instanceof MachineControllerBlockEntity controller
                && controller.getStructure() != null) {
            return controller;
        }
        return null;
    }

    @Nullable
    public PatternStartResult lastResult() {
        return lastResult;
    }

    public void setLastResult(@Nullable PatternStartResult result) {
        this.lastResult = result;
        this.noRecipe = false;
        this.noRecipeWhy = null;
    }

    /** Whether the last push was refused because no recipe of the machine fits the pattern. */
    public boolean lastHadNoRecipe() {
        return noRecipe;
    }

    /** @param why how the pattern differs from the closest recipe, if known */
    public void setNoRecipe(@Nullable PatternMismatch why) {
        this.lastResult = null;
        this.noRecipe = true;
        this.noRecipeWhy = why;
    }

    /** How the last refused pattern differs from the closest recipe, or null. */
    @Nullable
    public PatternMismatch noRecipeWhy() {
        return noRecipeWhy;
    }

    /** Why a pattern fits none of the machine's recipes, against the closest one; null if one fits. */
    @Nullable
    public PatternMismatch explainNoRecipe(MachineControllerBlockEntity machine, PatternSpec spec) {
        return PatternRecipeMatcher.explain(MachineRecipeManager.getRecipeForStructureIds(List.of(machine.getStructure().id())), spec);
    }

    /**
     * The machine's recipe a pattern is for, or null.
     *
     * @param pattern the pattern's identity (AE2's encoded pattern), the cache key with the machine's structure
     */
    @Nullable
    public RecipeModel recipeFor(Object pattern, MachineControllerBlockEntity machine, PatternSpec spec) {
        if (recipeGeneration != MachineRecipeManager.generation) {
            recipeByPattern.clear();
            recipeGeneration = MachineRecipeManager.generation;
        }
        var structure = machine.getStructure().id();
        var id = recipeByPattern.computeIfAbsent(List.of(pattern, structure), key -> Optional.ofNullable(
                PatternRecipeMatcher.match(MachineRecipeManager.getRecipeForStructureIds(List.of(structure)), spec)).map(RecipeModel::id));
        return id.map(MachineRecipeManager.RECIPES::get).orElse(null);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (NetworkLink.AVAILABLE && PatternPortCraftingMachine.isCraftingMachine(cap)) {
            if (craftingMachine == null) {
                craftingMachine = PatternPortCraftingMachine.capabilityFor(this);
            }
            return craftingMachine.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        if (craftingMachine != null) {
            craftingMachine.invalidate();
            craftingMachine = null;
        }
    }
}
