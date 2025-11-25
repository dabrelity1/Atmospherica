package dev.protomanly.pmweather;

import com.mojang.logging.LogUtils;
import dev.protomanly.pmweather.block.ModBlocks;
import dev.protomanly.pmweather.block.entity.ModBlockEntities;
import dev.protomanly.pmweather.compat.DistantHorizons;
import dev.protomanly.pmweather.config.ClientConfig;
import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.creative.ModCreativeTabs;
import dev.protomanly.pmweather.entity.ModEntities;
import dev.protomanly.pmweather.item.ModItems;
import dev.protomanly.pmweather.item.component.ModComponents;
import dev.protomanly.pmweather.multiblock.MultiBlocks;
import dev.protomanly.pmweather.sound.ModSounds;
import java.util.Random;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.javafmlmod.FMLModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;

@Mod("pmweather")
public class PMWeather {
   public static final String MOD_ID = "pmweather";
   public static final Logger LOGGER = LogUtils.getLogger();
   public static final Random RANDOM = new Random();

   public PMWeather(FMLModContainer container, IEventBus bus, Dist dist) {
      ModComponents.COMPONENTS.register(bus);
      ModEntities.ENTITY_TYPES.register(bus);
      ModBlockEntities.BLOCK_ENTITIES.register(bus);
      MultiBlocks.register();
      ModBlocks.BLOCKS.register(bus);
      ModItems.ITEMS.register(bus);
      ModCreativeTabs.CREATIVE_MODE_TABS.register(bus);
      ModSounds.SOUND_EVENTS.register(bus);
      if (dist.isClient()) {
         container.registerConfig(Type.CLIENT, ClientConfig.SPEC);

         try {
            DistantHorizons.initialize();
            LOGGER.info("Initialized Distant Horizons compatibility");
         } catch (Exception var5) {
            LOGGER.info("Failed to initialize Distant Horizons compatibility: {}", var5.getMessage());
         }
      }

      container.registerConfig(Type.SERVER, ServerConfig.SPEC);
      if (dist.isClient()) {
         container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
      }
   }

   public static ResourceLocation getPath(String path) {
      return ResourceLocation.fromNamespaceAndPath("pmweather", path);
   }
}
