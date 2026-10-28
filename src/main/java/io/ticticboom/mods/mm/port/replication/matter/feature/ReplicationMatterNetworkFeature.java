package io.ticticboom.mods.mm.port.replication.matter.feature;

import com.buuz135.replication.api.IMatterType;
import com.buuz135.replication.api.matter_fluid.IMatterTank;
import com.buuz135.replication.api.matter_fluid.MatterStack;
import com.buuz135.replication.api.network.IMatterTanksSupplier;
import com.buuz135.replication.network.DefaultMatterNetworkElement;
import com.buuz135.replication.network.MatterNetwork;
import com.hrznstudio.titanium.block_network.NetworkManager;
import io.ticticboom.mods.mm.port.replication.matter.MatterTypes;
import io.ticticboom.mods.mm.port.replication.matter.ReplicationMatterPortIngredient;
import io.ticticboom.mods.mm.port.replication.matter.ReplicationMatterPortStorage;
import io.ticticboom.mods.mm.port.replication.matter.ReplicationMatterPortTank;
import io.ticticboom.mods.mm.port.replication.matter.register.ReplicationMatterPortBlockEntity;
import io.ticticboom.mods.mm.recipe.MachineRecipeManager;
import io.ticticboom.mods.mm.recipe.input.consume.ConsumeRecipeIngredientEntry;
import io.ticticboom.mods.mm.structure.StructureManager;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ReplicationMatterNetworkFeature {

    private static final int PULL_INTERVAL = 5;
    private static final int RECIPE_SCAN_INTERVAL = 100;

    private final ReplicationMatterPortBlockEntity portBlockEntity;
    private boolean joined = false;
    private boolean unloaded = false;
    private List<IMatterType> recipeTypes = List.of();
    private long lastRecipeScan = Long.MIN_VALUE / 2;

    public ReplicationMatterNetworkFeature(ReplicationMatterPortBlockEntity portBlockEntity) {
        this.portBlockEntity = portBlockEntity;
    }

    public void onLoad() {
        if (!(portBlockEntity.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var manager = NetworkManager.get(level);
        if (manager.getElement(portBlockEntity.getBlockPos()) == null) {
            manager.addElement(new DefaultMatterNetworkElement(level, portBlockEntity.getBlockPos()));
        }
        joined = true;
    }

    public void onChunkUnloaded() {
        unloaded = true;
    }

    public void onRemoved() {
        if (unloaded || !joined) {
            return;
        }
        if (!(portBlockEntity.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var manager = NetworkManager.get(level);
        var element = manager.getElement(portBlockEntity.getBlockPos());
        manager.removeElement(portBlockEntity.getBlockPos());
        if (element != null && element.getNetwork() instanceof MatterNetwork matterNetwork) {
            matterNetwork.removeElement(element);
        }
        joined = false;
    }

    /**
     * Output ports are suppliers, and the network empties those into its tanks on its own. Input
     * ports are consumers, which the network never fills, so they go and take what they are short
     * of the way a replicator does.
     */
    public void tick() {
        if (!portBlockEntity.isInput()) {
            return;
        }
        if (!(portBlockEntity.getLevel() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        if (now % PULL_INTERVAL != 0) {
            return;
        }
        if (!(portBlockEntity.getStorage() instanceof ReplicationMatterPortStorage storage)) {
            return;
        }
        var network = network(level);
        if (network == null) {
            return;
        }
        var tanks = storage.getHandler().tanks();
        var covered = coveredTypes(tanks);
        var demanded = storage.hasFreshDemand(now) ? storage.demandedMatterTypes() : null;
        for (ReplicationMatterPortTank tank : tanks) {
            var type = tank.getRequestedType();
            if (type == null) {
                type = nextWantedType(now, covered, storage);
                if (type == null) {
                    continue;
                }
                covered.add(type);
            } else if (demanded != null && tank.getFilter() == null && !demanded.contains(type)) {
                continue;
            }
            double wanted = tank.getCapacity() - tank.getMatterAmount();
            if (wanted <= 0) {
                continue;
            }
            pull(level, network, tank, type, wanted);
        }
    }

    private Set<IMatterType> coveredTypes(List<ReplicationMatterPortTank> tanks) {
        var covered = new HashSet<IMatterType>();
        for (ReplicationMatterPortTank tank : tanks) {
            var type = tank.getRequestedType();
            if (type != null) {
                covered.add(type);
            }
        }
        return covered;
    }

    private IMatterType nextWantedType(long now, Set<IMatterType> covered, ReplicationMatterPortStorage storage) {
        List<IMatterType> candidates;
        if (storage.hasFreshDemand(now)) {
            candidates = storage.demandedMatterTypes();
        } else {
            if (now - lastRecipeScan >= RECIPE_SCAN_INTERVAL) {
                lastRecipeScan = now;
                recipeTypes = scanRecipeTypes();
            }
            candidates = recipeTypes;
        }
        for (IMatterType type : candidates) {
            if (!covered.contains(type)) {
                return type;
            }
        }
        return null;
    }

    private List<IMatterType> scanRecipeTypes() {
        var result = new LinkedHashSet<IMatterType>();
        for (var controllerId : portBlockEntity.getModel().controllerIds().getIds()) {
            for (var structure : StructureManager.getStructuresForController(controllerId)) {
                for (var recipe : MachineRecipeManager.getRecipesByStrucutreId(structure.id())) {
                    for (var entry : recipe.inputs().inputs()) {
                        if (entry instanceof ConsumeRecipeIngredientEntry consume
                                && consume.getIngredient() instanceof ReplicationMatterPortIngredient matter) {
                            var type = MatterTypes.get(matter.getMatterId());
                            if (type != null) {
                                result.add(type);
                            }
                        }
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    private void pull(ServerLevel level, MatterNetwork network, ReplicationMatterPortTank tank, IMatterType type, double wanted) {
        for (var element : network.getMatterStacksHolders()) {
            if (wanted <= 0) {
                return;
            }
            if (element.getLevel() != level || !level.isLoaded(element.getPos())) {
                continue;
            }
            if (element.getPos().equals(portBlockEntity.getBlockPos())) {
                continue;
            }
            if (!(level.getBlockEntity(element.getPos()) instanceof IMatterTanksSupplier supplier)) {
                continue;
            }
            for (IMatterTank source : supplier.getTanks()) {
                if (wanted <= 0) {
                    return;
                }
                if (source.getMatter().isEmpty() || source.getMatter().getMatterType() != type) {
                    continue;
                }
                var available = source.drain(new MatterStack(type, wanted), IFluidHandler.FluidAction.SIMULATE);
                if (available.isEmpty()) {
                    continue;
                }
                double accepted = tank.fill(available, IFluidHandler.FluidAction.SIMULATE);
                if (accepted <= 0) {
                    continue;
                }
                var taken = source.drain(new MatterStack(type, accepted), IFluidHandler.FluidAction.EXECUTE);
                if (taken.isEmpty()) {
                    continue;
                }
                wanted -= tank.fill(taken, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    private MatterNetwork network(ServerLevel level) {
        var element = NetworkManager.get(level).getElement(portBlockEntity.getBlockPos());
        if (element == null) {
            return null;
        }
        return element.getNetwork() instanceof MatterNetwork matterNetwork ? matterNetwork : null;
    }
}
