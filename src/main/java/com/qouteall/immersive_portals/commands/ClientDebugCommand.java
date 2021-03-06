package com.qouteall.immersive_portals.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.chunk_loading.ChunkVisibilityManager;
import com.qouteall.immersive_portals.chunk_loading.MyClientChunkManager;
import com.qouteall.immersive_portals.ducks.IEEntity;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.optifine_compatibility.UniformReport;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.MyBuiltChunkStorage;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.lang.ref.Reference;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class ClientDebugCommand {
    
    public static void register(
        CommandDispatcher<CommandSource> dispatcher
    ) {
        //for composite command arguments, put into then() 's bracket
        //for parallel command arguments, put behind then()
        
        LiteralArgumentBuilder<CommandSource> builder = Commands
            .literal("immersive_portals_debug")
            .requires(commandSource -> true)
            .then(Commands
                .literal("set_max_portal_layer")
                .then(Commands
                    .argument(
                        "argMaxPortalLayer", IntegerArgumentType.integer()
                    )
                    .executes(context -> setMaxPortalLayer(
                        IntegerArgumentType.getInteger(context, "argMaxPortalLayer")
                    ))
                )
            );
        builder = builder.then(Commands
            .literal("list_nearby_portals")
            .executes(context -> listNearbyPortals(context))
        );
        builder = builder.then(Commands
            .literal("is_client_chunk_loaded")
            .then(Commands
                .argument(
                    "chunkX", IntegerArgumentType.integer()
                )
                .then(Commands
                    .argument(
                        "chunkZ", IntegerArgumentType.integer()
                    )
                    .executes(
                        ClientDebugCommand::isClientChunkLoaded
                    )
                )
            )
        );
        builder = builder.then(Commands
            .literal("is_server_chunk_loaded")
            .then(Commands
                .argument(
                    "chunkX", IntegerArgumentType.integer()
                )
                .then(Commands
                    .argument(
                        "chunkZ", IntegerArgumentType.integer()
                    )
                    .executes(
                        context -> {
                            int chunkX = IntegerArgumentType.getInteger(context, "chunkX");
                            int chunkZ = IntegerArgumentType.getInteger(context, "chunkZ");
                            ServerPlayerEntity player = context.getSource().asPlayer();
                            IChunk chunk = McHelper.getServer()
                                .getWorld(player.dimension)
                                .getChunk(
                                    chunkX, chunkZ,
                                    ChunkStatus.FULL, false
                                );
                            McHelper.serverLog(
                                player,
                                chunk != null && !(chunk instanceof EmptyChunk) ? "yes" : "no"
                            );
                            return 0;
                        }
                    )
                )
            )
        );
        builder = builder.then(Commands
            .literal("add_portal")
            .executes(context -> addPortal(context))
        );
        builder = builder.then(Commands
            .literal("report_player_status")
            .executes(context -> reportPlayerStatus(context))
        );
        builder = builder.then(Commands
            .literal("client_remote_ticking_enable")
            .executes(context -> {
                CGlobal.isClientRemoteTickingEnabled = true;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("client_remote_ticking_disable")
            .executes(context -> {
                CGlobal.isClientRemoteTickingEnabled = false;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("advanced_frustum_culling_enable")
            .executes(context -> {
                CGlobal.doUseAdvancedFrustumCulling = true;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("advanced_frustum_culling_disable")
            .executes(context -> {
                CGlobal.doUseAdvancedFrustumCulling = false;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("hacked_chunk_render_dispatcher_enable")
            .executes(context -> {
                CGlobal.useHackedChunkRenderDispatcher = true;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("hacked_chunk_render_dispatcher_disable")
            .executes(context -> {
                CGlobal.useHackedChunkRenderDispatcher = false;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("front_culling_enable")
            .executes(context -> {
                CGlobal.useFrontCulling = true;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("front_culling_disable")
            .executes(context -> {
                CGlobal.useFrontCulling = false;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("report_server_entities")
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().asPlayer();
                List<Entity> entities = player.world.getEntitiesWithinAABB(
                    Entity.class,
                    new AxisAlignedBB(player.getPosition()).grow(32),
                    e -> true
                );
                McHelper.serverLog(player, entities.toString());
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("report_resource_consumption")
            .executes(ClientDebugCommand::reportResourceConsumption)
        );
        builder = builder.then(Commands
            .literal("report_render_info_num")
            .executes(context -> {
                String str = Helper.myToString(CGlobal.renderInfoNumMap.entrySet().stream());
                context.getSource().asPlayer().sendMessage(new StringTextComponent(str));
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("get_player_colliding_portal_client")
            .executes(context -> {
                Portal collidingPortal =
                    ((IEEntity) Minecraft.getInstance().player).getCollidingPortal();
                McHelper.serverLog(
                    context.getSource().asPlayer(),
                    collidingPortal != null ? collidingPortal.toString() : "null"
                );
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("report_rendering")
            .executes(context -> {
                String str = RenderStates.lastPortalRenderInfos
                    .stream()
                    .map(
                        list -> list.stream()
                            .map(Reference::get)
                            .collect(Collectors.toList())
                    )
                    .collect(Collectors.toList())
                    .toString();
                McHelper.serverLog(context.getSource().asPlayer(), str);
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("vanilla_chunk_culling_enable")
            .executes(context -> {
                Minecraft.getInstance().renderChunksMany = true;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("vanilla_chunk_culling_disable")
            .executes(context -> {
                Minecraft.getInstance().renderChunksMany = false;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("render_mode_normal")
            .executes(context -> {
                Global.renderMode = Global.RenderMode.normal;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("render_mode_compatibility")
            .executes(context -> {
                Global.renderMode = Global.RenderMode.compatibility;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("render_mode_debug")
            .executes(context -> {
                Global.renderMode = Global.RenderMode.debug;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("render_mode_none")
            .executes(context -> {
                Global.renderMode = Global.RenderMode.none;
                return 0;
            })
        );
        builder.then(Commands
            .literal("report_chunk_loaders")
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().asPlayer();
                ChunkVisibilityManager.getChunkLoaders(
                    player
                ).forEach(
                    loader -> McHelper.serverLog(
                        player, loader.toString()
                    )
                );
                return 0;
            })
        );
        builder.then(Commands
            .literal("check_light")
            .executes(context -> {
                Minecraft mc = Minecraft.getInstance();
                mc.execute(() -> {
                    mc.world.getChunkProvider().getLightManager().updateSectionStatus(
                        SectionPos.from(mc.player.getPosition()),
                        false
                    );
                });
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("uniform_report_textured")
            .executes(context -> {
                UniformReport.launchUniformReport(
                    new String[]{
                        "gbuffers_textured", "gbuffers_textured_lit"
                    },
                    s -> context.getSource().sendFeedback(
                        new StringTextComponent(s), true
                    )
                );
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("uniform_report_terrain")
            .executes(context -> {
                UniformReport.launchUniformReport(
                    new String[]{
                        "gbuffers_terrain", "gbuffers_terrain_solid"
                    },
                    s -> context.getSource().sendFeedback(
                        new StringTextComponent(s), true
                    )
                );
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("uniform_report_shadow")
            .executes(context -> {
                UniformReport.launchUniformReport(
                    new String[]{
                        "shadow_solid", "shadow"
                    },
                    s -> context.getSource().sendFeedback(
                        new StringTextComponent(s), true
                    )
                );
                return 0;
            })
        );
        builder.then(Commands
            .literal("erase_chunk")
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().asPlayer();
                
                eraseChunk(new ChunkPos(player.getPosition()), player.world, 0, 256);
                
                return 0;
            })
        );
        builder.then(Commands
            .literal("erase_chunk_large")
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().asPlayer();
                
                ChunkPos center = new ChunkPos(player.getPosition());
                
                for (int dx = -4; dx <= 4; dx++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        eraseChunk(
                            new ChunkPos(
                                player.chunkCoordX + dx,
                                player.chunkCoordZ + dz
                            ),
                            player.world, 0, 256
                        );
                    }
                }
                
                return 0;
            })
        );
        builder.then(Commands
            .literal("erase_chunk_large_middle")
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().asPlayer();
                
                ChunkPos center = new ChunkPos(player.getPosition());
                
                for (int dx = -4; dx <= 4; dx++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        eraseChunk(
                            new ChunkPos(
                                player.chunkCoordX + dx,
                                player.chunkCoordZ + dz
                            ),
                            player.world, 64, 128
                        );
                    }
                }
                
                return 0;
            })
        );
        builder.then(Commands
            .literal("report_rebuild_status")
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().asPlayer();
                Minecraft.getInstance().execute(() -> {
                    CGlobal.clientWorldLoader.clientWorldMap.forEach((dim, world) -> {
                        MyBuiltChunkStorage builtChunkStorage = (MyBuiltChunkStorage) ((IEWorldRenderer)
                            CGlobal.clientWorldLoader.getWorldRenderer(dim))
                            .getBuiltChunkStorage();
                        McHelper.serverLog(
                            player,
                            dim.toString() + builtChunkStorage.getDebugString()
                        );
                    });
                });
                
                return 0;
            })
        );
        registerSwitchCommand(
            builder,
            "render_fewer_on_fast_graphic",
            cond -> CGlobal.renderFewerInFastGraphic = cond
        );
        registerSwitchCommand(
            builder,
            "gl_check_error",
            cond -> Global.doCheckGlError = cond
        );
        registerSwitchCommand(
            builder,
            "smooth_chunk_unload",
            cond -> CGlobal.smoothChunkUnload = cond
        );
     
        registerSwitchCommand(
            builder,
            "early_light_update",
            cond -> CGlobal.earlyClientLightUpdate = cond
        );
        registerSwitchCommand(
            builder,
            "super_advanced_frustum_culling",
            cond -> CGlobal.useSuperAdvancedFrustumCulling = cond
        );
        registerSwitchCommand(
            builder,
            "teleportation_debug",
            cond -> Global.teleportationDebugEnabled = cond
        );
        registerSwitchCommand(
            builder,
            "cross_portal_entity_rendering",
            cond -> Global.correctCrossPortalEntityRendering = cond
        );
        registerSwitchCommand(
            builder,
            "loose_visible_chunk_iteration",
            cond -> Global.looseVisibleChunkIteration = cond
        );
        registerSwitchCommand(
            builder,
            "portal_placeholder_passthrough",
            cond -> Global.portalPlaceholderPassthrough = cond
        );
        registerSwitchCommand(
            builder,
            "early_cull_portal",
            cond -> CGlobal.earlyFrustumCullingPortal = cond
        );
        
        builder.then(Commands
            .literal("print_class_path")
            .executes(context -> {
                printClassPath();
                return 0;
            })
        );
        
        dispatcher.register(builder);
        
        Helper.log("Successfully initialized command /immersive_portals_debug");
    }
    
    public static void eraseChunk(ChunkPos chunkPos, World world, int yStart, int yEnd) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = yStart; y < yEnd; y++) {
                    world.setBlockState(
                        chunkPos.getBlock(
                            x, y, z
                        ),
                        Blocks.AIR.getDefaultState()
                    );
                }
            }
        }
    }
    
    private static void printClassPath() {
        System.out.println(
            Arrays.stream(
                ((URLClassLoader) ClassLoader.getSystemClassLoader()).getURLs()
            ).map(
                url -> "\"" + url.getFile().substring(1).replace("%20", " ") + "\""
            ).collect(Collectors.joining(",\n"))
        );
    }
    
    private static void registerSwitchCommand(
        LiteralArgumentBuilder<CommandSource> builder,
        String name,
        Consumer<Boolean> setFunction
    ) {
        builder = builder.then(Commands
            .literal(name + "_enable")
            .executes(context -> {
                setFunction.accept(true);
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal(name + "_disable")
            .executes(context -> {
                setFunction.accept(false);
                return 0;
            })
        );
    }
    
    private static int reportResourceConsumption(CommandContext<CommandSource> context) throws CommandSyntaxException {
        StringBuilder str = new StringBuilder();
        
        str.append("Client Chunk:\n");
        CGlobal.clientWorldLoader.clientWorldMap.values().forEach(world -> {
            str.append(String.format(
                "%s %s\n",
                world.dimension.getType(),
                ((MyClientChunkManager) world.getChunkProvider()).getLoadedChunksCount()
            ));
        });
        
        
        str.append("Chunk Renderers:\n");
        CGlobal.clientWorldLoader.worldRendererMap.forEach(
            (dimension, worldRenderer) -> {
                str.append(String.format(
                    "%s %s\n",
                    dimension,
                    ((MyBuiltChunkStorage) ((IEWorldRenderer) worldRenderer)
                        .getBuiltChunkStorage()
                    ).getManagedChunkNum()
                ));
            }
        );
        
        str.append("Server Chunks:\n");
        McHelper.getServer().getWorlds().forEach(
            world -> {
                str.append(String.format(
                    "%s %s\n",
                    world.dimension.getType(),
                    world.getForcedChunks().size()
                ));
            }
        );
        
        String result = str.toString();
        
        Helper.log(str);
        
        context.getSource().asPlayer().sendMessage(new StringTextComponent(result));
        
        return 0;
    }
    
    private static int isClientChunkLoaded(CommandContext<CommandSource> context) throws CommandSyntaxException {
        int chunkX = IntegerArgumentType.getInteger(context, "chunkX");
        int chunkZ = IntegerArgumentType.getInteger(context, "chunkZ");
        IChunk chunk = Minecraft.getInstance().world.getChunk(
            chunkX, chunkZ
        );
        McHelper.serverLog(
            context.getSource().asPlayer(),
            chunk != null && !(chunk instanceof EmptyChunk) ? "yes" : "no"
        );
        return 0;
    }
    
    private static int setMaxPortalLayer(int m) {
        Global.maxPortalLayer = m;
        return 0;
    }
    
    private static int listNearbyPortals(CommandContext<CommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity playerServer = context.getSource().asPlayer();
        ClientPlayerEntity playerClient = Minecraft.getInstance().player;
        
        McHelper.serverLog(playerServer, "Server Portals");
        McHelper.serverLog(
            playerServer,
            Helper.myToString(
                McHelper.getEntitiesNearby(
                    playerServer, Portal.class, 64
                )
            )
        );
        
        McHelper.serverLog(playerServer, "Client Portals");
        McHelper.serverLog(
            playerServer,
            Helper.myToString(
                McHelper.getEntitiesNearby(
                    playerClient, Portal.class, 64
                )
            )
        );
        
        return 0;
    }
    
    private static Consumer<ServerPlayerEntity> originalAddPortalFunctionality;
    private static Consumer<ServerPlayerEntity> addPortalFunctionality;
    
    static {
        originalAddPortalFunctionality = (player) -> {
            Vec3d fromPos = player.getPositionVec();
            Vec3d fromNormal = player.getLookVec().scale(-1);
            ServerWorld fromWorld = ((ServerWorld) player.world);
            
            addPortalFunctionality = (playerEntity) -> {
                Vec3d toPos = playerEntity.getPositionVec();
                DimensionType toDimension = player.dimension;
                
                Portal portal = new Portal(Portal.entityType, fromWorld);
                portal.setRawPosition(fromPos.x, fromPos.y, fromPos.z);
                
                portal.axisH = new Vec3d(0, 1, 0);
                portal.axisW = portal.axisH.crossProduct(fromNormal).normalize();
                
                portal.dimensionTo = toDimension;
                portal.destination = toPos;
                
                portal.width = 4;
                portal.height = 4;
                
                assert portal.isPortalValid();
                
                fromWorld.addEntity(portal);
                
                addPortalFunctionality = originalAddPortalFunctionality;
            };
        };
        
        addPortalFunctionality = originalAddPortalFunctionality;
    }
    
    private static int addPortal(CommandContext<CommandSource> context) {
        try {
            addPortalFunctionality.accept(context.getSource().asPlayer());
        }
        catch (CommandSyntaxException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    private static int reportPlayerStatus(CommandContext<CommandSource> context) throws CommandSyntaxException {
        //only invoked on single player
        
        ServerPlayerEntity playerMP = context.getSource().asPlayer();
        ClientPlayerEntity playerSP = Minecraft.getInstance().player;
        
        McHelper.serverLog(
            playerMP,
            "On Server " + playerMP.dimension + " " + playerMP.getPosition()
        );
        McHelper.serverLog(
            playerMP,
            "On Client " + playerSP.dimension + " " + playerSP.getPosition()
        );
        return 0;
    }
    
}
