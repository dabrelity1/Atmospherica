package dev.dabrelity.atmospherica.networking;

import dev.dabrelity.atmospherica.Atmospherica;
import dev.dabrelity.atmospherica.block.MetarBlock;
import dev.dabrelity.atmospherica.block.ModBlocks;
import dev.dabrelity.atmospherica.block.entity.RadarBlockEntity;
import dev.dabrelity.atmospherica.block.entity.WeatherPlatformBlockEntity;
import dev.dabrelity.atmospherica.event.GameBusClientEvents;
import dev.dabrelity.atmospherica.weather.Sounding;
import dev.dabrelity.atmospherica.weather.ThermodynamicEngine;
import dev.dabrelity.atmospherica.weather.WeatherHandlerClient;
import dev.dabrelity.atmospherica.weather.WindEngine;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class PacketNBTFromServer {
   private final CompoundTag compoundTag;

   public PacketNBTFromServer(CompoundTag compoundTag) {
      this.compoundTag = compoundTag == null ? new CompoundTag() : compoundTag;
   }

   public static void encode(PacketNBTFromServer packet, FriendlyByteBuf buf) {
      buf.writeNbt(packet.compoundTag);
   }

   public static PacketNBTFromServer decode(FriendlyByteBuf buf) {
      return new PacketNBTFromServer(buf.readNbt());
   }

   public static void handle(PacketNBTFromServer packet, Supplier<NetworkEvent.Context> ctx) {
      NetworkEvent.Context context = ctx.get();
      context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(packet)));
      context.setPacketHandled(true);
   }

   private void handle(Player player) {
      try {
         String packetCommand = this.compoundTag.getString("packetCommand");
         String command = this.compoundTag.getString("command");
         GameBusClientEvents.getClientWeather();
         WeatherHandlerClient weatherHandler = (WeatherHandlerClient)GameBusClientEvents.weatherHandler;
         if (packetCommand.equals("WeatherData")) {
            weatherHandler.nbtSyncFromServer(this.compoundTag);
         } else if (packetCommand.equals("LevelData")) {
            if (command.equals("syncMisc")) {
               weatherHandler.seed = this.compoundTag.getLong("seed");
               WindEngine.init(weatherHandler);
               ThermodynamicEngine.noise = WindEngine.simplexNoise;
            }
         } else if (packetCommand.equals("Radar")) {
            if (command.equals("syncBiomes")) {
               BlockPos blockPos = this.compoundTag.contains("blockPos") ? NbtUtils.readBlockPos(this.compoundTag.getCompound("blockPos")) : BlockPos.ZERO;
               Level level = player.level();
               BlockState state = level.getBlockState(blockPos);
               if (state.is(ModBlocks.RADAR.get()) && state.hasBlockEntity() && level.getBlockEntity(blockPos) instanceof RadarBlockEntity radarBlockEntity) {
                  radarBlockEntity.clientInit(level, this.compoundTag);
               }
            }
         } else if (packetCommand.equals("WeatherPlatform")) {
            if (command.equals("sync")) {
               CompoundTag data = this.compoundTag.getCompound("data");
               BlockPos blockPos = data.contains("blockPos") ? NbtUtils.readBlockPos(data.getCompound("blockPos")) : BlockPos.ZERO;
               Level level = player.level();
               BlockState state = level.getBlockState(blockPos);
               if (state.is(ModBlocks.WEATHER_PLATFORM.get())
                  && state.hasBlockEntity()
                  && level.getBlockEntity(blockPos) instanceof WeatherPlatformBlockEntity weatherPlatformBlockEntity) {
                  weatherPlatformBlockEntity.deserializeNBT(data);
               }
            } else if (command.equals("syncSounding")) {
               CompoundTag data = this.compoundTag.getCompound("data");
               BlockPos blockPos = this.compoundTag.contains("blockPos") ? NbtUtils.readBlockPos(this.compoundTag.getCompound("blockPos")) : BlockPos.ZERO;
               Level level = player.level();
               BlockState state = level.getBlockState(blockPos);
               if (state.is(ModBlocks.WEATHER_PLATFORM.get())
                  && state.hasBlockEntity()
                  && level.getBlockEntity(blockPos) instanceof WeatherPlatformBlockEntity weatherPlatformBlockEntity) {
                  weatherPlatformBlockEntity.sounding = new Sounding(GameBusClientEvents.weatherHandler, data, blockPos.getCenter());
               }
            }
         } else if (packetCommand.equals("Metar") && command.equals("sendData")) {
            MetarBlock.sendMessage(this.compoundTag);
         }
      } catch (Exception var11) {
         Atmospherica.LOGGER.error(var11.getMessage(), var11);
      }
   }

   public CompoundTag getCompoundTag() {
      return this.compoundTag;
   }

   @OnlyIn(Dist.CLIENT)
   private static class ClientHandler {
      static void handle(PacketNBTFromServer packet) {
         Player player = Minecraft.getInstance().player;
         if (player != null) {
            packet.handle(player);
         }
      }
   }
}
