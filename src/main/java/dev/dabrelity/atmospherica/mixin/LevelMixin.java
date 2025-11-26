package dev.dabrelity.atmospherica.mixin;

import dev.dabrelity.atmospherica.event.GameBusClientEvents;
import dev.dabrelity.atmospherica.event.GameBusEvents;
import dev.dabrelity.atmospherica.weather.ThermodynamicEngine;
import dev.dabrelity.atmospherica.weather.ThermodynamicEngine.AtmosphericDataPoint;
import dev.dabrelity.atmospherica.weather.WeatherHandler;
import dev.dabrelity.atmospherica.weather.WeatherHandlerClient;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.concurrent.ConcurrentHashMap;

@Mixin({Level.class})
public class LevelMixin {
   // Cache for precipitation results - keyed by chunk position (x >> 4, z >> 4)
   // Cleared every 20 ticks (1 second) to balance accuracy vs performance
   @Unique
   private static final ConcurrentHashMap<Long, Float> atmospherica$precipCache = new ConcurrentHashMap<>();
   @Unique
   private static final ConcurrentHashMap<Long, Float> atmospherica$tempCache = new ConcurrentHashMap<>();
   @Unique
   private static long atmospherica$lastCacheClear = 0;
   
   @Unique
   private static long atmospherica$chunkKey(int x, int z) {
      return ((long)(x >> 4) << 32) | ((long)(z >> 4) & 0xFFFFFFFFL);
   }

   @Inject(
      method = {"m_46758_"},
      at = {@At("RETURN")},
      cancellable = true,
      remap = false
   )
   public void editRainingAt(BlockPos pos, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
      Level level = (Level)(Object)this;
      
      // Clear cache every 20 ticks
      long gameTime = level.getGameTime();
      if (gameTime - atmospherica$lastCacheClear > 20) {
         atmospherica$precipCache.clear();
         atmospherica$tempCache.clear();
         atmospherica$lastCacheClear = gameTime;
      }
      
      long chunkKey = atmospherica$chunkKey(pos.getX(), pos.getZ());
      
      float rain;
      float temperature;
      
      // Check cache first
      Float cachedRain = atmospherica$precipCache.get(chunkKey);
      Float cachedTemp = atmospherica$tempCache.get(chunkKey);
      
      if (cachedRain != null && cachedTemp != null) {
         rain = cachedRain;
         temperature = cachedTemp;
      } else {
         // Calculate and cache
         if (level.isClientSide()) {
            GameBusClientEvents.getClientWeather();
            WeatherHandlerClient weatherHandler = (WeatherHandlerClient)GameBusClientEvents.weatherHandler;
            if (weatherHandler == null) {
               callbackInfoReturnable.setReturnValue(false);
               return;
            }
            rain = weatherHandler.getPrecipitation();
            AtmosphericDataPoint dataPoint = ThermodynamicEngine.samplePoint(weatherHandler, pos.getCenter(), level, null, 0);
            temperature = dataPoint != null ? dataPoint.temperature() : 15.0F;
         } else {
            WeatherHandler weatherHandler = (WeatherHandler)GameBusEvents.MANAGERS.get(((Level)(Object)this).dimension());
            if (weatherHandler == null) {
               callbackInfoReturnable.setReturnValue(false);
               return;
            }
            rain = weatherHandler.getPrecipitation(pos.getCenter());
            AtmosphericDataPoint dataPoint = ThermodynamicEngine.samplePoint(weatherHandler, pos.getCenter(), level, null, 0);
            temperature = dataPoint != null ? dataPoint.temperature() : 15.0F;
         }
         
         atmospherica$precipCache.put(chunkKey, rain);
         atmospherica$tempCache.put(chunkKey, temperature);
      }

      if (rain <= 0.2F || temperature <= 0.0F) {
         callbackInfoReturnable.setReturnValue(false);
      } else if (!level.canSeeSky(pos)) {
         callbackInfoReturnable.setReturnValue(false);
      } else if (level.getHeightmapPos(Types.MOTION_BLOCKING, pos).getY() > pos.getY()) {
         callbackInfoReturnable.setReturnValue(false);
      } else {
         callbackInfoReturnable.setReturnValue(true);
      }
   }

   @Inject(
      method = {"m_46722_"},
      at = {@At("RETURN")},
      cancellable = true,
      remap = false
   )
   public void editRain(float delta, CallbackInfoReturnable<Float> callbackInfoReturnable) {
      Level level = (Level)(Object)this;
      if (level.isClientSide() && GameBusClientEvents.weatherHandler != null) {
         GameBusClientEvents.getClientWeather();
         callbackInfoReturnable.setReturnValue(((WeatherHandlerClient)GameBusClientEvents.weatherHandler).getPrecipitation());
      } else {
         callbackInfoReturnable.setReturnValue(0.0F);
      }
   }

   @Inject(
      method = {"m_46661_"},
      at = {@At("RETURN")},
      cancellable = true,
      remap = false
   )
   public void editThunder(float delta, CallbackInfoReturnable<Float> callbackInfoReturnable) {
      Level level = (Level)(Object)this;
      if (level.isClientSide() && GameBusClientEvents.weatherHandler != null) {
         GameBusClientEvents.getClientWeather();
         float rainAmount = ((WeatherHandlerClient)GameBusClientEvents.weatherHandler).getPrecipitation();
         callbackInfoReturnable.setReturnValue(rainAmount);
      } else {
         callbackInfoReturnable.setReturnValue(0.0F);
      }
   }
}
