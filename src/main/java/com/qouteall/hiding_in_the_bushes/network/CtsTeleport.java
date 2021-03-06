package com.qouteall.hiding_in_the_bushes.network;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.ModMain;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class CtsTeleport {
    DimensionType dimensionBefore;
    Vec3d posBefore;
    UUID portalEntityId;
    
    public CtsTeleport(DimensionType dimensionBefore, Vec3d posBefore, UUID portalEntityId) {
        this.dimensionBefore = dimensionBefore;
        this.posBefore = posBefore;
        this.portalEntityId = portalEntityId;
    }
    
    public CtsTeleport(PacketBuffer buf) {
        dimensionBefore = DimensionType.getById(buf.readInt());
        posBefore = new Vec3d(
            buf.readDouble(),
            buf.readDouble(),
            buf.readDouble()
        );
        portalEntityId = buf.readUniqueId();
    }
    
    public void encode(PacketBuffer buf) {
        buf.writeInt(dimensionBefore.getId());
        buf.writeDouble(posBefore.x);
        buf.writeDouble(posBefore.y);
        buf.writeDouble(posBefore.z);
        buf.writeUniqueId(portalEntityId);
    }
    
    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            Global.serverTeleportationManager.onPlayerTeleportedInClient(
                (ServerPlayerEntity) context.get().getSender(),
                dimensionBefore,
                posBefore,
                portalEntityId
            );
        });
        context.get().setPacketHandled(true);
    }
}
