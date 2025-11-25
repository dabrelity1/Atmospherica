package dev.protomanly.pmweather.weather;

import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.event.GameBusClientEvents;
import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.util.Util;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class WindEngine {
   public static SimplexNoise simplexNoise;

   public static void init(WeatherHandler weatherHandler) {
      simplexNoise = new SimplexNoise(new LegacyRandomSource(weatherHandler.seed));
   }

   public static double FBM(Vec3 pos, int octaves, float lacunarity, float gain, float amplitude) {
      double y = 0.0;
      if (simplexNoise != null) {
         for (int i = 0; i < Math.max(octaves, 1); i++) {
            y += amplitude * simplexNoise.getValue(pos.x, pos.y, pos.z);
            pos = pos.multiply(lacunarity, lacunarity, lacunarity);
            amplitude *= gain;
         }
      }

      return y;
   }

   public static float getSwirl(Vec3 position, Level level, float sampleSize) {
      Vec3 sample1Z = getWind(position.add(0.0, 0.0, sampleSize), level).normalize();
      Vec3 sample2Z = getWind(position.add(0.0, 0.0, -sampleSize), level).normalize();
      Vec3 sample1X = getWind(position.add(-sampleSize, 0.0, 0.0), level).normalize();
      Vec3 sample2X = getWind(position.add(sampleSize, 0.0, 0.0), level).normalize();
      double compZ = (-sample1Z.dot(sample2Z) + 1.0) / 2.0;
      double compX = (-sample1X.dot(sample2X) + 1.0) / 2.0;
      return (float)(compZ * compX);
   }

   public static Vec3 getWind(Vec3 position, Level level) {
      return getWind(position, level, false, false, true, false);
   }

   public static Vec3 getWind(Vec3 position, Level level, boolean ignoreStorms, boolean ignoreTornadoes, boolean windCheck) {
      return getWind(position, level, ignoreStorms, ignoreTornadoes, windCheck, false);
   }

   public static Vec3 getWind(Vec3 position, Level level, boolean ignoreStorms, boolean ignoreTornadoes, boolean windCheck, boolean windAnyway) {
      Vec3 wind = Vec3.ZERO;
      Vec3 rawWind = Vec3.ZERO;
      BlockPos blockPos = new BlockPos((int)position.x, (int)position.y, (int)position.z);
      List<Storm> tornadicStorms = new ArrayList<>();
      if (level == null) {
         PMWeather.LOGGER.warn("Level is null");
         return wind;
      } else {
         int worldHeight = level.getHeightmapPos(Types.MOTION_BLOCKING, blockPos).getY();
         if (windCheck && !windAnyway) {
            if (!Util.canWindAffect(position, level)) {
               return wind;
            }
         } else if (!windAnyway && position.y < worldHeight) {
            return wind;
         }

         if (simplexNoise != null) {
            float timeScale = 20000.0F;
            float scale = 12000.0F;
            double ang = FBM(
               new Vec3(position.x / (scale * 3.0F), position.z / (scale * 3.0F), (float)level.getGameTime() / (timeScale * 6.0F)), 5, 2.0F, 0.1F, 1.0F
            );
            ang *= Math.PI;
            Vec3 dir = new Vec3(Math.cos(ang), 0.0, Math.sin(ang)).normalize();
            double speed = Math.max(simplexNoise.getValue(-position.z / scale, -position.x / scale, -((float)level.getGameTime()) / timeScale) + 1.0, 0.0)
               * 10.0;
            wind = wind.add(dir.multiply(speed, speed, speed));
            WeatherHandler weatherHandler;
            if (level.isClientSide()) {
               weatherHandler = GameBusClientEvents.weatherHandler;
            } else {
               weatherHandler = GameBusEvents.MANAGERS.get(level.dimension());
            }

            if (weatherHandler != null && !ignoreStorms) {
               for (Storm storm : weatherHandler.getStorms()) {
                  if (!storm.visualOnly) {
                     if (storm.stage >= 3 && storm.stormType == 0) {
                        tornadicStorms.add(storm);
                     }

                     double distance = position.multiply(1.0, 0.0, 1.0).distanceTo(storm.position.multiply(1.0, 0.0, 1.0));
                     if (storm.stormType == 2) {
                        Vec3 relativePos = position.subtract(storm.position);
                        Vec3 inward = new Vec3(-relativePos.x, 0.0, -relativePos.z).normalize();
                        Vec3 rotational = new Vec3(relativePos.z, 0.0, -relativePos.x).normalize();
                        double pullStrngth = storm.windspeed * 0.3F;
                        double rotStrngth = storm.windspeed * 0.7F;
                        float mult = (float)Math.pow(1.0 - Math.clamp(distance / storm.maxWidth, 0.0, 1.0), 3.0);
                        if (distance < storm.maxWidth * 0.1F) {
                           mult += (float)Math.pow(1.0 - Math.clamp(distance / (storm.maxWidth * 0.1F), 0.0, 1.0), 0.75) * 0.15F;
                           mult = Math.clamp(mult, 0.0F, 1.0F);
                        }

                        double d = storm.maxWidth / (1.5F + storm.windspeed / 30.0F);
                        float effect = Math.clamp((float)distance / storm.maxWidth, 0.0F, 1.0F);
                        float noiseX = (float)FBM(
                           new Vec3(position.x / (storm.maxWidth * 0.5F), position.z / (storm.maxWidth * 0.5F), (float)level.getGameTime() / timeScale),
                           5,
                           2.0F,
                           0.2F,
                           1.0F
                        );
                        float noiseZ = (float)FBM(
                           new Vec3(position.z / (storm.maxWidth * 0.5F), position.x / (storm.maxWidth * 0.5F), (float)level.getGameTime() / timeScale),
                           5,
                           2.0F,
                           0.2F,
                           1.0F
                        );
                        relativePos = relativePos.add(new Vec3(noiseX * storm.maxWidth * 0.3F * effect, 0.0, noiseZ * storm.maxWidth * 0.3F * effect));
                        double angle = Math.atan2(relativePos.z, relativePos.x) - distance / d;
                        float bands = (float)Math.sin((angle + Math.toRadians(storm.tickCount / 8.0F)) * 4.0);
                        mult += Mth.lerp(
                           1.0F - Math.clamp((float)distance / (storm.maxWidth * 0.35F), 0.0F, 1.0F),
                           (float)Math.pow(Math.abs(bands), 2.0) * 0.5F * mult,
                           0.5F * mult
                        );
                        float noise = (float)FBM(
                           new Vec3(position.x / (storm.maxWidth * 0.5F), position.z / (storm.maxWidth * 0.5F), (float)level.getGameTime() / timeScale),
                           5,
                           2.0F,
                           0.2F,
                           1.0F
                        );
                        mult *= Math.clamp(noise, 0.0F, 1.0F) * 0.2F + 0.8F;
                        float noise2 = (float)FBM(
                           new Vec3(position.x / (storm.maxWidth * 0.1F), position.z / (storm.maxWidth * 0.1F), (float)level.getGameTime() / timeScale),
                           5,
                           2.0F,
                           0.2F,
                           1.0F
                        );
                        mult *= Math.clamp(noise2, 0.0F, 1.0F) * 0.1F + 0.9F;
                        mult *= 1.15F + (float)Math.pow(1.0 - Math.clamp((distance - storm.maxWidth * 0.1F) / (storm.maxWidth * 0.1F), 0.0, 1.0), 2.5) * 0.35F;
                        float eye = (float)Math.pow(
                           Math.clamp(distance / (storm.maxWidth * 0.1F), 0.0, 1.0), Mth.lerp(Math.clamp(storm.windspeed / 120.0F, 0.0F, 1.0F), 0.5F, 4.0F)
                        );
                        mult *= Mth.lerp((float)Math.pow(Math.clamp(storm.windspeed / 65.0F, 0.0F, 1.0F), 2.0), 1.0F, eye);
                        Vec3 vec = inward.multiply(pullStrngth, 0.0, pullStrngth)
                           .add(rotational.multiply(rotStrngth, 0.0, rotStrngth))
                           .multiply(mult, 0.0, mult);
                        if (vec.length() > storm.windspeed) {
                           double dif = vec.length() - storm.windspeed;
                           vec = vec.subtract(new Vec3(dif, 0.0, dif));
                        }

                        rawWind = rawWind.add(vec);

                        for (Vorticy vorticy : storm.vorticies) {
                           Vec3 pos = vorticy.getPosition();
                           Vec3 rPos = position.subtract(pos);
                           Vec3 in = new Vec3(-rPos.x, 0.0, -rPos.z).normalize();
                           Vec3 rot = new Vec3(rPos.z, 0.0, -rPos.x).normalize();
                           float width = vorticy.getWidth();
                           double dist = position.multiply(1.0, 0.0, 1.0).distanceTo(pos.multiply(1.0, 0.0, 1.0));
                           double pullStrn = vorticy.windspeedMult * storm.windspeed * 0.3F;
                           double rotStrn = vorticy.windspeedMult * storm.windspeed * 0.7F;
                           float m = (float)Math.pow(1.0F - Math.clamp((float)dist / width, 0.0F, 1.0F), 3.75);
                           m *= Math.clamp((float)dist / (width * 0.1F), 0.0F, 1.0F);
                           m *= 7.0F;
                           Vec3 v = in.multiply(pullStrn, 0.0, pullStrn).add(rot.multiply(rotStrn, 0.0, rotStrn)).multiply(m, 0.0, m);
                           rawWind = rawWind.add(v);
                        }
                     }

                     if (storm.stormType == 1) {
                        Vec2 v2fWorldPos = new Vec2((float)position.x, (float)position.z);
                        Vec2 stormVel = new Vec2((float)storm.velocity.x, (float)storm.velocity.z);
                        Vec2 v2fStormPos = new Vec2((float)storm.position.x, (float)storm.position.z);
                        Vec2 right = new Vec2(stormVel.y, -stormVel.x).normalized();
                        Vec2 fwd = stormVel.normalized();
                        Vec2 le = Util.mulVec2(right, -((float)ServerConfig.stormSize) * 5.0F);
                        Vec2 ri = Util.mulVec2(right, (float)ServerConfig.stormSize * 5.0F);
                        Vec2 off = Util.mulVec2(
                           fwd,
                           -((float)Math.pow(Mth.clamp(distance / ((float)ServerConfig.stormSize * 5.0F), 0.0, 1.0), 2.0))
                              * ((float)ServerConfig.stormSize * 1.5F)
                        );
                        le = le.add(off);
                        ri = ri.add(off);
                        le = le.add(v2fStormPos);
                        ri = ri.add(v2fStormPos);
                        float d = Util.minimumDistance(le, ri, v2fWorldPos);
                        Vec2 nearPoint = Util.nearestPoint(le, ri, v2fWorldPos);
                        Vec2 facing = v2fWorldPos.add(nearPoint.negated());
                        float behind = -facing.dot(fwd);
                        behind += (float)FBM(
                              new Vec3(
                                 position.x / (ServerConfig.stormSize * 2.0),
                                 position.z / (ServerConfig.stormSize * 2.0),
                                 (float)level.getGameTime() / timeScale
                              ),
                              5,
                              2.0F,
                              0.2F,
                              1.0F
                           )
                           * (float)ServerConfig.stormSize
                           * 0.25F;
                        float perc = 0.0F;
                        float sze = (float)ServerConfig.stormSize * 4.0F;
                        behind += (float)ServerConfig.stormSize;
                        if (behind > 0.0F) {
                           float p = Mth.clamp(Math.abs(behind) / sze, 0.0F, 1.0F);
                           float start = 0.06F;
                           if (storm.stage >= 3) {
                              start = Mth.lerp(storm.energy / 100.0F, start, start * 2.5F);
                           }

                           if (p <= start) {
                              p /= start;
                           } else {
                              p = 1.0F - (p - start) / (1.0F - start);
                              if (storm.stage >= 3) {
                                 p = (float)Math.pow(p, Mth.lerp(storm.energy / 100.0F, 1.0F, 0.75F));
                              }
                           }

                           perc = Mth.clamp(p, 0.0F, 1.0F);
                        }

                        if (storm.stage < 1) {
                           perc *= storm.energy / 100.0F;
                        } else if (storm.stage == 1) {
                           perc *= storm.energy / 125.0F + 1.0F;
                        } else if (storm.stage == 2) {
                           perc *= storm.energy / 200.0F + 1.8F;
                        } else {
                           perc *= storm.energy / 100.0F + 2.3F;
                        }

                        float gustNoise = (float)FBM(
                           new Vec3(
                              position.z / (ServerConfig.stormSize * 2.0), position.x / (ServerConfig.stormSize * 2.0), (float)level.getGameTime() / timeScale
                           ),
                           7,
                           2.0F,
                           0.4F,
                           1.0F
                        );
                        if (storm.stage >= 3) {
                           float px = storm.energy / 100.0F;
                           gustNoise *= 1.0F - px;
                           perc *= 1.0F + px * 0.3F;
                        }

                        perc *= Mth.lerp(
                           Mth.clamp(behind / ((float)ServerConfig.stormSize * 3.0F), 0.0F, 1.0F), (float)Math.pow(0.8F + gustNoise * 0.5F, 1.5), 0.5F
                        );
                        perc *= Mth.sqrt(1.0F - Mth.clamp(d / sze, 0.0F, 1.0F));
                        wind = wind.add(
                           storm.velocity
                              .multiply(perc * 13.0F * ServerConfig.squallStrengthMultiplier, 0.0, perc * 13.0F * ServerConfig.squallStrengthMultiplier)
                        );
                     }

                     if (storm.stormType == 0) {
                        Vec3 relativePosx = position.subtract(storm.position);
                        Vec3 inwardx = new Vec3(-relativePosx.x, 0.0, -relativePosx.z).normalize();
                        Vec3 rotationalx = new Vec3(relativePosx.z, 0.0, -relativePosx.x).normalize();
                        double pullStrngthx = 1.0 - Math.clamp(distance / (ServerConfig.stormSize * 4.0), 0.0, 1.0);
                        double rotStrngthx = 1.0 - Math.clamp(distance / ServerConfig.stormSize, 0.0, 1.0);
                        if (storm.stage < 1) {
                           pullStrngthx *= 0.5;
                           pullStrngthx *= storm.energy / 100.0F;
                           rotStrngthx *= 0.0;
                        } else if (storm.stage == 1) {
                           pullStrngthx *= storm.energy / 200.0F + 0.5F;
                           rotStrngthx *= storm.energy / 100.0F * 0.1F;
                        } else if (storm.stage == 2) {
                           pullStrngthx *= 1.0F + storm.energy / 100.0F;
                           rotStrngthx *= 0.1F + storm.energy / 100.0F * 0.4F;
                        } else {
                           pullStrngthx *= 2.0F + storm.windspeed / 400.0F;
                           rotStrngthx *= 0.5F + storm.windspeed / 400.0F;
                        }

                        pullStrngthx *= 0.5;
                        rotStrngthx *= 6.0;
                        Vec3 vec = inwardx.multiply(pullStrngthx, 0.0, pullStrngthx)
                           .add(rotationalx.multiply(rotStrngthx, 0.0, rotStrngthx))
                           .multiply(20.0, 20.0, 20.0);
                        wind = wind.add(vec);
                     }
                  }
               }
            }
         }

         if (wind.length() > 30.0) {
            double over = wind.length() - 40.0;
            double val = 30.0 + over / 3.0;
            wind = wind.normalize().multiply(val, val, val);
         }

         if (blockPos.getY() > 85) {
            float val = Math.clamp((blockPos.getY() - 85) / 40.0F, 0.0F, 1.0F) / 3.0F + 1.0F;
            wind = wind.multiply(val, val, val);
         }

         wind = wind.add(rawWind);
         int heightAbove = blockPos.getY() - worldHeight;
         if (heightAbove > 0) {
            float val = Math.clamp(heightAbove / 15.0F, 0.0F, 1.0F) / 3.0F + 1.0F;
            wind = wind.multiply(val, val, val);
         }

         float tornadicEffect = 0.0F;
         Vec3 tornadicWind = Vec3.ZERO;
         if (!ignoreStorms && !ignoreTornadoes) {
            for (Storm tornadicStorm : tornadicStorms) {
               Vec3 relativePosx = position.subtract(tornadicStorm.position);
               Vec3 inwardx = new Vec3(-relativePosx.x, 0.0, -relativePosx.z).normalize();
               Vec3 rotationalx = new Vec3(relativePosx.z, 0.0, -relativePosx.x).normalize();
               double distancex = position.distanceTo(tornadicStorm.position);
               if (!(distancex > tornadicStorm.width * 2.0F)) {
                  double windEffect = tornadicStorm.getWind(position);
                  tornadicEffect = Math.clamp((float)windEffect / Math.max(tornadicStorm.windspeed, 30), tornadicEffect, 1.0F);
                  if (Float.isNaN(tornadicEffect)) {
                     tornadicEffect = 0.0F;
                  }

                  double inPerc = 0.35;
                  tornadicWind = tornadicWind.add(inwardx.multiply(windEffect * inPerc, windEffect * inPerc, windEffect * inPerc))
                     .add(rotationalx.add(windEffect * (1.0 - inPerc), windEffect * (1.0 - inPerc), windEffect * (1.0 - inPerc)));
               }
            }
         }

         return wind.lerp(tornadicWind, tornadicEffect);
      }
   }

   public static Vec3 getWind(BlockPos position, Level level, boolean ignoreStorms, boolean ignoreTornadoes, boolean windCheck) {
      return getWind(new Vec3(position.getX(), position.getY() + 1, position.getZ()), level, ignoreStorms, ignoreTornadoes, windCheck, false);
   }

   public static Vec3 getWind(BlockPos position, Level level) {
      return getWind(new Vec3(position.getX(), position.getY() + 1, position.getZ()), level, false, false, true, false);
   }
}
