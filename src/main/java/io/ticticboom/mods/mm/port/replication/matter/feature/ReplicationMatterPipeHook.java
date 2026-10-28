package io.ticticboom.mods.mm.port.replication.matter.feature;

import com.buuz135.replication.block.MatterPipeBlock;
import io.ticticboom.mods.mm.port.replication.link.register.ReplicationLinkPortBlock;
import io.ticticboom.mods.mm.port.replication.matter.register.ReplicationMatterPortBlock;

/**
 * Titanium links the port into the matter network on its own, but a matter pipe only draws a
 * connection towards blocks in Replication's own namespace unless something adds to this list.
 * Without it the port works while looking unplugged.
 */
public class ReplicationMatterPipeHook {

    public static void register() {
        MatterPipeBlock.ALLOWED_CONNECTION_BLOCKS.add(block -> block instanceof ReplicationMatterPortBlock || block instanceof ReplicationLinkPortBlock);
    }
}
