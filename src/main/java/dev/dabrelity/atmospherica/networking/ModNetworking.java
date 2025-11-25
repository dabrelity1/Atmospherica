package dev.dabrelity.atmospherica.networking;

import dev.dabrelity.atmospherica.Atmospherica;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PacketDistributor.TargetPoint;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetworking {
   private static final String PROTOCOL_VERSION = "1";
   private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
      Atmospherica.getPath("main"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals
   );
   private static int packetId = 0;

   private ModNetworking() {
   }

   public static void init() {
      CHANNEL.registerMessage(packetId++, PacketNBTFromServer.class, PacketNBTFromServer::encode, PacketNBTFromServer::decode, PacketNBTFromServer::handle);
      CHANNEL.registerMessage(packetId++, PacketNBTFromClient.class, PacketNBTFromClient::encode, PacketNBTFromClient::decode, PacketNBTFromClient::handle);
   }

   public static void clientSendToSever(CompoundTag data) {
      CHANNEL.sendToServer(new PacketNBTFromClient(data));
   }

   public static void serverSendToClientAll(CompoundTag data) {
      CHANNEL.send(PacketDistributor.ALL.noArg(), new PacketNBTFromServer(data));
   }

   public static void serverSendToClientPlayer(CompoundTag data, Player player) {
      if (player instanceof ServerPlayer serverPlayer) {
         CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new PacketNBTFromServer(data));
      }
   }

   public static void serverSendToClientNear(CompoundTag data, Vec3 position, double distance, Level level) {
      CHANNEL.send(
         PacketDistributor.NEAR.with(() -> new TargetPoint(position.x, position.y, position.z, distance, level.dimension())),
         new PacketNBTFromServer(data)
      );
   }

   public static void serverSendToClientDimension(CompoundTag data, Level level) {
      if (level instanceof ServerLevel serverLevel) {
         CHANNEL.send(PacketDistributor.DIMENSION.with(serverLevel::dimension), new PacketNBTFromServer(data));
      }
   }
}
