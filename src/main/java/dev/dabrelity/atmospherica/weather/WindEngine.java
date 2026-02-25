package dev.dabrelity.atmospherica.weather;

import dev.dabrelity.atmospherica.Atmospherica;
import dev.dabrelity.atmospherica.config.ServerConfig;
import dev.dabrelity.atmospherica.event.GameBusClientEvents;
import dev.dabrelity.atmospherica.event.GameBusEvents;
import dev.dabrelity.atmospherica.util.Util;
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
      return FBM(pos.x, pos.y, pos.z, octaves, lacunarity, gain, amplitude);
   }

   public static double FBM(double x, double y, double z, int octaves, float lacunarity, float gain, float amplitude) {
      double val = 0.0;
      double px = x;
      double py = y;
      double pz = z;

      if (simplexNoise != null) {
         for (int i = 0; i < Math.max(octaves, 1); i++) {
            val += amplitude * simplexNoise.getValue(px, py, pz);
            px *= lacunarity;
            py *= lacunarity;
            pz *= lacunarity;
            amplitude *= gain;
         }
      }

      return val;
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
      double windX = 0.0;
      double windY = 0.0;
      double windZ = 0.0;
      double rawWindX = 0.0;
      double rawWindY = 0.0;
      double rawWindZ = 0.0;

      BlockPos blockPos = new BlockPos((int)position.x, (int)position.y, (int)position.z);
      List<Storm> tornadicStorms = new ArrayList<>();
      if (level == null) {
         Atmospherica.LOGGER.warn("Level is null");
         return Vec3.ZERO;
      } else {
         int worldHeight = level.getHeightmapPos(Types.MOTION_BLOCKING, blockPos).getY();
         if (windCheck && !windAnyway) {
            if (!Util.canWindAffect(position, level)) {
               return Vec3.ZERO;
            }
         } else if (!windAnyway && position.y < worldHeight) {
            return Vec3.ZERO;
         }

         if (simplexNoise != null) {
            float timeScale = 20000.0F;
            float scale = 12000.0F;
            double ang = FBM(
               position.x / (scale * 3.0F), position.z / (scale * 3.0F), (float)level.getGameTime() / (timeScale * 6.0F), 5, 2.0F, 0.1F, 1.0F
            );
            ang *= Math.PI;
            double dirX = Math.cos(ang);
            double dirZ = Math.sin(ang);

            double speed = Math.max(simplexNoise.getValue(-position.z / scale, -position.x / scale, -((float)level.getGameTime()) / timeScale) + 1.0, 0.0)
               * 10.0;

            windX += dirX * speed;
            windZ += dirZ * speed;

            WeatherHandler weatherHandler;
            if (level.isClientSide()) {
               weatherHandler = GameBusClientEvents.weatherHandler;
            } else {
               weatherHandler = (WeatherHandler)GameBusEvents.MANAGERS.get(level.dimension());
            }

            if (weatherHandler != null && !ignoreStorms) {
               for (Storm storm : weatherHandler.getStorms()) {
                  if (!storm.visualOnly) {
                     if (storm.stage >= 3 && storm.stormType == 0) {
                        tornadicStorms.add(storm);
                     }

                     double distanceSq = (position.x - storm.position.x)*(position.x - storm.position.x) + (position.z - storm.position.z)*(position.z - storm.position.z);
                     double distance = Math.sqrt(distanceSq);

                     if (storm.stormType == 2) {
                        double relX = position.x - storm.position.x;
                        double relY = position.y - storm.position.y;
                        double relZ = position.z - storm.position.z;

                        double invLen = Math.sqrt(relX * relX + relZ * relZ);
                        double inwardX = 0, inwardZ = 0;
                        if (invLen > 1.0E-4D) {
                           inwardX = -relX / invLen;
                           inwardZ = -relZ / invLen;
                        }

                        double rotX = 0, rotZ = 0;
                        if (invLen > 1.0E-4D) {
                           rotX = relZ / invLen;
                           rotZ = -relX / invLen;
                        }

                        double pullStrngth = storm.windspeed * 0.3F;
                        double rotStrngth = storm.windspeed * 0.7F;
                        float mult = (float)Math.pow(1.0 - Mth.clamp(distance / storm.maxWidth, 0.0, 1.0), 3.0);
                        if (distance < storm.maxWidth * 0.1F) {
                           mult += (float)Math.pow(1.0 - Mth.clamp(distance / (storm.maxWidth * 0.1F), 0.0, 1.0), 0.75) * 0.15F;
                           mult = Mth.clamp(mult, 0.0F, 1.0F);
                        }

                        double d = storm.maxWidth / (1.5F + storm.windspeed / 30.0F);
                        float effect = Mth.clamp((float)distance / storm.maxWidth, 0.0F, 1.0F);
                        float noiseX = (float)FBM(
                           position.x / (storm.maxWidth * 0.5F), position.z / (storm.maxWidth * 0.5F), (float)level.getGameTime() / timeScale,
                           5,
                           2.0F,
                           0.2F,
                           1.0F
                        );
                        float noiseZ = (float)FBM(
                           position.z / (storm.maxWidth * 0.5F), position.x / (storm.maxWidth * 0.5F), (float)level.getGameTime() / timeScale,
                           5,
                           2.0F,
                           0.2F,
                           1.0F
                        );

                        relX += noiseX * storm.maxWidth * 0.3F * effect;
                        relZ += noiseZ * storm.maxWidth * 0.3F * effect;

                        double angle = Math.atan2(relZ, relX) - distance / d;
                        float bands = (float)Math.sin((angle + Math.toRadians(storm.tickCount / 8.0F)) * 4.0);
                        mult += Mth.lerp(
                           1.0F - Mth.clamp((float)distance / (storm.maxWidth * 0.35F), 0.0F, 1.0F),
                           (float)Math.pow(Math.abs(bands), 2.0) * 0.5F * mult,
                           0.5F * mult
                        );
                        float noise = (float)FBM(
                           position.x / (storm.maxWidth * 0.5F), position.z / (storm.maxWidth * 0.5F), (float)level.getGameTime() / timeScale,
                           5,
                           2.0F,
                           0.2F,
                           1.0F
                        );
                        mult *= Mth.clamp(noise, 0.0F, 1.0F) * 0.2F + 0.8F;
                        float noise2 = (float)FBM(
                           position.x / (storm.maxWidth * 0.1F), position.z / (storm.maxWidth * 0.1F), (float)level.getGameTime() / timeScale,
                           5,
                           2.0F,
                           0.2F,
                           1.0F
                        );
                        mult *= Mth.clamp(noise2, 0.0F, 1.0F) * 0.1F + 0.9F;
                        mult *= 1.15F + (float)Math.pow(1.0 - Mth.clamp((distance - storm.maxWidth * 0.1F) / (storm.maxWidth * 0.1F), 0.0, 1.0), 2.5) * 0.35F;
                        float eye = (float)Math.pow(
                           Mth.clamp(distance / (storm.maxWidth * 0.1F), 0.0, 1.0), Mth.lerp(Mth.clamp(storm.windspeed / 120.0F, 0.0F, 1.0F), 0.5F, 4.0F)
                        );
                        mult *= Mth.lerp((float)Math.pow(Mth.clamp(storm.windspeed / 65.0F, 0.0F, 1.0F), 2.0), 1.0F, eye);

                        double vecX = (inwardX * pullStrngth + rotX * rotStrngth) * mult;
                        double vecY = 0.0;
                        double vecZ = (inwardZ * pullStrngth + rotZ * rotStrngth) * mult;

                        double vecLen = Math.sqrt(vecX*vecX + vecZ*vecZ);
                        if (vecLen > storm.windspeed) {
                           double dif = vecLen - storm.windspeed;
                           vecX -= dif;
                           vecZ -= dif;
                        }

                        rawWindX += vecX;
                        rawWindZ += vecZ;

                        for (Vorticy vorticy : storm.vorticies) {
                           Vec3 pos = vorticy.getPosition();
                           double rPosX = position.x - pos.x;
                           double rPosY = position.y - pos.y;
                           double rPosZ = position.z - pos.z;

                           double inLenV = Math.sqrt(rPosX*rPosX + rPosZ*rPosZ);
                           double inX = 0, inZ = 0;
                           if (inLenV > 1.0E-4D) {
                              inX = -rPosX / inLenV;
                              inZ = -rPosZ / inLenV;
                           }

                           double roX = 0, roZ = 0;
                           if (inLenV > 1.0E-4D) {
                              roX = rPosZ / inLenV;
                              roZ = -rPosX / inLenV;
                           }

                           float width = vorticy.getWidth();
                           double distV = Math.sqrt(rPosX*rPosX + rPosZ*rPosZ);
                           double pullStrn = vorticy.windspeedMult * storm.windspeed * 0.3F;
                           double rotStrn = vorticy.windspeedMult * storm.windspeed * 0.7F;
                           float m = (float)Math.pow(1.0F - Mth.clamp((float)distV / width, 0.0F, 1.0F), 3.75);
                           m *= Mth.clamp((float)distV / (width * 0.1F), 0.0F, 1.0F);
                           m *= 7.0F;

                           double vX = (inX * pullStrn + roX * rotStrn) * m;
                           double vZ = (inZ * pullStrn + roZ * rotStrn) * m;

                           rawWindX += vX;
                           rawWindZ += vZ;
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
                              position.x / (ServerConfig.stormSize * 2.0),
                              position.z / (ServerConfig.stormSize * 2.0),
                              (float)level.getGameTime() / timeScale,
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
                           position.z / (ServerConfig.stormSize * 2.0),
                           position.x / (ServerConfig.stormSize * 2.0),
                           (float)level.getGameTime() / timeScale,
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

                        double sMult = perc * 13.0F * ServerConfig.squallStrengthMultiplier;
                        windX += storm.velocity.x * sMult;
                        windZ += storm.velocity.z * sMult;
                     }

                     if (storm.stormType == 0) {
                        double rPx = position.x - storm.position.x;
                        double rPz = position.z - storm.position.z;

                        double lenIn = Math.sqrt(rPx*rPx + rPz*rPz);
                        double inX = 0, inZ = 0;
                        if (lenIn > 1.0E-4D) {
                           inX = -rPx / lenIn;
                           inZ = -rPz / lenIn;
                        }

                        double rotX = 0, rotZ = 0;
                        if (lenIn > 1.0E-4D) {
                           rotX = rPz / lenIn;
                           rotZ = -rPx / lenIn;
                        }

                        double pullStrngthx = 1.0 - Mth.clamp(distance / (ServerConfig.stormSize * 4.0), 0.0, 1.0);
                        double rotStrngthx = 1.0 - Mth.clamp(distance / ServerConfig.stormSize, 0.0, 1.0);
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

                        double vX = (inX * pullStrngthx + rotX * rotStrngthx) * 20.0;
                        double vZ = (inZ * pullStrngthx + rotZ * rotStrngthx) * 20.0;

                        windX += vX;
                        windZ += vZ;
                     }
                  }
               }
            }
         }

         double wLen = Math.sqrt(windX*windX + windY*windY + windZ*windZ);
         if (wLen > 30.0) {
            double over = wLen - 40.0;
            double val = 30.0 + over / 3.0;
            if (wLen > 1.0E-4D) {
               double scaleFactor = val / wLen;
               windX *= scaleFactor;
               windY *= scaleFactor;
               windZ *= scaleFactor;
            }
         }

         if (blockPos.getY() > 85) {
            float val = Mth.clamp((blockPos.getY() - 85) / 40.0F, 0.0F, 1.0F) / 3.0F + 1.0F;
            windX *= val;
            windY *= val;
            windZ *= val;
         }

         windX += rawWindX;
         windY += rawWindY;
         windZ += rawWindZ;

         int heightAbove = blockPos.getY() - worldHeight;
         if (heightAbove > 0) {
            float val = Mth.clamp(heightAbove / 15.0F, 0.0F, 1.0F) / 3.0F + 1.0F;
            windX *= val;
            windY *= val;
            windZ *= val;
         }

         float tornadicEffect = 0.0F;
         double tWindX = 0, tWindY = 0, tWindZ = 0;

         if (!ignoreStorms && !ignoreTornadoes) {
            for (Storm tornadicStorm : tornadicStorms) {
               double rPx = position.x - tornadicStorm.position.x;
               double rPz = position.z - tornadicStorm.position.z;

               double len = Math.sqrt(rPx*rPx + rPz*rPz);
               double inX = 0, inZ = 0;
               if (len > 1.0E-4D) {
                  inX = -rPx / len;
                  inZ = -rPz / len;
               }

               double rotX = 0, rotZ = 0;
               if (len > 1.0E-4D) {
                  rotX = rPz / len;
                  rotZ = -rPx / len;
               }

               double rPy = position.y - tornadicStorm.position.y;
               double distancex = Math.sqrt(rPx*rPx + rPy*rPy + rPz*rPz);

               if (!(distancex > tornadicStorm.width * 2.0F)) {
                  double windEffect = tornadicStorm.getWind(position);
                  tornadicEffect = Mth.clamp((float)windEffect / Math.max(tornadicStorm.windspeed, 30), tornadicEffect, 1.0F);
                  if (Float.isNaN(tornadicEffect)) {
                     tornadicEffect = 0.0F;
                  }

                  double inPerc = 0.35;
                  double val = windEffect * (1.0 - inPerc);

                  double term2X = rotX + val;
                  double term2Y = 0.0 + val;
                  double term2Z = rotZ + val;

                  double term1X = inX * (windEffect * inPerc);
                  double term1Y = 0.0 * (windEffect * inPerc);
                  double term1Z = inZ * (windEffect * inPerc);

                  tWindX += term1X + term2X;
                  tWindY += term1Y + term2Y;
                  tWindZ += term1Z + term2Z;
               }
            }
         }

         double finalX = windX + (tWindX - windX) * tornadicEffect;
         double finalY = windY + (tWindY - windY) * tornadicEffect;
         double finalZ = windZ + (tWindZ - windZ) * tornadicEffect;

         return new Vec3(finalX, finalY, finalZ);
      }
   }

   public static Vec3 getWind(BlockPos position, Level level, boolean ignoreStorms, boolean ignoreTornadoes, boolean windCheck) {
      return getWind(new Vec3(position.getX(), position.getY() + 1, position.getZ()), level, ignoreStorms, ignoreTornadoes, windCheck, false);
   }

   public static Vec3 getWind(BlockPos position, Level level) {
      return getWind(new Vec3(position.getX(), position.getY() + 1, position.getZ()), level, false, false, true, false);
   }
}
