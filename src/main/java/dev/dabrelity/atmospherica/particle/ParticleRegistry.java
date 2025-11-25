package dev.dabrelity.atmospherica.particle;

import dev.dabrelity.atmospherica.Atmospherica;
import java.util.Optional;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.SpriteSourceProvider;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class ParticleRegistry extends SpriteSourceProvider {
   public static TextureAtlasSprite rain;
   public static TextureAtlasSprite mist;
   public static TextureAtlasSprite splash;
   public static TextureAtlasSprite snow;
   public static TextureAtlasSprite snow1;
   public static TextureAtlasSprite snow2;
   public static TextureAtlasSprite snow3;
   public static TextureAtlasSprite sleet;

      public ParticleRegistry(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, existingFileHelper, Atmospherica.MOD_ID);
      }

      @Override
      protected void addSources() {
      this.addSprite(Atmospherica.getPath("particle/rain"));
      this.addSprite(Atmospherica.getPath("particle/mist"));
      this.addSprite(Atmospherica.getPath("particle/splash"));
      this.addSprite(Atmospherica.getPath("particle/snow"));
      this.addSprite(Atmospherica.getPath("particle/snow1"));
      this.addSprite(Atmospherica.getPath("particle/snow2"));
      this.addSprite(Atmospherica.getPath("particle/snow3"));
      this.addSprite(Atmospherica.getPath("particle/sleet"));
   }

   public void addSprite(ResourceLocation resourceLocation) {
      this.atlas(SpriteSourceProvider.PARTICLES_ATLAS).addSource(new SingleFile(resourceLocation, Optional.empty()));
   }

   @Mod.EventBusSubscriber(modid = Atmospherica.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
   public static class Events {
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
}
