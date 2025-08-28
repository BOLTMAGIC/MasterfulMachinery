package io.ticticboom.mods.mm.cap;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import vazkii.botania.api.block.WandHUD;
import vazkii.botania.api.mana.ManaReceiver;

public class BotaniaCapabilities {
    public static final Capability<WandHUD> WAND_HUD = CapabilityManager.get(new CapabilityToken<>(){});
    public static final Capability<ManaReceiver> MANA_RECEIVER = CapabilityManager.get(new CapabilityToken<>(){});
}
