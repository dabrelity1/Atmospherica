package dev.dabrelity.atmospherica.mixin;

import dev.dabrelity.atmospherica.event.GameBusClientEvents;
import dev.dabrelity.atmospherica.weather.WeatherHandlerClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ClientLevel.class})
public class ClientLevelMixin {
   @Inject(
      method = {"getSkyDarken"},
      at = {@At("RETURN")},
      cancellable = true
   )
   public void editSkyDarken(float partialTick, CallbackInfoReturnable<Float> callbackInfoReturnable) {
      float darken = 1.0F;
      WeatherHandlerClient weatherHandler = (WeatherHandlerClient)GameBusClientEvents.weatherHandler;
      if (weatherHandler != null) {
         darken = Mth.clamp(weatherHandler.getPrecipitation(), 0.0F, 1.0F) * 0.8F;
      }

      callbackInfoReturnable.setReturnValue((Float)callbackInfoReturnable.getReturnValue() * (1.0F - darken));
   }
}
