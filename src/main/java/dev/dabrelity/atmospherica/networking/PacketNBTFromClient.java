package dev.dabrelity.atmospherica.networking;

import dev.dabrelity.atmospherica.Atmospherica;
import dev.dabrelity.atmospherica.block.entity.RadarBlockEntity;
import dev.dabrelity.atmospherica.event.GameBusEvents;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PacketNBTFromClient {
   private final CompoundTag compoundTag;

   public PacketNBTFromClient(CompoundTag compoundTag) {
      this.compoundTag = compoundTag == null ? new CompoundTag() : compoundTag;
   }

   public static void encode(PacketNBTFromClient packet, FriendlyByteBuf buf) {
      buf.writeNbt(packet.compoundTag);
   }

   public static PacketNBTFromClient decode(FriendlyByteBuf buf) {
      return new PacketNBTFromClient(buf.readNbt());
   }

   public static void handle(PacketNBTFromClient packet, Supplier<NetworkEvent.Context> ctx) {
      NetworkEvent.Context context = ctx.get();
      context.enqueueWork(() -> {
         ServerPlayer serverPlayer = context.getSender();
         if (serverPlayer != null) {
            packet.handle(serverPlayer);
         }
      });
      context.setPacketHandled(true);
   }

   private void handle(ServerPlayer player) {
      try {
         String packetCommand = this.compoundTag.getString("packetCommand");
         String command = this.compoundTag.getString("command");
         if (packetCommand.equals("WeatherData")) {
            if (command.equals("syncFull")) {
               GameBusEvents.playerRequestsFullSync(player);
            }
         } else if (packetCommand.equals("Radar") && command.equals("syncBiomes")) {
            BlockPos blockPos = this.compoundTag.contains("blockPos") ? NbtUtils.readBlockPos(this.compoundTag.getCompound("blockPos")) : BlockPos.ZERO;
            RadarBlockEntity.playerRequestsSync(player, blockPos);
         }
      } catch (Exception var5) {
         Atmospherica.LOGGER.error(var5.getMessage(), var5);
      }
   }

   public CompoundTag getCompoundTag() {
      return this.compoundTag;
   }
}
