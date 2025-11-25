package dev.protomanly.pmweather.weather;

import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.config.ClientConfig;
import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.event.GameBusClientEvents;
import dev.protomanly.pmweather.particle.ParticleCube;
import dev.protomanly.pmweather.sound.ModSounds;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class WeatherHandlerClient extends WeatherHandler {
   public List<Lightning> lightnings = new ArrayList<>();

   public WeatherHandlerClient(ResourceKey<Level> dimension) {
      super(dimension);
   }

   @Override
   public Level getWorld() {
      return Minecraft.getInstance().level;
   }

   public float getHail() {
      Player player = Minecraft.getInstance().player;
      if (player == null) {
         return 0.0F;
      } else {
         float precip = 0.0F;

         for (Storm storm : this.getStorms()) {
            if (!storm.visualOnly) {
               double dist = player.position().distanceTo(new Vec3(storm.position.x + 2000.0, player.position().y, storm.position.z - 900.0));
               if (!(dist > ServerConfig.stormSize * 4.0)) {
                  double perc = 0.0;
                  if (storm.stormType == 0) {
                     perc = 1.0 - Math.clamp(dist / (ServerConfig.stormSize * 6.0), 0.0, 1.0);
                     if (storm.stage == 2) {
                        perc *= storm.energy / 100.0F;
                     }

                     if (storm.stage > 2) {
                        perc *= 1.0;
                     }

                     if (storm.stage < 2) {
                        perc *= 0.0;
                     }
                  }

                  precip += (float)perc;
               }
            }
         }

         return Math.clamp(precip, 0.0F, 1.0F);
      }
   }

   public float getPrecipitation() {
      Player player = Minecraft.getInstance().player;
      return player == null ? 0.0F : this.getPrecipitation(player.position());
   }

   public void strike(Vec3 pos) {
      Lightning lightning = new Lightning(pos, this.getWorld());
      this.lightnings.add(lightning);
      Player player = Minecraft.getInstance().player;
      if (player != null) {
         double dist = player.position().multiply(1.0, 0.0, 1.0).distanceTo(pos.multiply(1.0, 0.0, 1.0));
         if (dist > 256.0) {
            this.getWorld()
               .playLocalSound(
                  pos.x, pos.y, pos.z, (SoundEvent)ModSounds.THUNDER_FAR.value(), SoundSource.WEATHER, 5000.0F, PMWeather.RANDOM.nextFloat(0.8F, 1.0F), true
               );
         } else {
            this.getWorld()
               .playLocalSound(
                  pos.x, pos.y, pos.z, (SoundEvent)ModSounds.THUNDER_NEAR.value(), SoundSource.WEATHER, 5000.0F, PMWeather.RANDOM.nextFloat(0.8F, 1.0F), true
               );
         }
      }
   }

   @Override
   public void tick() {
      super.tick();
      Iterator<Lightning> iterator = this.lightnings.iterator();

      while (iterator.hasNext()) {
         Lightning lightning = iterator.next();
         if (!lightning.dead && lightning.level == this.getWorld()) {
            lightning.tick();
         } else {
            iterator.remove();
         }
      }
   }

   public void nbtSyncFromServer(CompoundTag compoundTag) {
      String command = compoundTag.getString("command");
      if (command.equals("syncStormNew")) {
         CompoundTag stormCompoundTag = compoundTag.getCompound("data");
         long ID = stormCompoundTag.getLong("ID");
         PMWeather.LOGGER.debug("syncStormNew, ID: {}", ID);
         Storm storm = new Storm(this, this.getWorld(), null, stormCompoundTag.getInt("stormType"));
         storm.getNBTCache().setNewNBT(stormCompoundTag);
         storm.nbtSyncFromServer();
         storm.getNBTCache().updateCacheFromNew();
         this.addStorm(storm);
      } else if (command.equals("syncStormRemove")) {
         CompoundTag stormCompoundTag = compoundTag.getCompound("data");
         long ID = stormCompoundTag.getLong("ID");
         Storm storm = this.lookupStormByID.get(ID);
         if (storm != null) {
            this.removeStorm(ID);
         }
      } else if (command.equals("syncStormUpdate")) {
         CompoundTag stormCompoundTag = compoundTag.getCompound("data");
         long ID = stormCompoundTag.getLong("ID");
         Storm storm = this.lookupStormByID.get(ID);
         if (storm != null) {
            storm.getNBTCache().setNewNBT(stormCompoundTag);
            storm.nbtSyncFromServer();
            storm.getNBTCache().updateCacheFromNew();
         }
      } else if (command.equals("syncBlockParticleNew")) {
         if (PMWeather.RANDOM.nextFloat() > ClientConfig.debrisParticleDensity) {
            return;
         }

         CompoundTag nbt = compoundTag.getCompound("data");
         Vec3 pos = new Vec3(nbt.getInt("positionX"), nbt.getInt("positionY") + 1, nbt.getInt("positionZ"));
         BlockState state = NbtUtils.readBlockState(this.getWorld().holderLookup(Registries.BLOCK), nbt.getCompound("blockstate"));
         long stormID = nbt.getLong("stormID");
         Storm storm = this.lookupStormByID.get(stormID);
         if (storm != null) {
            ParticleCube debris = new ParticleCube(
               (ClientLevel)this.getWorld(),
               pos.x + (PMWeather.RANDOM.nextFloat() - PMWeather.RANDOM.nextFloat()) * 3.0F,
               pos.y + (PMWeather.RANDOM.nextFloat() - PMWeather.RANDOM.nextFloat()) * 3.0F,
               pos.z + (PMWeather.RANDOM.nextFloat() - PMWeather.RANDOM.nextFloat()) * 3.0F,
               0.0,
               0.0,
               0.0,
               state
            );
            GameBusClientEvents.particleBehavior.initParticleCube(debris);
            storm.listParticleDebris.add(debris);
            debris.ignoreWind = true;
            debris.renderRange = 256.0F;
            debris.spawnAsDebrisEffect();
         }
      } else if (command.equals("syncLightningNew")) {
         CompoundTag nbt = compoundTag.getCompound("data");
         this.strike(new Vec3(nbt.getDouble("positionX"), nbt.getDouble("positionY"), nbt.getDouble("positionZ")));
      }
   }
}
