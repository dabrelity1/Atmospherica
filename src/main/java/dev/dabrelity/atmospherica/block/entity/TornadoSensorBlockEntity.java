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

         for (Storm storm : ((WeatherHandler)GameBusEvents.MANAGERS.get(level.dimension())).getStorms()) {
            double dist = blockPos.getCenter().multiply(1.0, 0.0, 1.0).distanceTo(storm.position.multiply(1.0, 0.0, 1.0));
            if (dist < ServerConfig.stormSize * 2.0 && storm.stage >= 3 && storm.stormType == 0) {
               nearTornado = true;
               break;
            }
         }

         if ((Boolean)blockState.getValue(TornadoSensorBlock.POWERED) != nearTornado) {
            level.setBlockAndUpdate(blockPos, (BlockState)blockState.setValue(TornadoSensorBlock.POWERED, nearTornado));
         }
      }
   }
}
