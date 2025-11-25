package dev.protomanly.pmweather.mixin;

import dev.protomanly.pmweather.event.GameBusClientEvents;
import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import dev.protomanly.pmweather.weather.WeatherHandler;
import dev.protomanly.pmweather.weather.WeatherHandlerClient;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Level.class})
public class LevelMixin {
   @Inject(
      method = {"isRainingAt"},
      at = {@At("RETURN")},
      cancellable = true
   )
   public void editRainingAt(BlockPos pos, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
      Level level = (Level)this;
      ThermodynamicEngine.AtmosphericDataPoint dataPoint;
      float rain;
      if (level.isClientSide()) {
         GameBusClientEvents.getClientWeather();
         WeatherHandlerClient weatherHandler = (WeatherHandlerClient)GameBusClientEvents.weatherHandler;
         rain = weatherHandler.getPrecipitation();
         dataPoint = ThermodynamicEngine.samplePoint(weatherHandler, pos.getCenter(), level, null, 0);
      } else {
         WeatherHandler weatherHandler = GameBusEvents.MANAGERS.get(((Level)this).dimension());
         rain = weatherHandler.getPrecipitation(pos.getCenter());
         dataPoint = ThermodynamicEngine.samplePoint(weatherHandler, pos.getCenter(), level, null, 0);
      }

      if (rain <= 0.2F || dataPoint.temperature() <= 0.0F) {
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
      method = {"getRainLevel"},
      at = {@At("RETURN")},
      cancellable = true
   )
   public void editRain(float delta, CallbackInfoReturnable<Float> callbackInfoReturnable) {
      Level level = (Level)this;
      if (level.isClientSide() && GameBusClientEvents.weatherHandler != null) {
         GameBusClientEvents.getClientWeather();
         callbackInfoReturnable.setReturnValue(((WeatherHandlerClient)GameBusClientEvents.weatherHandler).getPrecipitation());
      } else {
         callbackInfoReturnable.setReturnValue(0.0F);
      }
   }

   @Inject(
      method = {"getThunderLevel"},
      at = {@At("RETURN")},
      cancellable = true
   )
   public void editThunder(float delta, CallbackInfoReturnable<Float> callbackInfoReturnable) {
      Level level = (Level)this;
      if (level.isClientSide() && GameBusClientEvents.weatherHandler != null) {
         GameBusClientEvents.getClientWeather();
         float rainAmount = ((WeatherHandlerClient)GameBusClientEvents.weatherHandler).getPrecipitation();
         callbackInfoReturnable.setReturnValue(rainAmount);
      } else {
         callbackInfoReturnable.setReturnValue(0.0F);
      }
   }
}
