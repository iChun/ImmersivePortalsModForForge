package com.qouteall.immersive_portals.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.qouteall.hiding_in_the_bushes.alternate_dimension.AlternateDimension;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.OFInterface;
import com.qouteall.immersive_portals.block_manipulation.BlockManipulationClient;
import com.qouteall.immersive_portals.ducks.IEGameRenderer;
import com.qouteall.immersive_portals.ducks.IEMinecraftClient;
import com.qouteall.immersive_portals.ducks.IEParticleManager;
import com.qouteall.immersive_portals.ducks.IEPlayerListEntry;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.ducks.IEWorldRendererChunkInfo;
import com.qouteall.immersive_portals.render.context_management.DimensionRenderHelper;
import com.qouteall.immersive_portals.render.context_management.FogRendererContext;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import com.qouteall.immersive_portals.render.context_management.RenderDimensionRedirect;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.OverworldDimension;
import org.lwjgl.opengl.GL11;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class MyGameRenderer {
    public static Minecraft client = Minecraft.getInstance();
    
    public static void switchAndRenderTheWorld(
        ClientWorld newWorld,
        Vec3d thisTickCameraPos,
        Vec3d lastTickCameraPos,
        Consumer<Runnable> invokeWrapper
    ) {
    
        Entity cameraEntity = client.renderViewEntity;
    
        Vec3d oldEyePos = McHelper.getEyePos(cameraEntity);
        Vec3d oldLastTickEyePos = McHelper.getLastTickEyePos(cameraEntity);
        
        DimensionType oldEntityDimension = cameraEntity.dimension;
        ClientWorld oldEntityWorld = ((ClientWorld) cameraEntity.world);
        
        DimensionType newDimension = newWorld.dimension.getType();
        
        //switch the camera entity pos
        McHelper.setEyePos(cameraEntity, thisTickCameraPos, lastTickCameraPos);
        cameraEntity.dimension = newDimension;
        cameraEntity.world = newWorld;
        
        GlStateManager.enableAlphaTest();
        GlStateManager.enableCull();
        
        WorldRenderer worldRenderer = CGlobal.clientWorldLoader.getWorldRenderer(newDimension);
        
        CHelper.checkGlError();
        
        float tickDelta = RenderStates.tickDelta;
        
        if (CGlobal.useHackedChunkRenderDispatcher) {
            ((IEWorldRenderer) worldRenderer).getBuiltChunkStorage().updateChunkPositions(
                cameraEntity.getPosX(),
                cameraEntity.getPosZ()
            );
        }
        
        if (Global.looseVisibleChunkIteration) {
            client.renderChunksMany = false;
        }
        
        IEGameRenderer ieGameRenderer = (IEGameRenderer) client.gameRenderer;
        DimensionRenderHelper helper =
            CGlobal.clientWorldLoader.getDimensionRenderHelper(
                RenderDimensionRedirect.getRedirectedDimension(newDimension)
            );
        NetworkPlayerInfo playerListEntry = CHelper.getClientPlayerListEntry();
        ActiveRenderInfo newCamera = new ActiveRenderInfo();
        
        //store old state
        WorldRenderer oldWorldRenderer = client.worldRenderer;
        LightTexture oldLightmap = client.gameRenderer.getLightTexture();
        GameType oldGameMode = playerListEntry.getGameType();
        boolean oldNoClip = client.player.noClip;
        boolean oldDoRenderHand = ieGameRenderer.getDoRenderHand();
        OFInterface.createNewRenderInfosNormal.accept(worldRenderer);
        ObjectList oldVisibleChunks = ((IEWorldRenderer) oldWorldRenderer).getVisibleChunks();
        RayTraceResult oldCrosshairTarget = client.objectMouseOver;
        ActiveRenderInfo oldCamera = client.gameRenderer.getActiveRenderInfo();
        
        ((IEWorldRenderer) oldWorldRenderer).setVisibleChunks(new ObjectArrayList());
        
        //switch
        ((IEMinecraftClient) client).setWorldRenderer(worldRenderer);
        client.world = newWorld;
        ieGameRenderer.setLightmapTextureManager(helper.lightmapTexture);
        
        TileEntityRendererDispatcher.instance.world = newWorld;
        ((IEPlayerListEntry) playerListEntry).setGameMode(GameType.SPECTATOR);
        client.player.noClip = true;
        ieGameRenderer.setDoRenderHand(false);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        FogRendererContext.swappingManager.pushSwapping(
            RenderDimensionRedirect.getRedirectedDimension(newDimension)
        );
        ((IEParticleManager) client.particles).mySetWorld(newWorld);
        if (BlockManipulationClient.remotePointedDim == newDimension) {
            client.objectMouseOver = BlockManipulationClient.remoteHitResult;
        }
        ieGameRenderer.setCamera(newCamera);
        
        //update lightmap
        if (!RenderStates.isDimensionRendered(newDimension)) {
            helper.lightmapTexture.updateLightmap(0);
        }
        helper.lightmapTexture.enableLightmap();
        
        //invoke rendering
        invokeWrapper.accept(() -> {
            client.getProfiler().startSection("render_portal_content");
            client.gameRenderer.renderWorld(
                tickDelta,
                Util.nanoTime(),
                new MatrixStack()
            );
            client.getProfiler().endSection();
        });
        
        //recover
        ((IEMinecraftClient) client).setWorldRenderer(oldWorldRenderer);
        client.world = oldEntityWorld;
        ieGameRenderer.setLightmapTextureManager(oldLightmap);
        TileEntityRendererDispatcher.instance.world = oldEntityWorld;
        ((IEPlayerListEntry) playerListEntry).setGameMode(oldGameMode);
        client.player.noClip = oldNoClip;
        ieGameRenderer.setDoRenderHand(oldDoRenderHand);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.popMatrix();
        ((IEParticleManager) client.particles).mySetWorld(oldEntityWorld);
        client.objectMouseOver = oldCrosshairTarget;
        ieGameRenderer.setCamera(oldCamera);
        
        FogRendererContext.swappingManager.popSwapping();
        
        ((IEWorldRenderer) oldWorldRenderer).setVisibleChunks(oldVisibleChunks);
        
        if (Global.looseVisibleChunkIteration) {
            client.renderChunksMany = true;
        }
        
        client.getRenderManager()
            .cacheActiveRenderInfo(
                client.world,
                oldCamera,
                client.pointedEntity
            );
        
        CHelper.checkGlError();
        
        //restore the camera entity pos
        cameraEntity.dimension = oldEntityDimension;
        cameraEntity.world = oldEntityWorld;
        McHelper.setEyePos(cameraEntity, oldEyePos, oldLastTickEyePos);
    }
    
    public static void renderPlayerItself(Runnable doRenderEntity) {
        EntityRendererManager entityRenderDispatcher =
            ((IEWorldRenderer) client.worldRenderer).getEntityRenderDispatcher();
        NetworkPlayerInfo playerListEntry = CHelper.getClientPlayerListEntry();
        GameType originalGameMode = RenderStates.originalGameMode;
        
        Entity player = client.renderViewEntity;
        assert player != null;
        
        Vec3d oldPos = player.getPositionVec();
        Vec3d oldLastTickPos = McHelper.lastTickPosOf(player);
        GameType oldGameMode = playerListEntry.getGameType();
        
        McHelper.setPosAndLastTickPos(
            player, RenderStates.originalPlayerPos, RenderStates.originalPlayerLastTickPos
        );
        ((IEPlayerListEntry) playerListEntry).setGameMode(originalGameMode);
        
        doRenderEntity.run();

//        if (ClientTeleportationManager.isTeleportingTick&&(CGlobal.renderer.getPortalLayer()==1)) {
//            Helper.log(String.format(
//                "r%d %s",
//                CGlobal.clientTeleportationManager.tickTimeForTeleportation,
//                MyRenderHelper.tickDelta
//            ));
//        }
        
        McHelper.setPosAndLastTickPos(
            player, oldPos, oldLastTickPos
        );
        ((IEPlayerListEntry) playerListEntry).setGameMode(oldGameMode);
    }
    
    public static void resetFogState() {
        if (OFInterface.isFogDisabled.getAsBoolean()) {
            return;
        }
        
        if (OFInterface.isShaders.getAsBoolean()) {
            return;
        }
        
        forceResetFogState();
    }
    
    public static void forceResetFogState() {
        ActiveRenderInfo camera = client.gameRenderer.getActiveRenderInfo();
        float g = client.gameRenderer.getFarPlaneDistance();
        
        Vec3d cameraPos = camera.getProjectedView();
        double d = cameraPos.getX();
        double e = cameraPos.getY();
        double f = cameraPos.getZ();
        
        boolean bl2 = client.world.dimension.doesXZShowFog(
            MathHelper.floor(d),
            MathHelper.floor(e)
        ) || client.ingameGUI.getBossOverlay().shouldCreateFog();
        
        FogRenderer.setupFog(
            camera,
            FogRenderer.FogType.FOG_TERRAIN,
            Math.max(g - 16.0F, 32.0F),
            bl2
        );
        FogRenderer.applyFog();
    }
    
    public static void updateFogColor() {
        FogRenderer.updateFogColor(
            client.gameRenderer.getActiveRenderInfo(),
            RenderStates.tickDelta,
            client.world,
            client.gameSettings.renderDistanceChunks,
            client.gameRenderer.getBossColorModifier(RenderStates.tickDelta)
        );
    }
    
    public static void resetDiffuseLighting(MatrixStack matrixStack) {
        RenderHelper.setupLevelDiffuseLighting(matrixStack.getLast().getMatrix());
    }
    
    //render fewer chunks when rendering portal
    //only active when graphic option is not fancy
    //NOTE we should not prune these chunks in setupTerrain()
    //because if it's pruned there these chunks will be rebuilt
    //then it will generate lag when player cross the portal by building chunks
    //we want the far chunks to be built but not rendered
    public static void pruneVisibleChunksInFastGraphics(ObjectList<?> visibleChunks) {
        int renderDistance = client.gameSettings.renderDistanceChunks;
        Vec3d cameraPos = client.gameRenderer.getActiveRenderInfo().getProjectedView();
        double range = ((renderDistance * 16) / 3) * ((renderDistance * 16) / 3);
        
        Predicate<ChunkRenderDispatcher.ChunkRender> builtChunkPredicate = (builtChunk) -> {
            Vec3d center = builtChunk.boundingBox.getCenter();
            return center.squareDistanceTo(cameraPos) > range;
        };
        
        Helper.removeIf(
            (ObjectList<Object>) visibleChunks,
            obj -> builtChunkPredicate.test(((IEWorldRendererChunkInfo) obj).getBuiltChunk())
        );
    }
    
    public static void doPruneVisibleChunks(ObjectList<?> visibleChunks) {
        if (PortalRendering.isRendering()) {
            if (CGlobal.renderFewerInFastGraphic) {
                if (!Minecraft.getInstance().gameSettings.fancyGraphics) {
                    MyGameRenderer.pruneVisibleChunksInFastGraphics(visibleChunks);
                }
            }
        }
    }
    
    public static void renderSkyFor(
        DimensionType dimension,
        MatrixStack matrixStack,
        float tickDelta
    ) {
        
        ClientWorld newWorld = CGlobal.clientWorldLoader.getWorld(dimension);
        
        if (client.world.dimension instanceof AlternateDimension &&
            newWorld.dimension instanceof OverworldDimension
        ) {
            //avoid redirecting alternate to overworld
            //or sky will be dark when camera pos is low
            client.worldRenderer.renderSky(matrixStack, tickDelta);
            return;
        }
        
        WorldRenderer newWorldRenderer = CGlobal.clientWorldLoader.getWorldRenderer(dimension);
        
        ClientWorld oldWorld = client.world;
        WorldRenderer oldWorldRenderer = client.worldRenderer;
        FogRendererContext.swappingManager.pushSwapping(dimension);
        MyGameRenderer.forceResetFogState();
        
        client.world = newWorld;
        ((IEMinecraftClient) client).setWorldRenderer(newWorldRenderer);
        
        newWorldRenderer.renderSky(matrixStack, tickDelta);
        
        client.world = oldWorld;
        ((IEMinecraftClient) client).setWorldRenderer(oldWorldRenderer);
        FogRendererContext.swappingManager.popSwapping();
        MyGameRenderer.forceResetFogState();
    }
    
    
}
