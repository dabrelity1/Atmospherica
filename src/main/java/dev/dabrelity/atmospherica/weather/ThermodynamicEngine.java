package dev.dabrelity.atmospherica.weather;

import dev.dabrelity.atmospherica.block.entity.RadarBlockEntity;
import dev.dabrelity.atmospherica.compat.SereneSeasons;
import dev.dabrelity.atmospherica.config.ServerConfig;
import dev.dabrelity.atmospherica.util.Util;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class ThermodynamicEngine {
   public static SimplexNoise noise = null;
   public static float xzScale = 15000.0F;
   public static float yScale = 2000.0F;
   public static float timeScale = 20000.0F;
   public static float cachedBiomeTemp = 0.0F;
   public static float cachedHumidity = 0.0F;
   public static float cachedPBLHeight = 0.0F;
   public static float cachedSfcTNoise = 0.0F;
   public static float cachedPNoise = 0.0F;
   public static float cachedNoise = 0.0F;
   public static float cachedTime = 0.0F;
   public static Vec3 cachedPos = null;

   public static float FBM(Vec3 pos, int octaves, float lacunarity, float gain, float amplitude) {
      double y = 0.0;
      // Use primitive doubles to avoid Vec3 allocations - OPTIMIZATION
      double px = pos.x, py = pos.y, pz = pos.z;
      
      for (int i = 0; i < Math.max(octaves, 1); i++) {
         y += amplitude * noise.getValue(px, py, pz);
         px *= lacunarity;
         py *= lacunarity;
         pz *= lacunarity;
         amplitude *= gain;
      }

      return (float)y;
   }

   public static ThermodynamicEngine.Precipitation getPrecipitationType(WeatherHandler weatherHandler, Vec3 pos, Level level, int advance) {
      return getPrecipitationType(weatherHandler, pos, level, advance, 250);
   }

   public static ThermodynamicEngine.Precipitation getPrecipitationType(WeatherHandler weatherHandler, Vec3 pos, Level level, int advance, int delta) {
      int start = 4000;
      ThermodynamicEngine.Precipitation precip = ThermodynamicEngine.Precipitation.SNOW;
      float groundTemp = samplePoint(weatherHandler, pos, level, null, advance).temperature();
      int y = start;

      while (y >= 0) {
         float rainTemp = samplePoint(weatherHandler, pos.add(0.0, y, 0.0), level, null, advance).temperature();
         if (rainTemp < 3.0F && rainTemp > -1.0F) {
            precip = ThermodynamicEngine.Precipitation.WINTRY_MIX;
         } else if (rainTemp <= 0.0F) {
            precip = switch (precip) {
               case RAIN, WINTRY_MIX -> ThermodynamicEngine.Precipitation.SLEET;
               default -> precip;
            };
         } else {
            precip = switch (precip) {
               case SLEET, SNOW, WINTRY_MIX -> ThermodynamicEngine.Precipitation.RAIN;
               default -> precip;
            };
         }

         y -= delta;
      }

      if ((precip == ThermodynamicEngine.Precipitation.RAIN || precip == ThermodynamicEngine.Precipitation.WINTRY_MIX) && groundTemp <= 0.0F) {
         precip = ThermodynamicEngine.Precipitation.FREEZING_RAIN;
      }

      return precip;
   }

   public static ThermodynamicEngine.AtmosphericDataPoint samplePoint(
      WeatherHandler weatherHandler, Vec3 pos, Level level, @Nullable RadarBlockEntity radarBlockEntity, int advance
   ) {
      return samplePoint(weatherHandler, pos, level, radarBlockEntity, advance, radarBlockEntity != null ? radarBlockEntity.getBlockPos().getY() : null);
   }

   @Nullable
   public static Float GetSST(WeatherHandler weatherHandler, Vec3 pos, Level level, @Nullable RadarBlockEntity radarBlockEntity, int advance) {
      BlockPos blockPos = new BlockPos((int)pos.x, level.getSeaLevel(), (int)pos.z);
      float sst = 0.0F;
      noise = WindEngine.simplexNoise;
      if (noise == null) {
         return null;
      } else {
         float time = (float)(level.getDayTime() + advance);
         float biomeTemp = 0.0F;
         float humidity = 0.0F;
         int c = 0;
         boolean isOcean = false;

         for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
               if (Mth.abs(x) != 1 || Mth.abs(z) != 1) {
                  c++;
                  BlockPos p = blockPos.offset(new Vec3i(x * 64, 0, z * 64));
                  Holder<Biome> biome;
                  if (radarBlockEntity != null && radarBlockEntity.init) {
                     biome = radarBlockEntity.getNearestBiome(p);
                  } else {
                     biome = level.getBiome(p);
                  }

                  String rn = biome.unwrapKey().map(k -> k.location().toString()).orElse("").toLowerCase();
                  boolean ocean = rn.contains("ocean");
                  if (x == 0 && z == 0) {
                     isOcean = ocean;
                  }

                  float bt = SereneSeasons.getBiomeTemperature(level, biome, p);
                  if (ocean) {
                     if (rn.contains("frozen")) {
                        bt -= 0.5F;
                     }

                     if (rn.contains("cold")) {
                        bt -= 0.35F;
                     }

                     if (rn.contains("lukewarm")) {
                        bt += 0.05F;
                     } else if (rn.contains("warm")) {
                        bt += 0.25F;
                     }
                  }

                  bt += 0.075F;
                  biomeTemp += bt;
                  humidity += ((Biome)biome.value()).getModifiedClimateSettings().downfall();
               }
            }
         }

         if (!isOcean) {
            return null;
         } else {
            float daytime = (float)(level.getDayTime() + advance) / 24000.0F;
            double x = (daytime - 0.18) * Math.PI * 2.0;
            double timeFactor = Math.sin(x + Math.sin(x) / -2.0);
            humidity /= c;
            biomeTemp /= c;
            biomeTemp -= 0.15F;
            float sfcTNoise = FBM(pos.multiply(1.0F / xzScale, 0.0, 1.0F / xzScale).add(0.0, time / timeScale, 0.0), 6, 2.0F, 0.5F, 1.0F);
            sfcTNoise *= 4.0F;
            sfcTNoise += 6.0F;
            if (biomeTemp <= 0.0F) {
               sst = Mth.lerp(-biomeTemp, 0.0F, -25.0F + sfcTNoise);
            } else {
               sst = Mth.lerp((float)Math.pow(biomeTemp / 1.85, 0.5), 0.0F, 30.0F + sfcTNoise);
            }

            sst += humidity * 3.0F;
            float sfcTempTimeMod = (float)timeFactor * 5.0F * Math.max(1.0F - humidity, 0.05F);
            sfcTempTimeMod += 5.0F;
            sst += sfcTempTimeMod / 8.5F;
            sst += 6.0F;
            return sst;
         }
      }
   }

   public static ThermodynamicEngine.AtmosphericDataPoint samplePoint(
      WeatherHandler weatherHandler, Vec3 pos, Level level, @Nullable RadarBlockEntity radarBlockEntity, int advance, @Nullable Integer groundHeight
   ) {
      BlockPos blockPos = new BlockPos((int)pos.x, (int)pos.y, (int)pos.z);
      ThermodynamicEngine.noise = WindEngine.simplexNoise;
      if (ThermodynamicEngine.noise == null) {
         return new ThermodynamicEngine.AtmosphericDataPoint(30.0F, 30.0F, 1013.0F, 30.0F);
      } else {
         float time = (float)(level.getDayTime() + advance);
         float biomeTemp = 0.0F;
         float humidity = 0.0F;
         int c = 0;
         boolean cached = false;
         if (cachedPos != null && cachedPos.equals(pos.multiply(1.0, 0.0, 1.0)) && Math.abs(time - cachedTime) < 20.0F) {
            biomeTemp = cachedBiomeTemp;
            humidity = cachedHumidity;
            cached = true;
         } else {
            for (int x = -1; x <= 1; x++) {
               for (int z = -1; z <= 1; z++) {
                  if (Mth.abs(x) != 1 || Mth.abs(z) != 1) {
                     c++;
                     BlockPos p = blockPos.offset(new Vec3i(x * 64, 0, z * 64));
                     Holder<Biome> biome;
                     if (radarBlockEntity != null && radarBlockEntity.init) {
                        biome = radarBlockEntity.getNearestBiome(p);
                     } else {
                        biome = level.getBiome(p);
                     }

                     biomeTemp += SereneSeasons.getBiomeTemperature(level, biome, p);
                     humidity += ((Biome)biome.value()).getModifiedClimateSettings().downfall();
                  }
               }
            }

            humidity /= c;
            biomeTemp /= c;
            cachedPos = pos.multiply(1.0, 0.0, 1.0);
            cachedBiomeTemp = biomeTemp;
            cachedHumidity = humidity;
            cachedTime = time;
         }

         biomeTemp -= 0.15F;
         int elevation;
         if (groundHeight != null) {
            elevation = Math.max(level.getSeaLevel(), groundHeight);
         } else {
            elevation = Math.max(level.getSeaLevel(), level.getHeight(Types.MOTION_BLOCKING, blockPos.getX(), blockPos.getZ()));
         }

         Holder<Biome> biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS);
         float gBiomeTemp = SereneSeasons.getBiomeTemperature(level, biome, blockPos);
         float gHumidity = ((Biome)biome.value()).getModifiedClimateSettings().downfall();
         humidity = Mth.lerp(Mth.clamp((float)pos.y() / 16000.0F, 0.0F, 0.15F), humidity, gHumidity);
         biomeTemp = Mth.lerp(Mth.clamp((float)pos.y() / 16000.0F, 0.0F, 0.15F), biomeTemp, gBiomeTemp - 0.15F);
         if (humidity > 0.4F) {
            humidity -= 0.4F;
            humidity /= 2.0F;
            humidity += 0.4F;
         }

         humidity = (float)Math.pow(humidity, 0.3F);
         int elevationSeaLevel = elevation - level.getSeaLevel();
         float aboveSeaLevel = (float)pos.y() - level.getSeaLevel();
         float altitude = Math.max((float)pos.y() - elevation, 0.0F);
         float daytime = (float)(level.getDayTime() + advance) / 24000.0F;
         double x = (daytime - 0.18) * Math.PI * 2.0;
         double timeFactor = Math.sin(x + Math.sin(x) / -2.0);
         float pblHeight;
         if (cached) {
            pblHeight = cachedPBLHeight;
         } else {
            pblHeight = FBM(pos.multiply(1.0F / xzScale, 0.0, 1.0F / xzScale).add(0.0, time / timeScale, 0.0), 2, 2.0F, 0.5F, 1.0F);
         }

         cachedPBLHeight = pblHeight;
         pblHeight = (Mth.clamp(pblHeight + 1.0F, 0.0F, 2.0F) + 1.0F) * 500.0F;
         double timeFactorHeightAffected = Mth.lerp(Mth.clamp(altitude / pblHeight, 0.0F, 1.0F), timeFactor, 1.0);
         float sfcPressure = 1013.25F;
         float sfcTNoise;
         if (cached) {
            sfcTNoise = cachedSfcTNoise;
         } else {
            sfcTNoise = FBM(pos.multiply(1.0F / xzScale, 0.0, 1.0F / xzScale).add(0.0, time / timeScale, 0.0), 3, 2.0F, 0.5F, 1.0F);
         }

         cachedSfcTNoise = sfcTNoise;
         sfcTNoise *= 5.0F;
         float sfcTemp;
         if (biomeTemp <= 0.0F) {
            sfcTemp = Mth.lerp(-biomeTemp, 0.0F, -20.0F + sfcTNoise);
         } else {
            sfcTemp = Mth.lerp((float)Math.pow(biomeTemp / 1.85, 0.5), 0.0F, 35.0F + sfcTNoise);
         }

         sfcTemp += humidity * 3.0F;
         float sfcTempTimeMod = (float)timeFactorHeightAffected * 5.0F * Math.max(1.0F - humidity, 0.05F);
         sfcTempTimeMod += 5.0F;
         sfcTemp += sfcTempTimeMod;
         float tNoise = sfcTNoise / 5.0F;
         sfcTemp += tNoise * 2.0F;
         sfcTemp -= elevationSeaLevel / 20.0F;
         float pNoise;
         if (cached) {
            pNoise = cachedPNoise;
         } else {
            pNoise = FBM(pos.multiply(1.0F / -xzScale, 0.0, 1.0F / -xzScale).add(0.0, time / timeScale, 0.0), 3, 2.0F, 0.5F, 1.0F);
         }

         cachedPNoise = pNoise;
         sfcPressure += pNoise * 7.0F;
         float stormCooling = 0.0F;

         for (Storm storm : weatherHandler.getStorms()) {
            if (storm.stormType == 1) {
               double distance = pos.multiply(1.0, 0.0, 1.0).distanceTo(storm.position.multiply(1.0, 0.0, 1.0));
               Vec2 v2fWorldPos = new Vec2((float)pos.x, (float)pos.z);
               Vec2 stormVel = new Vec2((float)storm.velocity.x, (float)storm.velocity.z);
               Vec2 v2fStormPos = new Vec2((float)storm.position.x, (float)storm.position.z);
               Vec2 right = new Vec2(stormVel.y, -stormVel.x).normalized();
               Vec2 fwd = stormVel.normalized();
               Vec2 le = Util.mulVec2(right, -((float)ServerConfig.stormSize) * 5.0F);
               Vec2 ri = Util.mulVec2(right, (float)ServerConfig.stormSize * 5.0F);
               Vec2 off = Util.mulVec2(
                  fwd, -((float)Math.pow(Mth.clamp(distance / ((float)ServerConfig.stormSize * 5.0F), 0.0, 1.0), 2.0)) * ((float)ServerConfig.stormSize * 1.5F)
               );
               le = le.add(off);
               ri = ri.add(off);
               le = le.add(v2fStormPos);
               ri = ri.add(v2fStormPos);
               Vec2 nearPoint = Util.nearestPoint(le, ri, v2fWorldPos);
               Vec2 facing = v2fWorldPos.add(nearPoint.negated());
               float behind = -facing.dot(fwd);
               behind += FBM(
                     new Vec3(pos.x / (ServerConfig.stormSize * 2.0), pos.z / (ServerConfig.stormSize * 2.0), (float)level.getGameTime() / 20000.0F),
                     5,
                     2.0F,
                     0.2F,
                     1.0F
                  )
                  * (float)ServerConfig.stormSize
                  * 0.25F;
               behind += (float)ServerConfig.stormSize;
               float sze = (float)ServerConfig.stormSize * 12.0F;
               if (behind > 0.0F) {
                  float p = Mth.clamp(Math.abs(behind) / sze, 0.0F, 1.0F);
                  float start = 0.02F;
                  if (p <= start) {
                     p /= start;
                  } else {
                     p = 1.0F - (p - start) / (1.0F - start);
                  }

                  stormCooling = Math.max(stormCooling, Mth.clamp(p, 0.0F, 1.0F) * 15.0F * (float)Math.pow((float)storm.coldEnergy / storm.maxColdEnergy, 0.75));
               }
            }
         }

         stormCooling *= 1.0F - Mth.clamp(advance / 12000.0F, 0.0F, 1.0F);
         sfcTemp -= stormCooling * Mth.clamp(1.0F - altitude / 3000.0F, 0.0F, 1.0F);
         float var81;
         if (humidity > 0.5F) {
            var81 = (float)Math.pow(2.0F * (humidity - 0.5F), 0.25) + 0.5F;
         } else {
            var81 = (float)Math.pow(2.0F * humidity, 4.0) * 0.5F;
         }

         float dewP = Mth.clamp(
            (float)Mth.lerp(0.7F, (ThermodynamicEngine.noise.getValue(pos.z / 2200.0, time / 9000.0F + pos.y / 100.0, pos.x / 300.0) + 1.0) / 2.0, var81),
            0.2F,
            1.0F
         );
         float sfcDew = Math.min(sfcTemp - sfcTempTimeMod, 32.0F) - Mth.clamp((1.0F - dewP) * (sfcTemp - sfcTempTimeMod), 0.0F, 15.0F);
         if (sfcDew > 0.0F) {
            sfcDew *= humidity * 0.9F + 0.1F;
         }

         sfcDew -= Mth.lerp(1.0F - var81, 0.0F, 5.0F);
         sfcDew = Math.min(sfcDew, sfcTemp);
         sfcPressure = getPressureAtHeight(elevationSeaLevel, sfcTemp, sfcPressure);
         float lapseRate = 5.5F;
         float lrNoise = tNoise;
         if (tNoise > 0.0F) {
            lrNoise = (float)Math.pow(tNoise, 1.25);
            lrNoise *= 2.0F;
         }

         lapseRate += lrNoise;
         lapseRate *= 0.4F + (1.0F - humidity);
         float var94 = Mth.lerp((tNoise + 1.0F) / 2.0F, Mth.lerp(humidity, 0.4F, 0.1F), Mth.lerp(humidity, 0.65F, 0.3F));
         float var83 = sfcTemp - lapseRate * (altitude / 1000.0F);
         float dp = sfcDew - lapseRate * (altitude / 1000.0F) * var94;
         float noise;
         if (cached) {
            noise = cachedNoise;
         } else {
            noise = FBM(pos.multiply(1.0F / xzScale, 0.0, 1.0F / -xzScale).add(0.0, time / timeScale, 0.0), 2, 2.0F, 0.5F, 1.0F);
         }

         cachedNoise = noise;
         float bumpH = elevation + Mth.clamp(noise + 0.5F, 0.5F, 1.5F) * 1250.0F;
         noise = FBM(pos.multiply(1.0F / -xzScale, 0.0, 1.0F / xzScale).add(0.0, time / timeScale, 0.0), 2, 2.0F, 0.5F, 1.0F);
         float bumpStrength = Mth.clamp(noise + 0.5F, 0.0F, 1.5F) * 5.5F * Mth.clamp(1.0F - humidity, 0.0F, 1.0F);
         bumpStrength -= 4.0F * humidity;
         if (altitude > bumpH) {
            float i = Mth.clamp((altitude - bumpH) / 150.0F, 0.0F, 1.0F);
            var83 += Mth.lerp(i, 0.0F, bumpStrength);
            dp -= Mth.lerp(i, 0.0F, bumpStrength);
         }

         float a = Mth.clamp(altitude, 0.0F, 1000.0F);
         var83 -= lapseRate * (a / 1000.0F) * 0.25F;
         dp -= lapseRate * (a / 1000.0F) * var94 * 0.25F;
         noise = FBM(pos.multiply(1.0F / xzScale, 0.0, 1.0F / xzScale).add(0.0, time / timeScale, 0.0), 2, 2.0F, 0.5F, 1.0F);
         float inversionHeight = elevationSeaLevel + Mth.lerp(Mth.clamp(noise, 0.0F, 1.0F), 12000.0F, 16000.0F);
         if (altitude > inversionHeight) {
            float dif = altitude - inversionHeight;
            float i = Mth.clamp(dif / 1500.0F, 0.0F, 1.0F);
            var83 += Mth.lerp(i, 0.0F, lapseRate * (dif / 1000.0F));
            dp += Mth.lerp(i, 0.0F, lapseRate * (dif / 1000.0F) * var94);
         }

         float offset = FBM(pos.multiply(1.0F / xzScale, 1.0F / yScale, 1.0F / xzScale).add(0.0, time / -timeScale, 0.0), 4, 2.0F, 0.5F, 1.0F);
         offset *= 1.5F;
         var83 += offset;
         dp -= offset * 1.5F;
         float p = getPressureAtHeight(aboveSeaLevel, var83, elevationSeaLevel, sfcPressure);
         float dewMin = FBM(pos.multiply(1.0F / xzScale, 1.0F / yScale, 1.0F / xzScale).add(0.0, time / -timeScale, 0.0), 4, 2.0F, 0.5F, 1.0F);
         dewMin = Mth.clamp(dewMin + 1.0F, 0.0F, 2.0F) * 2.0F;
         dewMin += (float)Math.pow(pos.y / 16000.0, 2.0) * 40.0F * (1.0F - humidity);
         float td = var83 - dewMin;
         if (dp > td) {
            float dif = dp - td;
            dp -= dif * Mth.clamp(dif / 4.0F, 0.0F, 1.0F);
         }

         dp = Math.min(var83, dp);
         return new ThermodynamicEngine.AtmosphericDataPoint(var83, dp, p, calcVTemp(var83, dp, sfcPressure));
      }
   }

   public static float getPressureAtHeight(float altitude, float temp, float sfcPressure) {
      return getPressureAtHeight(altitude, temp, 0.0F, sfcPressure);
   }

   public static float getPressureAtHeight(float altitude, float temp, float refAltitude, float refPressure) {
      return refPressure * (float)Math.exp(-(0.2841926F * (altitude - refAltitude) / (8.31432F * celsiusToKelvin(temp))));
   }

   public static float kelvinToCelsius(float k) {
      return k - 273.15F;
   }

   public static float celsiusToKelvin(float c) {
      return c + 273.15F;
   }

   public static float calcVTemp(float t, float dp, float p) {
      return kelvinToCelsius(celsiusToKelvin(t) / (1.0F - 0.379F * (6.11F * (float)Math.pow(10.0, 7.5F * dp / (237.3F + dp)) / p)));
   }

   public static ThermodynamicEngine.AtmosphericDataPoint deserializeDataPoint(CompoundTag data) {
      return new ThermodynamicEngine.AtmosphericDataPoint(
         data.getFloat("temperature"), data.getFloat("dewpoint"), data.getFloat("pressure"), data.getFloat("virtualTemperature")
      );
   }

   public record AtmosphericDataPoint(float temperature, float dewpoint, float pressure, float virtualTemperature) {
      public String toString() {
         return String.format(
            "Temperature: %s, DewPoint: %s, Pressure: %s, Virtual Temperature: %s",
            Math.floor(this.temperature * 10.0F) / 10.0,
            Math.floor(this.dewpoint * 10.0F) / 10.0,
            Math.floor(this.pressure * 10.0F) / 10.0,
            Math.floor(this.virtualTemperature * 10.0F) / 10.0
         );
      }

      public CompoundTag serializeNBT() {
         CompoundTag data = new CompoundTag();
         data.putFloat("temperature", this.temperature);
         data.putFloat("dewpoint", this.dewpoint);
         data.putFloat("pressure", this.pressure);
         data.putFloat("virtualTemperature", this.virtualTemperature);
         return data;
      }
   }

   public static enum Precipitation {
      RAIN,
      FREEZING_RAIN,
      SLEET,
      SNOW,
      WINTRY_MIX,
      HAIL;
   }
}
