package io.ticticboom.mods.mm.port.ae2.pattern.feature;

import io.ticticboom.mods.mm.util.LegacyNbt;
import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import io.ticticboom.mods.mm.compat.ae2.Ae2KeyBridge;
import io.ticticboom.mods.mm.compat.ae2.Ae2KeyBridges;
import io.ticticboom.mods.mm.compat.ae2.Ae2PushContext;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.port.ae2.pattern.register.Ae2PatternPortBlockEntity;
import io.ticticboom.mods.mm.port.fluid.FluidPortIngredient;
import io.ticticboom.mods.mm.port.IPortIngredient;
import io.ticticboom.mods.mm.port.fluid.FluidPortStorage;
import io.ticticboom.mods.mm.port.item.ItemPortStorage;
import io.ticticboom.mods.mm.port.item.SingleItemPortIngredient;
import io.ticticboom.mods.mm.recipe.MachineRecipeManager;
import io.ticticboom.mods.mm.recipe.RecipeModel;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.recipe.input.consume.ConsumeRecipeIngredientEntry;
import io.ticticboom.mods.mm.recipe.output.simple.SimpleRecipeOutputEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Ae2PatternFeature implements ICraftingProvider {

    private static final int OUTPUT_INTERVAL = 5;
    private static final long PUSH_GRACE = 40;

    private final Ae2PatternPortBlockEntity port;
    private final IManagedGridNode node;
    private final Map<AEItemKey, Identifier> recipesByPattern = new HashMap<>();

    private List<IPatternDetails> patterns = List.of();
    private Identifier patternStructure = null;
    private int patternRecipeVersion = -1;
    private Identifier pushedRecipe = null;
    private long pushedAt = 0;
    private List<Ae2KeyBridge> bridges;

    public Ae2PatternFeature(Ae2PatternPortBlockEntity port) {
        this.port = port;
        this.node = GridHelper.createManagedNode(this, NodeListener.INSTANCE)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setInWorldNode(true)
                .setIdlePowerUsage(1)
                .setVisualRepresentation(port.getBlockState().getBlock())
                .addService(ICraftingProvider.class, this);
    }

    public IManagedGridNode getNode() {
        return node;
    }

    public void onLoad() {
        if (port.getLevel() instanceof ServerLevel) {
            GridHelper.onFirstTick(port, be -> node.create(be.getLevel(), be.getBlockPos()));
        }
    }

    public void onRemoved() {
        node.destroy();
    }

    public void saveNode(CompoundTag tag) {
        tag.merge(LegacyNbt.save(node, LegacyNbt.registries(null)));
    }

    public void loadNode(CompoundTag tag) {
        LegacyNbt.load(node, LegacyNbt.registries(null), tag);
    }

    public void tick() {
        if (!(port.getLevel() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        var controller = port.getPatternStorage().getController(now);
        refreshPatterns(level, controller);
        if (now % OUTPUT_INTERVAL != 0 || controller == null) {
            return;
        }
        pushOutputs(level, controller);
        if (pushedRecipe != null && now - pushedAt > PUSH_GRACE && !controller.isRecipeRunning(pushedRecipe)) {
            pushedRecipe = null;
        }
    }

    private void refreshPatterns(ServerLevel level, MachineControllerBlockEntity controller) {
        var structure = controller == null ? null : controller.getStructure();
        var structureId = structure == null ? null : structure.id();
        if (Objects.equals(structureId, patternStructure) && patternRecipeVersion == MachineRecipeManager.RECIPE_VERSION) {
            return;
        }
        patternStructure = structureId;
        patternRecipeVersion = MachineRecipeManager.RECIPE_VERSION;
        recipesByPattern.clear();
        patterns = structureId == null ? List.of() : buildPatterns(level, structureId);
        ICraftingProvider.requestUpdate(node);
    }

    private List<IPatternDetails> buildPatterns(ServerLevel level, Identifier structureId) {
        var result = new ArrayList<IPatternDetails>();
        for (RecipeModel recipe : MachineRecipeManager.getRecipesByStrucutreId(structureId)) {
            var inputs = patternInputs(recipe);
            var outputs = patternOutputs(recipe);
            if (inputs.isEmpty() || outputs.isEmpty()) {
                continue;
            }
            var encoded = PatternDetailsHelper.encodeProcessingPattern(inputs, outputs);
            var details = PatternDetailsHelper.decodePattern(encoded, level);
            if (details == null) {
                continue;
            }
            result.add(details);
            recipesByPattern.put(details.getDefinition(), recipe.id());
        }
        return List.copyOf(result);
    }

    private List<Ae2KeyBridge> bridges() {
        if (bridges == null) {
            bridges = Ae2KeyBridges.enabled(port.getPatternStorage().getExcluded());
        }
        return bridges;
    }

    private List<GenericStack> patternInputs(RecipeModel recipe) {
        var result = new ArrayList<GenericStack>();
        for (var entry : recipe.inputs().inputs()) {
            if (!(entry instanceof ConsumeRecipeIngredientEntry consume)) {
                continue;
            }
            var stack = inputStack(consume.getIngredient(), consume.isPerTick(), recipe.ticks());
            if (stack != null) {
                result.add(stack);
            }
        }
        return result;
    }

    private List<GenericStack> patternOutputs(RecipeModel recipe) {
        var result = new ArrayList<GenericStack>();
        for (var entry : recipe.outputs().outputs()) {
            if (!(entry instanceof SimpleRecipeOutputEntry simple)) {
                continue;
            }
            var stack = outputStack(simple.getIngredient());
            if (stack != null) {
                result.add(stack);
            }
        }
        return result;
    }

    private GenericStack inputStack(IPortIngredient ingredient, boolean perTick, int ticks) {
        var stack = builtInStack(ingredient);
        if (stack != null) {
            return stack;
        }
        for (Ae2KeyBridge bridge : bridges()) {
            stack = bridge.input(ingredient, perTick, ticks);
            if (stack != null) {
                return stack;
            }
        }
        return null;
    }

    private GenericStack outputStack(IPortIngredient ingredient) {
        var stack = builtInStack(ingredient);
        if (stack != null) {
            return stack;
        }
        for (Ae2KeyBridge bridge : bridges()) {
            stack = bridge.output(ingredient);
            if (stack != null) {
                return stack;
            }
        }
        return null;
    }

    private GenericStack builtInStack(IPortIngredient ingredient) {
        if (ingredient instanceof SingleItemPortIngredient item) {
            return new GenericStack(AEItemKey.of(item.outputStack()), item.getCount());
        }
        if (ingredient instanceof FluidPortIngredient fluid) {
            var found = BuiltInRegistries.FLUID.getValue(fluid.getFluidId());
            return found == null ? null : new GenericStack(AEFluidKey.of(found), fluid.getAmountRange().max());
        }
        return null;
    }

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        return patterns;
    }

    @Override
    public int getPatternPriority() {
        return port.getPatternStorage().getPatternPriority();
    }

    @Override
    public boolean isBusy() {
        return pushedRecipe != null;
    }

    @Override
    public boolean pushPattern(IPatternDetails details, KeyCounter[] inputHolder) {
        if (isBusy() || !(port.getLevel() instanceof ServerLevel level)) {
            return false;
        }
        long now = level.getGameTime();
        var controller = port.getPatternStorage().getController(now);
        if (controller == null) {
            return false;
        }
        var recipeId = recipesByPattern.get(details.getDefinition());
        var recipe = recipeId == null ? null : MachineRecipeManager.RECIPES.get(recipeId);
        var storages = controller.getPortStorages();
        if (recipe == null || storages == null) {
            return false;
        }
        var grid = node.getGrid();
        MEStorage me = grid == null ? null : grid.getStorageService().getInventory();
        var source = IActionSource.ofMachine(port);
        if (!insertInputs(new Ae2PushContext(recipe, storages, me, source, true), inputHolder)) {
            return false;
        }
        insertInputs(new Ae2PushContext(recipe, storages, me, source, false), inputHolder);
        controller.requestRecipe(this, recipeId, now, 1);
        pushedRecipe = recipeId;
        pushedAt = now;
        return true;
    }

    private boolean insertInputs(Ae2PushContext context, KeyCounter[] inputHolder) {
        for (KeyCounter counter : inputHolder) {
            for (var entry : counter) {
                if (!insertKey(context, entry.getKey(), entry.getLongValue())) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean insertKey(Ae2PushContext context, AEKey key, long amount) {
        if (amount <= 0) {
            return true;
        }
        for (Ae2KeyBridge bridge : bridges()) {
            if (bridge.handles(key)) {
                return bridge.insert(context, key, amount);
            }
        }
        var storages = context.storages();
        boolean simulate = context.simulate();
        if (key instanceof AEItemKey item) {
            int remaining = (int) amount;
            for (ItemPortStorage storage : storages.getInputStorages(ItemPortStorage.class)) {
                var stack = item.toStack(remaining);
                remaining = simulate ? storage.canInsert(stack, remaining) : storage.insert(stack, remaining);
                if (remaining <= 0) {
                    return true;
                }
            }
            return false;
        }
        if (key instanceof AEFluidKey fluid) {
            int remaining = (int) amount;
            var action = simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE;
            for (FluidPortStorage storage : storages.getInputStorages(FluidPortStorage.class)) {
                remaining -= storage.getWrappedHandler().fill(new FluidStack(fluid.getFluid(), remaining), action);
                if (remaining <= 0) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private void pushOutputs(ServerLevel level, MachineControllerBlockEntity controller) {
        var grid = node.getGrid();
        var storages = controller.getPortStorages();
        if (grid == null || storages == null || !node.isActive()) {
            return;
        }
        var me = grid.getStorageService().getInventory();
        var source = IActionSource.ofMachine(port);
        var kept = new HashMap<AEItemKey, Integer>();
        for (ItemPortStorage storage : storages.getOutputStorages(ItemPortStorage.class)) {
            var handler = storage.getHandler();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                var key = AEItemKey.of(stack);
                int count = handler.getActualCount(slot);
                int keep = Math.min(count, controller.reservedOutputCount(stack) - kept.getOrDefault(key, 0));
                if (keep > 0) {
                    kept.merge(key, keep, Integer::sum);
                    count -= keep;
                }
                if (count <= 0) {
                    continue;
                }
                long inserted = me.insert(key, count, Actionable.MODULATE, source);
                if (inserted > 0) {
                    handler.extractItem(slot, (int) inserted, false);
                }
            }
        }
        for (FluidPortStorage storage : storages.getOutputStorages(FluidPortStorage.class)) {
            var handler = storage.getWrappedHandler();
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                var stack = handler.getFluidInTank(tank);
                if (stack.isEmpty()) {
                    continue;
                }
                long inserted = me.insert(AEFluidKey.of(stack), stack.getAmount(), Actionable.MODULATE, source);
                if (inserted > 0) {
                    handler.drain(new FluidStack(stack.getFluid(), (int) inserted), IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
        for (Ae2KeyBridge bridge : bridges()) {
            bridge.pushOutputs(storages, me, source);
        }
    }

    private static class NodeListener implements IGridNodeListener<Ae2PatternFeature> {
        private static final NodeListener INSTANCE = new NodeListener();

        @Override
        public void onSaveChanges(Ae2PatternFeature owner, IGridNode node) {
            owner.port.setChanged();
        }
    }
}
