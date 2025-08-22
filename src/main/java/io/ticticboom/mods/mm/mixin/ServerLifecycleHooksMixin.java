package io.ticticboom.mods.mm.mixin;

import io.ticticboom.mods.mm.structure.StructureManager;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLifecycleHooks.class)
public class ServerLifecycleHooksMixin {

    @Inject(method = "handleServerStarted", at = @At("TAIL"), remap = false)
    private static void handleServerStarted(MinecraftServer server, CallbackInfo ci) {
        StructureManager.validateAllPieces();
    }
}
