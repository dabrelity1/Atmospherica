package dev.dabrelity.atmospherica;

import com.mojang.logging.LogUtils;
import dev.dabrelity.atmospherica.block.ModBlocks;
import dev.dabrelity.atmospherica.block.entity.ModBlockEntities;
import dev.dabrelity.atmospherica.compat.DistantHorizons;
import dev.dabrelity.atmospherica.compat.EmbeddiumCompat;
import dev.dabrelity.atmospherica.compat.OculusCompat;
import dev.dabrelity.atmospherica.config.ClientConfig;
import dev.dabrelity.atmospherica.config.ServerConfig;
import dev.dabrelity.atmospherica.creative.ModCreativeTabs;
import dev.dabrelity.atmospherica.entity.ModEntities;
import dev.dabrelity.atmospherica.item.ModItems;
import dev.dabrelity.atmospherica.item.component.ModComponents;
import dev.dabrelity.atmospherica.multiblock.MultiBlocks;
import dev.dabrelity.atmospherica.networking.ModNetworking;
import dev.dabrelity.atmospherica.sound.ModSounds;
import java.util.Random;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(Atmospherica.MOD_ID)
public class Atmospherica {
   public static final String MOD_ID = "atmospherica";
   public static final Logger LOGGER = LogUtils.getLogger();
   public static final Random RANDOM = new Random();

   public Atmospherica() {
      IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
      // ModComponents uses NBT in 1.20.1 - no registry needed
      ModEntities.ENTITY_TYPES.register(modEventBus);
      ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
      MultiBlocks.register();
      ModBlocks.BLOCKS.register(modEventBus);
      ModItems.ITEMS.register(modEventBus);
      ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
      ModSounds.SOUND_EVENTS.register(modEventBus);
      ModNetworking.init();

      ModLoadingContext.get().registerConfig(Type.CLIENT, ClientConfig.SPEC);
      ModLoadingContext.get().registerConfig(Type.SERVER, ServerConfig.SPEC);

      if (FMLEnvironment.dist == Dist.CLIENT) {
         ClientOnly.init();
      }

      MinecraftForge.EVENT_BUS.register(this);
   }

   @OnlyIn(Dist.CLIENT)
   private static class ClientOnly {
      static void init() {
         // Initialize mod compatibility layers
         try {
            DistantHorizons.initialize();
            LOGGER.info("Initialized Distant Horizons compatibility");
         } catch (Exception ex) {
            LOGGER.debug("Distant Horizons not available: {}", ex.getMessage());
         }
         
         try {
            OculusCompat.initialize();
            if (OculusCompat.isOculusPresent()) {
               LOGGER.info("Oculus (Iris) detected - shader compatibility enabled");
            }
         } catch (Exception ex) {
            LOGGER.debug("Oculus compatibility check failed: {}", ex.getMessage());
         }
         
         try {
            EmbeddiumCompat.initialize();
            if (EmbeddiumCompat.isSodiumLikeModPresent()) {
               LOGGER.info("Embeddium/Rubidium detected - rendering compatibility enabled");
            }
         } catch (Exception ex) {
            LOGGER.debug("Embeddium compatibility check failed: {}", ex.getMessage());
         }
      }
   }

   public static ResourceLocation getPath(String path) {
      return new ResourceLocation(MOD_ID, path);
   }
}
