package io.ticticboom.mods.mm.port.replication.matter.register;

import com.buuz135.replication.api.network.IMatterTanksSupplier;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class ReplicationMatterOutputPortBlockEntity extends ReplicationMatterPortBlockEntity implements IMatterTanksSupplier {

    public ReplicationMatterOutputPortBlockEntity(PortModel model, RegistryGroupHolder groupHolder, BlockPos pos, BlockState state) {
        super(model, groupHolder, false, pos, state);
    }
}
