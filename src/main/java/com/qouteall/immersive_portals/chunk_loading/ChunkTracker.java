package com.qouteall.immersive_portals.chunk_loading;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Streams;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.my_util.SignalBiArged;
import com.qouteall.immersive_portals.portal.Portal;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketType;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ChunkTracker {
    
    //if a chunk is not watched for 15 seconds, it will be unloaded
    private static final int unloadIdleTickTime = 20 * 15;
    private static final int unloadIdleTickTimeSameDimension = 20 * 3;
    
    
    public static class Edge {
        public DimensionalChunkPos chunkPos;
        public ServerPlayerEntity player;
        public long lastActiveGameTime;
        public boolean isSent = false;
        
        public Edge(
            DimensionalChunkPos chunkPos,
            ServerPlayerEntity player,
            long lastActiveGameTime
        ) {
            this.chunkPos = chunkPos;
            this.player = player;
            this.lastActiveGameTime = lastActiveGameTime;
        }
    }
    
    private static final TicketType<ChunkPos> immersiveTicketType =
        TicketType.create(
            "immersive_portal_ticket",
            Comparator.comparingLong(ChunkPos::asLong)
        );
    
    public static final int portalLoadingRange = 48;
    public static final int secondaryPortalLoadingRange = 16;
    
    public final SignalBiArged<ServerPlayerEntity, DimensionalChunkPos> beginWatchChunkSignal = new SignalBiArged<>();
    public final SignalBiArged<ServerPlayerEntity, DimensionalChunkPos> endWatchChunkSignal = new SignalBiArged<>();
    
    //TODO optimize using nested map
    private Multimap<DimensionalChunkPos, Edge> chunkPosToEdges = HashMultimap.create();
    private Multimap<ServerPlayerEntity, Edge> playerToEdges = HashMultimap.create();
    
    public ChunkTracker() {
        ModMain.postServerTickSignal.connectWithWeakRef(this, ChunkTracker::tick);
    }
    
    public void cleanUp() {
        chunkPosToEdges.clear();
        playerToEdges.clear();
    }
    
    public void setIsLoadedByPortal(
        DimensionType dimension,
        ChunkPos chunkPos,
        boolean isLoadedNow
    ) {
        ServerWorld world = Helper.getServer().getWorld(dimension);
    
        world.forceChunk(chunkPos.x, chunkPos.z, isLoadedNow);
        //world.method_14178().setChunkForced(chunkPos, isLoadedNow);
    }
    
    //@Nullable
    private Edge getEdge(DimensionalChunkPos chunkPos, ServerPlayerEntity playerEntity) {
        return chunkPosToEdges.get(chunkPos)
            .stream()
            .filter(edge -> edge.player == playerEntity)
            .findAny()
            .orElse(null);
    }
    
    private Edge getOrAddEdge(DimensionalChunkPos chunkPos, ServerPlayerEntity playerEntity) {
        return chunkPosToEdges.get(chunkPos)
            .stream()
            .filter(edge -> edge.player == playerEntity)
            .findAny()
            .orElseGet(() -> addEdge(chunkPos, playerEntity));
    }
    
    private Edge addEdge(DimensionalChunkPos chunkPos, ServerPlayerEntity player) {
        Edge edge = new Edge(chunkPos, player, Helper.getServerGameTime());
        chunkPosToEdges.put(chunkPos, edge);
        playerToEdges.put(player, edge);
    
        ModMain.serverTaskList.addTask(() -> {
            beginWatchChunkSignal.emit(player, chunkPos);
            return true;
        });
        
        return edge;
    }
    
    private void removeEdge(Edge edge) {
        chunkPosToEdges.entries().removeIf(
            entry -> entry.getValue() == edge
        );
        playerToEdges.entries().removeIf(
            entry -> entry.getValue() == edge
        );
    
        ModMain.serverTaskList.addTask(() -> {
            if (!edge.player.removed) {
                endWatchChunkSignal.emit(edge.player, edge.chunkPos);
            }
            return true;
        });
    }
    
    private void updatePlayer(ServerPlayerEntity playerEntity) {
        Set<DimensionalChunkPos> newPlayerViewingChunks = getPlayerViewingChunks(
            playerEntity
        );
        newPlayerViewingChunks.forEach(chunkPos -> {
            Edge edge = getOrAddEdge(chunkPos, playerEntity);
            edge.lastActiveGameTime = Helper.getServerGameTime();
        });
    
        removeInactiveEdges(playerEntity);
    }
    
    private Set<DimensionalChunkPos> getPlayerViewingChunks(
        ServerPlayerEntity player
    ) {
        int renderDistance = getRenderDistanceOnServer();
        return Streams.concat(
            //directly watching chunks
            getNearbyChunkPoses(
                player.dimension,
                player.getPosition(),
                renderDistance
            ),
    
            //indirectly watching chunks
            getViewingPortals(player).flatMap(
                portal -> getNearbyChunkPoses(
                    portal.dimensionTo,
                    new BlockPos(portal.destination),
                    portal.loadFewerChunks ? (renderDistance / 3) : renderDistance
                )
            )
        ).collect(Collectors.toSet());
    }
    
    //not only the portals near player
    //but also the portals that player can see from other portals
    private Stream<Portal> getViewingPortals(ServerPlayerEntity player) {
        return Helper.getEntitiesNearby(
            player,
            Portal.class,
            portalLoadingRange
        ).flatMap(
            portal -> Streams.concat(
                Stream.of(portal),
                Helper.getEntitiesNearby(
                    Helper.getServer().getWorld(portal.dimensionTo),
                    portal.destination,
                    Portal.class,
                    secondaryPortalLoadingRange
                )
            )
        ).distinct();
    }
    
    private void tick() {
        long currTime = Helper.getServerGameTime();
        for (ServerPlayerEntity player : Helper.getCopiedPlayerList()) {
            if (currTime % 50 == player.getEntityId() % 50) {
                updatePlayer(player);
    
                updateForcedChunks();
            }
        }
    
        if (currTime % 100 == 66) {
            cleanupForRemovedPlayers();
        }
    }
    
    private void removeInactiveEdges(ServerPlayerEntity playerEntity) {
        long serverGameTime = Helper.getServerGameTime();
        playerToEdges.get(playerEntity).stream()
            .filter(
                edge -> shouldUnload(serverGameTime, edge)
            )
            .collect(Collectors.toCollection(ArrayDeque::new))
            .forEach(this::removeEdge);
    }
    
    private boolean shouldUnload(long serverGameTime, Edge edge) {
        if (edge.player.dimension == edge.chunkPos.dimension) {
            return serverGameTime - edge.lastActiveGameTime > unloadIdleTickTimeSameDimension;
        }
        else {
            return serverGameTime - edge.lastActiveGameTime > unloadIdleTickTime;
        }
    }
    
    private void updateForcedChunks() {
        Map<DimensionType, List<DimensionalChunkPos>> newForcedChunkMap =
            chunkPosToEdges.keySet().stream().collect(
                Collectors.groupingBy(chunkPos -> chunkPos.dimension)
            );
        Helper.getServer().getWorlds().forEach(world -> {
            List<DimensionalChunkPos> newForcedChunks =
                newForcedChunkMap.computeIfAbsent(
                    world.dimension.getType(),
                    k -> new ArrayList<>()
                );
            LongSet oldForcedChunks = new LongOpenHashSet(world.getForcedChunks());
            Helper.compareOldAndNew(
                oldForcedChunks,
                newForcedChunks.stream()
                    .map(chunkPos -> chunkPos.getChunkPos().asLong())
                    .collect(Collectors.toSet()),
                longChunkPos -> setIsLoadedByPortal(
                    world.dimension.getType(),
                    new ChunkPos(longChunkPos),
                    false
                ),
                longChunkPos -> setIsLoadedByPortal(
                    world.dimension.getType(),
                    new ChunkPos(longChunkPos),
                    true
                )
            );
        });
    }
    
    private void cleanupForRemovedPlayers() {
        playerToEdges.entries().stream()
            .filter(entry -> entry.getKey().removed)
            .map(Map.Entry::getValue)
            .collect(Collectors.toCollection(ArrayDeque::new))
            .forEach(this::removeEdge);
    }
    
    private Stream<DimensionalChunkPos> getNearbyChunkPoses(
        DimensionType dimension,
        BlockPos pos, int radius
    ) {
        ArrayDeque<DimensionalChunkPos> chunkPoses = new ArrayDeque<>();
        ChunkPos portalChunkPos = new ChunkPos(pos);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                chunkPoses.add(new DimensionalChunkPos(
                    dimension,
                    portalChunkPos.x + dx,
                    portalChunkPos.z + dz
                ));
            }
        }
        return chunkPoses.stream();
    }
    
    public Stream<ServerPlayerEntity> getPlayersViewingChunk(
        DimensionType dimensionType,
        ChunkPos chunkPos
    ) {
        assert dimensionType != null;
        return chunkPosToEdges
            .get(new DimensionalChunkPos(dimensionType, chunkPos))
            .stream()
            .map(edge -> edge.player);
    }
    
    public boolean isPlayerWatchingChunk(
        ServerPlayerEntity player,
        DimensionalChunkPos chunkPos
    ) {
        return chunkPosToEdges.get(chunkPos).stream()
            .anyMatch(edge -> edge.player == player);
    }
    
    public void onChunkDataSent(ServerPlayerEntity player, DimensionalChunkPos chunkPos) {
        Edge edge = getOrAddEdge(chunkPos, player);
        if (edge.isSent) {
            Helper.log(String.format(
                "chunk data sent twice! %s %s",
                player, chunkPos
            ));
        }
        edge.isSent = true;
    }
    
    public boolean isChunkDataSent(ServerPlayerEntity player, DimensionalChunkPos chunkPos) {
        Edge edge = getEdge(chunkPos, player);
        return edge != null && edge.isSent;
    }
    
    public void onPlayerRespawn(ServerPlayerEntity oldPlayer) {
        playerToEdges.removeAll(oldPlayer);
        chunkPosToEdges.entries().removeIf(entry ->
            entry.getValue().player == oldPlayer
        );
    }
    
    public static int getRenderDistanceOnServer() {
        return Helper.getIEStorage(DimensionType.OVERWORLD).getWatchDistance();
    }
    
}