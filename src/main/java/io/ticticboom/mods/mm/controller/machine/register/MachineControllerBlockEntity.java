package io.ticticboom.mods.mm.controller.machine.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.controller.IControllerBlockEntity;
import io.ticticboom.mods.mm.controller.IControllerPart;
import io.ticticboom.mods.mm.model.ControllerModel;
import io.ticticboom.mods.mm.model.RecipeSelectionMode;
import io.ticticboom.mods.mm.port.MMPortRegistry;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.port.fluid.FluidPortStorage;
import io.ticticboom.mods.mm.port.energy.EnergyPortStorage;
import io.ticticboom.mods.mm.port.botania.mana.BotaniaManaPortStorage;
import io.ticticboom.mods.mm.port.item.SingleItemPortIngredient;
import io.ticticboom.mods.mm.port.pneumaticcraft.air.PneumaticAirPortStorage;
import io.ticticboom.mods.mm.port.kinetic.CreateKineticPortStorage;
import io.ticticboom.mods.mm.port.mekanism.chemical.MekanismChemicalPortStorage;
import lombok.Setter;
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
    // Redstone control for the controller: IGNORE, RUN_WHEN_POWERED, RUN_WHEN_UNPOWERED
    private enum RedstoneMode { IGNORED, WITH_REDSTONE, WITHOUT_REDSTONE }
    private RedstoneMode redstoneMode = RedstoneMode.IGNORED;
    private long lastTick = 0;
    // cached view of storage contents to avoid rebuilding every tick when recipes are running
    private final java.util.Set<ResourceLocation> cachedAvailableItemIds = new java.util.HashSet<>();
    private final java.util.Set<ResourceLocation> cachedAvailableFluidIds = new java.util.HashSet<>();
    private final java.util.Set<ResourceLocation> cachedAvailableMekanismIds = new java.util.HashSet<>();
    private final java.util.Map<ResourceLocation, java.util.List<ResourceLocation>> cachedAvailableItemStackKeys = new java.util.HashMap<>();
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
    // debounce flag for validation thread creation
    private volatile boolean validationScheduled = false;
    // track last-request ms (optional)
    @Getter
    @Setter
    private volatile long lastValidationRequestMs = 0L;
    // track last activity time per active recipe to detect stalls
    private final Map<ResourceLocation, Long> activeRecipeLastUpdate = new HashMap<>();
    // Spread-out recipe scanning to avoid checking all recipes every tick when controller is searching
    private List<RecipeModel> cachedStructureRecipes = null;
    private int nextRecipeCheckIndex = 0;
    private ResourceLocation lastStartedRecipeId = null;
    private ResourceLocation lastStartedInputItemId = null;
    private long recipeSelectionSequence = 0L;
    private final Map<ResourceLocation, Long> inputItemLastStartedSequence = new HashMap<>();
    // track last-start sequence per recipe to break ties when multiple recipes
    // compete for the same input item key during round-robin selection
    private final Map<ResourceLocation, Long> recipeLastStartedSequence = new HashMap<>();

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

    private boolean isAllowedByRedstone() {
        if (level == null) return true;
        try {
            if (redstoneMode == RedstoneMode.IGNORED) return true;
            boolean powered = level.hasNeighborSignal(getBlockPos());
            if (redstoneMode == RedstoneMode.WITH_REDSTONE) return powered;
            if (redstoneMode == RedstoneMode.WITHOUT_REDSTONE) return !powered;
        } catch (Throwable ignored) { }
        return true;
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
            portStorages = (structure == null) ? null : structure.getStorages(level, getBlockPos());
        }
        detectExternalStorageChanges();
        long gameTime = (level == null) ? 0L : level.getGameTime();
        if (!storageContentCacheValid) rebuildStorageCacheIfNeeded(gameTime);
        boolean allowed = isAllowedByRedstone();
        if (allowed) processActiveRecipeOutputs();
        if (structure != null && allowed) scanAndStartRecipes(gameTime);
        performRecipeTick();
    }

    // Helper split to reduce runRecipe complexity
    private void detectExternalStorageChanges() {
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
    }

    private void rebuildStorageCacheIfNeeded(long gameTime) {
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
                cachedAvailableItemStackKeys.clear();
                for (ItemPortStorage s : itemStorages) {
                    var handler = s.getHandler();
                    if (handler == null) continue;
                    for (int i = 0; i < handler.getSlots(); i++) {
                        var stack = handler.getStackInSlot(i);
                        int actual = handler.getActualCount(i);
                        if (!stack.isEmpty() && actual > 0) {
                            var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
                            if (key != null) {
                                cachedAvailableItemIds.add(key);
                                // compute NBT fingerprint for this exact stack
                                ResourceLocation composed = key;
                                try {
                                    if (stack.hasTag()) {
                                        String json = io.ticticboom.mods.mm.util.NbtMatchUtils.toJson(stack.getTag()).toString();
                                        String hex = Integer.toHexString(json.hashCode());
                                        String namespaced = key.getNamespace() + ":" + key.getPath() + "__W__" + hex;
                                        var parsed = ResourceLocation.tryParse(namespaced);
                                        if (parsed != null) composed = parsed;
                                    }
                                } catch (Throwable ignored) { }
                                cachedAvailableItemStackKeys.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(composed);
                            }
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

    private void processActiveRecipeOutputs() {
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
        for (ResourceLocation id : toRemove) {
            activeRecipes.remove(id);
            activeRecipeLastUpdate.remove(id);
        }
    }

    private void scanAndStartRecipes(long gameTime) {
        // Spread recipe checks across multiple ticks to avoid scanning all recipes every tick.
        if (cachedStructureRecipes == null) cachedStructureRecipes = new ArrayList<>(MachineRecipeManager.getRecipesByStrucutreId(structure.id()));
        if (cachedStructureRecipes.isEmpty()) return;
        int total = cachedStructureRecipes.size();
        int maxRecipeChecksPerTick = controllerModel.recipeSelectionMode().fairScheduling() ? total : 5;
        int checks = Math.min(maxRecipeChecksPerTick, total);
        int idx = nextRecipeCheckIndex % total;
        int performed = 0;
        RecipeModel deferredRecipe = null;
        ResourceLocation deferredPrimaryInputItemId = null;
        RecipeModel selectedRoundRobinRecipe = null;
        ResourceLocation selectedRoundRobinInputItemId = null;
        long selectedRoundRobinLastUse = Long.MAX_VALUE;
        boolean startedRecipeThisPass = false;
        while (performed < checks) {
            RecipeModel recipe = cachedStructureRecipes.get(idx);
            idx = (idx + 1) % total;
            performed++;

            if (activeRecipes.containsKey(recipe.id())) continue;
            if (recipeNextCheckTime.getOrDefault(recipe.id(), 0L) > gameTime) continue;

            // skip recipe if outputs can't process
            if (!recipe.outputs().canProcess(level, portStorages, new RecipeStateModel())) {
                continue;
            }

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
                    recipeNextCheckTime.put(recipe.id(), gameTime + recipeSkipCooldownTicks);
                    continue;
                }
            }

            if (portStorages != null && storageContentCacheValid) {
                if (!requiredItemIds.isEmpty() && !cachedAvailableItemIds.containsAll(requiredItemIds)) {
                    recipeNextCheckTime.put(recipe.id(), gameTime + recipeSkipCooldownTicks);
                    continue;
                }
                if (!requiredFluidIds.isEmpty() && !cachedAvailableFluidIds.containsAll(requiredFluidIds)) {
                    recipeNextCheckTime.put(recipe.id(), gameTime + recipeSkipCooldownTicks);
                    continue;
                }
                if (needsEnergy && !cachedHasEnergyAvailable) {
                    recipeNextCheckTime.put(recipe.id(), gameTime + recipeSkipCooldownTicks);
                    continue;
                }
                if (needsMana && !cachedHasManaAvailable) {
                    recipeNextCheckTime.put(recipe.id(), gameTime + recipeSkipCooldownTicks);
                    continue;
                }
                if (needsPneumatic && !cachedHasPneumaticAir) {
                    recipeNextCheckTime.put(recipe.id(), gameTime + recipeSkipCooldownTicks);
                    continue;
                }
                if (needsKinetic && !cachedHasKinetic) {
                    recipeNextCheckTime.put(recipe.id(), gameTime + recipeSkipCooldownTicks);
                    continue;
                }
                if (needsMekanismChemical && !cachedHasMekanismChemical) {
                    recipeNextCheckTime.put(recipe.id(), gameTime + recipeSkipCooldownTicks);
                    continue;
                }
                if (!requiredMekanismIds.isEmpty() && !cachedAvailableMekanismIds.containsAll(requiredMekanismIds)) {
                    recipeNextCheckTime.put(recipe.id(), gameTime + recipeSkipCooldownTicks);
                    continue;
                }
            }

            if (!recipe.inputs().canProcess(level, portStorages, new RecipeStateModel())) {
                continue;
            }

            if (canStartRecipeGivenParallelRules(recipe)) {
                ResourceLocation primaryInputItemId = getPrimaryConsumedItemInputId(recipe);
                RecipeSelectionMode selectionMode = controllerModel.recipeSelectionMode();
                if (selectionMode == RecipeSelectionMode.ROUND_ROBIN_INPUT_ITEM && primaryInputItemId != null) {
                    // primaryInputItemId may be a composed key (with __ suffix) or a base id.
                    ResourceLocation baseId = getResourceLocation(primaryInputItemId);
                    // choose least-recently-used available stack key for this recipe's primary base item id
                    var available = cachedAvailableItemStackKeys.get(baseId);
                    if (available != null && !available.isEmpty()) {
                        ResourceLocation bestKey = null;
                        long bestLastUse = Long.MAX_VALUE;
                        for (ResourceLocation candidate : available) {
                            long lastUse = inputItemLastStartedSequence.getOrDefault(candidate, Long.MIN_VALUE);
                            if (bestKey == null || lastUse < bestLastUse) {
                                bestKey = candidate;
                                bestLastUse = lastUse;
                            }
                        }
                        if (bestKey != null) {
                            boolean take = false;
                            if (selectedRoundRobinRecipe == null) take = true;
                            else if (bestLastUse < selectedRoundRobinLastUse) take = true;
                            else if (bestLastUse == selectedRoundRobinLastUse) {
                                long a = recipeLastStartedSequence.getOrDefault(recipe.id(), Long.MIN_VALUE);
                                long b = recipeLastStartedSequence.getOrDefault(selectedRoundRobinRecipe.id(), Long.MIN_VALUE);
                                if (a < b) take = true; // prefer recipe which was started less recently
                            }
                            if (take) {
                                selectedRoundRobinRecipe = recipe;
                                selectedRoundRobinInputItemId = bestKey;
                                selectedRoundRobinLastUse = bestLastUse;
                            }
                        }
                    }
                    else {
                        // if available contains only base ids (no per-stack fingerprint), try to find
                        // matching stacks in the storages for this recipe's primary ingredient and build
                        // per-stack candidate keys (by NBT hash or slot id)
                        if (available == null) continue;
                        if (portStorages != null) {
                            // find the single-item ingredient for this recipe
                            io.ticticboom.mods.mm.port.item.SingleItemPortIngredient singleIng = null;
                            for (var in : recipe.inputs().inputs()) {
                                if (in instanceof ConsumeRecipeIngredientEntry cre) {
                                    var ingr = cre.getIngredient();
                                    if (ingr instanceof io.ticticboom.mods.mm.port.item.SingleItemPortIngredient s) { singleIng = s; break; }
                                }
                            }
                            if (singleIng != null) {
                                var detailed = new java.util.ArrayList<ResourceLocation>();
                                var itemStorages2 = portStorages.getInputStorages(ItemPortStorage.class);
                                for (ItemPortStorage s : itemStorages2) {
                                    var handler = s.getHandler();
                                    if (handler == null) continue;
                                    for (int i = 0; i < handler.getSlots(); i++) {
                                        var stack = handler.getStackInSlot(i);
                                        int actual = handler.getActualCount(i);
                                        if (stack.isEmpty() || actual <= 0) continue;
                                        var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
                                        if (key == null) continue;
                                        if (!key.equals(baseId)) continue;
                                        // check NBT match according to ingredient
                                        net.minecraft.nbt.CompoundTag req = singleIng.getRequiredNbt();
                                        net.minecraft.nbt.CompoundTag stackTag = stack.getTag();
                                        boolean matches;
                                        if (req == null) {
                                            matches = true;
                                        } else {
                                            if (singleIng.isNbtStrong()) {
                                                matches = (stackTag != null && stackTag.equals(req));
                                            } else {
                                                matches = io.ticticboom.mods.mm.util.NbtMatchUtils.matchesWeak(req, stackTag);
                                            }
                                        }
                                        if (!matches) continue;
                                        // compose per-stack key
                                        ResourceLocation composed = key;
                                        try {
                                            if (stackTag != null) {
                                                String json = io.ticticboom.mods.mm.util.NbtMatchUtils.toJson(stackTag).toString();
                                                String hex = Integer.toHexString(json.hashCode());
                                                String namespaced = key.getNamespace() + ":" + key.getPath() + "__W__" + hex;
                                                var parsed = ResourceLocation.tryParse(namespaced);
                                                if (parsed != null) composed = parsed;
                                            } else {
                                                // use storage uid and slot index to make a distinct key per slot
                                                String sid = s.getStorageUid().toString().replace(':', '_').replace('/', '_');
                                                String namespaced = key.getNamespace() + ":" + key.getPath() + "__slot_" + sid + "_" + i;
                                                var parsed = ResourceLocation.tryParse(namespaced);
                                                if (parsed != null) composed = parsed;
                                            }
                                        } catch (Throwable ignored) { }
                                        detailed.add(composed);
                                    }
                                }
                                if (!detailed.isEmpty()) {
                                    ResourceLocation bestKey2 = null;
                                    long bestLastUse2 = Long.MAX_VALUE;
                                    for (ResourceLocation candidate : detailed) {
                                        long lastUse = inputItemLastStartedSequence.getOrDefault(candidate, Long.MIN_VALUE);
                                        if (bestKey2 == null || lastUse < bestLastUse2) {
                                            bestKey2 = candidate;
                                            bestLastUse2 = lastUse;
                                        }
                                    }
                                    if (bestKey2 != null) {
                                        boolean take2 = false;
                                        if (selectedRoundRobinRecipe == null) take2 = true;
                                        else if (bestLastUse2 < selectedRoundRobinLastUse) take2 = true;
                                        else if (bestLastUse2 == selectedRoundRobinLastUse) {
                                            long a2 = recipeLastStartedSequence.getOrDefault(recipe.id(), Long.MIN_VALUE);
                                            long b2 = recipeLastStartedSequence.getOrDefault(selectedRoundRobinRecipe.id(), Long.MIN_VALUE);
                                            if (a2 < b2) take2 = true;
                                        }
                                        if (take2) {
                                            selectedRoundRobinRecipe = recipe;
                                            selectedRoundRobinInputItemId = bestKey2;
                                            selectedRoundRobinLastUse = bestLastUse2;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    continue;
                }
                if (shouldDeferRecipeBySelectionMode(recipe, primaryInputItemId)) {
                    if (deferredRecipe == null) {
                        deferredRecipe = recipe;
                        deferredPrimaryInputItemId = primaryInputItemId;
                    }
                    continue;
                }
                startRecipe(recipe, gameTime, primaryInputItemId);
                startedRecipeThisPass = true;
            }
        }
        if (!startedRecipeThisPass && selectedRoundRobinRecipe != null
                && !activeRecipes.containsKey(selectedRoundRobinRecipe.id())
                && canStartRecipeGivenParallelRules(selectedRoundRobinRecipe)
                && selectedRoundRobinRecipe.inputs().canProcess(level, portStorages, new RecipeStateModel())
                && selectedRoundRobinRecipe.outputs().canProcess(level, portStorages, new RecipeStateModel())) {
            startRecipe(selectedRoundRobinRecipe, gameTime, selectedRoundRobinInputItemId);
            startedRecipeThisPass = true;
        }
        if (!startedRecipeThisPass && deferredRecipe != null && !activeRecipes.containsKey(deferredRecipe.id())
                && canStartRecipeGivenParallelRules(deferredRecipe)
                && deferredRecipe.inputs().canProcess(level, portStorages, new RecipeStateModel())
                && deferredRecipe.outputs().canProcess(level, portStorages, new RecipeStateModel())) {
            startRecipe(deferredRecipe, gameTime, deferredPrimaryInputItemId);
        }
        nextRecipeCheckIndex = idx;
    }

    private static @Nullable ResourceLocation getResourceLocation(ResourceLocation primaryInputItemId) {
        ResourceLocation baseId = primaryInputItemId;
        try {
            String path = primaryInputItemId.getPath();
            int idxSep = path.indexOf("__");
            if (idxSep > 0) {
                baseId = ResourceLocation.tryParse(primaryInputItemId.getNamespace() + ":" + path.substring(0, idxSep));
            }
        } catch (Throwable ignored) { }
        return baseId;
    }

    private boolean canStartRecipeGivenParallelRules(RecipeModel recipe) {
        boolean allowParallel = recipe.parallelProcessing();
        if (recipe.parallelProcessing() == MMConfig.PARALLEL_PROCESSING_DEFAULT) {
            allowParallel = controllerModel.parallelProcessingDefault();
        }
        int controllerLimit = controllerModel.maxParallelRecipes();
        if (structure != null) {
            int structLimit = structure.maxParallelRecipes();
            if (structLimit >= 0) controllerLimit = structLimit;
        }
        if (controllerLimit < 0) controllerLimit = MMConfig.MAX_PARALLEL_RECIPES;
        boolean canStartBasedOnParallelFlag = allowParallel || activeRecipes.isEmpty();
        boolean canStartBasedOnLimit = controllerLimit == 0
                ? activeRecipes.isEmpty()
                : activeRecipes.size() < controllerLimit;
        return canStartBasedOnParallelFlag && canStartBasedOnLimit;
    }

    private void startRecipe(RecipeModel recipe, long gameTime, @Nullable ResourceLocation primaryInputItemId) {
        RecipeStateModel newState = new RecipeStateModel();
        recipe.inputs().process(level, portStorages, newState);
        storageContentCacheValid = false;
        newState.setCanProcess(true);
        activeRecipes.put(recipe.id(), newState);
        activeRecipeLastUpdate.put(recipe.id(), gameTime);
        lastStartedRecipeId = recipe.id();
        recipeSelectionSequence++;
        // record recipe start sequence for tie-breaking among recipes sharing input keys
        recipeLastStartedSequence.put(recipe.id(), recipeSelectionSequence);
        if (primaryInputItemId != null) {
            lastStartedInputItemId = primaryInputItemId;
            inputItemLastStartedSequence.put(primaryInputItemId, recipeSelectionSequence);
        }
        setChanged();
    }

    private boolean shouldDeferRecipeBySelectionMode(RecipeModel recipe, @SuppressWarnings("unused") @Nullable ResourceLocation primaryInputItemId) {
        RecipeSelectionMode mode = controllerModel.recipeSelectionMode();
        if (mode == RecipeSelectionMode.AVOID_SAME_RECIPE) {
            return lastStartedRecipeId != null && lastStartedRecipeId.equals(recipe.id());
        }
        // ROUND_ROBIN_INPUT_ITEM is handled in the recipe scan by choosing the
        // least-recently-used consumed item input among all currently runnable recipes.
        return false;
    }

    @Nullable
    private ResourceLocation getPrimaryConsumedItemInputId(RecipeModel recipe) {
        for (var input : recipe.inputs().inputs()) {
            if (input instanceof ConsumeRecipeIngredientEntry cre) {
                var ingr = cre.getIngredient();
                if (ingr instanceof io.ticticboom.mods.mm.port.item.SingleItemPortIngredient single) {
                    try {
                        ResourceLocation id = single.getItemId();
                        if (id == null) continue;
                        // include required NBT fingerprint in the selection key so different NBT variants
                        // of the same item id are considered distinct for round-robin scheduling
                        try {
                            net.minecraft.nbt.CompoundTag req = single.getRequiredNbt();
                            if (req != null) {
                                // include nbtStrong bit and compute a short hex hash of the NBT JSON to append
                                String json = io.ticticboom.mods.mm.util.NbtMatchUtils.toJson(req).toString();
                                String namespaced = getString(single, json, id);
                                ResourceLocation composed = ResourceLocation.tryParse(namespaced);
                                if (composed != null) return composed;
                            }
                            return id;
                        } catch (Throwable ignored) { return id; }
                    } catch (Throwable ignored) { }
                }
            }
        }
        return null;
    }

    private static @NotNull String getString(SingleItemPortIngredient single, String json, ResourceLocation id) {
        String hex = Integer.toHexString(json.hashCode());
        boolean strong;
        strong = single.isNbtStrong();
        String suffix = (strong ? "S" : "W") + "__" + hex;
        return id.getNamespace() + ":" + id.getPath() + suffix;
    }

    private void performRecipeTick() {
        long gameTime = (level == null) ? 0L : level.getGameTime();
        // stall timeout in ticks: if a recipe hasn't updated for this many ticks, ditch it
        int recipeStallTimeoutTicks = 200;
        boolean allowed = isAllowedByRedstone();
        if (!allowed) {
            for (ResourceLocation rid : activeRecipes.keySet()) {
                activeRecipeLastUpdate.put(rid, gameTime);
            }
            if (!activeRecipes.isEmpty()) {
                currentRecipe = MachineRecipeManager.RECIPES.get(activeRecipes.keySet().iterator().next());
            } else {
                currentRecipe = null;
            }
            return;
        }
        List<ResourceLocation> toRemove = new ArrayList<>();
        for (Map.Entry<ResourceLocation, RecipeStateModel> entry : activeRecipes.entrySet()) {
            ResourceLocation recipeId = entry.getKey();
            RecipeStateModel state = entry.getValue();
            RecipeModel recipe = MachineRecipeManager.RECIPES.get(recipeId);
            // check for stalled recipe
            long last = activeRecipeLastUpdate.getOrDefault(recipeId, gameTime);
            if (gameTime - last > recipeStallTimeoutTicks) {
                // give up on this recipe, return inputs where possible
                try {
                    if (recipe != null && portStorages != null) {
                        recipe.ditchRecipe(level, state, portStorages);
                    }
                } catch (Throwable ignored) { }
                toRemove.add(recipeId);
                continue;
            }
            if (recipe != null) {
                int prevProgress = state.getTickProgress();
                // First process per-tick inputs (e.g. energy consumed per tick)
                try {
                    // We need custom handling for per-tick energy ingredients so that
                    // when the recipe's ingredient 'amount' represents total energy,
                    // it is distributed across recipe.ticks(). For all other inputs
                    // or non-energy per-tick inputs, delegate to the ingredient entry.
                    for (var inputEntry : recipe.inputs().inputs()) {
                        try {
                            if (inputEntry instanceof ConsumeRecipeIngredientEntry cre) {
                                var ingr = cre.getIngredient();
                                if (cre.isPerTick() && ingr instanceof EnergyPortIngredient epi) {
                                    int total = epi.getAmount();
                                    int ticks = Math.max(1, recipe.ticks());
                                    int base = total / ticks;
                                    int rem = total % ticks;
                                    int tickIndex = state.getTickProgress();
                                    int toExtract = base + ((tickIndex == ticks - 1) ? rem : 0);
                                    if (toExtract > 0 && portStorages != null) {
                                        int remaining = toExtract;
                                        var inputStorages = portStorages.getInputStorages(EnergyPortStorage.class);
                                        for (EnergyPortStorage storage : inputStorages) {
                                            var extracted = storage.internalExtract(remaining, false);
                                            remaining -= extracted;
                                            if (extracted > 0) storageContentCacheValid = false;
                                            if (remaining <= 0) break;
                                        }
                                    }
                                    continue;
                                }
                            }
                            // default processing for other ingredient types
                            inputEntry.processTick(level, portStorages, state);
                        } catch (Throwable ignoredInner) { }
                    }
                    // input tick processing may have modified storages; invalidate cached view so next tick rebuilds
                    storageContentCacheValid = false;
                } catch (Throwable ignored) { }

                // Then process per-tick outputs
                recipe.outputs().processTick(level, portStorages, state);
                // outputs tick may have modified storages; invalidate cached view so next tick rebuilds
                storageContentCacheValid = false;
                if (!state.isCanFinish()) state.proceedTick();
                state.setTickPercentage(((double) state.getTickProgress() / recipe.ticks()) * 100);
                boolean progressed = state.getTickProgress() != prevProgress;
                if (state.getTickProgress() >= recipe.ticks()) {
                    state.setCanFinish(true);
                    boolean canOutputs = recipe.outputs().canProcess(level, portStorages, state);
                    if (canOutputs) {
                        recipe.outputs().process(level, portStorages, state);
                        toRemove.add(recipeId);
                        // outputs processed - storages changed
                        storageContentCacheValid = false;
                        progressed = true;
                    }
                }
                if (progressed) {
                    activeRecipeLastUpdate.put(recipeId, gameTime);
                }
            }
        }
        for (ResourceLocation id : toRemove) {
            activeRecipes.remove(id);
            activeRecipeLastUpdate.remove(id);
        }

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
        cachedAvailableItemStackKeys.clear();
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
        if (lastStartedRecipeId != null) tag.putString("lastStartedRecipeId", lastStartedRecipeId.toString());
        if (lastStartedInputItemId != null) tag.putString("lastStartedInputItemId", lastStartedInputItemId.toString());
        tag.putLong("recipeSelectionSequence", recipeSelectionSequence);
        if (!inputItemLastStartedSequence.isEmpty()) {
            CompoundTag inputHistoryTag = new CompoundTag();
            for (Map.Entry<ResourceLocation, Long> entry : inputItemLastStartedSequence.entrySet()) {
                inputHistoryTag.putLong(entry.getKey().toString(), entry.getValue());
            }
            tag.put("inputItemLastStartedSequence", inputHistoryTag);
        }
        tag.putBoolean("filler", true);
        // persist redstone mode
        try {
            tag.putInt("redstoneMode", redstoneMode.ordinal());
        } catch (Throwable ignored) { }
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
        lastStartedRecipeId = tag.contains("lastStartedRecipeId") ? ResourceLocation.tryParse(tag.getString("lastStartedRecipeId")) : null;
        lastStartedInputItemId = tag.contains("lastStartedInputItemId") ? ResourceLocation.tryParse(tag.getString("lastStartedInputItemId")) : null;
        recipeSelectionSequence = tag.contains("recipeSelectionSequence") ? tag.getLong("recipeSelectionSequence") : 0L;
        inputItemLastStartedSequence.clear();
        if (tag.contains("inputItemLastStartedSequence")) {
            CompoundTag inputHistoryTag = tag.getCompound("inputItemLastStartedSequence");
            for (String key : inputHistoryTag.getAllKeys()) {
                ResourceLocation itemId = ResourceLocation.tryParse(key);
                if (itemId != null) {
                    inputItemLastStartedSequence.put(itemId, inputHistoryTag.getLong(key));
                }
            }
        }
        if (!activeRecipes.isEmpty()) {
            currentRecipe = MachineRecipeManager.RECIPES.get(activeRecipes.keySet().iterator().next());
        } else {
            currentRecipe = null;
        }
        // load redstone mode
        try {
            if (tag.contains("redstoneMode")) {
                int ord = tag.getInt("redstoneMode");
                RedstoneMode[] vals = RedstoneMode.values();
                if (ord >= 0 && ord < vals.length) redstoneMode = vals[ord];
            } else {
                redstoneMode = RedstoneMode.IGNORED;
            }
        } catch (Throwable ignored) { redstoneMode = RedstoneMode.IGNORED; }
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

    // Redstone mode accessors (ordinal used for network/GUI)
    public int getRedstoneModeOrdinal() {
        return redstoneMode.ordinal();
    }

    public void setRedstoneModeOrdinal(int ord) {
        try {
            RedstoneMode[] vals = RedstoneMode.values();
            if (ord >= 0 && ord < vals.length) {
                this.redstoneMode = vals[ord];
                setChanged();
                if (level != null) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        } catch (Throwable ignored) { }
    }

    public String getRedstoneModeName() {
        try { return redstoneMode.name(); } catch (Throwable ignored) { return "IGNORED"; }
    }

    @Override
    public void onLoad() {
        if (level != null && !level.isClientSide() && level.getServer() != null) {
            MinecraftServer server = level.getServer();
            server.execute(() -> {
                try {
                    validateStructure(level);
                    setChanged();
                    assert level != null;
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

        // debounce creation of the delayed validation thread so we don't spawn many threads
        synchronized (this) {
            lastValidationRequestMs = System.currentTimeMillis();
            if (validationScheduled) return;
            validationScheduled = true;
        }
        Thread t = getThread();
        t.start();
    }

    private @NotNull Thread getThread() {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
                synchronized (MachineControllerBlockEntity.this) { validationScheduled = false; }
                return;
            }
            if (level == null || level.isClientSide() || level.getServer() == null) {
                synchronized (MachineControllerBlockEntity.this) { validationScheduled = false; }
                return;
            }
            MinecraftServer server = level.getServer();
            try {
                server.execute(() -> {
                    try {
                        validateStructure(level);
                        setChanged();
                        if (level != null) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
                    } catch (Throwable t2) {
                        try { Ref.LOG.error("Error during delayed validation for controller at {}: {}", getBlockPos(), t2.toString()); }
                        catch (Throwable ignored) { }
                    }
                });
            } finally {
                synchronized (MachineControllerBlockEntity.this) { validationScheduled = false; }
            }
        }, "mm-controller-validate-delayed");
        t.setDaemon(true);
        return t;
    }
}
