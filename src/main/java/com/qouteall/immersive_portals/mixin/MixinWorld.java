package com.qouteall.immersive_portals.mixin;

import com.qouteall.immersive_portals.exposer.IEWorld;
import net.minecraft.world.World;
import net.minecraft.world.chunk.AbstractChunkProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(World.class)
public class MixinWorld implements IEWorld {
    @Shadow
    @Final
    @Mutable
    protected AbstractChunkProvider chunkProvider;
    
    @Override
    public void setChunkManager(AbstractChunkProvider manager) {
        chunkProvider = manager;
    }
}