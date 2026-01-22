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
import net.minecraft.server.MinecraftServer;
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
import java.util.concurrent.atomic.AtomicInteger;

import static io.ticticboom.mods.mm.config.MMConfigSetup.COMMON;

public class MachineControllerBlockEntity extends BlockEntity implements IControllerBlockEntity, IControllerPart {
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
    private final Map<ResourceLocation, RecipeStateModel> activeRecipes = new HashMap<>();
    private RecipeStorages portStorages = null;
    private boolean isFormed = false;
    private RecipeModel currentRecipe;
    private final ControllerModel controllerModel;
    private long lastTick = 0;

    public void tick() {
        if (level == null || level.isClientSide() || isRemoved()) {
            return;
        }
        runMachineTick();

        setChanged();
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private void runMachineTick() {
        if (level == null) return;
        long gameTime = level.getGameTime();
        if ((!isFormed || gameTime % COMMON.structureValidationRate.get() == 0) && lastTick != gameTime) {
            if (COMMON.asyncStructureValidation.get()) {
                validateStructureAsync(level);
            } else {
                validateStructure(level);
            }
        }
        lastTick = gameTime;
        if (isFormed) {
            runRecipe();
        }
    }

    private void validateStructure(Level level) {
        if (level == null) return;
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
        // Run validation on the server thread; no background future bookkeeping here.
        if (level == null || level.getServer() == null) {
            validateStructure(level);
            return;
        }
        MinecraftServer server = level.getServer();
        server.execute(() -> {
            try {
                validateStructure(level);
            } catch (Throwable t) {
                try {
                    Ref.LOG.error("Error during structure validation on server thread at {}: {}", getBlockPos(), t.toString());
                } catch (Throwable ignored) { }
            }
        });
    }

    private void runRecipe() {
        if (portStorages == null) {
            portStorages = structure.getStorages(level, getBlockPos());
        }
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
        for (ResourceLocation id : toRemove) activeRecipes.remove(id);

        for (RecipeModel recipe : MachineRecipeManager.getRecipesByStrucutreId(structure.id())) {
            if (!activeRecipes.containsKey(recipe.id()) && recipe.inputs().canProcess(level, portStorages, new RecipeStateModel())) {
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
        performRecipeTick();
    }

    private void performRecipeTick() {
        List<ResourceLocation> toRemove = new ArrayList<>();
        for (Map.Entry<ResourceLocation, RecipeStateModel> entry : activeRecipes.entrySet()) {
            ResourceLocation recipeId = entry.getKey();
            RecipeStateModel state = entry.getValue();
            RecipeModel recipe = MachineRecipeManager.RECIPES.get(recipeId);
            if (recipe != null) {
                recipe.outputs().processTick(level, portStorages, state);
                if (!state.isCanFinish()) state.proceedTick();
                state.setTickPercentage(((double) state.getTickProgress() / recipe.ticks()) * 100);
                if (state.getTickProgress() >= recipe.ticks()) {
                    state.setCanFinish(true);
                    boolean canOutputs = recipe.outputs().canProcess(level, portStorages, state);
                    if (canOutputs) {
                        recipe.outputs().process(level, portStorages, state);
                        toRemove.add(recipeId);
                    }
                }
            }
        }
        for (ResourceLocation id : toRemove) activeRecipes.remove(id);

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
        invalidateRecipe(false);
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
    public ControllerModel getModel() { return model; }

    @Override
    public Component getDisplayName() { return Component.literal(model.name()); }

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
        if (structure != null) tag.putString("structureId", structure.id().toString());
        tag.putBoolean("isFormed", isFormed);
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
            String s = tag.getString("structureId");
            ResourceLocation structureId = ResourceLocation.tryParse(s);
            structure = StructureManager.STRUCTURES.get(structureId);
        } else {
            structure = null;
        }
        if (tag.contains("isFormed")) {
            isFormed = tag.getBoolean("isFormed");
        } else {
            isFormed = (structure != null);
        }
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
    public void handleUpdateTag(CompoundTag tag) { load(tag); }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // No background validation futures to cancel (validations run on server thread)
    }

    public RecipeStateModel getRecipeState() {
        if (activeRecipes.isEmpty()) return null;
        return activeRecipes.values().iterator().next();
    }

    @Override
    public void onLoad() {
        if (level != null && !level.isClientSide() && level.getServer() != null) {
            MinecraftServer server = level.getServer();
            server.execute(() -> {
                try {
                    validateStructure(level);
                    setChanged();
                    level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
                } catch (Throwable t) {
                    try { Ref.LOG.error("Exception during onLoad structure validation at {}: {}", getBlockPos(), t.toString()); }
                    catch (Throwable ignored) { }
                }
            });
        }
    }

    public void reformTo(StructureModel newStructure) { setStructure(newStructure, true); }

    public void setStructure(StructureModel newStructure, boolean triggerRecipe) {
        boolean structureChanged = false;
        if (this.structure != newStructure) { this.structure = newStructure; structureChanged = true; }
        boolean newIsFormed = newStructure != null;
        if (this.isFormed != newIsFormed) { this.isFormed = newIsFormed; structureChanged = true; }
        if (structureChanged) {
            setChanged();
            if (level != null) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            if (this.structure == null) invalidateProgress();
            else if (this.isFormed && triggerRecipe) runRecipe();
        }
    }

    public void setFormed(boolean formed, boolean triggerRecipe) {
        if (this.isFormed == formed) return;
        this.isFormed = formed;
        setChanged();
        if (level != null) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        if (formed) { if (triggerRecipe && this.structure != null) runRecipe(); } else { invalidateProgress(); }
    }

    public void requestValidation() {
        if (level == null || level.isClientSide() || level.getServer() == null) return;
        MinecraftServer server = level.getServer();
        server.execute(() -> {
            try {
                validateStructure(level);
                setChanged();
                if (level != null) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            } catch (Throwable t) {
                try { Ref.LOG.error("Error while requesting validation for controller at {}: {}", getBlockPos(), t.toString()); }
                catch (Throwable ignored) { }
            }
        });

        Thread t = new Thread(() -> {
            try { Thread.sleep(100); } catch (InterruptedException ignored) { return; }
            if (level == null || level.isClientSide() || level.getServer() == null) return;
            level.getServer().execute(() -> {
                try {
                    validateStructure(level);
                    setChanged();
                    if (level != null) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
                } catch (Throwable t2) {
                    try { Ref.LOG.error("Error during delayed validation for controller at {}: {}", getBlockPos(), t2.toString()); }
                    catch (Throwable ignored) { }
                }
            });
        }, "mm-controller-validate-delayed");
        t.setDaemon(true);
        t.start();
    }
}
