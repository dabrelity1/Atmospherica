package dev.dabrelity.atmospherica.block.entity;

import dev.dabrelity.atmospherica.config.ClientConfig;
import dev.dabrelity.atmospherica.config.ServerConfig;
import dev.dabrelity.atmospherica.event.GameBusClientEvents;
import dev.dabrelity.atmospherica.sound.ModSounds;
import dev.dabrelity.atmospherica.weather.Storm;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TornadoSirenBlockEntity extends BlockEntity {
   private long lastSirenSound = 0L;

   public TornadoSirenBlockEntity(BlockPos pos, BlockState blockState) {
      super((BlockEntityType)ModBlockEntities.TORNADO_SIREN_BE.get(), pos, blockState);
   }

   public void tick(Level level, BlockPos blockPos, BlockState blockState) {
      if (level.getGameTime() % 20L == 0L && level.isClientSide() && System.currentTimeMillis() > this.lastSirenSound) {
         boolean nearTornado = false;

         double bx = blockPos.getX() + 0.5D;
         double bz = blockPos.getZ() + 0.5D;
         double rangeSq = (ServerConfig.stormSize * 1.15F) * (ServerConfig.stormSize * 1.15F);

         for (Storm storm : GameBusClientEvents.weatherHandler.getStorms()) {
            if (level == storm.level && storm.stage >= 3 && storm.stormType == 0) {
               double dx = bx - storm.position.x;
               double dz = bz - storm.position.z;
               double distSq = dx * dx + dz * dz;

               if (distSq < rangeSq) {
                  nearTornado = true;
                  break;
               }
            }
         }

         if (nearTornado) {
            this.lastSirenSound = System.currentTimeMillis() + 120000L;
            ModSounds.playBlockSound(level, blockState, blockPos, ModSounds.SIREN.get(), (float)ClientConfig.sirenVolume, 1.0F, 120.0F);
         }
      }
   }
}
