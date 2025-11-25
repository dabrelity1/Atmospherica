package dev.dabrelity.atmospherica.weather;

import dev.dabrelity.atmospherica.Atmospherica;
import dev.dabrelity.atmospherica.config.ServerConfig;
import dev.dabrelity.atmospherica.data.LevelSavedData;
import dev.dabrelity.atmospherica.interfaces.IWorldData;
import dev.dabrelity.atmospherica.util.Util;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

public abstract class WeatherHandler implements IWorldData {
   private List<Storm> storms = new ArrayList();
   private ResourceKey<Level> dimension;
   public HashMap<Long, Storm> lookupStormByID = new HashMap();
   public long seed;

   public WeatherHandler(ResourceKey<Level> dimension) {
      this.dimension = dimension;
   }

   public void tick() {
      Level level = this.getWorld();
      if (level != null) {
         List<Storm> stormList = this.getStorms();

         for (int i = 0; i < stormList.size(); i++) {
            Storm storm = (Storm)stormList.get(i);
            if (this instanceof WeatherHandlerServer weatherHandlerServer && storm.dead) {
               this.removeStorm(storm.ID);
               weatherHandlerServer.syncStormRemove(storm);
            } else if (!storm.dead) {
               storm.tick();
            } else {
               this.removeStorm(storm.ID);
            }
         }
      }
   }

   public List<Storm> getStorms() {
      return this.storms;
   }

   public void addStorm(Storm storm) {
      if (!this.lookupStormByID.containsKey(storm.ID)) {
         this.storms.add(storm);
         this.lookupStormByID.put(storm.ID, storm);
      } else {
         Atmospherica.LOGGER.warn("Tried to add a storm with existing ID: {}", storm.ID);
      }
   }

   public void removeStorm(long id) {
      Storm storm = (Storm)this.lookupStormByID.get(id);
      if (storm != null) {
         storm.remove();
         this.storms.remove(storm);
         this.lookupStormByID.remove(id);
      } else {
         Atmospherica.LOGGER.warn("Tried to remove a non-existent storm with ID: {}", id);
      }
   }

   public float getPrecipitation(Vec3 pos) {
      float precip = 0.0F;
      float cloudDensity = Clouds.getCloudDensity(this, new Vector2f((float)pos.x, (float)pos.z), 0.0F);
      if (cloudDensity > 0.15F) {
         precip += (cloudDensity - 0.15F) * 2.0F;
      }

      for (Storm storm : this.getStorms()) {
         if (!storm.visualOnly) {
            double dist = pos.distanceTo(new Vec3(storm.position.x, pos.y, storm.position.z));
            double perc = 0.0;
            float smoothStage = storm.stage + storm.energy / 100.0F;
            if (storm.stage == 3) {
               smoothStage = 3.0F;
            }

            if (storm.stormType == 2) {
               Vec3 cPos = storm.position.multiply(1.0, 0.0, 1.0);
               float intensity = (float)Math.pow(Mth.clamp(storm.windspeed / 65.0F, 0.0F, 1.0F), 0.85F);
               Vec3 relPos = cPos.subtract(pos);
               double d = storm.maxWidth / (3.0F + storm.windspeed / 12.0F);
               double d2 = storm.maxWidth / (1.15F + storm.windspeed / 12.0F);
               double dE = storm.maxWidth * 0.65F / (1.75F + storm.windspeed / 12.0F);
               double fac = 1.0 + Math.max((dist - storm.maxWidth * 0.2F) / storm.maxWidth, 0.0) * 2.0;
               d *= fac;
               d2 *= fac;
               double angle = Math.atan2(relPos.z, relPos.x) - dist / d;
               double angle2 = Math.atan2(relPos.z, relPos.x) - dist / d2;
               double angleE = Math.atan2(relPos.z, relPos.x) - dist / dE;
               float weak = 0.0F;
               float strong = 0.0F;
               float intense = 0.0F;
               float staticBands = (float)Math.sin(angle - (Math.PI / 2));
               staticBands *= (float)Math.pow(Mth.clamp(dist / (storm.maxWidth * 0.25F), 0.0, 1.0), 0.1F);
               staticBands *= 1.25F * (float)Math.pow(intensity, 0.75);
               if (staticBands < 0.0F) {
                  weak += Math.abs(staticBands);
               } else {
                  weak += Math.abs(staticBands) * (float)Math.pow(1.0 - Mth.clamp(dist / (storm.maxWidth * 0.65F), 0.0, 1.0), 0.5);
                  weak *= Mth.clamp((storm.windspeed - 70.0F) / 40.0F, 0.0F, 1.0F);
               }

               float rotatingBands = (float)Math.sin((angle2 + Math.toRadians(storm.tickCount / 8.0F)) * 6.0);
               rotatingBands *= (float)Math.pow(Mth.clamp(dist / (storm.maxWidth * 0.25F), 0.0, 1.0), 0.1F);
               rotatingBands *= 1.25F * (float)Math.pow(intensity, 0.75);
               strong += Mth.lerp(0.45F, Math.abs(rotatingBands) * 0.3F + 0.7F, weak);
               intense += Mth.lerp(0.3F, Math.abs(rotatingBands) * 0.2F + 0.8F, weak);
               weak = (Math.abs(rotatingBands) * 0.3F + 0.6F) * weak;
               float localRain = 0.0F;
               localRain += Mth.lerp(
                  Mth.clamp((storm.windspeed - 120.0F) / 60.0F, 0.0F, 1.0F),
                  Mth.lerp(Mth.clamp((storm.windspeed - 40.0F) / 90.0F, 0.0F, 1.0F), weak, strong),
                  intense
               );
               float eye = (float)Math.sin((angleE + Math.toRadians(storm.tickCount / 4.0F)) * 2.0);
               float efc = Mth.lerp(Mth.clamp((storm.windspeed - 100.0F) / 50.0F, 0.0F, 1.0F), 0.15F, 0.4F);
               localRain = Math.max(
                  (float)Math.pow(1.0 - Mth.clamp(dist / (storm.maxWidth * efc), 0.0, 1.0), 0.5) * (Math.abs(eye * 0.1F) + 0.9F) * 1.35F * intensity,
                  localRain
               );
               localRain *= (float)Math.pow(1.0 - Mth.clamp(dist / storm.maxWidth, 0.0, 1.0), 0.5);
               localRain *= Mth.lerp(
                  0.5F + Mth.clamp((storm.windspeed - 65.0F) / 40.0F, 0.0F, 1.0F) * 0.5F,
                  1.0F,
                  (float)Math.pow(Mth.clamp(dist / (storm.maxWidth * 0.1F), 0.0, 1.0), 2.0)
               );
               if (localRain > 0.6F) {
                  float dif = (localRain - 0.6F) / 2.5F;
                  localRain -= dif;
               }

               precip += Math.max(localRain - 0.15F, 0.0F) * 2.0F;
            }

            if (storm.stormType == 1) {
               Vec2 v2fWorldPos = new Vec2((float)pos.x, (float)pos.z);
               Vec2 stormVel = new Vec2((float)storm.velocity.x, (float)storm.velocity.z);
               Vec2 v2fStormPos = new Vec2((float)storm.position.x, (float)storm.position.z);
               Vec2 right = new Vec2(stormVel.y, -stormVel.x).normalized();
               Vec2 fwd = stormVel.normalized();
               Vec2 le = Util.mulVec2(right, -((float)ServerConfig.stormSize) * 5.0F);
               Vec2 ri = Util.mulVec2(right, (float)ServerConfig.stormSize * 5.0F);
               Vec2 off = Util.mulVec2(
                  fwd, -((float)Math.pow(Mth.clamp(dist / ((float)ServerConfig.stormSize * 5.0F), 0.0, 1.0), 2.0)) * ((float)ServerConfig.stormSize * 1.5F)
               );
               le = le.add(off);
               ri = ri.add(off);
               le = le.add(v2fStormPos);
               ri = ri.add(v2fStormPos);
               float dx = Util.minimumDistance(le, ri, v2fWorldPos);
               if (dx > ServerConfig.stormSize * 16.0) {
                  continue;
               }

               Vec2 nearPoint = Util.nearestPoint(le, ri, v2fWorldPos);
               Vec2 facing = v2fWorldPos.add(nearPoint.negated());
               float behind = -facing.dot(fwd);
               float sze = (float)ServerConfig.stormSize * 1.5F;
               sze *= Mth.lerp(Mth.clamp(smoothStage - 1.0F, 0.0F, 1.0F), 4.0F, 12.0F);
               behind += (float)ServerConfig.stormSize / 2.0F;
               if (behind > 0.0F) {
                  float p = Mth.clamp(Math.abs(behind) / sze, 0.0F, 1.0F);
                  float start = 0.06F;
                  if (p <= start) {
                     p /= start;
                  } else {
                     p = 1.0F - (p - start) / (1.0F - start);
                  }

                  perc = (float)Math.pow(Mth.clamp(p, 0.0F, 1.0F), 3.0);
               }

               if (storm.stage <= 0) {
                  perc = 0.0;
               } else if (storm.stage == 1) {
                  perc *= storm.energy / 100.0F;
               }

               perc *= Mth.sqrt(1.0F - Mth.clamp(dx / sze, 0.0F, 1.0F));
            }

            if (storm.stormType == 0) {
               double coreDist = pos.distanceTo(new Vec3(storm.position.x + 2000.0, pos.y, storm.position.z - 900.0));
               if (Math.min(dist, coreDist) > ServerConfig.stormSize * 6.0) {
                  continue;
               }

               perc = 1.0 - Mth.clamp(dist / ServerConfig.stormSize, 0.0, 1.0);
               if (storm.stage == 0) {
                  perc *= storm.energy / 100.0F;
               }

               if (storm.stage >= 2) {
                  perc *= Mth.lerp(Mth.clamp(smoothStage - 2.0F, 0.0F, 1.0F), 1.0F, storm.occlusion * 0.5F + 0.5F);
               }

               double p = 1.0 - Mth.clamp(coreDist / (ServerConfig.stormSize * 6.0), 0.0, 1.0);
               if (storm.stage <= 1) {
                  p *= 0.0;
               }

               if (storm.stage >= 2) {
                  p *= Mth.clamp((smoothStage - 2.0F) / 0.5F, 0.0F, 1.0F);
               }

               perc = Math.max(p, perc);
            }

            precip += (float)perc;
         }
      }

      return Mth.clamp(precip * (float)ServerConfig.rainStrength, 0.0F, 1.0F);
   }

   public abstract Level getWorld();

   @Override
   public CompoundTag save(CompoundTag data) {
      Atmospherica.LOGGER.debug("WeatherHandler save");
      CompoundTag listStormsNBT = new CompoundTag();

      for (int i = 0; i < this.storms.size(); i++) {
         Storm storm = (Storm)this.storms.get(i);
         storm.getNBTCache().setUpdateForced(true);
         storm.write();
         storm.getNBTCache().setUpdateForced(false);
         listStormsNBT.put("storm_" + storm.ID, storm.getNBTCache().getNewNBT());
      }

      data.put("stormData", listStormsNBT);
      data.putLong("lastUsedIDStorm", Storm.LastUsedStormID);
      return null;
   }

   public void read() {
      LevelSavedData savedData = ((ServerLevel)this.getWorld())
         .getDataStorage()
         .computeIfAbsent(LevelSavedData::load, LevelSavedData::new, "Atmospherica_weather_data");
      savedData.setDataHandler(this);
      Atmospherica.LOGGER.debug("Weather Data: {}", savedData.getData());
      CompoundTag data = savedData.getData();
      Storm.LastUsedStormID = data.getLong("lastUsedIDStorm");
      CompoundTag storms = data.getCompound("stormData");

      for (String tagName : storms.getAllKeys()) {
         CompoundTag stormData = storms.getCompound(tagName);
         Storm storm = new Storm(this, this.getWorld(), null, stormData.getInt("stormType"));

         try {
            storm.getNBTCache().setNewNBT(stormData);
            storm.read();
            storm.getNBTCache().updateCacheFromNew();
         } catch (Exception var9) {
            Atmospherica.LOGGER.error(var9.getMessage(), var9);
         }

         this.addStorm(storm);
      }
   }
}
