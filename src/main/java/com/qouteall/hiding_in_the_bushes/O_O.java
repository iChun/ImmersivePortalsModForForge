package com.qouteall.hiding_in_the_bushes;

import com.qouteall.hiding_in_the_bushes.fix_model_data.ModelDataHacker;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class O_O {
    public static boolean isForge() {
        return true;
    }
    
    @OnlyIn(Dist.CLIENT)
    public static void onPlayerChangeDimensionClient(
        DimensionType from, DimensionType to
    ) {
        ModelDataHacker.updateForgeModelData();
    }
    
    @OnlyIn(Dist.CLIENT)
    public static void segregateClientEntity(
        ClientWorld fromWorld,
        Entity entity
    ) {
        ((IEClientWorld_MA) fromWorld).removeEntityWhilstMaintainingCapability(entity);
        entity.revive();
    }
    
    public static void segregateServerEntity(
        ServerWorld fromWorld,
        Entity entity
    ) {
        fromWorld.removeEntity(entity, true);
        entity.revive();
    }
    
    public static void segregateServerPlayer(
        ServerWorld fromWorld,
        ServerPlayerEntity player
    ) {
        fromWorld.removePlayer(player, true);
        player.revive();
    }
    
    public static void onPlayerTravelOnServer(
        ServerPlayerEntity player,
        DimensionType from,
        DimensionType to
    ) {
        Global.serverTeleportationManager.isFiringMyChangeDimensionEvent = true;
        net.minecraftforge.fml.hooks.BasicEventHooks.firePlayerChangedDimensionEvent(
            player, from, to
        );
        Global.serverTeleportationManager.isFiringMyChangeDimensionEvent = false;
    }
    
    public static void loadConfigFabric() {
        //nothing
    }
    
    public static boolean isObsidian(IWorld world, BlockPos obsidianPos) {
        return world.getBlockState(obsidianPos).isPortalFrame(world, obsidianPos);
    }
    
    public static final boolean isReachEntityAttributesPresent = false;
    
    public static void registerDimensionsForge() {
        try {
            DimensionManager.fireRegister();
        }
        catch (Throwable e) {
            Helper.err("Exception When Registering Dimensions " + e);
        }
        
    }
    
    public static boolean detectOptiFine() {
        try {
            //do not load other optifine classes that loads vanilla classes
            //that would load the class before mixin
            Class.forName("optifine.ZipResourceProvider");
            return true;
        }
        catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    public static void postChunkUnloadEventForge(Chunk chunk) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
            new net.minecraftforge.event.world.ChunkEvent.Unload(chunk)
        );
    }
    
    public static void postChunkLoadEventForge(Chunk chunk) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
            new net.minecraftforge.event.world.ChunkEvent.Load(chunk)
        );
    }
    
    @Deprecated
    public static FileInputStream getLanguageFileStream(String modid) {
        try {
            return new FileInputStream(
                ModList.get().getModFileById(modid).getFile().findResource(
                    "assets/" + modid + "/lang/en_us.json"
                ).toFile()
            );
            
        }
        catch (FileNotFoundException ugh) {
            throw new RuntimeException(ugh);
        }
    }
    
    public static boolean isDedicatedServer() {
        return FMLEnvironment.dist == Dist.DEDICATED_SERVER;
    }
    
    
}
