package io.ticticboom.mods.mm.port.replication.matter;

import com.buuz135.replication.api.IMatterType;
import com.buuz135.replication.api.matter_fluid.MatterStack;
import com.buuz135.replication.api.matter_fluid.MatterTank;
import io.ticticboom.mods.mm.port.common.INotifyChangeFunction;
import net.minecraft.resources.Identifier;

public class ReplicationMatterPortTank extends MatterTank {

    private final Identifier filter;
    private final INotifyChangeFunction changed;

    public ReplicationMatterPortTank(int capacity, Identifier filter, INotifyChangeFunction changed) {
        super(capacity);
        this.filter = filter;
        this.changed = changed;
        setValidator(this::matches);
    }

    private boolean matches(MatterStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (filter == null) {
            return true;
        }
        return filter.equals(MatterTypes.idOf(stack.getMatterType()));
    }

    public Identifier getFilter() {
        return filter;
    }

    /**
     * The matter this tank will ask the network for: whatever it already holds, falling back to
     * the type the pack locked it to.
     */
    public IMatterType getRequestedType() {
        if (!getMatter().isEmpty()) {
            return getMatter().getMatterType();
        }
        return MatterTypes.get(filter);
    }

    @Override
    protected void onContentsChanged() {
        if (changed != null) {
            changed.call();
        }
    }
}
