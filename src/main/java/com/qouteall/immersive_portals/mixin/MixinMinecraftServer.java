package com.qouteall.immersive_portals.mixin;

import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.datafixers.DataFixer;
import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.ducks.IEMinecraftServer;
import net.minecraft.command.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.FrameTimer;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.listener.IChunkStatusListenerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.Proxy;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer implements IEMinecraftServer {
    @Shadow
    @Final
    private FrameTimer frameTimer;
    
    @Shadow
    @Final
    private File anvilFile;
    
    private boolean portal_areAllWorldsLoaded;
    
    @Inject(
        method = "Lnet/minecraft/server/MinecraftServer;<init>(Ljava/io/File;Ljava/net/Proxy;Lcom/mojang/datafixers/DataFixer;Lnet/minecraft/command/Commands;Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;Lcom/mojang/authlib/minecraft/MinecraftSessionService;Lcom/mojang/authlib/GameProfileRepository;Lnet/minecraft/server/management/PlayerProfileCache;Lnet/minecraft/world/chunk/listener/IChunkStatusListenerFactory;Ljava/lang/String;)V",
        at = @At("RETURN")
    )
    private void onServerConstruct(
        File file_1,
        Proxy proxy_1,
        DataFixer dataFixer_1,
        Commands commandManager_1,
        YggdrasilAuthenticationService yggdrasilAuthenticationService_1,
        MinecraftSessionService minecraftSessionService_1,
        GameProfileRepository gameProfileRepository_1,
        PlayerProfileCache userCache_1,
        IChunkStatusListenerFactory worldGenerationProgressListenerFactory_1,
        String string_1,
        CallbackInfo ci
    ) {
        McHelper.refMinecraftServer = new WeakReference<>((MinecraftServer) ((Object) this));
    
        O_O.loadConfigFabric();
    }
    
    @Inject(
        method = "Lnet/minecraft/server/MinecraftServer;updateTimeLightAndEntities(Ljava/util/function/BooleanSupplier;)V",
        at = @At("TAIL")
    )
    private void onServerTick(BooleanSupplier booleanSupplier_1, CallbackInfo ci) {
        ModMain.postServerTickSignal.emit();
    }
    
    @Inject(
        method = "Lnet/minecraft/server/MinecraftServer;run()V",
        at = @At("RETURN")
    )
    private void onServerClose(CallbackInfo ci) {
        NewChunkTrackingGraph.cleanup();
        ModMain.serverTaskList.forceClearTasks();
    }
    
    @Inject(
        method = "Lnet/minecraft/server/MinecraftServer;loadAllWorlds(Ljava/lang/String;Ljava/lang/String;JLnet/minecraft/world/WorldType;Lcom/google/gson/JsonElement;)V",
        at = @At("RETURN")
    )
    private void onFinishedLoadingAllWorlds(
        String name,
        String serverName,
        long seed,
        WorldType generatorType,
        JsonElement generatorSettings,
        CallbackInfo ci
    ) {
        portal_areAllWorldsLoaded = true;
    }
    
    @Override
    public FrameTimer getMetricsDataNonClientOnly() {
        return frameTimer;
    }
    
    @Override
    public boolean portal_getAreAllWorldsLoaded() {
        return portal_areAllWorldsLoaded;
    }
}
