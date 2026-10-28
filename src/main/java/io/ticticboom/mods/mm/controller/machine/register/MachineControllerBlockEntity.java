package io.ticticboom.mods.mm.controller.machine.register;

import io.ticticboom.mods.mm.util.LegacyNbt;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.TreeMap;
import io.ticticboom.mods.mm.builder.TierPrefs;
import io.ticticboom.mods.mm.builder.PortTiers;
import net.minecraft.server.level.ServerLevel;
import io.ticticboom.mods.mm.networklink.NetworkLink;
import io.ticticboom.mods.mm.networklink.LinkData;
import io.ticticboom.mods.mm.compat.interop.MMInteropManager;
import io.ticticboom.mods.mm.recipe.condition.RecipeConditionContext;
import io.ticticboom.mods.mm.util.ColorUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import io.ticticboom.mods.mm.structure.StructureDiagnosis;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.compat.oritech.BlackHoleShield;
import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.controller.IControllerBlockEntity;
import io.ticticboom.mods.mm.controller.IControllerPart;
import io.ticticboom.mods.mm.model.ControllerModel;
import io.ticticboom.mods.mm.model.RecipeSelectionMode;
import io.ticticboom.mods.mm.port.IControllerAwareStorage;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.IRecipeDemandListener;
import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.port.ITickSpreadIngredient;
import io.ticticboom.mods.mm.port.energy.EnergyPortStorage;
import io.ticticboom.mods.mm.port.entity.EntityPortStorage;
import io.ticticboom.mods.mm.port.entity.EntityPortStorageModel;
import io.ticticboom.mods.mm.port.fluid.FluidPortStorage;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.recipe.MachineRecipeManager;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.recipe.input.consume.ConsumeRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.input.flex.FlexRecipeIngredientEntry;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.structure.attachment.visuals.FluidVolumeState;
import io.ticticboom.mods.mm.structure.attachment.visuals.VisualsStructureAttachment;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.pattern.PatternBuffer;
import io.ticticboom.mods.mm.pattern.PatternReturn;
import io.ticticboom.mods.mm.pattern.PatternRun;
import io.ticticboom.mods.mm.pattern.PatternStartResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import io.ticticboom.mods.mm.client.geo.GeoModelConfig;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.ticticboom.mods.mm.config.MMConfigSetup.COMMON;

public class MachineControllerBlockEntity extends BlockEntity implements IControllerBlockEntity, IControllerPart {

    private static final long REQUEST_TIMEOUT = 40;
    private static final int SYNC_INTERVAL = 5;
    private static final int IDLE_SCAN_INTERVAL = 5;
    private static final int SKIP_COOLDOWN = 20;
    private static final int OUTPUT_BLOCKED_COOLDOWN = 100;
    private static final int SEARCH_CHECKS_PER_TICK = 5;
    private static final int RECENT_RECIPE_TICKS = 20;
    public static final int MAX_NAME_LENGTH = 50;

    private enum RedstoneMode { IGNORED, WITH_REDSTONE, WITHOUT_REDSTONE }

    private record Candidate(RecipeModel recipe, @Nullable Identifier inputKey, RecipeStateModel state, long lastUse) {
    }

    private final ControllerModel model;
    private final RegistryGroupHolder groupHolder;
    private final Identifier controllerId;
    private final int validationOffset;

    @Getter
    private StructureModel structure = null;
    private Rotation formedRotation = null;
    private int[] structureChunks = null;
    private StructureModel structureChunksOf = null;
    private Rotation structureChunksRotation = null;
    private boolean isFormed = false;
    private boolean pendingValidation = true;
    private long lastTick = Long.MIN_VALUE;

    private final Map<Identifier, RecipeStateModel> activeRecipes = new HashMap<>();
    // crafts pushed by AE2 into a Pattern Port; they run next to activeRecipes, each with its own buffer
    private final List<PatternRun> patternRuns = new ArrayList<>();
    @Getter
    private RecipeModel currentRecipe;
    private RecipeStorages portStorages = null;
    private List<EntityPortStorage> speedStorages = List.of();
    private RedstoneMode redstoneMode = RedstoneMode.IGNORED;
    private final Set<Identifier> failedRecipes = new HashSet<>();

    private final Set<Identifier> cachedItemIds = new HashSet<>();
    private final Set<Identifier> cachedFluidIds = new HashSet<>();
    private boolean cachedHasEnergy = false;
    private boolean storageContentCacheValid = false;
    private long lastResourceScanTime = -1;
    private long lastStorageChangeCount = -1;
    private final Map<Identifier, Long> recipeNextCheckTime = new HashMap<>();

    private List<RecipeModel> cachedStructureRecipes = null;
    private int cachedRecipeVersion = -1;
    private RecipeCandidateIndex recipeIndex = null;
    // positions in cachedStructureRecipes of the recipes the cached port contents might run; null = rebuild
    private int[] candidatePositions = null;
    private int nextRecipeCheckIndex = 0;
    private Identifier lastStartedRecipeId = null;
    private long lastRecipeStartTime = Long.MIN_VALUE;
    private long lastProgressTime = Long.MIN_VALUE;
    private RecipeSelectionMode recipeModeOverride = null;
    private String customName = null;
    private long recipeSelectionSequence = 0L;
    private final Map<Identifier, Long> inputItemLastStartedSequence = new HashMap<>();
    private final Map<Identifier, Long> recipeLastStartedSequence = new HashMap<>();

    @Getter
    private Identifier selectedRecipeId = null;
    private Identifier requestedRecipeId = null;
    private Object requestOwner = null;
    private long requestExpiresAt = 0L;
    private int requestedCrafts = 0;
    private int requestStarts = 0;
    private Object reservationOwner = null;
    private ItemStack reservedOutput = ItemStack.EMPTY;
    private int reservedCount = 0;
    private long reservationExpiresAt = 0L;

    private boolean syncPending = false;
    private RecipeStorages linkedStorages = null;
    private ControllerState linkedState = null;
    private int comparatorSignal = 0;
    private boolean soundMuted = false;
    // set from the controller screen: the machine's GeckoLib model is animated, stands still or isn't drawn
    @Getter
    private ModelDisplay modelDisplay = ModelDisplay.ON;
    private LinkData networkLink = null;
    private boolean linkExportRequested = false;
    private long lastLinkExport = -1L;
    private final TierPrefs assemblyTiers = new TierPrefs();
    private Identifier assemblyStructureId = null;
    private boolean wasActive = false;
    private long lastSync = Long.MIN_VALUE / 2;
    @Getter
    private List<FluidVolumeState> fluidVolumes = List.of();
    @Getter
    private AABB visualBounds = null;
    private RecipeStorages visualStorages = null;
    private long lastVisualChangeCount = -1;
    @Nullable
    private Object geoAnimatable;

    public MachineControllerBlockEntity(ControllerModel model, RegistryGroupHolder groupHolder, BlockPos pos, BlockState state) {
        super(groupHolder.getBe().get(), pos, state);
        this.model = model;
        this.groupHolder = groupHolder;
        this.controllerId = Ref.id(model.id());
        this.validationOffset = Math.floorMod(pos.hashCode(), 100);
    }

    public void tick() {
        if (level == null || isRemoved()) {
            return;
        }
        if (level.isClientSide()) {
            if (getBlockState().getBlock() instanceof MachineControllerBlock block) {
                block.tickWorkingEffects(getBlockState(), level, getBlockPos(), soundMuted);
            }
            return;
        }
        long gameTime = level.getGameTime();
        if (gameTime == lastTick) {
            return;
        }
        lastTick = gameTime;
        drainPatternRuns();
        boolean loaded = !isFormed || structureChunksLoaded();
        if (loaded && (pendingValidation || (gameTime + validationOffset) % COMMON.structureValidationRate.get() == 0)) {
            pendingValidation = false;
            validateStructure();
        }
        if (isFormed && loaded) {
            if (portsReplaced()) {
                formed(formedRotation);
            }
            runRecipe(gameTime);
        }
        updateVisuals();
        updateControllerState();
        NetworkLink.tickController((ServerLevel) level, this);
        boolean active = !activeRecipes.isEmpty() || !patternRuns.isEmpty();
        if (active || wasActive) {
            markChanged();
        }
        wasActive = active;
        if (syncPending && gameTime - lastSync >= SYNC_INTERVAL) {
            syncPending = false;
            lastSync = gameTime;
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void updateVisuals() {
        var visuals = structure == null ? null : structure.getAttachments().get(Ref.StructureAttachments.VISUALS, VisualsStructureAttachment.class);
        if (!isFormed || portStorages == null || formedRotation == null || visuals == null) {
            visualStorages = null;
            if (!fluidVolumes.isEmpty()) {
                fluidVolumes = List.of();
                markChanged();
            }
            return;
        }
        long changeCount = portStorages.changeCount();
        if (portStorages == visualStorages && changeCount == lastVisualChangeCount) {
            return;
        }
        visualStorages = portStorages;
        lastVisualChangeCount = changeCount;
        var next = visuals.snapshot(level, getBlockPos(), structure, formedRotation, portStorages);
        if (!FluidVolumeState.same(fluidVolumes, next)) {
            fluidVolumes = next;
            markChanged();
        }
    }

    private void loadFluidVolumes(CompoundTag tag, HolderLookup.Provider registries) {
        fluidVolumes = tag.contains("FluidVolumes") ? FluidVolumeState.loadAll(tag.getListOrEmpty("FluidVolumes"), registries) : List.of();
        AABB bounds = null;
        for (FluidVolumeState volume : fluidVolumes) {
            if (volume.getBounds() != null) {
                var box = volume.getBounds().move(getBlockPos());
                bounds = bounds == null ? box : bounds.minmax(box);
            }
        }
        visualBounds = bounds == null ? null : bounds.minmax(new AABB(getBlockPos()));
    }

    public void scheduleValidation() {
        pendingValidation = true;
    }

    protected void markChanged() {
        setChanged();
        syncPending = true;
    }

    private void validateStructure() {
        var pos = getBlockPos();
        if (structure != null) {
            var rotation = structure.formedRotation(level, pos, formedRotation);
            if (rotation != null) {
                formed(rotation);
                return;
            }
        }
        for (StructureModel candidate : StructureManager.getStructuresForController(controllerId)) {
            if (candidate == structure) {
                continue;
            }
            var rotation = candidate.formedRotation(level, pos, null);
            if (rotation != null) {
                switchStructure(candidate);
                formed(rotation);
                return;
            }
        }
        unformed();
    }

    private void formed(Rotation rotation) {
        formedRotation = rotation;
        BlackHoleShield.shield(level, getBlockPos(), structure, rotation);
        portStorages = collectStorages(rotation);
        speedStorages = resolveSpeedStorages();
        if (structure != structureChunksOf || rotation != structureChunksRotation) {
            structureChunks = structure.layout().chunkBounds(getBlockPos(), rotation);
            structureChunksOf = structure;
            structureChunksRotation = rotation;
        }
        if (!isFormed) {
            isFormed = true;
            markChanged();
        }
    }

    private boolean structureChunksLoaded() {
        if (structureChunks == null) {
            return true;
        }
        for (int x = structureChunks[0]; x <= structureChunks[2]; x++) {
            for (int z = structureChunks[1]; z <= structureChunks[3]; z++) {
                if (!level.hasChunk(x, z)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean portsReplaced() {
        if (portStorages == null || formedRotation == null) {
            return false;
        }
        for (var source : portStorages.sources()) {
            if (source instanceof BlockEntity port && port.isRemoved()) {
                return true;
            }
        }
        return false;
    }

    protected RecipeStorages collectStorages(Rotation rotation) {
        return structure.getStorages(level, getBlockPos(), rotation);
    }

    private void unformed() {
        BlackHoleShield.unshield(level, getBlockPos());
        portStorages = null;
        speedStorages = List.of();
        if (isFormed) {
            isFormed = false;
            markChanged();
        }
    }

    private void updateControllerState() {
        var blockState = getBlockState();
        ControllerState next;
        if (!isFormed || portStorages == null) {
            next = ControllerState.UNFORMED;
        } else if (isAllowedByRedstone() && isWorking()) {
            next = ControllerState.WORKING;
        } else {
            next = ControllerState.IDLE;
        }
        if (blockState.hasProperty(ControllerState.PROPERTY) && blockState.getValue(ControllerState.PROPERTY) != next) {
            level.setBlock(getBlockPos(), blockState.setValue(ControllerState.PROPERTY, next), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        int signal = computeComparatorSignal(next);
        if (signal != comparatorSignal) {
            comparatorSignal = signal;
            level.updateNeighbourForOutputSignal(getBlockPos(), blockState.getBlock());
        }
        if (next != linkedState || portStorages != linkedStorages) {
            linkPorts(linkedStorages, portStorages, next);
            linkedStorages = portStorages;
            linkedState = next;
        }
    }

    private int computeComparatorSignal(ControllerState state) {
        if (state != ControllerState.WORKING) {
            return 0;
        }
        double percent = 100;
        if (!activeRecipes.isEmpty()) {
            percent = 0;
            for (var recipeState : activeRecipes.values()) {
                percent = Math.max(percent, recipeState.getTickPercentage());
            }
        }
        return 1 + (int) Math.round(Math.max(0, Math.min(100, percent)) / 100 * 14);
    }

    public int getComparatorSignal() {
        return comparatorSignal;
    }

    private void linkPorts(@Nullable RecipeStorages previous, @Nullable RecipeStorages current, ControllerState state) {
        if (previous != null && previous != current) {
            Set<IPortBlockEntity> kept = Collections.newSetFromMap(new IdentityHashMap<>());
            if (current != null) {
                kept.addAll(current.sources());
            }
            for (var source : previous.sources()) {
                if (source instanceof AbstractPortBlockEntity port && !kept.contains(source) && !port.isRemoved()) {
                    port.setMachineInfo(null, null, ControllerState.UNFORMED, null);
                }
            }
        }
        if (current != null) {
            Direction front = getBlockState().getValue(HorizontalDirectionalBlock.FACING).getOpposite();
            int[] colors = machineColors();
            for (var source : current.sources()) {
                if (source instanceof AbstractPortBlockEntity port) {
                    port.setMachineInfo(getBlockPos(), front, state, colors);
                }
            }
        }
    }

    private @Nullable int[] machineColors() {
        int[] colors = new int[ControllerState.values().length];
        boolean any = false;
        for (ControllerState state : ControllerState.values()) {
            Integer color = ColorUtil.parse(model.screenColor(state.getSerializedName()));
            colors[state.ordinal()] = color == null ? -1 : color;
            any |= color != null;
        }
        return any ? colors : null;
    }

    private void switchStructure(StructureModel next) {
        activeRecipes.keySet().removeIf(id -> {
            var recipe = MachineRecipeManager.RECIPES.get(id);
            return recipe == null || !recipe.runsIn(next.id());
        });
        structure = next;
        formedRotation = null;
        cachedStructureRecipes = null;
        storageContentCacheValid = false;
        recipeNextCheckTime.clear();
        updateCurrentRecipe();
        markChanged();
    }

    public boolean isAllowedByRedstone() {
        return switch (redstoneMode) {
            case IGNORED -> true;
            case WITH_REDSTONE -> level.hasNeighborSignal(getBlockPos());
            case WITHOUT_REDSTONE -> !level.hasNeighborSignal(getBlockPos());
        };
    }

    private void runRecipe(long gameTime) {
        if (portStorages == null) {
            return;
        }
        long changeCount = portStorages.changeCount();
        if (changeCount != lastStorageChangeCount) {
            lastStorageChangeCount = changeCount;
            storageContentCacheValid = false;
            recipeNextCheckTime.clear();
        }
        attachStorages(gameTime);
        broadcastDemand(gameTime);
        if (!storageContentCacheValid) {
            rebuildStorageCache(gameTime);
        }
        boolean allowed = isAllowedByRedstone();
        if (allowed) {
            processActiveRecipeOutputs();
            scanAndStartRecipes(gameTime);
        }
        performRecipeTick(gameTime, allowed);
    }

    private void attachStorages(long gameTime) {
        for (var storage : portStorages.inputStorages()) {
            if (storage instanceof IControllerAwareStorage aware) {
                aware.attachController(this, gameTime);
            }
        }
        for (var storage : portStorages.outputStorages()) {
            if (storage instanceof IControllerAwareStorage aware) {
                aware.attachController(this, gameTime);
            }
        }
    }

    private void broadcastDemand(long gameTime) {
        var requested = activeRequest(gameTime);
        if (requested == null && !isManualSelection()) {
            return;
        }
        var demandedId = requested != null ? requested : selectedRecipeId;
        var demanded = demandedId == null ? null : MachineRecipeManager.RECIPES.get(demandedId);
        for (var storage : portStorages.inputStorages()) {
            if (storage instanceof IRecipeDemandListener listener) {
                listener.setRecipeDemand(gameTime, demanded);
            }
        }
    }

    private void rebuildStorageCache(long gameTime) {
        if (activeRecipes.isEmpty() && lastResourceScanTime >= 0 && gameTime - lastResourceScanTime < IDLE_SCAN_INTERVAL) {
            return;
        }
        lastResourceScanTime = gameTime;
        // the candidate list only depends on which ids are present: keep it while they stay the same
        var previousItemIds = Set.copyOf(cachedItemIds);
        var previousFluidIds = Set.copyOf(cachedFluidIds);
        boolean previousHasEnergy = cachedHasEnergy;
        cachedItemIds.clear();
        cachedFluidIds.clear();
        cachedHasEnergy = false;
        for (ItemPortStorage storage : portStorages.getInputStorages(ItemPortStorage.class)) {
            var handler = storage.getHandler();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                var stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty() && handler.getActualCount(slot) > 0) {
                    cachedItemIds.add(BuiltInRegistries.ITEM.getKey(stack.getItem()));
                }
            }
        }
        for (FluidPortStorage storage : portStorages.getInputStorages(FluidPortStorage.class)) {
            var handler = storage.getHandler();
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                var fluid = handler.getFluidInTank(tank);
                if (fluid.getAmount() > 0) {
                    cachedFluidIds.add(BuiltInRegistries.FLUID.getKey(fluid.getFluid()));
                }
            }
        }
        for (EnergyPortStorage storage : portStorages.getInputStorages(EnergyPortStorage.class)) {
            if (storage.getStoredEnergy() > 0) {
                cachedHasEnergy = true;
                break;
            }
        }
        if (cachedHasEnergy != previousHasEnergy || !cachedItemIds.equals(previousItemIds) || !cachedFluidIds.equals(previousFluidIds)) {
            candidatePositions = null;
        }
        storageContentCacheValid = true;
        recipeNextCheckTime.clear();
    }

    private void processActiveRecipeOutputs() {
        activeRecipes.entrySet().removeIf(entry -> {
            var recipe = MachineRecipeManager.RECIPES.get(entry.getKey());
            var state = entry.getValue();
            if (recipe == null || !state.isCanFinish() || !recipe.outputs().canProcess(level, portStorages, state)) {
                return false;
            }
            recipe.outputs().process(level, portStorages, state);
            recipeFinished(recipe);
            storageContentCacheValid = false;
            lastProgressTime = level.getGameTime();
            return true;
        });
    }

    private void scanAndStartRecipes(long gameTime) {
        if (cachedStructureRecipes == null || cachedRecipeVersion != MachineRecipeManager.RECIPE_VERSION) {
            cachedStructureRecipes = new ArrayList<>(MachineRecipeManager.getRecipesByStrucutreId(structure.id()));
            cachedRecipeVersion = MachineRecipeManager.RECIPE_VERSION;
            recipeIndex = new RecipeCandidateIndex(cachedStructureRecipes);
            candidatePositions = null;
            failedRecipes.clear();
            recipeNextCheckTime.clear();
        }
        int total = cachedStructureRecipes.size();
        if (total == 0) {
            return;
        }
        // every parallel slot is taken: no recipe could start, so don't check their conditions, inputs and outputs
        if (!hasFreeParallelSlot()) {
            return;
        }
        // only recipes whose inputs could be in the ports are checked; that list changes with the port contents
        int[] candidates = scanCandidates();
        if (candidates.length == 0) {
            return;
        }
        var mode = getRecipeSelectionMode();
        int checks = mode.fairScheduling() ? candidates.length : Math.min(SEARCH_CHECKS_PER_TICK, candidates.length);
        // continue from where the last pass stopped, in the structure's recipe order
        int found = Arrays.binarySearch(candidates, nextRecipeCheckIndex % total);
        int index = found >= 0 ? found : -found - 1;
        int nextPosition = nextRecipeCheckIndex;
        var requested = activeRequest(gameTime);
        Candidate roundRobin = null;
        Candidate deferred = null;
        boolean started = false;
        for (int performed = 0; performed < checks; performed++) {
            int position = candidates[index % candidates.length];
            index = (index + 1) % candidates.length;
            nextPosition = position + 1;
            RecipeModel recipe = cachedStructureRecipes.get(position);
            if (!isEligible(recipe, requested, gameTime)) {
                continue;
            }
            var needs = recipeIndex.requirements(position);
            if (!mayHaveInputs(needs)) {
                recipeNextCheckTime.put(recipe.id(), gameTime + SKIP_COOLDOWN);
                continue;
            }
            var state = newRecipeProbe();
            if (!canStart(recipe, state)) {
                continue;
            }
            // flex recipes: the kind whose turn it is (the flex input already picked kinds in history order)
            var flexPrimary = flexPrimaryKey(recipe, state);
            var primary = flexPrimary != null ? flexPrimary : needs.primaryItem();
            if (mode == RecipeSelectionMode.ROUND_ROBIN_INPUT_ITEM && primary != null) {
                if (flexPrimary != null || cachedItemIds.contains(primary)) {
                    var candidate = new Candidate(recipe, primary, state, inputItemLastStartedSequence.getOrDefault(primary, Long.MIN_VALUE));
                    if (roundRobin == null || prefers(candidate, roundRobin)) {
                        roundRobin = candidate;
                    }
                }
                continue;
            }
            if (mode == RecipeSelectionMode.AVOID_SAME_RECIPE && recipe.id().equals(lastStartedRecipeId)) {
                if (deferred == null) {
                    deferred = new Candidate(recipe, primary, state, 0);
                }
                continue;
            }
            started |= startRecipe(recipe, gameTime, primary, state);
        }
        nextRecipeCheckIndex = nextPosition % total;
        if (!started && roundRobin != null && canStart(roundRobin.recipe(), roundRobin.state())) {
            started = startRecipe(roundRobin.recipe(), gameTime, roundRobin.inputKey(), roundRobin.state());
        }
        if (!started && deferred != null && canStart(deferred.recipe(), deferred.state())) {
            startRecipe(deferred.recipe(), gameTime, deferred.inputKey(), deferred.state());
        }
    }

    /** Positions to scan: every recipe while the port contents aren't known, else the ones they might run. */
    private int[] scanCandidates() {
        if (!storageContentCacheValid) {
            return recipeIndex.all();
        }
        if (candidatePositions == null) {
            candidatePositions = recipeIndex.candidates(cachedItemIds, cachedFluidIds, cachedHasEnergy);
        }
        return candidatePositions;
    }

    /** A state for checking a new run; in Round Robin mode flex inputs take the kinds that waited longest first. */
    private RecipeStateModel newRecipeProbe() {
        var state = new RecipeStateModel();
        if (getRecipeSelectionMode() == RecipeSelectionMode.ROUND_ROBIN_INPUT_ITEM) {
            state.setFlexInputOrder(Comparator.comparingLong(stack ->
                    inputItemLastStartedSequence.getOrDefault(flexInputKey(stack), Long.MIN_VALUE)));
        }
        return state;
    }

    /** The Round Robin key of a flex kind: its item id, plus its components when it has any. */
    private static Identifier flexInputKey(ItemStack stack) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        var components = stack.getComponentsPatch();
        if (components.isEmpty()) return id;
        return Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "__flex_c_" + Integer.toHexString(components.hashCode()));
    }

    /** The kind, of those the flex inputs took into this state, that waited longest; null without flex kinds. */
    @Nullable
    private Identifier flexPrimaryKey(RecipeModel recipe, RecipeStateModel state) {
        Identifier key = null;
        long oldest = Long.MAX_VALUE;
        for (var input : recipe.inputs().inputs()) {
            if (input instanceof FlexRecipeIngredientEntry flex) {
                for (ItemStack kind : state.getCaptured(flex.getName())) {
                    Identifier kindKey = flexInputKey(kind);
                    long lastUse = inputItemLastStartedSequence.getOrDefault(kindKey, Long.MIN_VALUE);
                    if (key == null || lastUse < oldest) {
                        key = kindKey;
                        oldest = lastUse;
                    }
                }
            }
        }
        return key;
    }

    private boolean isEligible(RecipeModel recipe, @Nullable Identifier requested, long gameTime) {
        if (activeRecipes.containsKey(recipe.id())) {
            return false;
        }
        if (requested != null) {
            if (!recipe.id().equals(requested) || requestedCrafts <= 0) {
                return false;
            }
        } else {
            boolean picked = isManualSelection() && recipe.id().equals(selectedRecipeId);
            if ((isManualSelection() || recipe.requestOnly()) && !picked) {
                return false;
            }
        }
        return recipeNextCheckTime.getOrDefault(recipe.id(), 0L) <= gameTime
                && recipe.conditions().canRun(conditionContext());
    }

    private RecipeConditionContext conditionContext() {
        return new RecipeConditionContext(level, getBlockPos(), structure);
    }

    private boolean mayHaveInputs(RecipeRequirements needs) {
        if (!storageContentCacheValid) {
            return true;
        }
        return cachedItemIds.containsAll(needs.itemIds())
                && cachedFluidIds.containsAll(needs.fluidIds())
                && (!needs.energy() || cachedHasEnergy);
    }

    private boolean canStart(RecipeModel recipe, RecipeStateModel state) {
        return !activeRecipes.containsKey(recipe.id())
                && canRunAlongside(recipe)
                && recipe.inputs().canProcess(level, portStorages, state)
                && recipe.outputs().canProcess(level, portStorages, state);
    }

    private boolean prefers(Candidate candidate, Candidate current) {
        if (candidate.lastUse() != current.lastUse()) {
            return candidate.lastUse() < current.lastUse();
        }
        long a = recipeLastStartedSequence.getOrDefault(candidate.recipe().id(), Long.MIN_VALUE);
        long b = recipeLastStartedSequence.getOrDefault(current.recipe().id(), Long.MIN_VALUE);
        return a < b;
    }

    /** Whether some recipe could start next to the running ones, whatever its own parallel setting. */
    private boolean hasFreeParallelSlot() {
        int limit = parallelLimit();
        int running = runningCount();
        return limit == 0 ? running == 0 : running < limit;
    }

    private boolean canRunAlongside(RecipeModel recipe) {
        boolean allowParallel = recipe.parallelProcessing();
        if (recipe.parallelProcessing() == MMConfig.PARALLEL_PROCESSING_DEFAULT) {
            allowParallel = model.parallelProcessingDefault();
        }
        int limit = parallelLimit();
        int running = runningCount();
        boolean underLimit = limit == 0 ? running == 0 : running < limit;
        return (allowParallel || running == 0) && underLimit;
    }

    private boolean startRecipe(RecipeModel recipe, long gameTime, @Nullable Identifier inputKey, RecipeStateModel state) {
        if (MMInteropManager.KUBEJS.isPresent() && !MMInteropManager.KUBEJS.get().postRecipeStarted(this, recipe.id())) {
            recipeNextCheckTime.put(recipe.id(), gameTime + SKIP_COOLDOWN);
            return false;
        }
        recipe.inputs().process(level, portStorages, state);
        if (recipe.id().equals(requestedRecipeId) && requestedCrafts > 0) {
            requestedCrafts--;
            requestStarts++;
        }
        storageContentCacheValid = false;
        state.setCanProcess(true);
        activeRecipes.put(recipe.id(), state);
        lastStartedRecipeId = recipe.id();
        lastRecipeStartTime = gameTime;
        lastProgressTime = gameTime;
        recipeSelectionSequence++;
        recipeLastStartedSequence.put(recipe.id(), recipeSelectionSequence);
        if (inputKey != null) {
            inputItemLastStartedSequence.put(inputKey, recipeSelectionSequence);
        }
        // every kind this flex run took has had its turn, not only the primary one
        for (var input : recipe.inputs().inputs()) {
            if (input instanceof FlexRecipeIngredientEntry flex) {
                for (ItemStack kind : state.getCaptured(flex.getName())) {
                    inputItemLastStartedSequence.put(flexInputKey(kind), recipeSelectionSequence);
                }
            }
        }
        state.setFlexInputOrder(null);
        markChanged();
        return true;
    }

    private void recipeFinished(RecipeModel recipe) {
        linkExportRequested = true;
        MMInteropManager.KUBEJS.ifPresent(kubejs -> kubejs.postRecipeFinished(this, recipe.id()));
    }

    private List<EntityPortStorage> resolveSpeedStorages() {
        if (portStorages == null) {
            return List.of();
        }
        var found = new ArrayList<EntityPortStorage>();
        for (var storage : portStorages.getInputStorages(EntityPortStorage.class)) {
            if (((EntityPortStorageModel) storage.getStorageModel()).speedPerEntity() > 0) {
                found.add(storage);
            }
        }
        return found.isEmpty() ? List.of() : found;
    }

    private double recipeSpeedMultiplier() {
        double multiplier = 1;
        for (var storage : speedStorages) {
            multiplier += storage.speedBonus();
        }
        return multiplier;
    }

    private void performRecipeTick(long gameTime, boolean allowed) {
        if (allowed) {
            activeRecipes.entrySet().removeIf(entry -> {
                var recipe = MachineRecipeManager.RECIPES.get(entry.getKey());
                return recipe == null || tickRecipe(recipe, entry.getValue(), gameTime);
            });
            tickPatternRuns(gameTime);
        }
        updateCurrentRecipe();
    }

    private boolean tickRecipe(RecipeModel recipe, RecipeStateModel state, long gameTime) {
        return tickRecipe(recipe, state, gameTime, portStorages);
    }

    /** One tick of a running recipe against these storages (the ports, or a pattern run's buffer first). */
    private boolean tickRecipe(RecipeModel recipe, RecipeStateModel state, long gameTime, RecipeStorages storages) {
        if (!state.isCanFinish() && !recipe.conditions().isEmpty() && !recipe.conditions().canRun(conditionContext())) {
            return false;
        }
        if (!state.isCanFinish()) {
            boolean fed;
            try {
                // a per-tick output that doesn't fit makes the recipe wait for room instead of throwing it away
                fed = recipe.outputs().canProcessTick(level, storages, state) && processTickInputs(recipe, state, storages);
                if (fed) {
                    recipe.outputs().processTick(level, storages, state);
                }
            } catch (RuntimeException e) {
                if (failedRecipes.add(recipe.id())) {
                    Ref.LOG.error("Recipe {} failed while running in the machine at {}", recipe.id(), getBlockPos(), e);
                }
                fed = false;
            }
            storageContentCacheValid = false;
            if (fed) {
                state.proceedTick(recipeSpeedMultiplier());
                lastProgressTime = gameTime;
            }
        }
        state.setTickPercentage(((double) state.getTickProgress() / recipe.ticks()) * 100);
        if (state.getTickProgress() < recipe.ticks()) {
            return false;
        }
        state.setCanFinish(true);
        if (recipe.outputs().canProcess(level, storages, state)) {
            lastProgressTime = gameTime;
            recipe.outputs().process(level, storages, state);
            recipeFinished(recipe);
            storageContentCacheValid = false;
            return true;
        }
        recipeNextCheckTime.put(recipe.id(), gameTime + OUTPUT_BLOCKED_COOLDOWN);
        return false;
    }

    private boolean processTickInputs(RecipeModel recipe, RecipeStateModel state, RecipeStorages storages) {
        for (var input : recipe.inputs().inputs()) {
            if (input instanceof ConsumeRecipeIngredientEntry entry && entry.isPerTick()) {
                // flex recipes pay per-tick inputs with the run's cost multiplier too
                var ingredient = entry.ingredientFor(state);
                if (ingredient instanceof ITickSpreadIngredient spread) {
                    if (!drawTickSpread(recipe, state, spread, storages)) {
                        return false;
                    }
                    continue;
                }
                if (!ingredient.canProcess(level, storages, state)) {
                    return false;
                }
            }
            input.processTick(level, storages, state);
        }
        return true;
    }

    private boolean drawTickSpread(RecipeModel recipe, RecipeStateModel state, ITickSpreadIngredient spread, RecipeStorages storages) {
        long total = spread.resolveAmount(state);
        int ticks = Math.max(1, recipe.ticks());
        long toExtract = total / ticks + (state.getTickProgress() == ticks - 1 ? total % ticks : 0);
        if (toExtract <= 0) {
            return true;
        }
        if (spread.extractFromInputs(storages, toExtract, true) < toExtract) {
            return false;
        }
        spread.extractFromInputs(storages, toExtract, false);
        return true;
    }

    /** The crafts AE2 pushed into a Pattern Port of this machine, running or giving back their outputs. */
    public List<PatternRun> getPatternRuns() {
        return Collections.unmodifiableList(patternRuns);
    }

    /** Recipes running now: normal ones and pattern runs that haven't finished. */
    public int runningCount() {
        int count = activeRecipes.size();
        for (PatternRun run : patternRuns) {
            if (!run.isReturning()) count++;
        }
        return count;
    }

    /**
     * Starts a craft AE2 pushed: the recipe's pattern inputs are already in the buffer, everything else (energy,
     * catalysts...) comes from the machine's ports, the same checks as a normal recipe start apply.
     *
     * @param returnPos where to give the outputs back (the pattern provider), through its {@code returnFace} side
     */
    public PatternStartResult startPatternRun(RecipeModel recipe, PatternBuffer buffer, BlockPos returnPos, Direction returnFace) {
        if (level == null || structure == null || !isFormed || portStorages == null) {
            return PatternStartResult.NOT_FORMED;
        }
        if (!canRunAlongside(recipe)) {
            return PatternStartResult.PARALLEL_FULL;
        }
        if (!isAllowedByRedstone() || !recipe.conditions().canRun(conditionContext())
                || (MMInteropManager.KUBEJS.isPresent() && !MMInteropManager.KUBEJS.get().postRecipeStarted(this, recipe.id()))) {
            return PatternStartResult.CONDITIONS;
        }
        var state = new RecipeStateModel();
        RecipeStorages storages = runStorages(buffer);
        if (!recipe.inputs().canProcess(level, storages, state)) {
            return PatternStartResult.MISSING_INPUTS;
        }
        if (!recipe.outputs().canProcess(level, storages, state)) {
            return PatternStartResult.OUTPUTS_BLOCKED;
        }
        recipe.inputs().process(level, storages, state);
        state.setCanProcess(true);
        patternRuns.add(new PatternRun(recipe.id(), state, buffer, returnPos, returnFace));
        long gameTime = level.getGameTime();
        lastStartedRecipeId = recipe.id();
        lastRecipeStartTime = gameTime;
        lastProgressTime = gameTime;
        storageContentCacheValid = false;
        markChanged();
        return PatternStartResult.STARTED;
    }

    /** A pattern run's storages: its buffer first, then the machine's ports for what a pattern doesn't carry. */
    private RecipeStorages runStorages(PatternBuffer buffer) {
        var inputs = new ArrayList<>(buffer.inputs());
        inputs.addAll(portStorages.inputStorages());
        var outputs = new ArrayList<>(buffer.outputs());
        for (IPortStorage storage : portStorages.outputStorages()) {
            if (!storage.carriedByPatterns()) {
                outputs.add(storage);
            }
        }
        return new RecipeStorages(inputs, outputs);
    }

    /**
     * Finished pattern runs give what they hold back to their pattern provider, whatever state the machine is in;
     * the ones that gave everything back end.
     */
    private void drainPatternRuns() {
        if (!patternRuns.isEmpty() && level instanceof ServerLevel serverLevel
                && patternRuns.removeIf(run -> run.isReturning() && PatternReturn.drain(serverLevel, run))) {
            markChanged();
        }
    }

    /** One tick of every pattern run, while the machine is formed and allowed to run (else they wait). */
    private void tickPatternRuns(long gameTime) {
        if (patternRuns.isEmpty() || portStorages == null) {
            return;
        }
        for (PatternRun run : patternRuns) {
            if (run.isReturning()) continue;
            RecipeModel recipe = MachineRecipeManager.RECIPES.get(run.getRecipeId());
            // a reload removed the recipe: give back what the run holds
            if (recipe == null || tickRecipe(recipe, run.getState(), gameTime, runStorages(run.getBuffer()))) {
                run.setReturning();
                markChanged();
            }
        }
    }

    private void updateCurrentRecipe() {
        currentRecipe = activeRecipes.isEmpty() ? null : MachineRecipeManager.RECIPES.get(activeRecipes.keySet().iterator().next());
        if (currentRecipe == null) {
            for (PatternRun run : patternRuns) {
                if (!run.isReturning()) {
                    currentRecipe = MachineRecipeManager.RECIPES.get(run.getRecipeId());
                    break;
                }
            }
        }
    }

    public void invalidateProgress() {
        linkPorts(linkedStorages, null, ControllerState.UNFORMED);
        linkedStorages = null;
        linkedState = null;
        activeRecipes.clear();
        currentRecipe = null;
        structure = null;
        formedRotation = null;
        isFormed = false;
        BlackHoleShield.unshield(level, getBlockPos());
        portStorages = null;
        speedStorages = List.of();
        cachedStructureRecipes = null;
        storageContentCacheValid = false;
        recipeNextCheckTime.clear();
        markChanged();
    }

    public void reformTo(StructureModel newStructure) {
        if (newStructure != structure) {
            switchStructure(newStructure);
        }
        scheduleValidation();
    }

    @Nullable
    public GeoModelConfig geoModel() {
        return GeoModelConfig.resolve(model == null ? null : model.config(), structure == null ? null : structure.getConfig());
    }

    @Nullable
    public Object getGeoAnimatable() {
        return geoAnimatable;
    }

    public void setGeoAnimatable(@Nullable Object geoAnimatable) {
        this.geoAnimatable = geoAnimatable;
    }

    @Override
    public ControllerModel getModel() {
        return model;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getName();
    }

    public Component getName() {
        return customName != null ? Component.literal(customName) : model.displayName();
    }

    public @Nullable String getCustomName() {
        return customName;
    }

    public void setCustomName(@Nullable String name) {
        customName = name == null || name.isBlank() ? null : name.strip();
        if (customName != null && customName.length() > MAX_NAME_LENGTH) {
            customName = customName.substring(0, MAX_NAME_LENGTH);
        }
        markChanged();
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter input) {
        super.applyImplicitComponents(input);
        var name = input.get(DataComponents.CUSTOM_NAME);
        if (name != null) {
            setCustomName(name.getString());
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (customName != null) {
            components.set(DataComponents.CUSTOM_NAME, Component.literal(customName));
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        output.discard("CustomName");
    }

    public @Nullable LinkData getNetworkLink() {
        return networkLink;
    }

    public void setNetworkLink(@Nullable LinkData link) {
        networkLink = link;
        markChanged();
    }

    public TierPrefs getAssemblyTiers() {
        return assemblyTiers;
    }

    public void setAssemblyTier(String key, int rank) {
        int valid = TierPrefs.validate(PortTiers.maxTiers(getAssemblyCandidates()), key, rank);
        if (valid != TierPrefs.REJECTED && assemblyTiers.set(key, valid)) {
            markChanged();
        }
    }

    /** The structure picked in the assembly tab, if any (also read by the GameTests). */
    public @Nullable Identifier getAssemblyStructureId() {
        return assemblyStructureId;
    }

    public void setAssemblyStructureId(Identifier id) {
        if (findAssemblyCandidate(id) == null || id.equals(assemblyStructureId)) {
            return;
        }
        assemblyStructureId = id;
        markChanged();
    }

    public List<StructureModel> getAssemblyCandidates() {
        var byId = new TreeMap<Identifier, StructureModel>();
        for (StructureModel candidate : StructureManager.getStructuresForController(controllerId)) {
            byId.put(candidate.id(), candidate);
        }
        return List.copyOf(byId.values());
    }

    public @Nullable StructureModel getAssemblyStructure() {
        StructureModel chosen = findAssemblyCandidate(assemblyStructureId);
        if (chosen != null) {
            return chosen;
        }
        StructureModel current = structure == null ? null : findAssemblyCandidate(structure.id());
        if (current != null) {
            return current;
        }
        List<StructureModel> candidates = getAssemblyCandidates();
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    public @Nullable StructureModel findAssemblyCandidate(@Nullable Identifier id) {
        if (id == null) {
            return null;
        }
        for (StructureModel candidate : getAssemblyCandidates()) {
            if (candidate.id().equals(id)) {
                return candidate;
            }
        }
        return null;
    }

    public boolean isSoundMuted() {
        return soundMuted;
    }

    public void setSoundMuted(boolean muted) {
        soundMuted = muted;
        markChanged();
    }

    public void setModelDisplay(ModelDisplay display) {
        modelDisplay = display;
        markChanged();
    }

    public RecipeSelectionMode getRecipeSelectionMode() {
        return recipeModeOverride != null ? recipeModeOverride : model.recipeSelectionMode();
    }

    public void setRecipeSelectionMode(RecipeSelectionMode mode) {
        recipeModeOverride = mode == model.recipeSelectionMode() ? null : mode;
        recipeNextCheckTime.clear();
        markChanged();
    }

    public @Nullable RecipeModel getDisplayedRecipe() {
        if (currentRecipe != null) {
            return currentRecipe;
        }
        if (level == null || lastStartedRecipeId == null || lastRecipeStartTime == Long.MIN_VALUE || level.getGameTime() - lastRecipeStartTime > RECENT_RECIPE_TICKS) {
            return null;
        }
        return MachineRecipeManager.RECIPES.get(lastStartedRecipeId);
    }

    public boolean isWorking() {
        return level != null && lastProgressTime != Long.MIN_VALUE && level.getGameTime() - lastProgressTime <= RECENT_RECIPE_TICKS;
    }

    public String statusKey() {
        if (!isFormed || portStorages == null) return "not_formed";
        if (!isAllowedByRedstone()) return "paused";
        if (isWorking()) return "running";
        if (runningCount() > 0) return "stalled";
        return "idle";
    }

    public int getActiveRecipeCount() {
        return runningCount();
    }

    public int getDisplayedParallelLimit() {
        if (!model.parallelProcessingDefault()) {
            return 1;
        }
        return Math.max(1, parallelLimit());
    }

    private int parallelLimit() {
        int limit = model.maxParallelRecipes();
        if (structure != null && structure.maxParallelRecipes() >= 0) {
            limit = structure.maxParallelRecipes();
        }
        return limit < 0 ? MMConfig.MAX_PARALLEL_RECIPES : limit;
    }

    public StructureDiagnosis diagnoseStructure() {
        if (level == null || isFormed) {
            return StructureDiagnosis.NONE;
        }
        try {
            return StructureDiagnosis.diagnose(level, getBlockPos(), StructureManager.getStructuresForController(controllerId));
        } catch (RuntimeException e) {
            Ref.LOG.error("Failed to diagnose the structure of the controller at {}", getBlockPos(), e);
            return StructureDiagnosis.NONE;
        }
    }

    public List<BlockPos> getPortPositions() {
        if (portStorages == null) {
            return List.of();
        }
        var inputs = new ArrayList<BlockPos>();
        var outputs = new ArrayList<BlockPos>();
        for (var source : portStorages.sources()) {
            if (source instanceof BlockEntity port) {
                (source.isInput() ? inputs : outputs).add(port.getBlockPos());
            }
        }
        inputs.addAll(outputs);
        return inputs;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory inv, @NotNull Player player) {
        return new MachineControllerMenu(model, groupHolder, windowId, inv, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        var tag = new CompoundTag();
        saveLegacy(tag, LegacyNbt.registries(level));
        LegacyNbt.write(output, tag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        loadLegacy(LegacyNbt.read(input), input.lookup());
    }

    protected void saveLegacy(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag recipesTag = new CompoundTag();
        for (Map.Entry<Identifier, RecipeStateModel> entry : activeRecipes.entrySet()) {
            recipesTag.put(entry.getKey().toString(), entry.getValue().save(new CompoundTag(), registries));
        }
        tag.put("activeRecipes", recipesTag);
        if (!patternRuns.isEmpty()) {
            var runsTag = new ListTag();
            for (PatternRun run : patternRuns) {
                runsTag.add(run.save(registries));
            }
            tag.put("PatternRuns", runsTag);
        }
        if (structure != null) {
            tag.putString("structureId", structure.id().toString());
        }
        tag.putBoolean("isFormed", isFormed);
        if (lastStartedRecipeId != null) {
            tag.putString("lastStartedRecipeId", lastStartedRecipeId.toString());
            tag.putLong("lastRecipeStartTime", lastRecipeStartTime);
        }
        tag.putLong("lastProgressTime", lastProgressTime);
        if (recipeModeOverride != null) {
            tag.putString("RecipeSelectionMode", recipeModeOverride.serializedName());
        }
        if (customName != null) {
            tag.putString("CustomName", customName);
        }
        if (soundMuted) {
            tag.putBoolean("SoundMuted", true);
        }
        if (modelDisplay != ModelDisplay.ON) {
            tag.putString("ModelDisplay", modelDisplay.serializedName());
        }
        if (networkLink != null) {
            tag.put("NetworkLink", networkLink.save());
        }
        if (lastLinkExport >= 0) {
            tag.putLong("LastLinkExport", lastLinkExport);
        }
        if (!assemblyTiers.asMap().isEmpty()) {
            tag.put("AssemblyTiers", assemblyTiers.save());
        }
        if (assemblyStructureId != null) {
            tag.putString("AssemblyStructure", assemblyStructureId.toString());
        }
        tag.putLong("recipeSelectionSequence", recipeSelectionSequence);
        if (!inputItemLastStartedSequence.isEmpty()) {
            CompoundTag inputHistoryTag = new CompoundTag();
            for (Map.Entry<Identifier, Long> entry : inputItemLastStartedSequence.entrySet()) {
                inputHistoryTag.putLong(entry.getKey().toString(), entry.getValue());
            }
            tag.put("inputItemLastStartedSequence", inputHistoryTag);
        }
        if (selectedRecipeId != null) {
            tag.putString("selectedRecipe", selectedRecipeId.toString());
        }
        tag.putBoolean("filler", true);
        tag.putInt("redstoneMode", redstoneMode.ordinal());
    }

    protected void loadLegacy(@NotNull CompoundTag tag, HolderLookup.Provider registries) {
        loadFluidVolumes(tag, registries);
        activeRecipes.clear();
        patternRuns.clear();
        for (Tag element : tag.getListOrEmpty("PatternRuns")) {
            PatternRun run = PatternRun.load((CompoundTag) element, registries);
            if (run != null) {
                patternRuns.add(run);
            }
        }
        CompoundTag recipesTag = tag.getCompoundOrEmpty("activeRecipes");
        for (String key : recipesTag.keySet()) {
            Identifier recipeId = Identifier.tryParse(key);
            if (recipeId != null) {
                activeRecipes.put(recipeId, RecipeStateModel.load(recipesTag.getCompoundOrEmpty(key), registries));
            }
        }
        structure = tag.contains("structureId") ? StructureManager.STRUCTURES.get(Identifier.tryParse(tag.getStringOr("structureId", ""))) : null;
        isFormed = tag.contains("isFormed") ? tag.getBooleanOr("isFormed", false) : structure != null;
        lastStartedRecipeId = tag.contains("lastStartedRecipeId") ? Identifier.tryParse(tag.getStringOr("lastStartedRecipeId", "")) : null;
        lastRecipeStartTime = tag.contains("lastRecipeStartTime") ? tag.getLongOr("lastRecipeStartTime", 0L) : Long.MIN_VALUE;
        lastProgressTime = tag.contains("lastProgressTime") ? tag.getLongOr("lastProgressTime", 0L) : Long.MIN_VALUE;
        recipeModeOverride = tag.contains("RecipeSelectionMode") ? RecipeSelectionMode.parse(tag.getStringOr("RecipeSelectionMode", "")) : null;
        customName = tag.contains("CustomName") ? tag.getStringOr("CustomName", "") : null;
        soundMuted = tag.getBooleanOr("SoundMuted", false);
        ModelDisplay display = ModelDisplay.parse(tag.getStringOr("ModelDisplay", ""));
        modelDisplay = display == null ? ModelDisplay.ON : display;
        networkLink = tag.contains("NetworkLink") ? LinkData.load(tag.getCompoundOrEmpty("NetworkLink")) : null;
        lastLinkExport = tag.contains("LastLinkExport") ? tag.getLongOr("LastLinkExport", 0L) : -1L;
        assemblyTiers.copyFrom(TierPrefs.load(tag.getCompoundOrEmpty("AssemblyTiers")));
        assemblyStructureId = tag.contains("AssemblyStructure") ? Identifier.tryParse(tag.getStringOr("AssemblyStructure", "")) : null;
        selectedRecipeId = tag.contains("selectedRecipe") ? Identifier.tryParse(tag.getStringOr("selectedRecipe", "")) : null;
        recipeSelectionSequence = tag.getLongOr("recipeSelectionSequence", 0L);
        inputItemLastStartedSequence.clear();
        CompoundTag inputHistoryTag = tag.getCompoundOrEmpty("inputItemLastStartedSequence");
        for (String key : inputHistoryTag.keySet()) {
            Identifier itemId = Identifier.tryParse(key);
            if (itemId != null) {
                inputItemLastStartedSequence.put(itemId, inputHistoryTag.getLongOr(key, 0L));
            }
        }
        updateCurrentRecipe();
        int mode = tag.getIntOr("redstoneMode", 0);
        var modes = RedstoneMode.values();
        redstoneMode = mode >= 0 && mode < modes.length ? modes[mode] : RedstoneMode.IGNORED;
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        saveLegacy(tag, registries);
        if (!fluidVolumes.isEmpty()) {
            tag.put("FluidVolumes", FluidVolumeState.saveAll(fluidVolumes, registries));
        }
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public RecipeStateModel getRecipeState() {
        if (activeRecipes.isEmpty()) {
            // a running pattern run, so a machine busy with AE2 crafts shows its progress too
            for (PatternRun run : patternRuns) {
                if (!run.isReturning()) return run.getState();
            }
            return null;
        }
        return activeRecipes.values().iterator().next();
    }

    public int getRedstoneModeOrdinal() {
        return redstoneMode.ordinal();
    }

    public void setRedstoneModeOrdinal(int ordinal) {
        var modes = RedstoneMode.values();
        if (ordinal >= 0 && ordinal < modes.length) {
            redstoneMode = modes[ordinal];
            markChanged();
        }
    }

    public String getRedstoneModeName() {
        return redstoneMode.name();
    }

    public boolean isManualSelection() {
        return getRecipeSelectionMode() == RecipeSelectionMode.MANUAL;
    }

    public void selectRecipe(@Nullable Identifier recipeId) {
        if (recipeId != null) {
            var recipe = MachineRecipeManager.RECIPES.get(recipeId);
            if (recipe == null || structure == null || !recipe.runsIn(structure.id())) {
                return;
            }
        }
        selectedRecipeId = recipeId;
        recipeNextCheckTime.clear();
        markChanged();
    }

    public void requestRecipe(Object owner, Identifier recipeId, long gameTime, int crafts) {
        if (!recipeId.equals(requestedRecipeId)) {
            var recipe = MachineRecipeManager.RECIPES.get(recipeId);
            if (recipe == null || structure == null || !recipe.runsIn(structure.id())) {
                return;
            }
            requestedRecipeId = recipeId;
            recipeNextCheckTime.remove(recipeId);
        }
        requestOwner = owner;
        requestedCrafts = Math.max(0, crafts);
        requestExpiresAt = gameTime + REQUEST_TIMEOUT;
    }

    public void clearRequest(Object owner, Identifier recipeId) {
        if (owner == requestOwner && recipeId.equals(requestedRecipeId)) {
            requestedRecipeId = null;
            requestOwner = null;
            requestedCrafts = 0;
            requestStarts = 0;
        }
    }

    public int takeRequestStarts(Object owner) {
        if (owner != requestOwner) {
            return 0;
        }
        int starts = requestStarts;
        requestStarts = 0;
        return starts;
    }

    public void reserveOutput(Object owner, ItemStack stack, int count, long gameTime) {
        reservationOwner = owner;
        reservedOutput = stack.copyWithCount(1);
        reservedCount = Math.max(0, count);
        reservationExpiresAt = gameTime + REQUEST_TIMEOUT;
    }

    public void releaseOutput(Object owner) {
        if (owner == reservationOwner) {
            reservationOwner = null;
            reservedOutput = ItemStack.EMPTY;
            reservedCount = 0;
        }
    }

    public int reservedOutputCount(ItemStack stack) {
        if (reservedCount <= 0 || reservedOutput.isEmpty() || level == null || level.getGameTime() > reservationExpiresAt) {
            return 0;
        }
        return ItemStack.isSameItemSameComponents(reservedOutput, stack) ? reservedCount : 0;
    }

    public boolean takeLinkExportRequest() {
        boolean requested = linkExportRequested;
        linkExportRequested = false;
        return requested;
    }

    public void markLinkExported(long gameTime) {
        lastLinkExport = gameTime;
        syncPending = true;
    }

    public long getLastLinkExport() {
        return lastLinkExport;
    }

    public boolean isRecipeRunning(Identifier recipeId) {
        return activeRecipes.containsKey(recipeId);
    }

    private @Nullable Identifier activeRequest(long gameTime) {
        if (requestedRecipeId != null && gameTime > requestExpiresAt) {
            requestedRecipeId = null;
            requestOwner = null;
            requestedCrafts = 0;
        }
        return requestedRecipeId;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        BlackHoleShield.watch(level, this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        BlackHoleShield.unwatch(level, this);
        BlackHoleShield.unshield(level, getBlockPos());
    }

    public void validateNow() {
        if (level != null && !level.isClientSide() && !isRemoved()) {
            validateStructure();
        }
    }

    public boolean isFormed() {
        return isFormed;
    }

    public @Nullable Rotation getFormedRotation() {
        return isFormed ? formedRotation : null;
    }

    public @Nullable RecipeStorages getPortStorages() {
        return portStorages;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        invalidateProgress();
        super.preRemoveSideEffects(pos, state);
    }
}
