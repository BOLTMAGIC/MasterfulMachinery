package io.ticticboom.mods.mm.controller.machine.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.controller.IControllerBlockEntity;
import io.ticticboom.mods.mm.controller.IControllerPart;
import io.ticticboom.mods.mm.model.ControllerModel;
import io.ticticboom.mods.mm.port.MMPortRegistry;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.port.fluid.FluidPortStorage;
import io.ticticboom.mods.mm.port.energy.EnergyPortStorage;
import io.ticticboom.mods.mm.port.botania.mana.BotaniaManaPortStorage;
import io.ticticboom.mods.mm.port.pneumaticcraft.air.PneumaticAirPortStorage;
import io.ticticboom.mods.mm.port.kinetic.CreateKineticPortStorage;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorage;
import net.minecraftforge.registries.ForgeRegistries;
import io.ticticboom.mods.mm.recipe.MachineRecipeManager;
import io.ticticboom.mods.mm.recipe.input.consume.ConsumeRecipeIngredientEntry;
import io.ticticboom.mods.mm.port.item.BaseItemPortIngredient;
import io.ticticboom.mods.mm.port.fluid.FluidPortIngredient;
import io.ticticboom.mods.mm.port.energy.EnergyPortIngredient;
import io.ticticboom.mods.mm.port.botania.mana.BotaniaManaPortIngredient;
import io.ticticboom.mods.mm.port.pneumaticcraft.air.PneumaticAirPortIngredient;
import io.ticticboom.mods.mm.port.kinetic.CreateKineticPortIngredient;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortIngredient;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.ticticboom.mods.mm.config.MMConfigSetup.COMMON;

public class MachineControllerBlockEntity extends BlockEntity implements IControllerBlockEntity, IControllerPart {

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
    @Getter
    private RecipeModel currentRecipe;
    private final ControllerModel controllerModel;
    private long lastTick = 0;
    // cached view of storage contents to avoid rebuilding every tick when recipes are running
    private final java.util.Set<ResourceLocation> cachedAvailableItemIds = new java.util.HashSet<>();
    private final java.util.Set<ResourceLocation> cachedAvailableFluidIds = new java.util.HashSet<>();
    private final java.util.Set<ResourceLocation> cachedAvailableMekanismIds = new java.util.HashSet<>();
    private boolean cachedHasEnergyAvailable = false;
    private boolean cachedHasManaAvailable = false;
    private boolean cachedHasPneumaticAir = false;
    private boolean cachedHasKinetic = false;
    private boolean cachedHasMekanismChemical = false;
    private boolean storageContentCacheValid = false;
    private long lastResourceScanTime = -1;
    private final Map<ResourceLocation, Long> recipeNextCheckTime = new HashMap<>();
    // signature of the last observed storage contents; used to detect external changes
    private long lastStorageSignature = 0L;
    // Spread-out recipe scanning to avoid checking all recipes every tick when controller is searching
    private List<RecipeModel> cachedStructureRecipes = null;
    private int nextRecipeCheckIndex = 0;

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
            for (StructureModel structureModel : StructureManager.getStructuresForController(controllerId)) {
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
        // detect external changes to storages (player inserted/removed items, fluid changes, etc.)
        try {
            if (portStorages != null) {
                long sig = 1469598103934665603L; // FNV offset basis
                var itemStorages = portStorages.getInputStorages(ItemPortStorage.class);
                for (ItemPortStorage s : itemStorages) {
                    var handler = s.getHandler();
                    if (handler == null) continue;
                    for (int i = 0; i < handler.getSlots(); i++) {
                        var stack = handler.getStackInSlot(i);
                        int actual = handler.getActualCount(i);
                        int idHash = 0;
                        try {
                            var key = stack.isEmpty() ? null : ForgeRegistries.ITEMS.getKey(stack.getItem());
                            if (key != null) idHash = key.hashCode();
                        } catch (Throwable ignored) { }
                        sig ^= idHash + actual;
                        sig *= 1099511628211L; // FNV prime
                    }
                }

                var fluidStorages = portStorages.getInputStorages(FluidPortStorage.class);
                for (FluidPortStorage s : fluidStorages) {
                    var handler = s.getHandler();
                    if (handler == null) continue;
                    for (int i = 0; i < handler.getTanks(); i++) {
                        var fs = handler.getFluidInTank(i);
                        int amount = fs.getAmount();
                        int idHash = 0;
                        try {
                            var key = fs.getFluid() == null ? null : ForgeRegistries.FLUIDS.getKey(fs.getFluid());
                            if (key != null) idHash = key.hashCode();
                        } catch (Throwable ignored) { }
                        sig ^= idHash + amount;
                        sig *= 1099511628211L;
                    }
                }

                var energyStorages = portStorages.getInputStorages(EnergyPortStorage.class);
                for (EnergyPortStorage s : energyStorages) {
                    sig ^= s.getStoredEnergy();
                    sig *= 1099511628211L;
                }

                var manaStorages = portStorages.getInputStorages(BotaniaManaPortStorage.class);
                for (BotaniaManaPortStorage s : manaStorages) {
                    sig ^= s.getStored();
                    sig *= 1099511628211L;
                }

                var pneuStorages = portStorages.getInputStorages(PneumaticAirPortStorage.class);
                for (PneumaticAirPortStorage s : pneuStorages) {
                    sig ^= s.getAir();
                    sig *= 1099511628211L;
                }

                var kineticStorages = portStorages.getInputStorages(CreateKineticPortStorage.class);
                for (CreateKineticPortStorage s : kineticStorages) {
                    sig ^= Double.doubleToLongBits(s.getSpeed());
                    sig *= 1099511628211L;
                }

                var mechStorages = portStorages.getInputStorages(MekanismChemicalPortStorage.class);
                //noinspection rawtypes
                for (MekanismChemicalPortStorage s : mechStorages) {
                    try {
                        var stack = s.chemicalTank.getStack();
                        int amount = (int) Math.min(Integer.MAX_VALUE, stack.getAmount());
                        int idHash = 0;
                        try {
                            var typeId = stack.getType();
                            idHash = typeId.hashCode();
                        } catch (Throwable ignored) { }
                        sig ^= idHash + amount;
                        sig *= 1099511628211L;
                    } catch (Throwable ignored) { }
                }

                long currentSignature = sig;
                if (currentSignature != lastStorageSignature) {
                    // external change - force cache rebuild and allow recipes to be rechecked now
                    lastStorageSignature = currentSignature;
                    storageContentCacheValid = false;
                    recipeNextCheckTime.clear();
                }
            }
        } catch (Throwable ignored) { }
        long gameTime = (level == null) ? 0L : level.getGameTime();

        if (!storageContentCacheValid) {
            // throttle rebuilds when controller is only searching (no active recipes)
            boolean doRebuild = false;
            if (!activeRecipes.isEmpty()) {
                doRebuild = true; // running recipes -> keep cache up-to-date
            } else {
                // throttling / cooldowns for expensive scans when controller is only searching
                // when no active recipes, only scan storages every N ticks
                int resourceScanIntervalTicks = 5;
                if (lastResourceScanTime < 0 || gameTime - lastResourceScanTime >= resourceScanIntervalTicks) {
                    doRebuild = true;
                    lastResourceScanTime = gameTime;
                }
            }

            //noinspection StatementWithEmptyBody
            if (doRebuild) {
                cachedAvailableItemIds.clear();
                cachedAvailableFluidIds.clear();
                cachedAvailableMekanismIds.clear();
                cachedHasEnergyAvailable = false;
                cachedHasManaAvailable = false;
                cachedHasPneumaticAir = false;
                cachedHasKinetic = false;
                cachedHasMekanismChemical = false;

                if (portStorages != null) {
                var itemStorages = portStorages.getInputStorages(ItemPortStorage.class);
                for (ItemPortStorage s : itemStorages) {
                    var handler = s.getHandler();
                    if (handler == null) continue;
                    for (int i = 0; i < handler.getSlots(); i++) {
                        var stack = handler.getStackInSlot(i);
                        int actual = handler.getActualCount(i);
                        if (!stack.isEmpty() && actual > 0) {
                            var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
                            if (key != null) cachedAvailableItemIds.add(key);
                        }
                    }
                }

                var fluidStorages = portStorages.getInputStorages(FluidPortStorage.class);
                for (FluidPortStorage s : fluidStorages) {
                    var handler = s.getHandler();
                    if (handler == null) continue;
                    for (int i = 0; i < handler.getTanks(); i++) {
                        var fs = handler.getFluidInTank(i);
                        if (fs.getAmount() > 0) {
                            var key = ForgeRegistries.FLUIDS.getKey(fs.getFluid());
                            if (key != null) cachedAvailableFluidIds.add(key);
                        }
                    }
                }

                var energyStorages = portStorages.getInputStorages(EnergyPortStorage.class);
                for (EnergyPortStorage s : energyStorages) {
                    if (s.getStoredEnergy() > 0) { cachedHasEnergyAvailable = true; break; }
                }

                var manaStorages = portStorages.getInputStorages(BotaniaManaPortStorage.class);
                for (BotaniaManaPortStorage s : manaStorages) {
                    if (s.getStored() > 0) { cachedHasManaAvailable = true; break; }
                }

                var pneuStorages = portStorages.getInputStorages(PneumaticAirPortStorage.class);
                for (PneumaticAirPortStorage s : pneuStorages) {
                    if (s.getAir() > 0) { cachedHasPneumaticAir = true; break; }
                }

                var kineticStorages = portStorages.getInputStorages(CreateKineticPortStorage.class);
                for (CreateKineticPortStorage s : kineticStorages) {
                    if (s.getSpeed() > 0) { cachedHasKinetic = true; break; }
                }

                var mechStorages = portStorages.getInputStorages(MekanismChemicalPortStorage.class);
                //noinspection rawtypes
                for (MekanismChemicalPortStorage s : mechStorages) {
                    try {
                        var stack = s.chemicalTank.getStack();
                        if (!stack.isEmpty() && stack.getAmount() > 0) {
                            // chemical type -> registry name is a ResourceLocation string
                            try {
                                var rl = stack.getType().getRegistryName();
                                cachedAvailableMekanismIds.add(rl);
                            } catch (Throwable ignored) { }
                            cachedHasMekanismChemical = true;
                        }
                    } catch (Throwable ignored) { }
                }
            }

                // reflect cached booleans into local variables
                storageContentCacheValid = true;
                // after a rebuild allow recipes to be rechecked immediately
                recipeNextCheckTime.clear();
            } else {
                // Not rebuilding this tick; leave local variables as cached (may be stale/empty).
                // To avoid false negatives, we'll skip content-based pre-checks when cache is not valid.
            }
        }
        List<ResourceLocation> toRemove = new ArrayList<>();
        for (Map.Entry<ResourceLocation, RecipeStateModel> entry : activeRecipes.entrySet()) {
            ResourceLocation recipeId = entry.getKey();
            RecipeStateModel state = entry.getValue();
            RecipeModel recipe = MachineRecipeManager.RECIPES.get(recipeId);
            if (recipe != null && state.isCanFinish() && recipe.outputs().canProcess(level, portStorages, state)) {
                recipe.outputs().process(level, portStorages, state);
                toRemove.add(recipeId);
                // outputs changed storages; mark cache invalid so we rebuild before next decisions
                storageContentCacheValid = false;
            }
        }
        for (ResourceLocation id : toRemove) activeRecipes.remove(id);

        // Spread recipe checks across multiple ticks to avoid scanning all recipes every tick.
        //noinspection StatementWithEmptyBody
        if (structure == null) {
            // nothing to check
        } else {
            if (cachedStructureRecipes == null) cachedStructureRecipes = new ArrayList<>(MachineRecipeManager.getRecipesByStrucutreId(structure.id()));
            if (!cachedStructureRecipes.isEmpty()) {
                int total = cachedStructureRecipes.size();
                // tune this: how many recipes to probe per tick when searching
                int maxRecipeChecksPerTick = 5;
                int checks = Math.min(maxRecipeChecksPerTick, total);
                int idx = nextRecipeCheckIndex % total;
                int performed = 0;
                while (performed < checks) {
                    RecipeModel recipe = cachedStructureRecipes.get(idx);
                    idx = (idx + 1) % total;
                    performed++;

                    if (activeRecipes.containsKey(recipe.id())) continue;
                    if (recipeNextCheckTime.getOrDefault(recipe.id(), 0L) > gameTime) continue;

                    // lightweight capability pre-check: compute required port types from recipe inputs
                    java.util.Set<ResourceLocation> requiredTypes = new java.util.HashSet<>();
                    for (var input : recipe.inputs().inputs()) {
                        if (input instanceof ConsumeRecipeIngredientEntry cre) {
                            var ingr = cre.getIngredient();
                            if (ingr instanceof BaseItemPortIngredient) requiredTypes.add(Ref.Ports.ITEM);
                            else if (ingr instanceof FluidPortIngredient) requiredTypes.add(Ref.Ports.FLUID);
                            else if (ingr instanceof EnergyPortIngredient) requiredTypes.add(Ref.Ports.ENERGY);
                            else if (ingr instanceof BotaniaManaPortIngredient) requiredTypes.add(Ref.Ports.BOTANIA_MANA);
                            else if (ingr instanceof PneumaticAirPortIngredient) requiredTypes.add(Ref.Ports.PNEUMATIC_AIR);
                            else if (ingr instanceof CreateKineticPortIngredient) requiredTypes.add(Ref.Ports.CREATE_KINETIC);
                            else //noinspection rawtypes
                                if (ingr instanceof MekanismChemicalPortIngredient mech) {
                                try { var typeId = mech.getTypeId(); if (typeId != null) requiredTypes.add(typeId); }
                                catch (Throwable ignored) { }
                            }
                        }
                    }

                    // Also gather any specific resource ids (items/fluids) that the recipe requires
                    java.util.Set<ResourceLocation> requiredItemIds = new java.util.HashSet<>();
                    java.util.Set<ResourceLocation> requiredFluidIds = new java.util.HashSet<>();
                    java.util.Set<ResourceLocation> requiredMekanismIds = new java.util.HashSet<>();
                    boolean needsEnergy = false;
                    boolean needsMana = false;
                    boolean needsPneumatic = false;
                    boolean needsKinetic = false;
                    boolean needsMekanismChemical = false;
                    for (var input : recipe.inputs().inputs()) {
                        if (input instanceof ConsumeRecipeIngredientEntry cre) {
                            var ingr = cre.getIngredient();
                            if (ingr instanceof BaseItemPortIngredient) {
                                if (ingr instanceof io.ticticboom.mods.mm.port.item.SingleItemPortIngredient single) {
                                    try { var id = single.getItemId(); if (id != null) requiredItemIds.add(id); } catch (Throwable ignored) {}
                                }
                            } else if (ingr instanceof FluidPortIngredient fp) {
                                try { var id = fp.getFluidId(); if (id != null) requiredFluidIds.add(id); } catch (Throwable ignored) {}
                            } else if (ingr instanceof EnergyPortIngredient) needsEnergy = true;
                            else if (ingr instanceof BotaniaManaPortIngredient) needsMana = true;
                            else if (ingr instanceof PneumaticAirPortIngredient) needsPneumatic = true;
                            else if (ingr instanceof CreateKineticPortIngredient) needsKinetic = true;
                            else //noinspection rawtypes
                                if (ingr instanceof MekanismChemicalPortIngredient mech) {
                                try {
                                    var chemId = mech.getChemicalId();
                                    if (chemId != null) requiredMekanismIds.add(chemId);
                                    needsMekanismChemical = true; // also keep generic flag for quick checks
                                } catch (Throwable ignored) { }
                            }
                        }
                    }

                    // when a recipe is skipped due to missing resources, wait N ticks before rechecking
                    int recipeSkipCooldownTicks = 20;
                    if (!requiredTypes.isEmpty()) {
                        var available = MMPortRegistry.PORT_TYPES_BY_CONTROLLER.get(controllerId);
                        if (available != null && !available.containsAll(requiredTypes)) {
                            Ref.LOG.debug("Skipping recipe {} on controller {}: required types {} but available {}", recipe.id(), controllerId, requiredTypes, available);
                            recipeNextCheckTime.put(recipe.id(), gameTime + recipeSkipCooldownTicks);
                            continue;
                        }
                    }

                    if (portStorages != null && storageContentCacheValid) {
                        if (!requiredItemIds.isEmpty() && !cachedAvailableItemIds.containsAll(requiredItemIds)) {
                            Ref.LOG.debug("Skipping recipe {} on controller {}: missing required items {} (available {})", recipe.id(), controllerId, requiredItemIds, cachedAvailableItemIds);
                            recipeNextCheckTime.put(recipe.id(), gameTime + recipeSkipCooldownTicks);
                            continue;
                        }
                        if (!requiredFluidIds.isEmpty() && !cachedAvailableFluidIds.containsAll(requiredFluidIds)) {
                            Ref.LOG.debug("Skipping recipe {} on controller {}: missing required fluids {} (available {})", recipe.id(), controllerId, requiredFluidIds, cachedAvailableFluidIds);
                            recipeNextCheckTime.put(recipe.id(), gameTime + recipeSkipCooldownTicks);
                            continue;
                        }
                        if (needsEnergy && !cachedHasEnergyAvailable) {
                            Ref.LOG.debug("Skipping recipe {} on controller {}: needs energy but none available", recipe.id(), controllerId);
                            recipeNextCheckTime.put(recipe.id(), gameTime + recipeSkipCooldownTicks);
                            continue;
                        }
                        if (needsMana && !cachedHasManaAvailable) {
                            Ref.LOG.debug("Skipping recipe {} on controller {}: needs botania mana but none available", recipe.id(), controllerId);
                            recipeNextCheckTime.put(recipe.id(), gameTime + recipeSkipCooldownTicks);
                            continue;
                        }
                        if (needsPneumatic && !cachedHasPneumaticAir) {
                            Ref.LOG.debug("Skipping recipe {} on controller {}: needs pneumatic air but none available", recipe.id(), controllerId);
                            recipeNextCheckTime.put(recipe.id(), gameTime + recipeSkipCooldownTicks);
                            continue;
                        }
                        if (needsKinetic && !cachedHasKinetic) {
                            Ref.LOG.debug("Skipping recipe {} on controller {}: needs kinetic rotation but none available", recipe.id(), controllerId);
                            recipeNextCheckTime.put(recipe.id(), gameTime + recipeSkipCooldownTicks);
                            continue;
                        }
                        if (needsMekanismChemical && !cachedHasMekanismChemical) {
                            Ref.LOG.debug("Skipping recipe {} on controller {}: needs mekanism chemical but none available", recipe.id(), controllerId);
                            recipeNextCheckTime.put(recipe.id(), gameTime + recipeSkipCooldownTicks);
                            continue;
                        }
                        if (!requiredMekanismIds.isEmpty() && !cachedAvailableMekanismIds.containsAll(requiredMekanismIds)) {
                            Ref.LOG.debug("Skipping recipe {} on controller {}: missing required mekanism chemicals {} (available {})", recipe.id(), controllerId, requiredMekanismIds, cachedAvailableMekanismIds);
                            recipeNextCheckTime.put(recipe.id(), gameTime + recipeSkipCooldownTicks);
                            continue;
                        }
                    }

                    if (!recipe.inputs().canProcess(level, portStorages, new RecipeStateModel())) {
                        continue;
                    }

                    boolean allowParallel = recipe.parallelProcessing();
                    if (recipe.parallelProcessing() == MMConfig.PARALLEL_PROCESSING_DEFAULT) {
                        allowParallel = controllerModel.parallelProcessingDefault();
                    }
                    if (allowParallel || activeRecipes.isEmpty()) {
                        if (activeRecipes.size() < MMConfig.MAX_PARALLEL_RECIPES) {
                            RecipeStateModel newState = new RecipeStateModel();
                            recipe.inputs().process(level, portStorages, newState);
                            // inputs consumed/changed storages; invalidate cached view
                            storageContentCacheValid = false;
                            newState.setCanProcess(true);
                            activeRecipes.put(recipe.id(), newState);
                            Ref.LOG.debug("Controller {} started recipe {}", controllerId, recipe.id());
                            setChanged();
                        }
                    }

                }
                nextRecipeCheckIndex = idx;
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
                // outputs tick may have modified storages; invalidate cached view so next tick rebuilds
                storageContentCacheValid = false;
                if (!state.isCanFinish()) state.proceedTick();
                state.setTickPercentage(((double) state.getTickProgress() / recipe.ticks()) * 100);
                if (state.getTickProgress() >= recipe.ticks()) {
                    state.setCanFinish(true);
                    boolean canOutputs = recipe.outputs().canProcess(level, portStorages, state);
                    if (canOutputs) {
                        recipe.outputs().process(level, portStorages, state);
                        toRemove.add(recipeId);
                        // outputs processed - storages changed
                        storageContentCacheValid = false;
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
        // clear caches and active recipes when structure is lost
        invalidateRecipe(false);
        cachedStructureRecipes = null;
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
        // clear cached views and backoff timers as recipe state is reset
        storageContentCacheValid = false;
        cachedAvailableItemIds.clear();
        cachedAvailableFluidIds.clear();
        cachedAvailableMekanismIds.clear();
        cachedHasEnergyAvailable = false;
        cachedHasManaAvailable = false;
        cachedHasPneumaticAir = false;
        cachedHasKinetic = false;
        cachedHasMekanismChemical = false;
        recipeNextCheckTime.clear();
        cachedStructureRecipes = null;
    }

    @Override
    public ControllerModel getModel() { return model; }

    @Override
    public @NotNull Component getDisplayName() { return Component.literal(model.name()); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory inv, @NotNull Player player) {
        return new MachineControllerMenu(model, groupHolder, windowId, inv, this);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
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
    public void load(@NotNull CompoundTag tag) {
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
    public @NotNull CompoundTag getUpdateTag() {
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

        Thread t = getThread();
        t.start();
    }

    private @NotNull Thread getThread() {
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
        return t;
    }
}
