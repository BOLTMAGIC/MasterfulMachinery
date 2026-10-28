package io.ticticboom.mods.mm.builder;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class PlayerMaterialSource implements MaterialSource {
    private final Player player;

    public PlayerMaterialSource(Player player) {
        this.player = player;
    }

    @Override
    public boolean has(Block block) {
        return PlayerMaterials.has(player, block);
    }

    @Override
    public ItemStack take(Block block) {
        ItemStack taken = PlayerMaterials.take(player, block);
        return taken == null ? ItemStack.EMPTY : taken;
    }

    @Override
    public void refund(ItemStack stack) {
        PlayerMaterials.refund(player, stack);
    }

    @Override
    public boolean free() {
        return player.getAbilities().instabuild;
    }
}
