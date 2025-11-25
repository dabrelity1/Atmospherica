package dev.protomanly.pmweather.weather;

import dev.protomanly.pmweather.networking.ModNetworking;
import dev.protomanly.pmweather.util.CachedNBTTagCompound;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class WeatherHandlerServer extends WeatherHandler {
   private final ServerLevel level;

   public WeatherHandlerServer(ServerLevel level) {
      super(level.dimension());
      this.level = level;
      this.seed = level.getSeed();
   }

   @Override
   public Level getWorld() {
      return this.level;
   }

   public void syncStormRemove(Storm storm) {
      CompoundTag data = new CompoundTag();
      data.putString("packetCommand", "WeatherData");
      data.putString("command", "syncStormRemove");
      storm.nbtSyncForClient();
      data.put("data", storm.getNBTCache().getNewNBT());
      data.getCompound("data").putBoolean("removed", true);
      ModNetworking.serverSendToClientDimension(data, this.getWorld());
   }

   public void syncStormNew(Storm storm) {
      this.syncStormNew(storm, null);
   }

   public void syncLightningNew(Vec3 pos) {
      CompoundTag data = new CompoundTag();
      data.putString("packetCommand", "WeatherData");
      data.putString("command", "syncLightningNew");
      CompoundTag compoundTag = new CompoundTag();
      compoundTag.putDouble("positionX", pos.x);
      compoundTag.putDouble("positionY", pos.y);
      compoundTag.putDouble("positionZ", pos.z);
      data.put("data", compoundTag);
      ModNetworking.serverSendToClientNear(data, pos, 1024.0, this.level);
   }

   public void syncStormNew(Storm storm, @Nullable ServerPlayer player) {
      CompoundTag data = new CompoundTag();
      data.putString("packetCommand", "WeatherData");
      data.putString("command", "syncStormNew");
      CachedNBTTagCompound cache = storm.getNBTCache();
      cache.setUpdateForced(true);
      storm.nbtSyncForClient();
      cache.setUpdateForced(false);
      data.put("data", cache.getNewNBT());
      if (player == null) {
         ModNetworking.serverSendToClientDimension(data, storm.level);
      } else {
         ModNetworking.serverSendToClientPlayer(data, player);
      }
   }

   public void syncStormUpdate(Storm storm) {
      CompoundTag data = new CompoundTag();
      data.putString("packetCommand", "WeatherData");
      data.putString("command", "syncStormUpdate");
      storm.getNBTCache().setNewNBT(new CompoundTag());
      storm.nbtSyncForClient();
      data.put("data", storm.getNBTCache().getNewNBT());
      ModNetworking.serverSendToClientDimension(data, this.getWorld());
   }

   public void playerJoinedWorldSyncFull(ServerPlayer player) {
      if (this.getWorld() instanceof ServerLevel serverLevel) {
         CompoundTag data = new CompoundTag();
         data.putString("packetCommand", "LevelData");
         data.putString("command", "syncMisc");
         data.putLong("seed", this.seed);
         ModNetworking.serverSendToClientPlayer(data, player);

         for (Storm storm : this.getStorms()) {
            this.syncStormNew(storm, player);
         }
      }
   }

   public void clearAllStorms() {
      for (Storm storm : this.getStorms()) {
         storm.remove();
         this.syncStormRemove(storm);
      }

      this.getStorms().clear();
      this.lookupStormByID.clear();
   }

   public void syncBlockParticleNew(BlockPos pos, BlockState state, Storm storm) {
      CompoundTag data = new CompoundTag();
      data.putString("packetCommand", "WeatherData");
      data.putString("command", "syncBlockParticleNew");
      CompoundTag nbt = new CompoundTag();
      nbt.putInt("positionX", pos.getX());
      nbt.putInt("positionY", pos.getY());
      nbt.putInt("positionZ", pos.getZ());
      nbt.put("blockstate", NbtUtils.writeBlockState(state));
      nbt.putLong("stormID", storm.ID);
      data.put("data", nbt);
      ModNetworking.serverSendToClientNear(data, new Vec3(pos.getX(), pos.getY(), pos.getZ()), 356.0, this.level);
   }
}
