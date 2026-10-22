package io.ticticboom.mods.mm.controller.machine.register;

import io.ticticboom.mods.mm.port.common.AbstractPortBlockEntity;
import io.ticticboom.mods.mm.config.MMClientConfig;
import io.ticticboom.mods.mm.compat.interop.MMInteropManager;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.builder.PortTiers;
import io.ticticboom.mods.mm.builder.TierPrefs;
import io.ticticboom.mods.mm.config.MMConfig;
import io.ticticboom.mods.mm.controller.IControllerBlockEntity;
import io.ticticboom.mods.mm.controller.IControllerPart;
import io.ticticboom.mods.mm.model.ControllerModel;
import io.ticticboom.mods.mm.model.RecipeSelectionMode;
import io.ticticboom.mods.mm.networklink.LinkData;
import io.ticticboom.mods.mm.networklink.NetworkLink;
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
import io.ticticboom.mods.mm.recipe.condition.RecipeConditionContext;
import io.ticticboom.mods.mm.recipe.MachineRecipeManager;
import io.ticticboom.mods.mm.pattern.PatternBuffer;
import io.ticticboom.mods.mm.pattern.PatternReturn;
import io.ticticboom.mods.mm.pattern.PatternRun;
import io.ticticboom.mods.mm.pattern.PatternStartResult;
import io.ticticboom.mods.mm.port.IPortStorage;
import io.ticticboom.mods.mm.recipe.input.consume.ConsumeRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStateModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.structure.StructureDiagnosis;
import io.ticticboom.mods.mm.structure.StructureManager;
import io.ticticboom.mods.mm.structure.StructureModel;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import io.ticticboom.mods.mm.client.geo.GeoModelConfig;
import net.minecraft.world.phys.AABB;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static io.ticticboom.mods.mm.config.MMConfigSetup.COMMON;

public class MachineControllerBlockEntity extends BlockEntity implements IControllerBlockEntity, IControllerPart, Nameable {

    private final ControllerModel model;
    private final RegistryGroupHolder groupHolder;
    @Getter
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
    // client only: the GeckoLib animatable of this machine's model (an Object so MM doesn't load GeckoLib types)
    @Nullable
    private Object geoAnimatable;
    private final Map<ResourceLocation, RecipeStateModel> activeRecipes = new HashMap<>();
    // crafts pushed by AE2 into a Pattern Port; they run next to activeRecipes, each with its own buffer
    private final List<PatternRun> patternRuns = new ArrayList<>();
    @Getter
    private int comparatorSignal = 0;
    private RecipeStorages portStorages = null;
    // the port block entities portStorages was gathered from: a chunk unload + load replaces them with new objects
    private List<BlockEntity> portBlockEntities = List.of();
    // chunks the formed structure covers (minX, minZ, maxX, maxZ), null until its ports are gathered
    private int @Nullable [] structureChunks = null;
    /**
     * -- GETTER --
     * Whether the structure was formed at the last validation; also known on the client.
     */
    // synced to clients in the update tag
    @Getter
    private boolean isFormed = false;
    @Getter
    private RecipeModel currentRecipe;
    private final ControllerModel controllerModel;
    // Redstone control for the controller: IGNORE, RUN_WHEN_POWERED, RUN_WHEN_UNPOWERED
    private enum RedstoneMode { IGNORED, WITH_REDSTONE, WITHOUT_REDSTONE }
    private RedstoneMode redstoneMode = RedstoneMode.IGNORED;
    // recipe order picked on the screen; null = the controller type's default
    @Nullable
    private RecipeSelectionMode recipeModeOverride = null;
    /** Assemble: preferred port tier per port type, and which structure to build (null = first). */
    @Getter
    private final TierPrefs assemblyTiers = new TierPrefs();
    @Nullable
    private ResourceLocation assemblyStructureId = null;
    // set from the controller screen: this machine plays no working sound (its particles stay)
    @Getter
    private boolean soundMuted = false;
    // set from the controller screen: the machine's GeckoLib model is animated, stands still or isn't drawn
    @Getter
    private ModelDisplay modelDisplay = ModelDisplay.ON;
    // name given by a player (screen, or a named controller item); null = the machine's name
    @Nullable
    private String customName = null;
    public static final int MAX_NAME_LENGTH = 50;
    // owner + AE2 network this machine is linked to (network linker), null when not linked
    @Getter
    @Nullable
    private LinkData networkLink = null;
    private long lastTick = 0;
    // players with the controller's screen open; while > 0 the controller is sent to clients every tick
    private int viewers = 0;
    private boolean wasActive = false;
    private int lastClientSignature = 0;
    private static final int SAVE_FALLBACK_TICKS = 20;
    // how often non-item input ports are checked for changes made from outside
    private static final int EXTERNAL_CHANGE_CHECK_TICKS = 5;
    // cached view of storage contents to avoid rebuilding every tick when recipes are running
    private final StorageCacheManager.StorageCache storageCache = new StorageCacheManager.StorageCache();
    private final Map<ResourceLocation, Long> recipeNextCheckTime = new HashMap<>();
    // signature of the last observed non-item storage contents
    private long lastStorageSignature = 0L;
    // debounce flag for validation thread creation
    private volatile boolean validationScheduled = false;
    // track last-request ms (optional)
    @Getter
    @Setter
    private volatile long lastValidationRequestMs = 0L;
    // track last activity time per active recipe to detect stalls
    private final Map<ResourceLocation, Long> activeRecipeLastUpdate = new HashMap<>();
    // the structure's recipes indexed by input item; null until first needed
    private RecipeCandidateIndex recipeIndex = null;
    // positions in recipeIndex of the recipes the current port contents might run; null = rebuild
    private int[] candidatePositions = null;
    // position in recipeIndex where the next recipe scan starts
    private int nextRecipeCheckIndex = 0;
    // when a recipe is skipped due to missing resources, wait N ticks before rechecking
    private static final int RECIPE_SKIP_COOLDOWN_TICKS = 20;
    private ResourceLocation lastStartedRecipeId = null;
    // game time of the last recipe start, synced so the screen can show recipes that finish within a tick
    private long lastRecipeStartTime = Long.MIN_VALUE;
    /** How long after its start a recipe still counts as running on the screen, in ticks. */
    private static final int RECENT_RECIPE_TICKS = 20;
    private static final int PORT_STATE_REFRESH_TICKS = 20;
    // ports whose status light this controller last set
    private Set<BlockPos> litPorts = new HashSet<>();
    private ResourceLocation lastStartedInputItemId = null;
    private long recipeSelectionSequence = 0L;
    private final Map<ResourceLocation, Long> inputItemLastStartedSequence = new HashMap<>();
    // track last-start sequence per recipe to break ties when multiple recipes
    // compete for the same input item key during round-robin selection
    private final Map<ResourceLocation, Long> recipeLastStartedSequence = new HashMap<>();

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
        runMachineTick();
        NetworkLink.tickController(level, this);
        updateControllerState();
        updateComparatorSignal();
        syncAndSave();
    }

    /**
     * Comparator output: 0 while unformed or idle, 1-15 while working, following the progress of the furthest
     * running recipe. Recipes that finish within a tick have no progress to show and count as done (15).
     */
    private int computeComparatorSignal() {
        BlockState blockState = getBlockState();
        if (!blockState.hasProperty(ControllerState.PROPERTY) || blockState.getValue(ControllerState.PROPERTY) != ControllerState.WORKING) {
            return 0;
        }
        double percent = activeRecipes.values().stream().mapToDouble(RecipeStateModel::getTickPercentage).max().orElse(100);
        return 1 + (int) Math.round(Mth.clamp(percent, 0, 100) / 100 * 14);
    }

    /** Neighbors (comparators) are only told when the signal actually changes. */
    private void updateComparatorSignal() {
        int next = computeComparatorSignal();
        if (next != comparatorSignal) {
            comparatorSignal = next;
            assert level != null;
            level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
        }
    }

    /**
     * Marks the controller for saving and sends it to clients only when that is needed, instead of every tick:
     * a running recipe changes its progress every tick, so it is saved while running (and on the tick it ends);
     * clients get the controller every tick only while someone has its screen open, otherwise only when
     * something they can see changes.
     */
    private void syncAndSave() {
        if (level == null) return;
        boolean active = !activeRecipes.isEmpty() || !patternRuns.isEmpty();
        int signature = clientSignature();
        boolean visibleChange = signature != lastClientSignature;
        if (active || wasActive || visibleChange || level.getGameTime() % SAVE_FALLBACK_TICKS == 0) {
            setChanged();
        }
        wasActive = active;
        if (viewers > 0 || visibleChange) {
            lastClientSignature = signature;
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** What the client shows about the controller, apart from recipe progress. */
    private int clientSignature() {
        return Objects.hash(isFormed, structure == null ? null : structure.id(), activeRecipes.keySet(),
                currentRecipe == null ? null : currentRecipe.id(), lastStartedRecipeId, lastRecipeStartTime,
                redstoneMode, networkLink);
    }

    /** A player opened the controller's screen (server side). */
    public void addViewer() {
        viewers++;
    }

    /** A player closed the controller's screen (server side). */
    public void removeViewer() {
        viewers = Math.max(0, viewers - 1);
    }

    private void runMachineTick() {
        if (level == null) return;
        drainPatternRuns();
        long gameTime = level.getGameTime();
        if (isFormed && !structureChunksLoaded()) {
            // part of the machine is in an unloaded chunk: wait, instead of loading it or working with its old ports
            pauseActiveRecipes(gameTime);
            lastTick = gameTime;
            return;
        }
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
        } else {
            pauseActiveRecipes(gameTime);
        }
    }

    /**
     * Mirrors the machine's state into the block state, which picks the controller's model and screen color.
     * Only clients are notified (no neighbor or shape updates), and only when the state actually changes.
     */
    private void updateControllerState() {
        if (level == null) return;
        BlockState blockState = getBlockState();
        if (!blockState.hasProperty(ControllerState.PROPERTY)) return;
        ControllerState next;
        if (!isFormed || structure == null) {
            next = ControllerState.UNFORMED;
        } else if (isAllowedByRedstone() && getDisplayedRecipe() != null) {
            // getDisplayedRecipe() also covers recipes that finish within a tick, so a busy machine doesn't flicker
            next = ControllerState.WORKING;
        } else {
            next = ControllerState.IDLE;
        }
        boolean changed = blockState.getValue(ControllerState.PROPERTY) != next;
        if (changed) {
            level.setBlock(getBlockPos(), blockState.setValue(ControllerState.PROPERTY, next), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        // re-check the ports now and then too, so a port swapped into a running machine picks up its state
        if (changed || level.getGameTime() % PORT_STATE_REFRESH_TICKS == 0) {
            updatePortStates(next);
        }
    }

    /**
     * Gives the machine's ports the controller's state (their status light). Ports that left the machine go back to unformed.
     */
    private void updatePortStates(ControllerState state) {
        if (level == null) return;
        Set<BlockPos> current = new HashSet<>();
        if (state != ControllerState.UNFORMED && structure != null) {
            try {
                current.addAll(structure.getPortPositions(level, getBlockPos()));
            } catch (Throwable ignored) {
            }
        }
        int[] colors = machineColors();
        for (BlockPos portPos : litPorts) {
            if (!current.contains(portPos)) {
                setPortState(portPos, ControllerState.UNFORMED);
                setPortColors(portPos, null);
            }
        }
        for (BlockPos portPos : current) {
            setPortState(portPos, state);
            setPortColors(portPos, colors);
        }
        litPorts = current;
    }

    /**
     * @return the controller's own screen colors (unformedColor / idleColor / workingColor from KubeJS or JSON)
     * for its ports' status lights, -1 where it has none; null when it sets no color at all
     */
    private int @Nullable [] machineColors() {
        int[] colors = new int[ControllerState.values().length];
        boolean any = false;
        for (ControllerState s : ControllerState.values()) {
            Integer color = MMClientConfig.parseColor(controllerModel.screenColor(s.getSerializedName()));
            colors[s.ordinal()] = color == null ? -1 : color;
            any |= color != null;
        }
        return any ? colors : null;
    }

    private void setPortColors(BlockPos portPos, int @Nullable [] colors) {
        if (level != null && level.isLoaded(portPos) && level.getBlockEntity(portPos) instanceof AbstractPortBlockEntity port) {
            port.setMachineColors(colors);
        }
    }

    /** Turns the ports' status lights back to unformed, used when the controller is broken. */
    public void resetPortStates() {
        updatePortStates(ControllerState.UNFORMED);
    }

    private void setPortState(BlockPos portPos, ControllerState state) {
        if (level == null || !level.isLoaded(portPos)) return;
        BlockState portState = level.getBlockState(portPos);
        if (portState.hasProperty(ControllerState.PROPERTY) && portState.getValue(ControllerState.PROPERTY) != state) {
            level.setBlock(portPos, portState.setValue(ControllerState.PROPERTY, state), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    public boolean isAllowedByRedstone() {
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
                    keepRecipesOf(structureModel);
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
         if (portStorages != null && portsReplaced()) {
             // a port's chunk was unloaded and loaded again: its block entity, and storage, is a new object now
             portStorages = null;
             storageCache.clear();
         }
         if (portStorages == null) {
             portStorages = (structure == null) ? null : structure.getStorages(level, getBlockPos());
             rememberPorts();
         }
         if (portStorages != null && portStorages.inputItemPortIndex().changedSinceLastPoll()) {
             storageCache.isValid = false;
             recipeNextCheckTime.clear();
         }
         detectExternalStorageChanges();
         long gameTime = (level == null) ? 0L : level.getGameTime();
         if (!storageCache.isValid) rebuildStorageCache();
         boolean allowed = isAllowedByRedstone();
        if (allowed) processActiveRecipeOutputs();
        if (structure != null && allowed) scanAndStartRecipes(gameTime);
        performRecipeTick();
        tickPatternRuns();
    }

    /** Remembers the block entities the port storages came from, and the chunks the structure covers. */
    private void rememberPorts() {
        portBlockEntities = List.of();
        structureChunks = null;
        if (portStorages == null || structure == null || level == null) {
            return;
        }
        var ports = new ArrayList<BlockEntity>();
        for (BlockPos pos : structure.getPortPositions(level, getBlockPos())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                ports.add(be);
            }
        }
        portBlockEntities = ports;
        int minX = getBlockPos().getX() >> 4, maxX = minX;
        int minZ = getBlockPos().getZ() >> 4, maxZ = minZ;
        for (BlockPos pos : structure.getPositions(level, getBlockPos())) {
            minX = Math.min(minX, pos.getX() >> 4);
            maxX = Math.max(maxX, pos.getX() >> 4);
            minZ = Math.min(minZ, pos.getZ() >> 4);
            maxZ = Math.max(maxZ, pos.getZ() >> 4);
        }
        structureChunks = new int[]{minX, minZ, maxX, maxZ};
    }

    /** Whether a port block entity the storages came from was removed or replaced (its chunk reloaded). */
    private boolean portsReplaced() {
        if (level == null) {
            return false;
        }
        for (BlockEntity be : portBlockEntities) {
            if (be.isRemoved() || (level.isLoaded(be.getBlockPos()) && level.getBlockEntity(be.getBlockPos()) != be)) {
                return true;
            }
        }
        return false;
    }

    /** Whether every chunk of the formed structure is loaded (true while they aren't known yet). */
    private boolean structureChunksLoaded() {
        if (level == null || structureChunks == null) {
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

    // Helper split to reduce runRecipe complexity
    private void detectExternalStorageChanges() {
        // Item handlers have revision counters; other port types are polled every five ticks.
        if (level == null || level.getGameTime() % EXTERNAL_CHANGE_CHECK_TICKS != 0) {
            return;
        }
        try {
            if (portStorages != null) {
                long sig = 1469598103934665603L; // FNV offset basis
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
                     storageCache.isValid = false;
                     recipeNextCheckTime.clear();
                }
            }
        } catch (Throwable ignored) { }
    }

     private void rebuildStorageCache() {
         // Delegate to StorageCacheManager to rebuild all cache fields
         StorageCacheManager.rebuildStorageCache(portStorages, storageCache,
                 getRecipeSelectionMode() == RecipeSelectionMode.ROUND_ROBIN_INPUT_ITEM);
         // after a rebuild allow recipes to be rechecked immediately
         recipeNextCheckTime.clear();
         candidatePositions = null;
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
                 MMInteropManager.KUBEJS.ifPresent(kjs -> kjs.onRecipeFinish(this, recipeId));
             }
         }
        for (ResourceLocation id : toRemove) {
            activeRecipes.remove(id);
            activeRecipeLastUpdate.remove(id);
        }
    }

     private void scanAndStartRecipes(long gameTime) {
         if (recipeIndex == null) {
             recipeIndex = new RecipeCandidateIndex(MachineRecipeManager.getRecipesByStrucutreId(structure.id()));
             candidatePositions = null;
         }
         if (recipeIndex.isEmpty()) return;
         // only recipes whose inputs could be in the ports are checked; that list is rebuilt when the ports change
         if (!storageCache.isValid) rebuildStorageCache();
         if (candidatePositions == null) {
             candidatePositions = recipeIndex.candidates(storageCache, MMPortRegistry.PORT_TYPES_BY_CONTROLLER.get(controllerId));
         }
         int[] candidates = candidatePositions;
         if (candidates.length == 0) return;
         int total = candidates.length;
         // Spread recipe checks across multiple ticks. For DEFAULT mode with <300 candidates, check all per tick.
         int checks = getChecks(total);
         // continue from where the last pass stopped, in the structure's recipe order
         int found = java.util.Arrays.binarySearch(candidates, nextRecipeCheckIndex);
         int idx = found >= 0 ? found : -found - 1;
         int nextPosition = nextRecipeCheckIndex;
        int performed = 0;
        RecipeModel deferredRecipe = null;
        ResourceLocation deferredPrimaryInputItemId = null;
        RecipeModel selectedRoundRobinRecipe = null;
        ResourceLocation selectedRoundRobinInputItemId = null;
        long selectedRoundRobinLastUse = Long.MAX_VALUE;
        boolean startedRecipeThisPass = false;
        while (performed < checks) {
            int position = candidates[idx % total];
            RecipeModel recipe = recipeIndex.get(position);
            idx = (idx + 1) % total;
            nextPosition = position + 1;
            performed++;

            if (activeRecipes.containsKey(recipe.id())) continue;
            if (recipeNextCheckTime.getOrDefault(recipe.id(), 0L) > gameTime) continue;

            if (!recipe.conditions().canRun(conditionContext())) {
                continue;
            }
            // one state for both checks: flex outputs need the item the flex input picked, scaled ones the count
            RecipeStateModel scanState = new RecipeStateModel();
            if (!recipe.inputs().canProcess(level, portStorages, scanState)) {
                // not enough of something; any change to the ports clears this wait
                recipeNextCheckTime.put(recipe.id(), gameTime + RECIPE_SKIP_COOLDOWN_TICKS);
                continue;
            }
            if (!recipe.outputs().canProcess(level, portStorages, scanState)) {
                // Do not consume inputs when the output ports are already full.
                recipeNextCheckTime.put(recipe.id(), gameTime + RECIPE_SKIP_COOLDOWN_TICKS);
                continue;
            }

             if (canStartRecipeGivenParallelRules(recipe)) {
                 ResourceLocation primaryInputItemId = getPrimaryConsumedItemInputId(recipe);
                 RecipeSelectionMode selectionMode = getRecipeSelectionMode();
                 if (selectionMode == RecipeSelectionMode.ROUND_ROBIN_INPUT_ITEM && primaryInputItemId != null) {
                     // primaryInputItemId may be a composed key (with __ suffix) or a base id.
                     ResourceLocation baseId = getResourceLocation(primaryInputItemId);
                     // choose least-recently-used available stack key for this recipe's primary base item id
                     var available = storageCache.availableItemStackKeys.get(baseId);
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
                if (startRecipe(recipe, gameTime, primaryInputItemId)) {
                    startedRecipeThisPass = true;
                }
            }
        }
        if (!startedRecipeThisPass && selectedRoundRobinRecipe != null
                && !activeRecipes.containsKey(selectedRoundRobinRecipe.id())
                && canStartRecipeGivenParallelRules(selectedRoundRobinRecipe)
                && inputsAndOutputsFit(selectedRoundRobinRecipe)) {
            startedRecipeThisPass = startRecipe(selectedRoundRobinRecipe, gameTime, selectedRoundRobinInputItemId);
        }
        if (!startedRecipeThisPass && deferredRecipe != null && !activeRecipes.containsKey(deferredRecipe.id())
                && canStartRecipeGivenParallelRules(deferredRecipe)
                && inputsAndOutputsFit(deferredRecipe)) {
            startRecipe(deferredRecipe, gameTime, deferredPrimaryInputItemId);
        }
        nextRecipeCheckIndex = nextPosition % recipeIndex.size();
    }

    /** Checks inputs and outputs against one state: flex and scaled outputs depend on what the inputs found. */
    private boolean inputsAndOutputsFit(RecipeModel recipe) {
        RecipeStateModel probe = new RecipeStateModel();
        return recipe.inputs().canProcess(level, portStorages, probe) && recipe.outputs().canProcess(level, portStorages, probe);
    }

    private int getChecks(int total) {
        int maxRecipeChecksPerTick;
        if (total < 300 && !getRecipeSelectionMode().fairScheduling()) {
            maxRecipeChecksPerTick = total; // scan all recipes every tick for small recipe sets
        } else if (getRecipeSelectionMode().fairScheduling()) {
            maxRecipeChecksPerTick = Math.max(total / 2, 50); // 50% of recipes per tick in fair mode
        } else {
            maxRecipeChecksPerTick = Math.max(total / 2, 100); // 50% or 100 minimum for large recipe sets
        }
        return Math.min(maxRecipeChecksPerTick, total);
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

    /**
     * @return how many recipes may run at once: the structure's limit, else the controller's, else the config's;
     * 0 means one at a time
     */
    public int getParallelLimit() {
        int limit = controllerModel.maxParallelRecipes();
        if (structure != null) {
            int structLimit = structure.maxParallelRecipes();
            if (structLimit >= 0) limit = structLimit;
        }
        if (limit < 0) limit = MMConfig.MAX_PARALLEL_RECIPES;
        return limit;
    }

    /**
     * @return how many different recipes can run at once, as shown to players: 1 when the controller doesn't
     * run recipes in parallel (a recipe may still enable it for itself), else {@link #getParallelLimit()}.
     * The same recipe never runs twice at once.
     */
    public int getDisplayedParallelLimit() {
        if (!controllerModel.parallelProcessingDefault()) {
            return 1;
        }
        return Math.max(1, getParallelLimit());
    }

    /**
     * @return what the machine is doing, as the suffix of the gui.mm.controller.status.* lang keys
     */
    public String statusKey() {
        if (structure == null || !isFormed) return "not_formed";
        if (!isAllowedByRedstone()) return "paused";
        if (getDisplayedRecipe() != null) return "running";
        return "idle";
    }

    private boolean canStartRecipeGivenParallelRules(RecipeModel recipe) {
        boolean allowParallel = recipe.parallelProcessing();
        if (recipe.parallelProcessing() == MMConfig.PARALLEL_PROCESSING_DEFAULT) {
            allowParallel = controllerModel.parallelProcessingDefault();
        }
        int controllerLimit = getParallelLimit();
        boolean canStartBasedOnParallelFlag = allowParallel || runningCount() == 0;
        boolean canStartBasedOnLimit = controllerLimit == 0
                ? runningCount() == 0
                : runningCount() < controllerLimit;
        return canStartBasedOnParallelFlag && canStartBasedOnLimit;
    }

    /**
     * @return false if a KubeJS script canceled the start (MMEvents.recipeStarted)
     */
      private boolean startRecipe(RecipeModel recipe, long gameTime, @Nullable ResourceLocation primaryInputItemId) {
          if (!MMInteropManager.KUBEJS.map(kjs -> kjs.onRecipeStart(this, recipe.id())).orElse(true)) {
              return false;
          }
          // Re-validate that inputs are still available before consuming, to prevent race conditions
          // where multiple recipes see the same resource availability but consume sequentially
          RecipeStateModel newState = new RecipeStateModel();
          if (!recipe.inputs().canProcess(level, portStorages, newState)) {
              recipeNextCheckTime.put(recipe.id(), gameTime + RECIPE_SKIP_COOLDOWN_TICKS);
              return false;
          }
          if (!recipe.outputs().canProcess(level, portStorages, newState)) {
              recipeNextCheckTime.put(recipe.id(), gameTime + RECIPE_SKIP_COOLDOWN_TICKS);
              return false;
          }

          recipe.inputs().process(level, portStorages, newState);
         // Candidate contents are checked by canProcess before use. The periodic input
         // signature refreshes this index when consuming inputs changes availability.
         newState.setCanProcess(true);
        activeRecipes.put(recipe.id(), newState);
        activeRecipeLastUpdate.put(recipe.id(), gameTime);
        lastStartedRecipeId = recipe.id();
        lastRecipeStartTime = gameTime;
        recipeSelectionSequence++;
        // record recipe start sequence for tie-breaking among recipes sharing input keys
        recipeLastStartedSequence.put(recipe.id(), recipeSelectionSequence);
        if (primaryInputItemId != null) {
            lastStartedInputItemId = primaryInputItemId;
            inputItemLastStartedSequence.put(primaryInputItemId, recipeSelectionSequence);
        }
        setChanged();
        return true;
    }

    private boolean shouldDeferRecipeBySelectionMode(RecipeModel recipe, @SuppressWarnings("unused") @Nullable ResourceLocation primaryInputItemId) {
        RecipeSelectionMode mode = getRecipeSelectionMode();
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

    private RecipeConditionContext conditionContext() {
        return new RecipeConditionContext(level, getBlockPos(), structure);
    }

    /** What one tick did to a running recipe, see {@link #tickRun}. */
    private enum RunStep { WAITING, PROGRESSED, OUTPUTS_BLOCKED, FINISHED }

    /**
     * One tick of a running recipe: its per-tick inputs and outputs, its progress and, once done, its outputs.
     * WAITING while its conditions don't hold (night ended...) or a per-tick output doesn't fit; OUTPUTS_BLOCKED when
     * it just reached the end but its outputs don't fit yet (it keeps its consumed inputs, the outputs are tried again
     * later).
     */
    private RunStep tickRun(RecipeModel recipe, RecipeStateModel state, RecipeStorages storages) {
        if (!recipe.conditions().canRun(conditionContext())) {
            return RunStep.WAITING;
        }
        if (!recipe.outputs().canProcessTick(level, storages, state)) {
            // a per-tick output doesn't fit: wait for room instead of throwing it away
            return RunStep.WAITING;
        }
        recipe.inputs().processTick(level, storages, state);
        recipe.outputs().processTick(level, storages, state);
        if (!state.isCanFinish()) state.proceedTick();
        state.setTickPercentage(((double) state.getTickProgress() / recipe.ticks()) * 100);
        if (state.getTickProgress() < recipe.ticks()) {
            return RunStep.PROGRESSED;
        }
        state.setCanFinish(true);
        if (!recipe.outputs().canProcess(level, storages, state)) {
            return RunStep.OUTPUTS_BLOCKED;
        }
        recipe.outputs().process(level, storages, state);
        return RunStep.FINISHED;
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
        if (!canStartRecipeGivenParallelRules(recipe)) {
            return PatternStartResult.PARALLEL_FULL;
        }
        if (!isAllowedByRedstone() || !recipe.conditions().canRun(conditionContext())
                || !MMInteropManager.KUBEJS.map(kjs -> kjs.onRecipeStart(this, recipe.id())).orElse(true)) {
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
        lastStartedRecipeId = recipe.id();
        lastRecipeStartTime = level.getGameTime();
        setChanged();
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
        if (!patternRuns.isEmpty() && level instanceof ServerLevel serverLevel) {
            patternRuns.removeIf(run -> run.isReturning() && PatternReturn.drain(serverLevel, run));
        }
    }

    /** One tick of every pattern run, while the machine is formed and allowed to run (else they wait). */
    private void tickPatternRuns() {
        if (patternRuns.isEmpty() || portStorages == null || !isAllowedByRedstone()) {
            return;
        }
        for (PatternRun run : patternRuns) {
            if (run.isReturning()) continue;
            RecipeModel recipe = MachineRecipeManager.RECIPES.get(run.getRecipeId());
            if (recipe == null) {
                // a reload removed the recipe: give back what the run holds
                run.setReturning();
                continue;
            }
            RecipeStorages storages = runStorages(run.getBuffer());
            boolean finished;
            if (run.getState().isCanFinish()) {
                // done, waiting for its outputs (that don't go to the buffer) to fit
                finished = recipe.outputs().canProcess(level, storages, run.getState());
                if (finished) recipe.outputs().process(level, storages, run.getState());
            } else {
                finished = tickRun(recipe, run.getState(), storages) == RunStep.FINISHED;
            }
            if (finished) {
                run.setReturning();
                MMInteropManager.KUBEJS.ifPresent(kjs -> kjs.onRecipeFinish(this, recipe.id()));
            }
        }
        if (currentRecipe == null) {
            for (PatternRun run : patternRuns) {
                if (!run.isReturning()) {
                    currentRecipe = MachineRecipeManager.RECIPES.get(run.getRecipeId());
                    break;
                }
            }
        }
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
            if (state.isCanFinish() && recipe != null) {
                // A completed recipe owns its consumed inputs until its outputs fit.
                // processActiveRecipeOutputs retries the output on the next tick.
                activeRecipeLastUpdate.put(recipeId, gameTime);
                continue;
            }
            // check for stalled recipe
            long last = activeRecipeLastUpdate.getOrDefault(recipeId, gameTime);
            if (gameTime - last > recipeStallTimeoutTicks) {
                // give up on this recipe (its inputs are lost); a paused machine refreshes the time, see pauseActiveRecipes
                try {
                    if (recipe != null && portStorages != null) {
                        recipe.ditchRecipe(level, state, portStorages);
                    }
                } catch (Throwable ignored) { }
                toRemove.add(recipeId);
                // Set a backoff timer so recipe doesn't immediately re-attempt if it's still blocked
                int recipeSkipCooldownTicks = 20;
                recipeNextCheckTime.put(recipeId, gameTime + recipeSkipCooldownTicks);
                continue;
            }
            if (recipe != null) {
                RunStep step = tickRun(recipe, state, portStorages);
                if (step == RunStep.FINISHED) {
                    toRemove.add(recipeId);
                    MMInteropManager.KUBEJS.ifPresent(kjs -> kjs.onRecipeFinish(this, recipeId));
                    continue;
                }
                if (step == RunStep.OUTPUTS_BLOCKED) {
                    int recipeSkipCooldownTicks = 100;
                    recipeNextCheckTime.put(recipeId, gameTime + recipeSkipCooldownTicks);
                }
                activeRecipeLastUpdate.put(recipeId, gameTime);
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

    /** A player is breaking the block at {@code pos}; only a block of the formed machine unforms it. */
    public void onNearbyBlockBroken(BlockPos pos) {
        if (structure != null && level != null && structure.getPositions(level, getBlockPos()).contains(pos)) {
            invalidateProgress();
        }
    }

    public void invalidateProgress() {
        setChanged();
        structure = null;
        isFormed = false;
        // running recipes already took their inputs: they wait (see pauseActiveRecipes) until the machine forms again
        forgetPorts();
    }

    /**
     * After the machine formed (again): running recipes of another structure can't continue, the others go on
     * where they stopped.
     */
    private void keepRecipesOf(StructureModel formed) {
        activeRecipes.keySet().removeIf(id -> {
            RecipeModel recipe = MachineRecipeManager.RECIPES.get(id);
            boolean other = recipe != null && !recipe.allStructureIds().contains(formed.id());
            if (other) {
                activeRecipeLastUpdate.remove(id);
            }
            return other;
        });
    }

    /** Running recipes don't stall (and get dropped) while the machine waits for its structure or its chunks. */
    private void pauseActiveRecipes(long gameTime) {
        for (ResourceLocation rid : activeRecipes.keySet()) {
            activeRecipeLastUpdate.put(rid, gameTime);
        }
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
         forgetPorts();
     }

     /** Drops the cached port storages and everything derived from them; they're collected again when needed. */
     private void forgetPorts() {
         portStorages = null;
         portBlockEntities = List.of();
         structureChunks = null;
         // clear cached views and backoff timers as recipe state is reset
         storageCache.clear();
         recipeNextCheckTime.clear();
         recipeIndex = null;
         candidatePositions = null;
     }

    @Override
    public ControllerModel getModel() { return model; }

    /** The GeckoLib model to draw now: the formed structure's geoModel, else the controller's (client side). */
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
    public AABB getRenderBoundingBox() {
        // a model can reach well outside the controller block: don't cull it with the block
        if (level != null && level.isClientSide() && geoModel() != null) {
            return INFINITE_EXTENT_AABB;
        }
        return super.getRenderBoundingBox();
    }

    @Override
    public @NotNull Component getDisplayName() { return getName(); }

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
        if (!patternRuns.isEmpty()) {
            var runsTag = new ListTag();
            for (PatternRun run : patternRuns) {
                runsTag.add(run.save());
            }
            tag.put("PatternRuns", runsTag);
        }
        if (structure != null) tag.putString("structureId", structure.id().toString());
        tag.putBoolean("isFormed", isFormed);
        tag.putLongArray("litPorts", litPorts.stream().mapToLong(BlockPos::asLong).toArray());
        if (lastStartedRecipeId != null) tag.putString("lastStartedRecipeId", lastStartedRecipeId.toString());
        tag.putLong("lastRecipeStartTime", lastRecipeStartTime);
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
        if (networkLink != null) {
            tag.put("NetworkLink", networkLink.save());
        }
        // persist redstone mode
        try {
            tag.putInt("redstoneMode", redstoneMode.ordinal());
        } catch (Throwable ignored) { }
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
        if (!assemblyTiers.asMap().isEmpty()) tag.put("AssemblyTiers", assemblyTiers.save());
        if (assemblyStructureId != null) tag.putString("AssemblyStructure", assemblyStructureId.toString());
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
        patternRuns.clear();
        for (Tag element : tag.getList("PatternRuns", Tag.TAG_COMPOUND)) {
            PatternRun run = PatternRun.load((CompoundTag) element);
            if (run != null) {
                patternRuns.add(run);
            }
        }
        if (tag.contains("structureId")) {
            String s = tag.getString("structureId");
            ResourceLocation structureId = ResourceLocation.tryParse(s);
            structure = StructureManager.STRUCTURES.get(structureId);
        } else {
            structure = null;
        }
        litPorts = new HashSet<>();
        for (long packed : tag.getLongArray("litPorts")) {
            litPorts.add(BlockPos.of(packed));
        }
        if (tag.contains("isFormed")) {
            isFormed = tag.getBoolean("isFormed");
        } else {
            isFormed = (structure != null);
        }
        lastStartedRecipeId = tag.contains("lastStartedRecipeId") ? ResourceLocation.tryParse(tag.getString("lastStartedRecipeId")) : null;
        lastRecipeStartTime = tag.contains("lastRecipeStartTime") ? tag.getLong("lastRecipeStartTime") : Long.MIN_VALUE;
        lastStartedInputItemId = tag.contains("lastStartedInputItemId") ? ResourceLocation.tryParse(tag.getString("lastStartedInputItemId")) : null;
        recipeSelectionSequence = tag.contains("recipeSelectionSequence") ? tag.getLong("recipeSelectionSequence") : 0L;
        networkLink = tag.contains("NetworkLink") ? LinkData.load(tag.getCompound("NetworkLink")) : null;
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
        recipeModeOverride = tag.contains("RecipeSelectionMode") ? RecipeSelectionMode.parse(tag.getString("RecipeSelectionMode")) : null;
        customName = tag.contains("CustomName") ? tag.getString("CustomName") : null;
        soundMuted = tag.getBoolean("SoundMuted");
        ModelDisplay display = ModelDisplay.parse(tag.getString("ModelDisplay"));
        modelDisplay = display == null ? ModelDisplay.ON : display;
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
        assemblyTiers.copyFrom(TierPrefs.load(tag.getCompound("AssemblyTiers")));
        assemblyStructureId = tag.contains("AssemblyStructure") ? ResourceLocation.tryParse(tag.getString("AssemblyStructure")) : null;
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

    /**
     * The recipe to show on the controller screen: the running one, or else the last one started if that
     * was within the last second. Recipes lasting a tick or so start and finish between two syncs, so
     * the running recipe alone would make a busy machine look idle.
     */
    @Nullable
    public RecipeModel getDisplayedRecipe() {
        if (currentRecipe != null) {
            return currentRecipe;
        }
        if (level == null || lastStartedRecipeId == null || lastRecipeStartTime == Long.MIN_VALUE || level.getGameTime() - lastRecipeStartTime > RECENT_RECIPE_TICKS) {
            return null;
        }
        return MachineRecipeManager.RECIPES.get(lastStartedRecipeId);
    }

    /**
     * What keeps the multiblock from forming, for the controller screen; empty when it is formed.
     */
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

    public void setNetworkLink(@Nullable LinkData link) {
        this.networkLink = link;
        setChanged();
    }

    /**
     * @return the formed machine's ports as last looked up by the controller, or null if not formed / not looked up yet
     */
    @Nullable
    public RecipeStorages getCachedPortStorages() {
        return structure == null ? null : portStorages;
    }

    public RecipeStateModel getRecipeState() {
        if (activeRecipes.isEmpty()) return null;
        return activeRecipes.values().iterator().next();
    }

    public int getActiveRecipeCount() {
        return activeRecipes.size();
    }

    /** The recipe order set on this controller (screen), else the controller type's default. */
    public RecipeSelectionMode getRecipeSelectionMode() {
        return recipeModeOverride != null ? recipeModeOverride : controllerModel.recipeSelectionMode();
    }

    public void setRecipeSelectionMode(RecipeSelectionMode mode) {
        recipeModeOverride = mode == controllerModel.recipeSelectionMode() ? null : mode;
        storageCache.isValid = false;
        setChanged();
        if (level != null) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    public void setSoundMuted(boolean muted) {
        soundMuted = muted;
        setChanged();
        if (level != null) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    public void setModelDisplay(ModelDisplay display) {
        modelDisplay = display;
        setChanged();
        if (level != null) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public @NotNull Component getName() {
        return customName != null ? Component.literal(customName) : Component.literal(model.name());
    }

    @Override
    public @Nullable Component getCustomName() {
        return customName == null ? null : Component.literal(customName);
    }

    /** A name the player gave this controller, or null/blank for the machine's own name. */
    public void setCustomName(@Nullable String name) {
        customName = name == null || name.isBlank() ? null : name.strip();
        if (customName != null && customName.length() > MAX_NAME_LENGTH) {
            customName = customName.substring(0, MAX_NAME_LENGTH);
        }
        setChanged();
        if (level != null) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
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
            else {
                keepRecipesOf(this.structure);
                if (this.isFormed && triggerRecipe) runRecipe();
            }
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

    /** Stores a tier a client chose; ignores keys this machine does not have and clamps the tier to what exists. */
    public void setAssemblyTier(String key, int rank) {
        int valid = TierPrefs.validate(PortTiers.maxTiers(getAssemblyCandidates()), key, rank);
        if (valid != TierPrefs.REJECTED && assemblyTiers.set(key, valid)) {
            syncAssemblySettings();
        }
    }

    public @Nullable ResourceLocation getAssemblyStructureId() {
        return assemblyStructureId;
    }

    /** Ignores ids that are not one of {@link #getAssemblyCandidates()}. */
    public void setAssemblyStructureId(ResourceLocation id) {
        if (findAssemblyCandidate(id) == null || id.equals(assemblyStructureId)) {
            return;
        }
        assemblyStructureId = id;
        syncAssemblySettings();
    }

    /** Structures this controller can assemble, one per id (the latest definition), sorted by id so client and server agree. */
    public List<StructureModel> getAssemblyCandidates() {
        var byId = new TreeMap<ResourceLocation, StructureModel>();
        for (StructureModel structure : StructureManager.getStructuresForController(controllerId)) {
            byId.put(structure.id(), structure); // last wins, like StructureManager.STRUCTURES
        }
        return List.copyOf(byId.values());
    }

    /** The chosen structure, else the one this controller has (formed or last found), else the first candidate. */
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

    public @Nullable StructureModel findAssemblyCandidate(@Nullable ResourceLocation id) {
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

    private void syncAssemblySettings() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
