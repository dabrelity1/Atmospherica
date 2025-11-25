package dev.dabrelity.atmospherica.event;

import dev.dabrelity.atmospherica.Atmospherica;
import dev.dabrelity.atmospherica.block.entity.ModBlockEntities;
import dev.dabrelity.atmospherica.entity.ModEntities;
import dev.dabrelity.atmospherica.entity.client.MovingBlockRenderer;
import dev.dabrelity.atmospherica.item.ModItems;
import dev.dabrelity.atmospherica.item.component.ModComponents;
import dev.dabrelity.atmospherica.render.AnemometerModel;
import dev.dabrelity.atmospherica.render.AnemometerRenderer;
import dev.dabrelity.atmospherica.render.RadarRenderer;
import dev.dabrelity.atmospherica.render.SoundingViewerRenderer;
import dev.dabrelity.atmospherica.render.WeatherBalloonModel;
import dev.dabrelity.atmospherica.render.WeatherPlatformRenderer;
import dev.dabrelity.atmospherica.shaders.ModShaders;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers;

@EventBusSubscriber(
   modid = Atmospherica.MOD_ID,
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public class ModBusClientEvents {
   @SubscribeEvent
   public static void reloadListeners(RegisterClientReloadListenersEvent event) {
      event.registerReloadListener(new SimplePreparableReloadListener<Object>() {
         protected Object prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            return null;
         }

         protected void apply(Object o, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            ModShaders.reload();
         }
      });
   }

   @SubscribeEvent
   public static void onClientSetup(FMLClientSetupEvent event) {
      EntityRenderers.register(ModEntities.MOVING_BLOCK.get(), MovingBlockRenderer::new);
      registerItemProp(
         event,
         (Item)ModItems.CONNECTOR.get(),
         Atmospherica.getPath("connected"),
         (itemStack, clientLevel, livingEntity, i) -> ModComponents.WEATHER_BALLOON_PLATFORM.has(itemStack) ? 1.0F : 0.0F
      );
   }

   public static void registerItemProp(FMLClientSetupEvent event, Item item, ResourceLocation propertyID, ItemPropertyFunction function) {
      event.enqueueWork(() -> ItemProperties.register(item, propertyID, function));
   }

   @SubscribeEvent
   public static void registerLayers(RegisterLayerDefinitions event) {
      event.registerLayerDefinition(AnemometerModel.LAYER_LOCATION, AnemometerModel::createBodyLayer);
      event.registerLayerDefinition(WeatherBalloonModel.LAYER_LOCATION, WeatherBalloonModel::createBodyLayer);
   }

   @SubscribeEvent
   public static void registerRenderers(RegisterRenderers event) {
      event.registerBlockEntityRenderer(ModBlockEntities.ANEMOMETER_BE.get(), AnemometerRenderer::new);
      event.registerBlockEntityRenderer(ModBlockEntities.RADAR_BE.get(), RadarRenderer::new);
      event.registerBlockEntityRenderer(ModBlockEntities.WEATHER_PLATFORM_BE.get(), WeatherPlatformRenderer::new);
      event.registerBlockEntityRenderer(ModBlockEntities.SOUNDING_VIEWER_BE.get(), SoundingViewerRenderer::new);
   }
}
