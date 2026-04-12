package dev.dabrelity.atmospherica.block.entity;

import dev.dabrelity.atmospherica.block.TornadoSensorBlock;
import dev.dabrelity.atmospherica.config.ServerConfig;
import dev.dabrelity.atmospherica.event.GameBusEvents;
import dev.dabrelity.atmospherica.weather.Storm;
import dev.dabrelity.atmospherica.weather.WeatherHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TornadoSensorBlockEntity extends BlockEntity {
   public TornadoSensorBlockEntity(BlockPos pos, BlockState blockState) {
      super((BlockEntityType)ModBlockEntities.TORNADO_SENSOR_BE.get(), pos, blockState);
   }

   public void tick(Level level, BlockPos blockPos, BlockState blockState) {
      if (level.getGameTime() % 20L == 0L && !level.isClientSide()) {
         boolean nearTornado = false;

         double bx = blockPos.getX() + 0.5D;
         double bz = blockPos.getZ() + 0.5D;
         double range = ServerConfig.stormSize * 2.0;
         double rangeSq = range * range;

         for (Storm storm : ((WeatherHandler)GameBusEvents.MANAGERS.get(level.dimension())).getStorms()) {
            if (storm.stage >= 3 && storm.stormType == 0) {
               double dx = bx - storm.position.x;
               double dz = bz - storm.position.z;
               if ((dx * dx + dz * dz) < rangeSq) {
                  nearTornado = true;
                  break;
               }
            }
         }

         if ((Boolean)blockState.getValue(TornadoSensorBlock.POWERED) != nearTornado) {
            level.setBlockAndUpdate(blockPos, (BlockState)blockState.setValue(TornadoSensorBlock.POWERED, nearTornado));
         }
      }
   }
}
