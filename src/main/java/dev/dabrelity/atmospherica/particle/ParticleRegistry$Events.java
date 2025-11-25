package dev.dabrelity.atmospherica.particle;

import dev.dabrelity.atmospherica.Atmospherica;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Atmospherica.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public class ParticleRegistry$Events {
   @SubscribeEvent
   @SuppressWarnings("deprecation")
   public static void getRegisteredParticles(TextureStitchEvent.Post event) {
      if (event.getAtlas().location().equals(TextureAtlas.LOCATION_PARTICLES)) {
         ParticleRegistry.rain = event.getAtlas().getSprite(Atmospherica.getPath("particle/rain"));
         ParticleRegistry.mist = event.getAtlas().getSprite(Atmospherica.getPath("particle/mist"));
         ParticleRegistry.snow = event.getAtlas().getSprite(Atmospherica.getPath("particle/snow"));
         ParticleRegistry.snow1 = event.getAtlas().getSprite(Atmospherica.getPath("particle/snow1"));
         ParticleRegistry.snow2 = event.getAtlas().getSprite(Atmospherica.getPath("particle/snow2"));
         ParticleRegistry.snow3 = event.getAtlas().getSprite(Atmospherica.getPath("particle/snow3"));
         ParticleRegistry.splash = event.getAtlas().getSprite(Atmospherica.getPath("particle/splash"));
         ParticleRegistry.sleet = event.getAtlas().getSprite(Atmospherica.getPath("particle/sleet"));
      }
   }
}
