package io.ticticboom.mods.mm.controller.machine.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.controller.IControllerBlockEntity;
import io.ticticboom.mods.mm.controller.IControllerPart;
import io.ticticboom.mods.mm.model.ControllerModel;
import io.ticticboom.mods.mm.recipe.MachineRecipeManager;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static io.ticticboom.mods.mm.config.MMConfigSetup.COMMON;

public class MachineControllerBlockEntity extends BlockEntity implements IControllerBlockEntity, IControllerPart {
    private static final Executor STRUCTURE_VALIDATION_EXECUTOR = Executors.newFixedThreadPool(
        Math.max(2, Runtime.getRuntime().availableProcessors() / 2), r -> {
        Thread thread = new Thread(r, "MM-Structures");
        thread.setDaemon(true);
        thread.setPriority(Thread.MAX_PRIORITY - 1);
        return thread;
    });
    
    private static final AtomicInteger ACTIVE_VALIDATIONS = new AtomicInteger(0);
    private static final int MAX_CONCURRENT_VALIDATIONS = Math.max(4, Runtime.getRuntime().availableProcessors());

    private final ControllerModel model;
    private final RegistryGroupHolder groupHolder;
    private final ResourceLocation controllerId;

    public MachineControllerBlockEntity(ControllerModel model, RegistryGroupHolder groupHolder, BlockPos pos, BlockState state) {
        super(groupHolder.getBe().get(), pos, state);
        this.model = model;
        this.controllerModel = model;
        this.groupHolder = groupHolder;
        controllerId = Ref.id(model.id());
    }

    @Getter
    private StructureModel structure = null;
    private Map<ResourceLocation, RecipeStateModel> activeRecipes = new HashMap<>();
    private RecipeStorages portStorages = null;
    private boolean isFormed = false;
    private RecipeModel currentRecipe = null;
    private ControllerModel controllerModel = null;
    private CompletableFuture<Void> structureValidationFuture = null;
    private long lastTick = 0;
    private long lastRecipeTick = 0;
    private List<RecipeModel> availableRecipes = null;

    public void tick() {
        if (level.isClientSide() || isRemoved()) {
            return;
        }
        runMachineTick();

        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private void runMachineTick() {
        //lastTick != level.getGameTime() if controller boosted by torcherino, we're not making validation more than once per tick
        if((!isFormed || level.getGameTime() % COMMON.structureValidationRate.get() == 0) && lastTick != level.getGameTime()) {
            if (COMMON.asyncStructureValidation.get()) {
                validateStructureAsync(getLevel());
            } else {
                validateStructure(getLevel());
            }
        }
        lastTick = level.getGameTime();
        if (isFormed) {
            runRecipe();
        }
        if (isFormed && level.getGameTime() % COMMON.recipeTickRate.get() == 0 && lastRecipeTick != level.getGameTime()) {
            performRecipeTick();
            lastRecipeTick = level.getGameTime();
        }
    }

    private void validateStructure(Level level) {
        if (structure == null) {
            for (StructureModel structureModel : StructureManager.STRUCTURES.values()) {
                if (!structureModel.controllerIds().contains(controllerId)) {
                    continue;
                }

                if (structureModel.formed(level, getBlockPos())) {
                    setChanged();
                    structure = structureModel;
                    runRecipe();
                    return;
                }
            }

            invalidateProgress();
            isFormed = false;
        } else {
            isFormed = structure.formed(level, getBlockPos());
        }
    }

    private void validateStructureAsync(Level level) {
        // If there's already a validation running, don't start another one
        if (structureValidationFuture != null && !structureValidationFuture.isDone()) {
            return;
        }

        // Rate limiting: don't start if too many validations are already running
        if (ACTIVE_VALIDATIONS.get() >= MAX_CONCURRENT_VALIDATIONS) {
            return;
        }

        // Capture current state for the async operation
        final Level capturedLevel = level;
        final BlockPos capturedPos = getBlockPos().immutable();
        final ResourceLocation capturedControllerId = controllerId;
        final StructureModel capturedStructure = structure;

        structureValidationFuture = CompletableFuture.runAsync(() -> {
            ACTIVE_VALIDATIONS.incrementAndGet();
            try {
                validateStructureInBackground(capturedLevel, capturedPos, capturedControllerId, capturedStructure);
            } finally {
                ACTIVE_VALIDATIONS.decrementAndGet();
            }
        }, STRUCTURE_VALIDATION_EXECUTOR).exceptionally(throwable -> {
            ACTIVE_VALIDATIONS.decrementAndGet();
            // Log the exception but don't crash the game
            System.err.println("Error during structure validation: " + throwable.getMessage());
            return null;
        });
    }

    private void validateStructureInBackground(Level level, BlockPos pos, ResourceLocation controllerId, StructureModel currentStructure) {
        boolean newIsFormed = false;
        StructureModel newStructure = currentStructure;

        if (currentStructure == null) {
            // Search for a matching structure
            for (StructureModel structureModel : StructureManager.STRUCTURES.values()) {
                if (!structureModel.controllerIds().contains(controllerId)) {
                    continue;
                }

                if (structureModel.formed(level, pos)) {
                    newStructure = structureModel;
                    newIsFormed = true;
                    break;
                }
            }
        } else {
            // Check if current structure is still formed
            newIsFormed = currentStructure.formed(level, pos);
        }
        if(!newIsFormed){
            newStructure = null;
        }
        // Update the main thread with results
        final boolean finalIsFormed = newIsFormed;
        final StructureModel finalStructure = newStructure;
        
        // Schedule the update on the main thread
        level.getServer().execute(() -> {
            updateStructureValidationResults(finalIsFormed, finalStructure);
        });
    }

    private void updateStructureValidationResults(boolean newIsFormed, StructureModel newStructure) {
        boolean structureChanged = false;

        if (structure != newStructure) {
            structure = newStructure;
            structureChanged = true;
        }

        if (isFormed != newIsFormed) {
            isFormed = newIsFormed;
            structureChanged = true;
        }

        if (structureChanged) {
            setChanged();
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            if (structure == null) {
                invalidateProgress();
            } else if (newIsFormed && structureChanged) {
                // Structure just formed, trigger recipe processing
                runRecipe();
            }
        }
    }

    private void runRecipe() {
        if (portStorages == null) {
            portStorages = structure.getStorages(level, getBlockPos());
        }
        if (availableRecipes == null) {
            availableRecipes = new ArrayList<>(MachineRecipeManager.getRecipesByStrucutreId(structure.id()));
        }

        // First, try to output for stuck recipes
        List<ResourceLocation> toRemove = new ArrayList<>();
        for (Map.Entry<ResourceLocation, RecipeStateModel> entry : activeRecipes.entrySet()) {
            ResourceLocation recipeId = entry.getKey();
            RecipeStateModel state = entry.getValue();
            RecipeModel recipe = MachineRecipeManager.RECIPES.get(recipeId);
            if (recipe != null && state.isCanFinish() && recipe.outputs().canProcess(level, portStorages, state)) {
                recipe.outputs().process(level, portStorages, state);
                toRemove.add(recipeId);
            }
        }
        for (ResourceLocation id : toRemove) {
            activeRecipes.remove(id);
        }

        // Start new recipes if possible
        for (RecipeModel recipe : availableRecipes) {
            if (!activeRecipes.containsKey(recipe.id()) && recipe.inputs().canProcess(level, portStorages, new RecipeStateModel()) && recipe.outputs().canProcess(level, portStorages, new RecipeStateModel())) {
                boolean allowParallel = recipe.parallelProcessing();
                if (recipe.parallelProcessing() == MMConfig.PARALLEL_PROCESSING_DEFAULT) {
                    allowParallel = controllerModel.parallelProcessingDefault();
                }
                if (allowParallel || activeRecipes.isEmpty()) {
                    if (activeRecipes.size() < MMConfig.MAX_PARALLEL_RECIPES) {
                        RecipeStateModel newState = new RecipeStateModel();
                        recipe.inputs().process(level, portStorages, newState);
                        newState.setCanProcess(true);
                        activeRecipes.put(recipe.id(), newState);
                        setChanged();
                    }
                }
            }
        }
    }

    private void performRecipeTick() {
        // Tick all active recipes
        List<ResourceLocation> toRemove = new ArrayList<>();
        for (Map.Entry<ResourceLocation, RecipeStateModel> entry : activeRecipes.entrySet()) {
            ResourceLocation recipeId = entry.getKey();
            RecipeStateModel state = entry.getValue();
            RecipeModel recipe = MachineRecipeManager.RECIPES.get(recipeId);
            if (recipe != null) {
                recipe.outputs().processTick(level, portStorages, state);
                if (!state.isCanFinish()) {
                    state.proceedTick();
                }
                state.setTickPercentage(((double) state.getTickProgress() / recipe.ticks()) * 100);
                if (state.getTickProgress() >= recipe.ticks()) {
                    state.setCanFinish(true);
                    // Try to output immediately
                    if (recipe.outputs().canProcess(level, portStorages, state)) {
                        recipe.outputs().process(level, portStorages, state);
                        toRemove.add(recipeId);
                    }
                    // Otherwise, it stays active and stuck
                }
            }
        }
        for (ResourceLocation id : toRemove) {
            activeRecipes.remove(id);
        }
        // Update currentRecipe
        if (!activeRecipes.isEmpty()) {
            currentRecipe = MachineRecipeManager.RECIPES.get(activeRecipes.keySet().iterator().next());
        } else {
            currentRecipe = null;
        }
    }

    public void invalidateProgress() {
        setChanged();
        structure = null;
        isFormed = false;
        
        // Cancel any ongoing structure validation
        if (structureValidationFuture != null && !structureValidationFuture.isDone()) {
            structureValidationFuture.cancel(true);
        }
        
        invalidateRecipe(false);
        availableRecipes = null;
    }

    public void invalidateRecipe(boolean typical) {
        for (Map.Entry<ResourceLocation, RecipeStateModel> entry : activeRecipes.entrySet()) {
            ResourceLocation recipeId = entry.getKey();
            RecipeStateModel state = entry.getValue();
            RecipeModel recipe = MachineRecipeManager.RECIPES.get(recipeId);
            if (recipe != null && !typical && portStorages != null) {
                recipe.ditchRecipe(this.level, state, portStorages);
            }
        }
        activeRecipes.clear();
        currentRecipe = null;
        portStorages = null;
    }

    @Override
    public ControllerModel getModel() {
        return model;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal(model.name());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player player) {
        return new MachineControllerMenu(model, groupHolder, windowId, inv, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        CompoundTag recipesTag = new CompoundTag();
        for (Map.Entry<ResourceLocation, RecipeStateModel> entry : activeRecipes.entrySet()) {
            recipesTag.put(entry.getKey().toString(), entry.getValue().save(new CompoundTag()));
        }
        tag.put("activeRecipes", recipesTag);
        if (structure != null) {
            tag.putString("structureId", structure.id().toString());
        }
        tag.putBoolean("filler", true);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        activeRecipes.clear();
        if (tag.contains("activeRecipes")) {
            CompoundTag recipesTag = tag.getCompound("activeRecipes");
            for (String key : recipesTag.getAllKeys()) {
                ResourceLocation recipeId = ResourceLocation.tryParse(key);
                if (recipeId != null) {
                    RecipeStateModel state = RecipeStateModel.load(recipesTag.getCompound(key));
                    activeRecipes.put(recipeId, state);
                }
            }
        }
        if (tag.contains("structureId")) {
            ResourceLocation structureId = ResourceLocation.tryParse(tag.get("structureId").getAsString());
            structure = StructureManager.STRUCTURES.get(structureId);
        } else {
            structure = null;
        }
        // Update currentRecipe
        if (!activeRecipes.isEmpty()) {
            currentRecipe = MachineRecipeManager.RECIPES.get(activeRecipes.keySet().iterator().next());
        } else {
            currentRecipe = null;
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        var tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Cancel any ongoing structure validation when the block entity is removed
        if (structureValidationFuture != null && !structureValidationFuture.isDone()) {
            structureValidationFuture.cancel(true);
        }
    }

    public RecipeStateModel getRecipeState() {
        if (activeRecipes.isEmpty()) {
            return null;
        }
        return activeRecipes.values().iterator().next();
    }
}
