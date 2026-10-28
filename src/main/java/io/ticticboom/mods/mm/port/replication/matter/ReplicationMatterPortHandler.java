package io.ticticboom.mods.mm.port.replication.matter;

import com.buuz135.replication.api.IMatterType;
import com.buuz135.replication.api.matter_fluid.IMatterHandler;
import com.buuz135.replication.api.matter_fluid.MatterStack;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ReplicationMatterPortHandler implements IMatterHandler {

    private final List<ReplicationMatterPortTank> tanks = new ArrayList<>();

    public ReplicationMatterPortHandler(ReplicationMatterPortStorageModel model, INotifyChangeFunction changed) {
        for (var filter : model.matter()) {
            tanks.add(new ReplicationMatterPortTank(model.capacity(), filter, changed));
        }
    }

    public List<ReplicationMatterPortTank> tanks() {
        return tanks;
    }

    @Override
    public int getTanks() {
        return tanks.size();
    }

    @Override
    public @NotNull MatterStack getMatterInTank(int tank) {
        return tank >= 0 && tank < tanks.size() ? tanks.get(tank).getMatter() : MatterStack.EMPTY;
    }

    @Override
    public double getTankCapacity(int tank) {
        return tank >= 0 && tank < tanks.size() ? tanks.get(tank).getCapacity() : 0;
    }

    @Override
    public boolean isMatterValid(int tank, @NotNull MatterStack stack) {
        return tank >= 0 && tank < tanks.size() && tanks.get(tank).isMatterValid(stack);
    }

    @Override
    public double fill(MatterStack resource, IFluidHandler.FluidAction action) {
        if (resource == null || resource.isEmpty()) {
            return 0;
        }
        double remaining = resource.getAmount();
        for (ReplicationMatterPortTank tank : sortedForFill(resource)) {
            if (remaining <= 0) {
                break;
            }
            remaining -= tank.fill(new MatterStack(resource.getMatterType(), remaining), action);
        }
        return resource.getAmount() - remaining;
    }

    @Override
    public @NotNull MatterStack drain(MatterStack resource, IFluidHandler.FluidAction action) {
        if (resource == null || resource.isEmpty()) {
            return MatterStack.EMPTY;
        }
        return drain(resource.getMatterType(), resource.getAmount(), action);
    }

    @Override
    public @NotNull MatterStack drain(double maxDrain, IFluidHandler.FluidAction action) {
        for (ReplicationMatterPortTank tank : tanks) {
            if (!tank.getMatter().isEmpty()) {
                return drain(tank.getMatter().getMatterType(), maxDrain, action);
            }
        }
        return MatterStack.EMPTY;
    }

    public @NotNull MatterStack drain(IMatterType type, double maxDrain, IFluidHandler.FluidAction action) {
        if (type == null || maxDrain <= 0) {
            return MatterStack.EMPTY;
        }
        double drained = 0;
        for (ReplicationMatterPortTank tank : tanks) {
            if (drained >= maxDrain) {
                break;
            }
            if (tank.getMatter().isEmpty() || tank.getMatter().getMatterType() != type) {
                continue;
            }
            drained += tank.drain(maxDrain - drained, action).getAmount();
        }
        return drained <= 0 ? MatterStack.EMPTY : new MatterStack(type, drained);
    }

    public double fill(IMatterType type, double amount, IFluidHandler.FluidAction action) {
        if (type == null || amount <= 0) {
            return 0;
        }
        return fill(new MatterStack(type, amount), action);
    }

    /**
     * Tanks already holding the matter come first so a partially filled tank tops up before an
     * empty one gets locked to the same type.
     */
    private List<ReplicationMatterPortTank> sortedForFill(MatterStack resource) {
        var result = new ArrayList<ReplicationMatterPortTank>(tanks.size());
        for (ReplicationMatterPortTank tank : tanks) {
            if (!tank.getMatter().isEmpty() && tank.getMatter().isMatterEqual(resource)) {
                result.add(tank);
            }
        }
        for (ReplicationMatterPortTank tank : tanks) {
            if (tank.getMatter().isEmpty()) {
                result.add(tank);
            }
        }
        return result;
    }
}
