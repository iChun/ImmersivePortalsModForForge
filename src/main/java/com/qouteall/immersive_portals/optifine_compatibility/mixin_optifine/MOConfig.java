package com.qouteall.immersive_portals.optifine_compatibility.mixin_optifine;

import com.qouteall.immersive_portals.optifine_compatibility.OFGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = net.optifine.Config.class, remap = false)
public class MOConfig {
    @Inject(method = "isShaders", at = @At("HEAD"), cancellable = true)
    private static void onIsShaders(CallbackInfoReturnable<Boolean> cir) {
        if (OFGlobal.shaderContextManager.isContextSwitched()) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}