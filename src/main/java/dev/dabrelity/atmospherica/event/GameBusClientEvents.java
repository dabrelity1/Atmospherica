package dev.dabrelity.atmospherica.event;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dabrelity.atmospherica.Atmospherica;
import dev.dabrelity.atmospherica.config.ClientConfig;
import dev.dabrelity.atmospherica.config.ServerConfig;
import dev.dabrelity.atmospherica.interfaces.ParticleData;
import dev.dabrelity.atmospherica.networking.ModNetworking;
import dev.dabrelity.atmospherica.particle.EntityRotFX;
import dev.dabrelity.atmospherica.particle.ParticleCube;
import dev.dabrelity.atmospherica.particle.ParticleManager;
import dev.dabrelity.atmospherica.particle.ParticleRegistry;
import dev.dabrelity.atmospherica.particle.ParticleTexExtraRender;
import dev.dabrelity.atmospherica.particle.ParticleTexFX;
import dev.dabrelity.atmospherica.particle.behavior.ParticleBehavior;
import dev.dabrelity.atmospherica.shaders.ModShaders;
import dev.dabrelity.atmospherica.sound.ModSounds;
import dev.dabrelity.atmospherica.util.ChunkCoordinatesBlock;
import dev.dabrelity.atmospherica.weather.ThermodynamicEngine;
import dev.dabrelity.atmospherica.weather.ThermodynamicEngine.Precipitation;
import dev.dabrelity.atmospherica.weather.WeatherHandler;
import dev.dabrelity.atmospherica.weather.WeatherHandlerClient;
import dev.dabrelity.atmospherica.weather.WindEngine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Atmospherica.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class GameBusClientEvents {
   public static Level lastLevel;
   public static WeatherHandler weatherHandler;
   public static ParticleManager particleManager;
   public static ParticleManager particleManagerDebris;
   public static ParticleBehavior particleBehavior = new ParticleBehavior(null);
   
   // Reusable MutableBlockPos for particle spawning - OPTIMIZATION
   private static final BlockPos.MutableBlockPos spawnPos = new BlockPos.MutableBlockPos();
   
   public static List<Block> LEAVES_BLOCKS = new ArrayList<>() {
      {
         this.add(Blocks.ACACIA_LEAVES);
         this.add(Blocks.AZALEA_LEAVES);
         this.add(Blocks.BIRCH_LEAVES);
         this.add(Blocks.DARK_OAK_LEAVES);
         this.add(Blocks.CHERRY_LEAVES);
         this.add(Blocks.FLOWERING_AZALEA_LEAVES);
         this.add(Blocks.MANGROVE_LEAVES);
         this.add(Blocks.OAK_LEAVES);
         this.add(Blocks.JUNGLE_LEAVES);
         this.add(Blocks.SPRUCE_LEAVES);
      }
   };
   public static ArrayList<ChunkCoordinatesBlock> soundLocations = new ArrayList<>();
   public static HashMap<ChunkCoordinatesBlock, Long> soundTimeLocations = new HashMap<>();
   public static long lastAmbientTick;
   public static long lastAmbientTickThreaded;
   public static long lastWindSoundTick;

   @SubscribeEvent
   public static void fogEvent(ViewportEvent.RenderFog event) {
      Minecraft minecraft = Minecraft.getInstance();
      Level level = minecraft.level;
      if (level != null && ClientConfig.baseGameFog) {
         RenderSystem.setShaderFogStart(10000.0F);
         RenderSystem.setShaderFogEnd(40000.0F);
      }
   }

   @SubscribeEvent
   public static void onStageRenderTick(RenderLevelStageEvent event) {
      if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES && weatherHandler != null) {
         particleManagerDebris.render(
            event.getPoseStack(),
            null,
            Minecraft.getInstance().gameRenderer.lightTexture(),
            event.getCamera(),
            event.getPartialTick(),
            event.getFrustum()
         );
      }
   }

   public static void doSnowParticles(float precip, Minecraft minecraft, Level level) {
      int spawnsNeeded = (int)(precip * 80.0F);
      int spawns = 0;
      int spawnAreaSize = 50;
      BlockPos playerPos = minecraft.player.blockPosition();

      for (int i = 0; i < ClientConfig.rainParticleDensity; i++) {
         // Use reusable MutableBlockPos instead of allocating new BlockPos - OPTIMIZATION
         spawnPos.set(
            playerPos.getX() + Atmospherica.RANDOM.nextInt(spawnAreaSize) - spawnAreaSize / 2,
            playerPos.getY() - 5 + Atmospherica.RANDOM.nextInt(25),
            playerPos.getZ() + Atmospherica.RANDOM.nextInt(spawnAreaSize) - spawnAreaSize / 2
         );
         if (canPrecipitateAt(level, spawnPos)) {
            TextureAtlasSprite particle = switch (Atmospherica.RANDOM.nextInt(4)) {
               case 1 -> ParticleRegistry.snow1;
               case 2 -> ParticleRegistry.snow2;
               case 3 -> ParticleRegistry.snow3;
               default -> ParticleRegistry.snow;
            };
            ParticleTexExtraRender snow = new ParticleTexExtraRender((ClientLevel)level, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 0.0, 0.0, 0.0, particle);
            snow.fullAlphaTarget = 1.0F;
            snow.renderOrder = 3;
            particleBehavior.initParticleSnow(
               snow, Math.max((int)(5.0F * precip), 1), (float)(WindEngine.getWind(spawnPos, level, false, false, true).length() / 45.0)
            );
            snow.setScale(Math.max(precip * 0.08F + (Atmospherica.RANDOM.nextFloat() - Atmospherica.RANDOM.nextFloat()) * 0.02F, 0.01F));
            snow.windWeight = 0.15F;
            snow.renderOrder = 3;
            snow.spawnAsWeatherEffect();
            if (++spawns > spawnsNeeded) {
               break;
            }
         }
      }
   }

   public static void doSleetParticles(float precip, Minecraft minecraft, Level level) {
      int spawnsNeeded = (int)(precip * 300.0F);
      int spawns = 0;
      int spawnAreaSize = 30;
      BlockPos playerPos = minecraft.player.blockPosition();

      for (int i = 0; i < ClientConfig.rainParticleDensity; i++) {
         // Use reusable MutableBlockPos - OPTIMIZATION
         spawnPos.set(
            playerPos.getX() + Atmospherica.RANDOM.nextInt(spawnAreaSize) - spawnAreaSize / 2,
            playerPos.getY() - 5 + Atmospherica.RANDOM.nextInt(25),
            playerPos.getZ() + Atmospherica.RANDOM.nextInt(spawnAreaSize) - spawnAreaSize / 2
         );
         if (canPrecipitateAt(level, spawnPos)) {
            ParticleTexExtraRender sleet = new ParticleTexExtraRender(
               (ClientLevel)level, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 0.0, 0.0, 0.0, ParticleRegistry.sleet
            );
            sleet.fullAlphaTarget = 1.0F;
            sleet.renderOrder = 3;
            particleBehavior.initParticleSleet(sleet, Math.max((int)(20.0F * precip), 1));
            sleet.setScale(Math.max(precip * 0.08F + (Atmospherica.RANDOM.nextFloat() - Atmospherica.RANDOM.nextFloat()) * 0.02F, 0.02F) * 0.3F);
            sleet.renderOrder = 3;
            sleet.spawnAsWeatherEffect();
            if (++spawns > spawnsNeeded) {
               break;
            }
         }
      }
   }

   public static void doRainParticles(float precip, Minecraft minecraft, Level level) {
      int spawnsNeeded = (int)(precip * 300.0F);
      int spawns = 0;
      int spawnAreaSize = 30;
      double windspeed = 0.0;
      if (weatherHandler != null) {
         windspeed = WindEngine.getWind(minecraft.player.position(), level, false, false, false, true).length();
      }
      BlockPos playerPos = minecraft.player.blockPosition();

      for (int i = 0; i < ClientConfig.rainParticleDensity; i++) {
         // Use reusable MutableBlockPos - OPTIMIZATION
         spawnPos.set(
            playerPos.getX() + Atmospherica.RANDOM.nextInt(spawnAreaSize) - spawnAreaSize / 2,
            playerPos.getY() - 5 + Atmospherica.RANDOM.nextInt(25),
            playerPos.getZ() + Atmospherica.RANDOM.nextInt(spawnAreaSize) - spawnAreaSize / 2
         );
         if (canPrecipitateAt(level, spawnPos)) {
            ParticleTexExtraRender rain = new ParticleTexExtraRender(
               (ClientLevel)level, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 0.0, 0.0, 0.0, ParticleRegistry.rain
            );
            rain.fullAlphaTarget = Mth.lerp(precip, 0.3F, 1.0F);
            rain.renderOrder = 3;
            particleBehavior.initParticleRain(rain, Math.max((int)(20.0F * precip), 1));
            if (windspeed > 50.0 && i < ClientConfig.rainParticleDensity / 3) {
               float strength = precip * (float)Mth.clamp((windspeed - 50.0) / 50.0, 0.0, 1.0);
               ParticleTexExtraRender mist = new ParticleTexExtraRender(
                  (ClientLevel)level, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 0.0, 0.0, 0.0, ParticleRegistry.mist
               );
               mist.fullAlphaTarget = Mth.lerp(strength, 0.3F, 1.0F);
               mist.renderOrder = 4;
               particleBehavior.initParticleRain(mist, Math.max((int)(5.0F * strength), 1));
               mist.setScale(0.5F + strength);
               mist.setColor(0.9F, 0.9F, 0.9F);
               mist.setGravity(0.5F);
            }

            if (++spawns > spawnsNeeded) {
               break;
            }
         }
      }

      int var17 = 40;

      for (int ix = 0; ix < ClientConfig.rainParticleDensity * 3 * precip; ix++) {
         // Use reusable MutableBlockPos - OPTIMIZATION
         spawnPos.set(
            playerPos.getX() + Atmospherica.RANDOM.nextInt(var17) - var17 / 2,
            playerPos.getY() - 5 + Atmospherica.RANDOM.nextInt(25),
            playerPos.getZ() + Atmospherica.RANDOM.nextInt(var17) - var17 / 2
         );
         BlockPos heightPos = level.getHeightmapPos(Types.MOTION_BLOCKING, spawnPos).below();
            BlockState state = level.getBlockState(heightPos);
            double maxY = 0.0;
         VoxelShape shape = state.getShape(level, heightPos);
         if (!shape.isEmpty()) {
            maxY = shape.bounds().maxY;
         }

         if (!(heightPos.distSqr(playerPos) > var17 / 2.0 * (var17 / 2.0)) && canPrecipitateAt(level, heightPos.above())) {
            int posY = heightPos.getY();
            if (level.getBlockState(heightPos).getBlock().defaultMapColor() == MapColor.WATER) {
               posY += 1;
            }

            ParticleTexFX rainx = new ParticleTexFX(
               (ClientLevel)level,
               heightPos.getX() + Atmospherica.RANDOM.nextFloat(),
               posY + 0.01 + maxY,
               heightPos.getZ() + Atmospherica.RANDOM.nextFloat(),
               0.0,
               0.0,
               0.0,
               ParticleRegistry.splash
            );
            rainx.fullAlphaTarget = Mth.lerp(precip, 0.2F, 0.8F) / 2.0F;
            rainx.renderOrder = 5;
            particleBehavior.initParticleGroundSplash(rainx);
            rainx.spawnAsWeatherEffect();
         }
      }
   }

   @SubscribeEvent
   public static void onTick(TickEvent.ClientTickEvent event) {
      if (event.phase != TickEvent.Phase.START) {
         return;
      }

      Minecraft minecraft = Minecraft.getInstance();
      Level level = minecraft.level;
      if (level != null && !minecraft.isPaused()) {
         getClientWeather();
         tryAmbientSounds();
         trySounds();
         weatherHandler.tick();
         particleManager.tick();
         particleManagerDebris.tick();
         ModShaders.tick();
         WeatherHandlerClient weatherHandlerClient = (WeatherHandlerClient)weatherHandler;
         if (minecraft.player != null) {
            Entity entity = minecraft.player;
            Vec3 w = WindEngine.getWind(entity.getPosition(1.0F), level, false, true, false);
            if (w.length() > 60.0 && !minecraft.player.isCreative() && !minecraft.player.isSpectator()) {
               double factor = Mth.lerp(Mth.clamp(w.length() / 125.0, 0.0, 1.0), 0.005, 0.02);
               float mult = 0.65F;
               if (!entity.onGround()) {
                  mult = 0.15F;
               }

               entity.addDeltaMovement(w.multiply(0.05F, 0.0, 0.05F).multiply(factor, 0.0, factor).multiply(mult, mult, mult));
            }

            particleManager.getParticles().forEach((particleRenderType, particles) -> {
               for (Particle particle : particles) {
                  if (particle instanceof ParticleData particleData) {
                     float affect = 1.0F;
                     if (particle instanceof EntityRotFX entityRotFX) {
                        if (entityRotFX.ignoreWind) {
                           affect = 0.0F;
                        } else {
                           affect = entityRotFX.windWeight;
                        }
                     }

                     if (affect > 0.0F) {
                        Vec3 wind = WindEngine.getWind(particleData.getPosition(), level, false, false, false);
                        particleData.addVelocity(wind.multiply(0.05F, 0.05F, 0.05F).multiply(0.04F, 0.04F, 0.04F).multiply(affect, affect, affect));
                        double l = wind.length() * 0.01;
                        if (particleData.getVelocity().length() < l) {
                           particleData.setVelocity(particleData.getVelocity().normalize().multiply(l, l, l));
                        }
                     }
                  }
               }
            });
            particleManagerDebris.getParticles().forEach((particleRenderType, particles) -> {
               for (Particle particle : particles) {
                  if (particle instanceof ParticleData particleData) {
                     float affect = 1.0F;
                     if (particle instanceof EntityRotFX entityRotFX) {
                        if (entityRotFX.ignoreWind) {
                           affect = 0.0F;
                        } else {
                           affect = entityRotFX.windWeight;
                        }
                     }

                     if (affect > 0.0F) {
                        Vec3 wind = WindEngine.getWind(particleData.getPosition(), level, false, false, false);
                        particleData.addVelocity(wind.multiply(0.05F, 0.05F, 0.05F).multiply(0.04F, 0.04F, 0.04F).multiply(affect, affect, affect));
                     }
                  }
               }
            });
            float hail = weatherHandlerClient.getHail();
            float precip = weatherHandlerClient.getPrecipitation();
            if (precip > 0.0F) {
               Precipitation precipType = ThermodynamicEngine.getPrecipitationType(
                  weatherHandlerClient, minecraft.player.position(), level, 0
               );
               if (precipType == Precipitation.RAIN
                  || precipType == Precipitation.FREEZING_RAIN
                  || precipType == Precipitation.WINTRY_MIX) {
                  doRainParticles(precip, minecraft, level);
               }

               if (precipType == Precipitation.SLEET || precipType == Precipitation.WINTRY_MIX) {
                  doSleetParticles(precip, minecraft, level);
               }

               if (precipType == Precipitation.SNOW || precipType == Precipitation.WINTRY_MIX) {
                  doSnowParticles(precip, minecraft, level);
               }
            }

            if (hail > 0.0F) {
               int spawnsNeeded = (int)(hail * 80.0F);
               int spawns = 0;
               int spawnAreaSize = 30;

               for (int i = 0; i < 15; i++) {
                  BlockPos pos = minecraft.player
                     .blockPosition()
                     .offset(
                        Atmospherica.RANDOM.nextInt(spawnAreaSize) - spawnAreaSize / 2,
                        -5 + Atmospherica.RANDOM.nextInt(25),
                        Atmospherica.RANDOM.nextInt(spawnAreaSize) - spawnAreaSize / 2
                     );
                  if (canPrecipitateAt(level, pos)) {
                     ParticleCube hailP = new ParticleCube(
                        (ClientLevel)level, pos.getX(), pos.getY(), pos.getZ(), 0.0, 0.0, 0.0, Blocks.PACKED_ICE.defaultBlockState()
                     );
                     particleBehavior.initParticleHail(hailP);
                     hailP.setScale(0.01F + Atmospherica.RANDOM.nextFloat() * hail * 0.08F);
                     hailP.renderOrder = 3;
                     hailP.spawnAsDebrisEffect();
                     if (++spawns >= spawnsNeeded) {
                        break;
                     }
                  }
               }
            }
         }
      }
   }

   public static boolean canPrecipitateAt(Level level, BlockPos pos) {
      return pos.getY() > ServerConfig.layer0Height ? false : level.getHeightmapPos(Types.MOTION_BLOCKING, pos).getY() <= pos.getY();
   }

   public static void resetClientWeather() {
      weatherHandler = null;
   }

   public static WeatherHandlerClient getClientWeather() {
      try {
         Level level = Minecraft.getInstance().level;
         if (weatherHandler == null || level != lastLevel) {
            init(level);
         }
      } catch (Exception var1) {
         Atmospherica.LOGGER.error(var1.getMessage(), var1);
      }

      return (WeatherHandlerClient)weatherHandler;
   }

   public static void trySounds() {
      try {
         Minecraft minecraft = Minecraft.getInstance();
         Level level = minecraft.level;
         Player player = minecraft.player;
         if (player == null || level == null) {
            return;
         }

         float hail = ((WeatherHandlerClient)weatherHandler).getHail();
         if (hail > 0.0F) {
            int chance = (int)Mth.lerp(hail, 20.0F, 2.0F);
            if (Atmospherica.RANDOM.nextInt(chance) == 0) {
               BlockPos pos = player.blockPosition().offset(Atmospherica.RANDOM.nextInt(-15, 16), 15, Atmospherica.RANDOM.nextInt(-15, 16));
               pos = level.getHeightmapPos(Types.MOTION_BLOCKING, pos);
               if (canPrecipitateAt(level, pos) && pos.distSqr(player.blockPosition()) < 225.0) {
                     level.playLocalSound(
                        pos, ModSounds.HAIL.get(), SoundSource.WEATHER, hail * 3.5F, 2.0F + Atmospherica.RANDOM.nextFloat() * 0.5F, false
                  );
               }
            }
         }

         if (lastWindSoundTick < System.currentTimeMillis()) {
            lastWindSoundTick = System.currentTimeMillis() + 4000L + Atmospherica.RANDOM.nextInt(0, 3000);
            Vec3 wind = WindEngine.getWind(player.position(), level);
            double windspeed = wind.length();
            if (windspeed > 55.0) {
               ModSounds.playPlayerLockedSound(
                  player.position(), ModSounds.WIND_STRONG.get(), (float)(windspeed / 200.0), 0.9F + Atmospherica.RANDOM.nextFloat() * 0.2F
               );
            }

            if (windspeed > 35.0) {
               ModSounds.playPlayerLockedSound(
                  player.position(), ModSounds.WIND_MED.get(), (float)(windspeed / 200.0), 0.9F + Atmospherica.RANDOM.nextFloat() * 0.2F
               );
            }

            if (windspeed > 5.0) {
               ModSounds.playPlayerLockedSound(
                  player.position(),
                  ModSounds.WIND_CALM.get(),
                  Math.min((float)(windspeed / 100.0), 0.1F),
                  0.9F + Atmospherica.RANDOM.nextFloat() * 0.2F
               );
            }
         }

         if (lastAmbientTick < System.currentTimeMillis()) {
            lastAmbientTick = System.currentTimeMillis() + 500L;
            int size = 32;
            BlockPos curBlockPos = player.blockPosition();

            for (int i = 0; i < soundLocations.size(); i++) {
               ChunkCoordinatesBlock chunkCoord = (ChunkCoordinatesBlock)soundLocations.get(i);
               if (Math.sqrt(chunkCoord.distSqr(curBlockPos)) > size) {
                  soundLocations.remove(i--);
                  soundTimeLocations.remove(chunkCoord);
               } else {
                  Block block = level.getBlockState(chunkCoord).getBlock();
                  if (block != null && (block.defaultMapColor() == MapColor.WATER || block.defaultMapColor() == MapColor.PLANT)) {
                     long lastPlayTime = 0L;
                     float soundMuffle = 0.6F;
                     if (soundTimeLocations.containsKey(chunkCoord)) {
                        lastPlayTime = (Long)soundTimeLocations.get(chunkCoord);
                     }

                     float maxLeavesVolume = 1.0F;
                     soundMuffle *= (float)ClientConfig.leavesVolume;
                     if (lastPlayTime < System.currentTimeMillis() && LEAVES_BLOCKS.contains(chunkCoord.block)) {
                        Vec3 windx = WindEngine.getWind(curBlockPos, level, false, false, false);
                        double windspeedx = windx.length();
                        soundTimeLocations.put(chunkCoord, System.currentTimeMillis() + 12000L + Atmospherica.RANDOM.nextInt(50));
                        minecraft.level
                           .playLocalSound(
                              chunkCoord,
                              ModSounds.CALM_AMBIENCE.get(),
                              SoundSource.AMBIENT,
                              (float)Math.min(maxLeavesVolume, windspeedx * soundMuffle * 0.05F),
                              0.9F + Atmospherica.RANDOM.nextFloat() * 0.2F,
                              false
                           );
                     }
                  } else {
                     soundLocations.remove(i);
                     soundTimeLocations.remove(chunkCoord);
                  }
               }
            }
         }
      } catch (Exception var17) {
         Atmospherica.LOGGER.error(var17.getMessage(), var17);
      }
   }

   // Reusable BlockPos.MutableBlockPos to avoid allocations in tight loops
   private static final BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
   
   public static void tryAmbientSounds() {
      Minecraft minecraft = Minecraft.getInstance();
      Level level = minecraft.level;
      Player player = minecraft.player;
      if (lastAmbientTickThreaded < System.currentTimeMillis() && ClientConfig.leavesVolume > 0.0) {
         lastAmbientTickThreaded = System.currentTimeMillis() + 500L;
         
         // Optimized: Use sparse sampling instead of checking every block
         // Sample every 4th block in each dimension (reduces checks by 64x: from 32768 to ~512)
         int size = 32;
         int hSize = size / 2;
         int step = 4; // Sample every 4 blocks
         BlockPos curBlockPos = player.blockPosition();
         int baseX = curBlockPos.getX();
         int baseY = curBlockPos.getY();
         int baseZ = curBlockPos.getZ();

         for (int x = baseX - hSize; x < baseX + hSize; x += step) {
            for (int y = baseY - hSize; y < baseY + hSize; y += step) {
               for (int z = baseZ - hSize; z < baseZ + hSize; z += step) {
                  mutableBlockPos.set(x, y, z);
                  Block block = level.getBlockState(mutableBlockPos).getBlock();
                  if (block.defaultMapColor() == MapColor.PLANT) {
                     boolean proxFail = false;
                     
                     // Check proximity to existing sound locations
                     // Use squared distance to avoid sqrt
                     int proxDistSq = 15 * 15; // 225
                     for (ChunkCoordinatesBlock soundLocation : soundLocations) {
                        if (soundLocation.distSqr(mutableBlockPos) < proxDistSq) {
                           proxFail = true;
                           break;
                        }
                     }

                     if (!proxFail) {
                        soundLocations.add(new ChunkCoordinatesBlock(x, y, z, block));
                     }
                  }
               }
            }
         }
      }
   }

   public static void init(Level level) {
      lastLevel = level;
      if (level != null) {
         weatherHandler = new WeatherHandlerClient(level.dimension());
         Minecraft minecraft = Minecraft.getInstance();
         if (particleManager == null) {
            particleManager = new ParticleManager(minecraft.level, minecraft.getTextureManager());
         } else {
            particleManager.setLevel((ClientLevel)level);
         }

         if (particleManagerDebris == null) {
            particleManagerDebris = new ParticleManager(minecraft.level, minecraft.getTextureManager());
         } else {
            particleManagerDebris.setLevel((ClientLevel)level);
         }

         CompoundTag data = new CompoundTag();
         data.putString("command", "syncFull");
         data.putString("packetCommand", "WeatherData");
         ModNetworking.clientSendToSever(data);
      }
   }
}
