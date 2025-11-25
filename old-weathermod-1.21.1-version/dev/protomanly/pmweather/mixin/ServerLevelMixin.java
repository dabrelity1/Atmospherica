package dev.protomanly.pmweather.mixin;

import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.block.ModBlocks;
import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import dev.protomanly.pmweather.weather.WeatherHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome.Precipitation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerLevel.class})
public class ServerLevelMixin {
   public boolean shouldFreeze(ServerLevel level, BlockPos water, boolean mustBeAtEdge) {
      if (water.getY() >= level.getMinBuildHeight() && water.getY() < level.getMaxBuildHeight() && level.getBrightness(LightLayer.BLOCK, water) < 10) {
         BlockState blockstate = level.getBlockState(water);
         FluidState fluidstate = level.getFluidState(water);
         if (fluidstate.getType() == Fluids.WATER && blockstate.getBlock() instanceof LiquidBlock) {
            if (!mustBeAtEdge) {
               return true;
            }

            boolean flag = level.isWaterAt(water.west()) && level.isWaterAt(water.east()) && level.isWaterAt(water.north()) && level.isWaterAt(water.south());
            if (!flag) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean shouldSnow(ServerLevel level, BlockPos pos) {
      if (pos.getY() >= level.getMinBuildHeight() && pos.getY() < level.getMaxBuildHeight()) {
         BlockState blockstate = level.getBlockState(pos);
         if ((blockstate.isAir() || blockstate.is(Blocks.SNOW)) && Blocks.SNOW.defaultBlockState().canSurvive(level, pos)) {
            return true;
         }
      }

      return false;
   }

   public boolean shouldIce(ServerLevel level, BlockPos pos) {
      if (pos.getY() >= level.getMinBuildHeight() && pos.getY() < level.getMaxBuildHeight()) {
         BlockState blockstate = level.getBlockState(pos);
         if ((blockstate.isAir() || blockstate.is((Block)ModBlocks.ICE_LAYER.get()))
            && ((Block)ModBlocks.ICE_LAYER.get()).defaultBlockState().canSurvive(level, pos)) {
            return true;
         }
      }

      return false;
   }

   public boolean shouldSleet(ServerLevel level, BlockPos pos) {
      if (pos.getY() >= level.getMinBuildHeight() && pos.getY() < level.getMaxBuildHeight()) {
         BlockState blockstate = level.getBlockState(pos);
         if ((blockstate.isAir() || blockstate.is((Block)ModBlocks.SLEET_LAYER.get()))
            && ((Block)ModBlocks.SLEET_LAYER.get()).defaultBlockState().canSurvive(level, pos)) {
            return true;
         }
      }

      return false;
   }

   @Inject(
      method = {"tickPrecipitation"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void editTickPrecipitation(BlockPos blockPos, CallbackInfo callbackInfo) {
      ServerLevel level = (ServerLevel)this;
      BlockPos blockpos = level.getHeightmapPos(Types.MOTION_BLOCKING, blockPos);
      BlockPos blockpos1 = blockpos.below();
      WeatherHandler weatherHandler = GameBusEvents.MANAGERS.get(level.dimension());
      ThermodynamicEngine.AtmosphericDataPoint dataPoint = ThermodynamicEngine.samplePoint(weatherHandler, blockpos.getCenter(), level, null, 0);
      float rain = weatherHandler.getPrecipitation(blockpos.getCenter());
      if (level.isAreaLoaded(blockpos1, 1) && dataPoint.temperature() <= 0.0F && this.shouldFreeze(level, blockpos1, true)) {
         level.setBlockAndUpdate(blockpos1, Blocks.ICE.defaultBlockState());
      }

      BlockState blockstate = level.getBlockState(blockpos);
      BlockState blockState1 = level.getBlockState(blockpos1);
      int c = 20;
      if (dataPoint.temperature() > 2.0F) {
         c = 10;
      }

      if (dataPoint.temperature() > 4.0F) {
         c = 4;
      }

      if (dataPoint.temperature() > 6.0F) {
         c = 1;
      }

      int decrease = 1;
      if (dataPoint.temperature() > 8.0F) {
         decrease = 2;
      }

      if (dataPoint.temperature() > 10.0F) {
         decrease = 3;
      }

      if ((blockstate.is(Blocks.SNOW) || blockstate.is((Block)ModBlocks.SLEET_LAYER.get()) || blockstate.is((Block)ModBlocks.ICE_LAYER.get()))
         && dataPoint.temperature() > 0.0F
         && PMWeather.RANDOM.nextInt(c) == 0) {
         int j = (Integer)blockstate.getValue(SnowLayerBlock.LAYERS);
         if (j > decrease) {
            BlockState blockstate1 = (BlockState)blockstate.setValue(SnowLayerBlock.LAYERS, j - decrease);
            Block.pushEntitiesUp(blockstate, blockstate1, level, blockpos);
            level.setBlockAndUpdate(blockpos, blockstate1);
         } else {
            level.removeBlock(blockpos, false);
         }
      }

      if (blockState1.is(Blocks.ICE) && dataPoint.temperature() > 0.0F && PMWeather.RANDOM.nextInt(c) == 0) {
         level.setBlockAndUpdate(blockpos1, Blocks.WATER.defaultBlockState());
      }

      if (rain > 0.1F) {
         int i = ServerConfig.snowAccumulationHeight;
         ThermodynamicEngine.Precipitation precip = ThermodynamicEngine.getPrecipitationType(weatherHandler, blockpos.getCenter(), level, 500);
         if (precip == ThermodynamicEngine.Precipitation.WINTRY_MIX) {
            precip = switch (PMWeather.RANDOM.nextInt(3)) {
               case 0 -> ThermodynamicEngine.Precipitation.SNOW;
               case 1 -> ThermodynamicEngine.Precipitation.SLEET;
               case 2 -> ThermodynamicEngine.Precipitation.FREEZING_RAIN;
               default -> ThermodynamicEngine.Precipitation.WINTRY_MIX;
            };
         }

         if (precip == ThermodynamicEngine.Precipitation.SNOW && i > 0 && this.shouldSnow(level, blockpos)) {
            BlockState blockstateW = level.getBlockState(blockpos);
            if (blockstateW.is(Blocks.SNOW)) {
               int j = (Integer)blockstateW.getValue(SnowLayerBlock.LAYERS);
               if (j < Math.min(i, 8)) {
                  BlockState blockstate1 = (BlockState)blockstateW.setValue(SnowLayerBlock.LAYERS, j + 1);
                  Block.pushEntitiesUp(blockstateW, blockstate1, level, blockpos);
                  level.setBlockAndUpdate(blockpos, blockstate1);
               }
            } else {
               level.setBlockAndUpdate(blockpos, Blocks.SNOW.defaultBlockState());
            }
         }

         if (precip == ThermodynamicEngine.Precipitation.SLEET && i > 0 && this.shouldSleet(level, blockpos)) {
            BlockState blockstateW = level.getBlockState(blockpos);
            if (blockstateW.is((Block)ModBlocks.SLEET_LAYER.get())) {
               int j = (Integer)blockstateW.getValue(SnowLayerBlock.LAYERS);
               if (j < Math.min(i, 8)) {
                  BlockState blockstate1 = (BlockState)blockstateW.setValue(SnowLayerBlock.LAYERS, j + 1);
                  Block.pushEntitiesUp(blockstateW, blockstate1, level, blockpos);
                  level.setBlockAndUpdate(blockpos, blockstate1);
               }
            } else {
               level.setBlockAndUpdate(blockpos, ((Block)ModBlocks.SLEET_LAYER.get()).defaultBlockState());
            }
         }

         if (precip == ThermodynamicEngine.Precipitation.FREEZING_RAIN && i > 0 && this.shouldIce(level, blockpos)) {
            BlockState blockstateW = level.getBlockState(blockpos);
            if (blockstateW.is((Block)ModBlocks.ICE_LAYER.get())) {
               int j = (Integer)blockstateW.getValue(SnowLayerBlock.LAYERS);
               if (j < Math.min(i, 8) && PMWeather.RANDOM.nextInt(3) == 0) {
                  BlockState blockstate1 = (BlockState)blockstateW.setValue(SnowLayerBlock.LAYERS, j + 1);
                  Block.pushEntitiesUp(blockstateW, blockstate1, level, blockpos);
                  level.setBlockAndUpdate(blockpos, blockstate1);
               }
            } else {
               level.setBlockAndUpdate(blockpos, ((Block)ModBlocks.ICE_LAYER.get()).defaultBlockState());
            }
         }

         Precipitation biome$precipitation = Precipitation.NONE;
         if (rain > 0.1F) {
            biome$precipitation = Precipitation.RAIN;
            if (dataPoint.temperature() <= 0.0F) {
               biome$precipitation = Precipitation.SNOW;
            }
         }

         if (biome$precipitation != Precipitation.NONE) {
            BlockState blockstate2 = level.getBlockState(blockpos1);
            blockstate2.getBlock().handlePrecipitation(blockstate2, level, blockpos1, biome$precipitation);
         }
      }

      callbackInfo.cancel();
   }
}
