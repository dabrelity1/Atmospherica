package dev.protomanly.pmweather.compat;

import dev.protomanly.pmweather.PMWeather;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public class SereneSeasons {
   private static boolean hasCheckInstalled = false;
   private static boolean installed = false;
   private static Method method_sereneseasons_getBiomeTemperature;

   public static float getBiomeTemperature(Level level, Holder<Biome> biome, BlockPos pos) {
      pos = new BlockPos(pos.getX(), level.getSeaLevel(), pos.getZ());
      if (isInstalled() && method_sereneseasons_getBiomeTemperature != null) {
         try {
            Holder<Biome> refBiome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.FOREST);
            float baseTemp = ((Biome)refBiome.value()).getBaseTemperature();
            float actualTemp = (Float)method_sereneseasons_getBiomeTemperature.invoke(null, level, refBiome, pos);
            float offset = actualTemp - baseTemp;
            return ((Biome)biome.value()).getBaseTemperature() + offset;
         } catch (Exception var7) {
            PMWeather.LOGGER.error(var7.getMessage(), var7);
         }
      }

      return ((Biome)biome.value()).getBaseTemperature();
   }

   public static boolean isInstalled() {
      if (!hasCheckInstalled) {
         try {
            hasCheckInstalled = true;
            Class class_sereneseasons_SeasonHooks = Class.forName("sereneseasons.season.SeasonHooks");
            if (class_sereneseasons_SeasonHooks != null) {
               method_sereneseasons_getBiomeTemperature = class_sereneseasons_SeasonHooks.getDeclaredMethod(
                  "getBiomeTemperature", Level.class, Holder.class, BlockPos.class
               );
               installed = true;
            }
         } catch (Exception var1) {
            installed = false;
         }

         if (installed) {
            PMWeather.LOGGER.info("PMWeather Compatibility found Serene Seasons");
         } else {
            PMWeather.LOGGER.info("PMWeather Compatibility did not find Serene Seasons");
         }
      }

      return installed;
   }
}
