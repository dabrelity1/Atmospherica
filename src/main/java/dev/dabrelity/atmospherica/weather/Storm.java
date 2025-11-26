package dev.dabrelity.atmospherica.weather;

import dev.dabrelity.atmospherica.Atmospherica;
import dev.dabrelity.atmospherica.block.ModBlocks;
import dev.dabrelity.atmospherica.config.Config;
import dev.dabrelity.atmospherica.config.ServerConfig;
import dev.dabrelity.atmospherica.entity.ModEntities;
import dev.dabrelity.atmospherica.entity.MovingBlock;
import dev.dabrelity.atmospherica.interfaces.ParticleData;
import dev.dabrelity.atmospherica.particle.EntityRotFX;
import dev.dabrelity.atmospherica.sound.ModSounds;
import dev.dabrelity.atmospherica.sound.MovingSoundStreamingSource;
import dev.dabrelity.atmospherica.util.CachedNBTTagCompound;
import dev.dabrelity.atmospherica.util.Util;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.common.Tags.Blocks;

public class Storm {
   public static long LastUsedStormID = 0L;
   private static final float resistance = 0.985F;
   public static final float tickConversion = 0.05F;
   @OnlyIn(Dist.CLIENT)
   public MovingSoundStreamingSource tornadicWind;
   @OnlyIn(Dist.CLIENT)
   public MovingSoundStreamingSource tornadicDamage;
   @OnlyIn(Dist.CLIENT)
   public MovingSoundStreamingSource supercellWind;
   @OnlyIn(Dist.CLIENT)
   public MovingSoundStreamingSource eyewallWind;
   @OnlyIn(Dist.CLIENT)
   public MovingSoundStreamingSource undergroundWind;
   public long ID;
   public WeatherHandler weatherHandler;
   public Vec3 position;
   public Vec3 lastPosition;
   public Vec3 velocity;
   public int windspeed;
   public float cycloneWindspeed = 0.0F;
   public float smoothWindspeed = 0.0F;
   public float width = 15.0F;
   public float smoothWidth = 15.0F;
   public float tornadoShape = Atmospherica.RANDOM.nextFloat() * 10.0F + 6.0F;
   public float spin = 0.0F;
   public float lastSpin = 0.0F;
   public int energy;
   public int stormType;
   public int stage;
   public int tickCount = 0;
   public int tornadoOnGroundTicks = 0;
   public boolean dead = false;
   public Level level;
   private final CachedNBTTagCompound nbtCache;
   public SimplexNoise simplexNoise;
   public float rankineFactor = 4.5F;
   public List<EntityRotFX> listParticleDebris;
   private final List<ChunkPos> forceLoadedChunks = new ArrayList();
   public int maxStage = 0;
   public int maxProgress = 0;
   public boolean isDying = false;
   public int growthSpeed = 20;
   public int maxWindspeed = 0;
   public int maxWidth = 15;
   public int ticksSinceDying = 0;
   public int touchdownSpeed = Atmospherica.RANDOM.nextInt(65, 120);
   public boolean onWater = false;
   public float occlusion = 0.0F;
   public boolean visualOnly = false;
   public boolean cirus = false;
   public boolean aimedAtPlayer = false;
   public int maxColdEnergy = 300;
   public int coldEnergy = 0;
   public List<Vorticy> vorticies = new ArrayList();

   public double FBM(Vec3 pos, int octaves, float lacunarity, float gain, float amplitude) {
      double y = 0.0;

      for (int i = 0; i < Math.max(octaves, 1); i++) {
         y += amplitude * this.simplexNoise.getValue(pos.x, pos.y, pos.z);
         pos = pos.multiply(lacunarity, lacunarity, lacunarity);
         amplitude *= gain;
      }

      return y;
   }

   public Vec3 rotateV3(Vec3 x, double angle) {
      double rx = x.x * Math.cos(angle) - x.z * Math.sin(angle);
      double rz = x.x * Math.sin(angle) + x.z * Math.cos(angle);
      return new Vec3(rx, x.y, rz);
   }

   public Storm(WeatherHandler weatherHandler, Level level, @Nullable Float risk, int stormType) {
      this.weatherHandler = weatherHandler;
      this.level = level;
      this.stormType = stormType;
      this.simplexNoise = new SimplexNoise(new LegacyRandomSource(weatherHandler.seed));
      this.nbtCache = new CachedNBTTagCompound();
      if (level.isClientSide()) {
         this.listParticleDebris = new ArrayList();
      } else {
         this.maxStage = 0;
         this.maxProgress = Atmospherica.RANDOM.nextInt(25, 99);
         float stage1Chance = 1.0F / ServerConfig.chanceInOneStage1;
         float stage2Chance = 1.0F / ServerConfig.chanceInOneStage2;
         float stage3Chance = 1.0F / ServerConfig.chanceInOneStage3;
         if (risk != null && ServerConfig.environmentSystem && stormType == 0) {
            stage1Chance *= risk * 1.75F + 0.05F;
            stage2Chance *= risk;
            stage3Chance *= risk * 0.75F;
            Atmospherica.LOGGER.debug("Readjusted stage chances: 1: {} 2: {} 3: {}", new Object[]{stage1Chance, stage2Chance, stage3Chance});
         }

         if (Atmospherica.RANDOM.nextFloat() <= stage1Chance) {
            this.maxStage = 1;
         }

         if (Atmospherica.RANDOM.nextFloat() <= stage2Chance) {
            this.maxStage = 2;
         }

         if (Atmospherica.RANDOM.nextFloat() <= stage3Chance) {
            this.maxStage = 3;
         }

         if (this.maxStage == 3 && stormType == 0) {
            this.maxProgress = 100;
            float mW;
            if (risk != null && ServerConfig.environmentSystem) {
               mW = risk * 80.0F;
            } else {
               mW = 125.0F;
            }

            mW += 55.0F;
            this.maxWindspeed = Math.min((int)Mth.lerp(Atmospherica.RANDOM.nextFloat(), 55.0F, mW), 220);
            this.touchdownSpeed = Atmospherica.RANDOM.nextInt(75, Math.max(25 + (int)(this.maxWindspeed * 1.1F), 100));
         }

         this.growthSpeed = Atmospherica.RANDOM.nextInt(30, 80);
         if (stormType == 1) {
            this.growthSpeed = Atmospherica.RANDOM.nextInt(40, 70);
         }

         this.maxWidth = Atmospherica.RANDOM.nextInt(15, 25 + (int)(Math.pow(this.maxWindspeed / 220.0F, 1.75) * (ServerConfig.maxTornadoWidth - 25.0)));
         Atmospherica.LOGGER
            .debug(
               "Max Stage: {}, Max Energy: {}, Max Windspeed: {}, Max Width: {}, Touchdown Speed: {}",
               new Object[]{this.maxStage, this.maxProgress, this.maxWindspeed, this.maxWidth, this.touchdownSpeed}
            );
      }
   }

   public void recalc(@Nullable Float risk) {
      if (this.maxStage == 3 && this.stormType == 0) {
         this.maxProgress = 100;
         float mW;
         if (risk != null && ServerConfig.environmentSystem) {
            mW = risk * 80.0F;
         } else {
            mW = 125.0F;
         }

         mW += 55.0F;
         this.maxWindspeed = Math.min((int)Mth.lerp(Atmospherica.RANDOM.nextFloat(), 55.0F, mW), 220);
         this.touchdownSpeed = Atmospherica.RANDOM.nextInt(75, Math.max(25 + (int)(this.maxWindspeed * 1.1F), 100));
      }

      this.growthSpeed = Atmospherica.RANDOM.nextInt(30, 80);
      if (this.stormType == 1) {
         this.growthSpeed = Atmospherica.RANDOM.nextInt(40, 70);
      }

      this.maxWidth = Atmospherica.RANDOM.nextInt(15, 25 + (int)(Math.pow(this.maxWindspeed / 220.0F, 1.75) * (ServerConfig.maxTornadoWidth - 25.0)));
      Atmospherica.LOGGER
         .debug(
            "Max Stage: {}, Max Energy: {}, Max Windspeed: {}, Max Width: {}, Touchdown Speed: {}",
            new Object[]{this.maxStage, this.maxProgress, this.maxWindspeed, this.maxWidth, this.touchdownSpeed}
         );
   }

   public void aimAtPlayer() {
      if (this.stormType != 1) {
         Player nearest = this.level.getNearestPlayer(this.position.x, this.position.y, this.position.z, 4096.0, false);
         if (nearest != null) {
            Vec3 aimPos = nearest.position()
               .add(
                  new Vec3(
                     (Atmospherica.RANDOM.nextFloat() - 0.5F) * ServerConfig.aimAtPlayerOffset,
                     0.0,
                     (Atmospherica.RANDOM.nextFloat() - 0.5F) * ServerConfig.aimAtPlayerOffset
                  )
               );
            if (this.position.distanceTo(aimPos) >= ServerConfig.aimAtPlayerOffset) {
               Vec3 toward = this.position.subtract(new Vec3(aimPos.x, this.position.y, aimPos.z)).multiply(1.0, 0.0, 1.0).normalize();
               double speed = Atmospherica.RANDOM.nextDouble() * 5.0 + 1.0;
               this.velocity = toward.multiply(-speed, 0.0, -speed);
            }

            this.aimedAtPlayer = true;
         }
      }
   }

   public void tick() {
      this.tickCount++;
      Iterator<Vorticy> vorts = this.vorticies.iterator();

      while (vorts.hasNext()) {
         Vorticy vorticy = (Vorticy)vorts.next();
         vorticy.tick();
         if (vorticy.dead) {
            vorts.remove();
         }
      }

      float vorticySpawnChance = 0.05F;
      if (this.isDying) {
         vorticySpawnChance = 0.25F;
      }

      vorticySpawnChance += Mth.clamp((float)Math.pow((this.windspeed - 100.0F) / 200.0F, 2.0), 0.0F, 0.5F);
      if (this.windspeed >= 39.0F && this.stormType == 2) {
         vorticySpawnChance *= 2.0F;
         if (!this.level.isClientSide && Atmospherica.RANDOM.nextFloat() < vorticySpawnChance * 0.05F && this.vorticies.size() < 10) {
            Vorticy vorticy = new Vorticy(
               this,
               (float)Math.pow(Atmospherica.RANDOM.nextFloat(), 0.75) * 0.1F,
               Atmospherica.RANDOM.nextFloat() * 0.15F + 0.05F,
               0.075F,
               Atmospherica.RANDOM.nextInt(900, 3000)
            );
            this.vorticies.add(vorticy);
         }
      }

      if (this.stage == 3 && this.windspeed >= 40.0F && this.stormType == 0) {
         this.tornadoOnGroundTicks++;
         if (!this.level.isClientSide && Atmospherica.RANDOM.nextFloat() < vorticySpawnChance * 0.05F && this.vorticies.size() < 10) {
            Vorticy vorticy = new Vorticy(
               this,
               (float)Math.pow(Atmospherica.RANDOM.nextFloat(), 0.75) * 0.4F,
               Atmospherica.RANDOM.nextFloat() * 0.3F + 0.05F,
               1.0F / this.rankineFactor * 0.5F,
               Atmospherica.RANDOM.nextInt(35, 120)
            );
            this.vorticies.add(vorticy);
         }
      }

      if (this.isDying) {
         this.ticksSinceDying++;
      }

      BlockPos blockPos = new BlockPos((int)this.position.x, (int)this.position.y, (int)this.position.z);
      if (!this.level.isClientSide() && this.stage >= 2 && this.stormType == 0) {
         float y = 0.0F;
         int count = 0;

         for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
               float r = Math.max(this.width, 45.0F);
               Vec3 samplePos = this.position.add(x * r * 0.5F, 0.0, z * r * 0.5F);
               BlockPos sample = this.level
                  .getHeightmapPos(Types.WORLD_SURFACE_WG, new BlockPos((int)samplePos.x, this.level.getMaxBuildHeight(), (int)samplePos.z));
               y += sample.getY();
               count++;
            }
         }

         y /= count;
         blockPos = new BlockPos((int)this.position.x, (int)y, (int)this.position.z);
         this.position = new Vec3(this.position.x, Mth.lerp(0.01F, this.position.y, y), this.position.z);
      }

      if (this.tickCount % 20 == 0 && !this.level.isClientSide() && this.level instanceof ServerLevel serverLevel) {
         if (this.windspeed > 40 && this.stormType == 0) {
            ChunkPos cChunkPos = new ChunkPos(blockPos);

            for (int x = -((int)this.width); x <= this.width; x += 16) {
               for (int z = -((int)this.width); z <= this.width; z += 16) {
                  ChunkPos chunkPos = new ChunkPos(blockPos.offset(x, 0, z));
                  if (!serverLevel.hasChunk(chunkPos.x, chunkPos.z) && !this.forceLoadedChunks.contains(chunkPos) && serverLevel.isInWorldBounds(blockPos)) {
                     this.forceLoadedChunks.add(chunkPos);
                     serverLevel.setChunkForced(chunkPos.x, chunkPos.z, true);
                  }
               }
            }

            Iterator<ChunkPos> iterator = this.forceLoadedChunks.iterator();

            while (iterator.hasNext()) {
               ChunkPos cpos = (ChunkPos)iterator.next();
               double dist = Math.sqrt((cpos.x - cChunkPos.x) * (cpos.x - cChunkPos.x) + (cpos.z - cChunkPos.z) * (cpos.z - cChunkPos.z));
               if (dist > this.width * 2.0F / 16.0F) {
                  iterator.remove();
                  serverLevel.setChunkForced(cpos.x, cpos.z, false);
               }
            }
         } else {
            Iterator<ChunkPos> iterator = this.forceLoadedChunks.iterator();

            while (iterator.hasNext()) {
               ChunkPos cpos = (ChunkPos)iterator.next();
               iterator.remove();
               serverLevel.setChunkForced(cpos.x, cpos.z, false);
            }
         }
      }

      if (this.tickCount % 10 == 0 && !this.level.isClientSide() && this.level instanceof ServerLevel serverLevelx) {
         float lightningChance = 0.0F;
         if (this.stage == 1) {
            lightningChance = this.energy / 100.0F;
         } else if (this.stage == 2) {
            lightningChance = 1.0F + this.energy / 100.0F;
         } else if (this.stage > 2) {
            lightningChance = 2.0F;
         }

         if (this.visualOnly) {
            lightningChance = 0.0F;
         }

         lightningChance = Math.min(lightningChance * 0.035F, 0.1F);
         if (this.stormType == 1) {
            lightningChance *= 3.0F;
         }

         if (Atmospherica.RANDOM.nextFloat() <= lightningChance * 0.5F) {
            Vec3 lPos = this.position
               .add(
                  Atmospherica.RANDOM.nextFloat((float)(-ServerConfig.stormSize), (float)ServerConfig.stormSize) / 2.0F,
                  0.0,
                  Atmospherica.RANDOM.nextFloat((float)(-ServerConfig.stormSize), (float)ServerConfig.stormSize) / 2.0F
               );
            if (this.stormType == 1) {
               Vec2 stormVel = new Vec2((float)this.velocity.x, (float)this.velocity.z);
               Vec2 right = new Vec2(stormVel.y, -stormVel.x).normalized();
               Vec2 fwd = stormVel.normalized();
               right = Util.mulVec2(right, Atmospherica.RANDOM.nextFloat((float)(-ServerConfig.stormSize), (float)ServerConfig.stormSize) * 5.0F);
               fwd = Util.mulVec2(fwd, Atmospherica.RANDOM.nextFloat((float)(-ServerConfig.stormSize), (float)ServerConfig.stormSize) / 2.0F);
               lPos = this.position.add(new Vec3(right.x, 0.0, right.y)).add(new Vec3(fwd.x, 0.0, fwd.y));
            }

            int height = this.level.getHeightmapPos(Types.MOTION_BLOCKING, new BlockPos((int)lPos.x, (int)lPos.y, (int)lPos.z)).getY();
            ((WeatherHandlerServer)this.weatherHandler).syncLightningNew(new Vec3(lPos.x, height, lPos.z));
         }
      }

      int gs = this.growthSpeed / 2;
      if (this.stormType == 0 && this.stage < 3) {
         gs = (int)(gs / 1.5F);
      }

      if (this.tickCount % gs == 0) {
         if (this.stormType == 2 && !this.level.isClientSide()) {
            this.stage = 0;
            if (this.windspeed >= 15) {
               this.stage = 1;
            }

            if (this.windspeed >= 25) {
               this.stage = 2;
            }

            if (this.windspeed >= 40) {
               this.stage = 3;
            }

            Float sst = ThermodynamicEngine.GetSST(this.weatherHandler, this.position, this.level, null, 0);
            if (sst != null && this.tickCount <= 48000) {
               if (sst > 32.0F) {
                  sst = 32.0F;
               }

               float v = 24.0F;
               if (this.cycloneWindspeed > 60.0F) {
                  v += (this.cycloneWindspeed - 60.0F) / 18.5F;
               }

               float growth = (sst - v) / 3.5F;
               if (this.windspeed > 165.0F) {
                  growth -= (this.windspeed - 165.0F) / 15.0F;
               }

               if (growth < 0.0F) {
                  growth = Math.max(growth, -1.5F);
               } else {
                  growth *= 1.25F;
                  growth = Math.min(growth, 3.0F);
               }

               this.cycloneWindspeed += growth;
            } else {
               float death = 1.0F;
               death += Math.max((this.cycloneWindspeed - 75.0F) / 100.0F, 0.0F);
               this.cycloneWindspeed -= death * 0.25F;
            }

            this.windspeed = Math.round(this.cycloneWindspeed);
            if (this.windspeed < -5) {
               this.dead = true;
            }
         } else if (!this.isDying) {
            int targetProgress = this.maxProgress;
            if (this.maxStage > this.stage) {
               targetProgress = 100;
            }

            if (this.energy < targetProgress) {
               this.energy++;
               if (this.stormType == 1) {
                  this.coldEnergy = Mth.clamp(this.coldEnergy + 1, 0, this.maxColdEnergy);
               }
            }

            if (this.stage >= 3 && this.stormType == 0) {
               if (this.windspeed < this.maxWindspeed) {
                  this.windspeed++;
                  this.occlusion = Mth.clamp(this.occlusion - 0.025F, 0.0F, 1.0F);
               }

               if (this.windspeed >= this.maxWindspeed) {
                  this.isDying = true;
                  this.growthSpeed = Atmospherica.RANDOM.nextInt(20, 70);
               }
            } else if (this.stage >= this.maxStage && this.energy >= targetProgress) {
               this.isDying = true;
               this.growthSpeed = Atmospherica.RANDOM.nextInt(40, 80);
               if (Atmospherica.RANDOM.nextInt(2) == 0 || this.maxWidth > 200) {
                  this.maxWidth = Math.min(this.maxWidth, Atmospherica.RANDOM.nextInt(5, 35));
               }
            }

            if (this.energy >= 100) {
               this.energy = 0;
               if (this.stormType == 0) {
                  if (this.stage < 3 && this.stage < this.maxStage) {
                     this.stage++;
                     if (this.stage == 3) {
                        this.windspeed = 0;
                     }
                  }
               } else if (this.stage < this.maxStage) {
                  this.stage++;
               }
            }
         } else if (this.ticksSinceDying > (this.stormType == 1 ? 2400 : 1200)) {
            if (this.stage >= 3 && this.stormType == 0) {
               if (this.windspeed >= 85 || this.windspeed <= 15) {
                  this.windspeed--;
               } else if (Atmospherica.RANDOM.nextInt(2) == 0 && !this.level.isClientSide()) {
                  this.windspeed--;
               }

               this.occlusion = Mth.clamp(this.occlusion + 0.015F, 0.0F, 1.0F);
               if (this.windspeed <= 0) {
                  this.windspeed = 0;
                  this.stage--;
                  this.energy = 100;
               }
            } else {
               this.energy--;
               if (this.energy <= 0) {
                  this.energy = 100;
                  this.stage--;
                  if (this.stage < 0) {
                     this.energy = 0;
                     this.stage = 0;
                     if (this.coldEnergy > 0) {
                        this.coldEnergy--;
                     } else {
                        this.dead = true;
                     }
                  }
               }
            }
         }

         if (Config.DEBUG) {
            Atmospherica.LOGGER.debug("Stage: {}, Energy: {}, Windspeed: {}, Width: {}", new Object[]{this.stage, this.energy, this.windspeed, this.width});
         }
      }

      if (this.stormType == 0) {
         this.width = Mth.lerp(0.025F, this.width, Math.max(5.0F, Mth.clamp((float)this.windspeed / this.maxWindspeed, 0.1F, 1.0F) * this.maxWidth));
      } else if (this.stormType == 2) {
         this.width = this.maxWidth;
      }

      Vec3 vel = this.velocity.multiply(0.05F, 0.05F, 0.05F).multiply(2.0, 0.0, 2.0);
      if (!this.aimedAtPlayer) {
         vel = vel.add(new Vec3(0.0, 0.0, -3.0).multiply(0.05F * this.occlusion, 0.05F * this.occlusion, 0.05F * this.occlusion));
      }

      this.position = this.position.add(vel);
      if (!this.aimedAtPlayer) {
         if (this.stormType != 1) {
            this.velocity = this.velocity.multiply(0.985F, 0.985F, 0.985F);
            Vec3 baseWind = WindEngine.getWind(
               new Vec3(this.position.x, this.level.getMaxBuildHeight() + 1, this.position.z), this.level, true, true, false, false
            );
            float factor = 0.018181818F;
            if (this.stormType == 2) {
               factor = 0.05F;
            }

            Vec3 velAdd = new Vec3(baseWind.x, 0.0, baseWind.z).multiply(factor, 0.0, factor);
            this.velocity = this.velocity.add(velAdd.multiply(0.05F, 0.05F, 0.05F));
         }

         if (!this.level.isClientSide() && this.stage >= 3 && ServerConfig.aimAtPlayer && this.stormType == 0) {
            this.aimAtPlayer();
         }
      }

      if (!this.level.isClientSide() && this.tickCount % this.getUpdateRate() == 0) {
         WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)this.weatherHandler;
         weatherHandlerServer.syncStormUpdate(this);
      }

      if (this.level.isClientSide()) {
         this.tickClient();
      } else if (this.stage >= 3 && this.stormType == 0) {
         if (this.windspeed >= 40) {
            AABB aabb = new AABB(this.position.x, this.position.y, this.position.z, this.position.x, this.position.y, this.position.z);
            aabb = aabb.inflate(this.width / 2.0, 85.0, this.width / 2.0);

            for (Entity entity : this.level.getEntities(null, aabb)) {
               if (entity instanceof Player player && !player.isCreative() && !player.isSpectator()) {
                  this.pull(entity, 2.5F);
               } else if (!(entity instanceof Player)) {
                  this.pull(entity, 2.5F);
               }
            }

            boolean dd = this.tickCount % 5 == 0 || !ServerConfig.damageEvery5thTick;
            if (dd) {
               int windfieldWidth = Math.max((int)this.width, 40);
               int numBlocks = Math.min(windfieldWidth * Math.max(windfieldWidth / 2, 20) + this.windspeed * 3 + 300, ServerConfig.maxBlocksDamagedPerTick);
               Map<Vec3i, Boolean> checkedMap = new HashMap();
               Map<ChunkPos, LevelChunk> chunkMap = new HashMap();
               int damaged = 0;
               int damageMax = (500 + (int)this.width) / 3;

               for (int i = 0; i < numBlocks && damaged < damageMax; i++) {
                  int x = (int)(Atmospherica.RANDOM.nextFloat() * windfieldWidth * 2.0F - windfieldWidth);
                  int zx = (int)(Atmospherica.RANDOM.nextFloat() * windfieldWidth * 2.0F - windfieldWidth);
                  Vec3i off = new Vec3i(x, 0, zx);
                  if (!checkedMap.containsKey(off)) {
                     checkedMap.put(off, true);
                     double dist = off.distSqr(Vec3i.ZERO);
                     if (!(dist > windfieldWidth * windfieldWidth)) {
                        float percAdj = 16.0F;
                        if (ServerConfig.damageEvery5thTick) {
                           percAdj *= 5.0F;
                        }

                        BlockPos bPos = blockPos.offset(off.getX(), 60, off.getZ());
                        if (this.level.isInWorldBounds(bPos)) {
                           BlockPos blockPosTop = this.level.getHeightmapPos(Types.MOTION_BLOCKING, bPos).below();
                           double windEffect = this.getWind(blockPosTop.getCenter());
                           if (!(windEffect < 40.0)) {
                              ChunkPos chunkPos = new ChunkPos(
                                 SectionPos.blockToSectionCoord(blockPosTop.getX()), SectionPos.blockToSectionCoord(blockPosTop.getZ())
                              );
                              LevelChunk chunk;
                              if (chunkMap.containsKey(chunkPos)) {
                                 chunk = (LevelChunk)chunkMap.get(chunkPos);
                              } else {
                                 Atmospherica.LOGGER.debug("{}", chunkPos);
                                 chunk = this.level.getChunk(chunkPos.x, chunkPos.z);
                                 chunkMap.put(chunkPos, chunk);
                              }

                              this.doDamage(chunk, blockPosTop, windEffect, percAdj, windfieldWidth);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void doDamage(LevelChunk chunk, BlockPos blockPosTop, double windEffect, float percAdj, int windfieldWidth) {
      BlockState state = chunk.getBlockState(blockPosTop);
      BlockPos randomDown = blockPosTop.below(Atmospherica.RANDOM.nextInt(10));
      BlockState stateDown = chunk.getBlockState(randomDown);
      boolean downBlacklisted = false;

      for (TagKey<Block> tag : ServerConfig.blacklistedBlockTags) {
         if (stateDown.is(tag)) {
            downBlacklisted = true;
            break;
         }
      }

      if (!downBlacklisted && !ServerConfig.blacklistedBlocks.contains(stateDown.getBlock())) {
         if (stateDown.is(net.minecraft.tags.BlockTags.IMPERMEABLE) || stateDown.getBlock().toString().contains("glass")) {
            double percChance = Mth.clamp((windEffect - 75.0) / 15.0, 0.0, 1.0);
            if (Atmospherica.RANDOM.nextFloat() <= percChance * (0.3F * percAdj) && Util.canWindAffect(randomDown.getCenter(), this.level)) {
               this.level.removeBlock(randomDown, false);
               this.level.playSound(null, randomDown, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, Atmospherica.RANDOM.nextFloat(0.8F, 1.2F));
            }
         }

         if (stateDown.is(BlockTags.LOGS) && !stateDown.getBlock().toString().contains("stripped") && ServerConfig.doDebarking) {
            double percChance = Mth.clamp((windEffect - 140.0) / 20.0, 0.0, 1.0);
            if (Atmospherica.RANDOM.nextFloat() <= percChance * (0.5F * percAdj) && Util.canWindAffect(randomDown.getCenter(), this.level)) {
               Block replacement = (Block)Util.STRIPPED_VARIANTS.getOrDefault(stateDown.getBlock(), net.minecraft.world.level.block.Blocks.STRIPPED_OAK_LOG);
               this.level
                  .setBlockAndUpdate(
                     randomDown,
                     (BlockState)replacement.defaultBlockState()
                        .trySetValue(BlockStateProperties.AXIS, (Axis)stateDown.getOptionalValue(BlockStateProperties.AXIS).orElse(Axis.Y))
                  );
            }
         }
      }

      BlockState aboveState = chunk.getBlockState(blockPosTop.above());
      if (!aboveState.isAir()) {
         Block aboveBlock = aboveState.getBlock();
         float blockStrength = getBlockStrength(aboveBlock, this.level, blockPosTop.above());
         double percChance = Mth.clamp(Math.pow(Mth.clamp(Math.max(windEffect - blockStrength, 0.0) / 20.0, 0.0, 1.0), 4.0) + 0.02, 0.0, 1.0)
            * 0.05
            * percAdj;
         if (windEffect < blockStrength) {
            percChance = 0.0;
         }

         if (aboveBlock.defaultDestroyTime() < 0.05F
            && aboveBlock.defaultDestroyTime() >= 0.0F
            && !ServerConfig.blacklistedBlocks.contains(aboveBlock)
            && Atmospherica.RANDOM.nextFloat() <= percChance) {
            this.level.removeBlock(blockPosTop.above(), false);
            return;
         }

         boolean blacklisted = false;

         for (TagKey<Block> tagx : ServerConfig.blacklistedBlockTags) {
            if (aboveBlock.defaultBlockState().is(tagx)) {
               blacklisted = true;
               break;
            }
         }

         if (windEffect >= blockStrength
            && aboveBlock.defaultDestroyTime() > 0.0F
            && !ServerConfig.blacklistedBlocks.contains(aboveBlock)
            && !blacklisted
            && state.getFluidState().isEmpty()
            && Atmospherica.RANDOM.nextFloat() <= percChance) {
            this.level.removeBlock(blockPosTop.above(), false);
         }
      }

      if (state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) || state.is((Block)ModBlocks.SCOURED_GRASS.get())) {
         double percChancex = Mth.clamp((windEffect - 140.0) / 80.0, 0.0, 1.0);
         if (Atmospherica.RANDOM.nextFloat() <= percChancex * (0.02F * percAdj)) {
            this.level.setBlockAndUpdate(blockPosTop, net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState());
         }
      } else if (state.is(net.minecraft.world.level.block.Blocks.DIRT)) {
         double percChancex = Mth.clamp((windEffect - 170.0) / 40.0, 0.0, 1.0);
         if (Atmospherica.RANDOM.nextFloat() <= percChancex * (0.02F * percAdj)) {
            this.level.setBlockAndUpdate(blockPosTop, ((Block)ModBlocks.MEDIUM_SCOURING.get()).defaultBlockState());
         }
      } else if (state.is((Block)ModBlocks.MEDIUM_SCOURING.get())) {
         double percChancex = Mth.clamp((windEffect - 200.0) / 30.0, 0.0, 1.0);
         if (Atmospherica.RANDOM.nextFloat() <= percChancex * (0.02F * percAdj)) {
            this.level.setBlockAndUpdate(blockPosTop, ((Block)ModBlocks.HEAVY_SCOURING.get()).defaultBlockState());
         }
      } else {
         Block block = state.getBlock();
         float blockStrengthx = getBlockStrength(block, this.level, blockPosTop);
         if (state.getBlock().toString().contains("stripped")) {
            blockStrengthx *= 2.0F;
         }

         if (ServerConfig.blockStrengths.containsKey(block)) {
            blockStrengthx = (Float)ServerConfig.blockStrengths.get(block);
         }

         double stretch = 35.0;
         if (state.is(BlockTags.LEAVES)) {
            stretch = 70.0;
         } else if (state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS)) {
            stretch = 50.0;
         }

         double percChancex = Mth.clamp(Math.pow(Mth.clamp(Math.max(windEffect - blockStrengthx, 0.0) / stretch, 0.0, 1.0), 4.0) + 0.02, 0.0, 1.0)
            * 0.05
            * percAdj;
         if (windEffect < blockStrengthx) {
            percChancex = 0.0;
         }

         if (block.defaultDestroyTime() < 0.05F
            && block.defaultDestroyTime() >= 0.0F
            && !ServerConfig.blacklistedBlocks.contains(block)
            && Atmospherica.RANDOM.nextFloat() <= percChancex) {
            this.level.removeBlock(blockPosTop, false);
         } else {
            boolean blacklisted = false;

            for (TagKey<Block> tagxx : ServerConfig.blacklistedBlockTags) {
               if (block.defaultBlockState().is(tagxx)) {
                  blacklisted = true;
                  break;
               }
            }

            if (windEffect >= blockStrengthx
               && block.defaultDestroyTime() > 0.0F
               && !ServerConfig.blacklistedBlocks.contains(block)
               && !blacklisted
               && state.getFluidState().isEmpty()
               && Atmospherica.RANDOM.nextFloat() <= percChancex) {
               MovingBlock movingBlock = (MovingBlock)((EntityType)ModEntities.MOVING_BLOCK.get()).create(this.level);
               if (movingBlock != null) {
                  movingBlock.setStartPos(blockPosTop);
                  movingBlock.setBlockState(state);
                  movingBlock.setPos(blockPosTop.getX(), blockPosTop.getY(), blockPosTop.getZ());
                  this.level.removeBlock(blockPosTop, false);
                  Player nearest = this.level.getNearestPlayer(blockPosTop.getX(), blockPosTop.getY(), blockPosTop.getZ(), 128.0, false);
                  if (Atmospherica.RANDOM.nextInt(Math.max(1, windfieldWidth / 10)) != 0
                     || nearest == null
                     || !(nearest.position().distanceTo(blockPosTop.getCenter()) < 128.0)) {
                     movingBlock.discard();
                     ((WeatherHandlerServer)this.weatherHandler).syncBlockParticleNew(blockPosTop, state, this);
                  } else if (this.level.isLoaded(blockPosTop)) {
                     this.level.addFreshEntity(movingBlock);
                  } else {
                     movingBlock.discard();
                  }
               }
            }
         }
      }
   }

   public float getRankine(double dist, int windfieldWidth) {
      float rankineWidth = windfieldWidth / this.rankineFactor;
      float perc = 0.0F;
      if (dist <= rankineWidth / 2.0F) {
         perc = (float)dist / (rankineWidth / 2.0F);
      } else if (dist <= windfieldWidth * 2.0F) {
         perc = Mth.clamp((float)Math.pow(1.0 - (dist - rankineWidth / 2.0F) / ((windfieldWidth * 2.0F - rankineWidth) / 2.0F), 1.5), 0.0F, 1.0F);
      }

      if (Float.isNaN(perc)) {
         perc = 0.0F;
      }

      return perc;
   }

   public float getWind(Vec3 pos) {
      int windfieldWidth = Math.max((int)this.width, 40);
      double dist = this.position.multiply(1.0, 0.0, 1.0).distanceTo(pos.multiply(1.0, 0.0, 1.0));
      float perc = this.getRankine(dist, windfieldWidth);
      float affectPerc = (float)Math.sqrt(1.0 - dist / (windfieldWidth * 2.0F));
      Vec3 relativePos = pos.subtract(this.position);
      Vec3 rotational = new Vec3(relativePos.z, 0.0, -relativePos.x).normalize();
      Vec3 rPosNoise = this.rotateV3(relativePos, this.tickCount / 60.0);
      double wNoise = this.FBM(new Vec3(rPosNoise.x / 100.0, rPosNoise.z / 100.0, this.tickCount / 200.0), 5, 2.0F, 0.5F, 1.0F);
      double realWind = this.windspeed * (1.0 + wNoise * 0.1);
      Vec3 motion = rotational.multiply(realWind * perc, 0.0, realWind * perc);
      motion = motion.add(this.velocity.multiply(15.0F * affectPerc, 0.0, 15.0F * affectPerc));

      for (Vorticy vorticy : this.vorticies) {
         double d = vorticy.getPosition().multiply(1.0, 0.0, 1.0).distanceTo(pos.multiply(1.0, 0.0, 1.0));
         Vec3 rPos = pos.subtract(vorticy.getPosition());
         Vec3 rot = new Vec3(rPos.z, 0.0, -rPos.x).normalize();
         int windWid = (int)(windfieldWidth * vorticy.widthPerc);
         float p = this.getRankine(d, windWid);
         float wind = vorticy.windspeedMult * this.windspeed;
         motion = motion.add(rot.multiply(wind * p, 0.0, wind * p));
      }

      return (float)motion.length();
   }

   public void initFirstTime() {
      this.ID = LastUsedStormID++;
   }

   public void pull(Particle particle, float multiplier) {
      int windfieldWidth = Math.max((int)this.width, 40);
      BlockPos blockPos = new BlockPos((int)particle.getPos().x, (int)particle.getPos().y, (int)particle.getPos().z);
      int worldHeight = this.level.getHeightmapPos(Types.MOTION_BLOCKING, blockPos).getY();
      if (worldHeight <= blockPos.getY()) {
         double dist = particle.getPos().distanceTo(new Vec3(this.position.x, particle.getPos().y, this.position.z));
         if (!(dist > windfieldWidth)) {
            Vec3 relativePos = particle.getPos().subtract(this.position);
            double heightDifference = particle.getPos().y - this.position.y;
            if (!(Math.abs(heightDifference) > 150.0)) {
               Vec3 inward = new Vec3(-relativePos.x, 0.0, -relativePos.z).normalize();
               Vec3 rotational = new Vec3(relativePos.z, 0.0, -relativePos.x).normalize();
               double windEffect = this.getWind(particle.getPos());
               double effectStrength = Mth.clamp(windEffect / Math.max(this.windspeed, 130.0F), 0.0, 1.0) * multiplier;
               double pullFactor = 4.0;
               pullFactor -= Math.max(heightDifference, 0.0) / 100.0 * 3.0;
               pullFactor /= Math.max(this.width / 100.0F, 1.0F);
               if (dist <= this.width / (this.rankineFactor * 2.0F)) {
                  pullFactor = -1.5;
               }

               Vec3 add = inward.multiply(effectStrength * pullFactor, effectStrength * pullFactor, effectStrength * pullFactor)
                  .add(rotational.multiply(effectStrength, effectStrength, effectStrength));
               add = add.add(new Vec3(0.0, effectStrength, 0.0));
               if (particle instanceof ParticleData particleData) {
                  particleData.addVelocity(add.multiply(0.05F, 0.05F, 0.05F));
               }
            }
         }
      }
   }

   public void pull(Entity entity, float multiplier) {
      int windfieldWidth = Math.max((int)this.width, 40);
      int worldHeight = this.level.getHeightmapPos(Types.MOTION_BLOCKING, entity.blockPosition()).getY();
      if (worldHeight <= entity.blockPosition().getY()) {
         double dist = entity.position().distanceTo(new Vec3(this.position.x, entity.position().y, this.position.z));
         if (!(dist > windfieldWidth)) {
            Vec3 relativePos = entity.position().subtract(this.position);
            double heightDifference = entity.position().y - this.position.y;
            if (!(Math.abs(heightDifference) > 150.0)) {
               Vec3 inward = new Vec3(-relativePos.x, 0.0, -relativePos.z).normalize();
               Vec3 rotational = new Vec3(relativePos.z, 0.0, -relativePos.x).normalize();
               double windEffect = this.getWind(entity.position());
               if (!(windEffect < 60.0)) {
                  double effectStrength = Mth.clamp((windEffect - 60.0) / Math.max(this.windspeed * 1.2F, 130.0F), 0.0, 1.0) * multiplier * 1.5;
                  double pullFactor = 4.0;
                  pullFactor -= Math.max(heightDifference, 0.0) / 65.0 * 3.0;
                  if (dist <= this.width / this.rankineFactor) {
                     pullFactor = -1.5;
                  }

                  Vec3 add = inward.multiply(effectStrength * pullFactor, effectStrength * pullFactor, effectStrength * pullFactor)
                     .add(rotational.multiply(effectStrength, effectStrength, effectStrength));
                  add = add.add(new Vec3(0.0, effectStrength, 0.0));
                  entity.addDeltaMovement(add.multiply(0.05F, 0.05F, 0.05F));
                  Vec3 motion = entity.getDeltaMovement();
                  if (motion.y > -0.25) {
                     entity.fallDistance = 0.0F;
                  }
               }
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public void tickClient() {
      Player player = Minecraft.getInstance().player;
      if (player != null && (this.undergroundWind == null || this.undergroundWind.isStopped()) && !this.dead) {
         this.undergroundWind = new MovingSoundStreamingSource(
            this, ModSounds.UNDERGROUND_WIND.get(), SoundSource.WEATHER, 0.1F, 1.0F, this.maxWidth, true, 3
         );
         Minecraft.getInstance().getSoundManager().play(this.undergroundWind);
      }

      if (player != null && this.stormType == 2) {
         this.smoothWidth = this.width;
         this.smoothWindspeed = Mth.lerp(0.1F, this.smoothWindspeed, this.windspeed);
         if ((this.eyewallWind == null || this.eyewallWind.isStopped()) && !this.dead) {
            this.eyewallWind = new MovingSoundStreamingSource(
               this, ModSounds.EYEWALL_WIND.get(), SoundSource.WEATHER, 0.1F, 1.0F, this.maxWidth, true, 2
            );
            Minecraft.getInstance().getSoundManager().play(this.eyewallWind);
         }
      }

      if (player != null && this.stormType == 0) {
         this.smoothWindspeed = Mth.lerp(0.1F, this.smoothWindspeed, this.windspeed);
         this.smoothWidth = Mth.lerp(0.05F, this.smoothWidth, this.width);
         if (this.stage >= 3) {
            if ((this.tornadicWind == null || this.tornadicWind.isStopped()) && !this.dead) {
               this.tornadicWind = new MovingSoundStreamingSource(
                  this, ModSounds.TORNADIC_WIND.get(), SoundSource.WEATHER, 0.1F, 1.0F, this.width, true, 1
               );
               Minecraft.getInstance().getSoundManager().play(this.tornadicWind);
            }

            if ((this.tornadicDamage == null || this.tornadicDamage.isStopped()) && !this.dead) {
               this.tornadicDamage = new MovingSoundStreamingSource(
                  this, ModSounds.TORNADIC_DAMAGE.get(), SoundSource.WEATHER, 0.1F, 1.0F, this.width, true, 4
               );
               Minecraft.getInstance().getSoundManager().play(this.tornadicDamage);
            }

            if (this.windspeed >= 40 && !player.isCreative() && !player.isSpectator()) {
               this.pull(player, 2.5F);
            }
         }

         if (this.stage >= 2 && (this.supercellWind == null || this.supercellWind.isStopped()) && !this.dead) {
            this.supercellWind = new MovingSoundStreamingSource(
               this, ModSounds.SUPERCELL_WIND.get(), SoundSource.WEATHER, 0.1F, 1.0F, this.width, true, 0
            );
            Minecraft.getInstance().getSoundManager().play(this.supercellWind);
         }

         if (this.stage < 3 && this.tornadicWind != null) {
            this.tornadicWind.stopPlaying();
            this.tornadicWind = null;
         }

         if (this.stage < 2 && this.supercellWind != null) {
            this.supercellWind.stopPlaying();
            this.supercellWind = null;
         }

         for (int i = 0; i < this.listParticleDebris.size(); i++) {
            EntityRotFX debris = (EntityRotFX)this.listParticleDebris.get(i);
            if (!debris.isAlive()) {
               this.listParticleDebris.remove(debris);
            } else {
               this.pull(debris, 1.0F);
            }
         }
      }
   }

   public void remove() {
      this.dead = true;
      if (EffectiveSide.get().equals(LogicalSide.CLIENT)) {
         this.cleanupClient();
      }

      this.cleanup();
   }

   public void cleanup() {
      this.weatherHandler = null;
      if (!this.level.isClientSide()) {
         for (ChunkPos chunkPos : this.forceLoadedChunks) {
            ((ServerLevel)this.level).setChunkForced(chunkPos.x, chunkPos.z, false);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public void cleanupClient() {
      if (this.tornadicWind != null) {
         this.tornadicWind.stopPlaying();
         this.tornadicWind = null;
      }

      if (this.tornadicDamage != null) {
         this.tornadicDamage.stopPlaying();
         this.tornadicDamage = null;
      }

      if (this.supercellWind != null) {
         this.supercellWind.stopPlaying();
         this.supercellWind = null;
      }

      if (this.eyewallWind != null) {
         this.eyewallWind.stopPlaying();
         this.eyewallWind = null;
      }

      if (this.undergroundWind != null) {
         this.undergroundWind.stopPlaying();
         this.undergroundWind = null;
      }
   }

   public void read() {
      this.nbtSyncFromServer();
   }

   public void write() {
      this.nbtSyncForClient();
   }

   public int getUpdateRate() {
      return this.stormType == 0 && this.stage >= 3 ? 2 : 40;
   }

   public void nbtSyncFromServer() {
      CachedNBTTagCompound nbt = this.getNBTCache();
      this.ID = nbt.getLong("ID");
      this.onWater = nbt.getBoolean("onWater");
      this.position = new Vec3(nbt.getDouble("positionX"), nbt.getDouble("positionY"), nbt.getDouble("positionZ"));
      this.velocity = new Vec3(nbt.getDouble("velocityX"), nbt.getDouble("velocityY"), nbt.getDouble("velocityZ"));
      this.windspeed = nbt.getInt("windspeed");
      this.cycloneWindspeed = this.windspeed;
      this.width = nbt.getFloat("width");
      this.energy = nbt.getInt("energy");
      this.coldEnergy = nbt.getInt("coldEnergy");
      this.stormType = nbt.getInt("stormType");
      this.stage = nbt.getInt("stage");
      this.dead = nbt.getBoolean("dead");
      this.isDying = nbt.getBoolean("isDying");
      this.maxWidth = nbt.getInt("maxWidth");
      this.maxWindspeed = nbt.getInt("maxWindspeed");
      this.maxStage = nbt.getInt("maxStage");
      this.maxProgress = nbt.getInt("maxProgress");
      this.ticksSinceDying = nbt.getInt("ticksSinceDying");
      this.growthSpeed = nbt.getInt("growthSpeed");
      this.visualOnly = nbt.getBoolean("visualOnly");
      this.aimedAtPlayer = nbt.getBoolean("aimedAtPlayer");
      this.cirus = nbt.getBoolean("cirus");
      this.touchdownSpeed = nbt.getInt("touchdownSpeed");
      this.occlusion = nbt.getFloat("occlusion");
      CompoundTag vorticiesData = nbt.get("vorticies");
      int vorticyCount = vorticiesData.getInt("vorticyCount");
      this.vorticies.clear();

      for (int i = 0; i < vorticyCount; i++) {
         CompoundTag vorticyData = vorticiesData.getCompound("vorticy" + i);
         Vorticy vorticy = new Vorticy(
            this,
            vorticyData.getFloat("maxWindspeedMult"),
            vorticyData.getFloat("widthPerc"),
            vorticyData.getFloat("distancePerc"),
            vorticyData.getInt("lifetime")
         );
         vorticy.dead = vorticyData.getBoolean("dead");
         vorticy.angle = vorticyData.getFloat("angle");
         vorticy.tickCount = vorticyData.getInt("tickCount");
         vorticy.windspeedMult = vorticyData.getFloat("windspeedMult");
         this.vorticies.add(vorticy);
      }
   }

   public void nbtSyncForClient() {
      CachedNBTTagCompound nbt = this.getNBTCache();
      CompoundTag vorticiesData = new CompoundTag();
      vorticiesData.putInt("vorticyCount", this.vorticies.size());

      for (int i = 0; i < this.vorticies.size(); i++) {
         Vorticy vorticy = (Vorticy)this.vorticies.get(i);
         CompoundTag vorticyData = new CompoundTag();
         vorticyData.putBoolean("dead", vorticy.dead);
         vorticyData.putFloat("windspeedMult", vorticy.windspeedMult);
         vorticyData.putFloat("maxWindspeedMult", vorticy.maxWindspeedMult);
         vorticyData.putFloat("widthPerc", vorticy.widthPerc);
         vorticyData.putFloat("distancePerc", vorticy.distancePerc);
         vorticyData.putFloat("angle", vorticy.angle);
         vorticyData.putInt("lifetime", vorticy.lifetime);
         vorticyData.putInt("tickCount", vorticy.tickCount);
         vorticiesData.put("vorticy" + i, vorticyData);
      }

      nbt.put("vorticies", vorticiesData);
      nbt.putBoolean("onWater", this.onWater);
      nbt.putInt("touchdownSpeed", this.touchdownSpeed);
      nbt.putBoolean("cirus", this.cirus);
      nbt.putBoolean("aimedAtPlayer", this.aimedAtPlayer);
      nbt.putBoolean("visualOnly", this.visualOnly);
      nbt.putBoolean("isDying", this.isDying);
      nbt.putInt("maxWidth", this.maxWidth);
      nbt.putInt("maxWindspeed", this.maxWindspeed);
      nbt.putInt("maxStage", this.maxStage);
      nbt.putInt("maxProgress", this.maxProgress);
      nbt.putInt("ticksSinceDying", this.ticksSinceDying);
      nbt.putInt("growthSpeed", this.growthSpeed);
      nbt.putFloat("occlusion", this.occlusion);
      nbt.putDouble("positionX", this.position.x);
      nbt.putDouble("positionY", this.position.y);
      nbt.putDouble("positionZ", this.position.z);
      nbt.putDouble("velocityX", this.velocity.x);
      nbt.putDouble("velocityY", this.velocity.y);
      nbt.putDouble("velocityZ", this.velocity.z);
      nbt.putLong("ID", this.ID);
      nbt.getNewNBT().putLong("ID", this.ID);
      nbt.putInt("windspeed", this.windspeed);
      nbt.putFloat("width", this.width);
      nbt.putInt("energy", this.energy);
      nbt.putInt("coldEnergy", this.coldEnergy);
      nbt.putInt("stormType", this.stormType);
      nbt.putInt("stage", this.stage);
      nbt.putBoolean("dead", this.dead);
   }

   public CachedNBTTagCompound getNBTCache() {
      return this.nbtCache;
   }

   public static float getBlockStrength(Block block, Level level, @Nullable BlockPos blockPos) {
      ItemStack item = new ItemStack(Items.IRON_AXE);
      float destroySpeed = block.defaultBlockState().getDestroySpeed(level, blockPos != null ? blockPos : BlockPos.ZERO);

      try {
         destroySpeed /= item.getDestroySpeed(block.defaultBlockState());
      } catch (Exception var6) {
         Atmospherica.LOGGER.warn(var6.getMessage());
      }

      return 60.0F + Mth.sqrt(destroySpeed) * 60.0F;
   }
}
