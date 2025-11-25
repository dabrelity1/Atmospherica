package dev.dabrelity.atmospherica.mixin;

import dev.dabrelity.atmospherica.Atmospherica;
import dev.dabrelity.atmospherica.block.ModBlocks;
import dev.dabrelity.atmospherica.config.ServerConfig;
import dev.dabrelity.atmospherica.event.GameBusEvents;
import dev.dabrelity.atmospherica.weather.ThermodynamicEngine;
import dev.dabrelity.atmospherica.weather.WeatherHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
   
   @Unique
   private boolean Atmospherica$shouldFreeze(ServerLevel level, BlockPos water, boolean mustBeAtEdge) {
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

   @Unique
   private boolean Atmospherica$shouldSnow(ServerLevel level, BlockPos pos) {
      if (pos.getY() >= level.getMinBuildHeight() && pos.getY() < level.getMaxBuildHeight()) {
         BlockState blockstate = level.getBlockState(pos);
         if ((blockstate.isAir() || blockstate.is(Blocks.SNOW)) && Blocks.SNOW.defaultBlockState().canSurvive(level, pos)) {
            return true;
         }
      }

      return false;
   }

   @Unique
   private boolean Atmospherica$shouldIce(ServerLevel level, BlockPos pos) {
      if (pos.getY() >= level.getMinBuildHeight() && pos.getY() < level.getMaxBuildHeight()) {
         BlockState blockstate = level.getBlockState(pos);
         if ((blockstate.isAir() || blockstate.is((Block)ModBlocks.ICE_LAYER.get()))
            && ((Block)ModBlocks.ICE_LAYER.get()).defaultBlockState().canSurvive(level, pos)) {
            return true;
         }
      }

      return false;
   }

   @Unique
   private boolean Atmospherica$shouldSleet(ServerLevel level, BlockPos pos) {
      if (pos.getY() >= level.getMinBuildHeight() && pos.getY() < level.getMaxBuildHeight()) {
         BlockState blockstate = level.getBlockState(pos);
         if ((blockstate.isAir() || blockstate.is((Block)ModBlocks.SLEET_LAYER.get()))
            && ((Block)ModBlocks.SLEET_LAYER.get()).defaultBlockState().canSurvive(level, pos)) {
            return true;
         }
      }

      return false;
   }

   /**
    * In 1.20.1, precipitation is handled inside tickChunk. We inject at TAIL to add our custom
    * precipitation logic after vanilla's chunk tick.
    */
   @Inject(method = "tickChunk", at = @At("TAIL"))
   private void Atmospherica$onTickChunk(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
      ServerLevel level = (ServerLevel)(Object)this;
      
      // Only process if we have a weather handler for this dimension
      WeatherHandler weatherHandler = GameBusEvents.MANAGERS.get(level.dimension());
      if (weatherHandler == null) {
         return;
      }
      
      // Get a random position within the chunk for our custom precipitation
      BlockPos randomPos = level.getBlockRandomPos(chunk.getPos().getMinBlockX(), 0, chunk.getPos().getMinBlockZ(), 15);
      BlockPos blockpos = level.getHeightmapPos(Types.MOTION_BLOCKING, randomPos);
      BlockPos blockpos1 = blockpos.below();
      
      ThermodynamicEngine.AtmosphericDataPoint dataPoint = ThermodynamicEngine.samplePoint(weatherHandler, blockpos.getCenter(), level, null, 0);
      float rain = weatherHandler.getPrecipitation(blockpos.getCenter());
      
      if (level.isAreaLoaded(blockpos1, 1) && dataPoint.temperature() <= 0.0F && Atmospherica$shouldFreeze(level, blockpos1, true)) {
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
         && Atmospherica.RANDOM.nextInt(c) == 0) {
         int j = blockstate.getValue(SnowLayerBlock.LAYERS);
         if (j > decrease) {
            BlockState blockstate1 = blockstate.setValue(SnowLayerBlock.LAYERS, j - decrease);
            Block.pushEntitiesUp(blockstate, blockstate1, level, blockpos);
            level.setBlockAndUpdate(blockpos, blockstate1);
         } else {
            level.removeBlock(blockpos, false);
         }
      }

      if (blockState1.is(Blocks.ICE) && dataPoint.temperature() > 0.0F && Atmospherica.RANDOM.nextInt(c) == 0) {
         level.setBlockAndUpdate(blockpos1, Blocks.WATER.defaultBlockState());
      }

      if (rain > 0.1F) {
         int i = ServerConfig.snowAccumulationHeight;
         ThermodynamicEngine.Precipitation precip = ThermodynamicEngine.getPrecipitationType(weatherHandler, blockpos.getCenter(), level, 500);
         if (precip == ThermodynamicEngine.Precipitation.WINTRY_MIX) {
            int rand = Atmospherica.RANDOM.nextInt(3);
            if (rand == 0) {
               precip = ThermodynamicEngine.Precipitation.SNOW;
            } else if (rand == 1) {
               precip = ThermodynamicEngine.Precipitation.SLEET;
            } else {
               precip = ThermodynamicEngine.Precipitation.FREEZING_RAIN;
            }
         }

         if (precip == ThermodynamicEngine.Precipitation.SNOW && i > 0 && Atmospherica$shouldSnow(level, blockpos)) {
            BlockState blockstateW = level.getBlockState(blockpos);
            if (blockstateW.is(Blocks.SNOW)) {
               int j = blockstateW.getValue(SnowLayerBlock.LAYERS);
               if (j < Math.min(i, 8)) {
                  BlockState blockstate1 = blockstateW.setValue(SnowLayerBlock.LAYERS, j + 1);
                  Block.pushEntitiesUp(blockstateW, blockstate1, level, blockpos);
                  level.setBlockAndUpdate(blockpos, blockstate1);
               }
            } else {
               level.setBlockAndUpdate(blockpos, Blocks.SNOW.defaultBlockState());
            }
         }

         if (precip == ThermodynamicEngine.Precipitation.SLEET && i > 0 && Atmospherica$shouldSleet(level, blockpos)) {
            BlockState blockstateW = level.getBlockState(blockpos);
            if (blockstateW.is((Block)ModBlocks.SLEET_LAYER.get())) {
               int j = blockstateW.getValue(SnowLayerBlock.LAYERS);
               if (j < Math.min(i, 8)) {
                  BlockState blockstate1 = blockstateW.setValue(SnowLayerBlock.LAYERS, j + 1);
                  Block.pushEntitiesUp(blockstateW, blockstate1, level, blockpos);
                  level.setBlockAndUpdate(blockpos, blockstate1);
               }
            } else {
               level.setBlockAndUpdate(blockpos, ((Block)ModBlocks.SLEET_LAYER.get()).defaultBlockState());
            }
         }

         if (precip == ThermodynamicEngine.Precipitation.FREEZING_RAIN && i > 0 && Atmospherica$shouldIce(level, blockpos)) {
            BlockState blockstateW = level.getBlockState(blockpos);
            if (blockstateW.is((Block)ModBlocks.ICE_LAYER.get())) {
               int j = blockstateW.getValue(SnowLayerBlock.LAYERS);
               if (j < Math.min(i, 8) && Atmospherica.RANDOM.nextInt(3) == 0) {
                  BlockState blockstate1 = blockstateW.setValue(SnowLayerBlock.LAYERS, j + 1);
                  Block.pushEntitiesUp(blockstateW, blockstate1, level, blockpos);
                  level.setBlockAndUpdate(blockpos, blockstate1);
               }
            } else {
               level.setBlockAndUpdate(blockpos, ((Block)ModBlocks.ICE_LAYER.get()).defaultBlockState());
            }
         }

         Biome.Precipitation biome$precipitation = Biome.Precipitation.NONE;
         if (rain > 0.1F) {
            biome$precipitation = Biome.Precipitation.RAIN;
            if (dataPoint.temperature() <= 0.0F) {
               biome$precipitation = Biome.Precipitation.SNOW;
            }
         }

         if (biome$precipitation != Biome.Precipitation.NONE) {
            BlockState blockstate2 = level.getBlockState(blockpos1);
            blockstate2.getBlock().handlePrecipitation(blockstate2, level, blockpos1, biome$precipitation);
         }
      }
   }
}
