package dev.dabrelity.atmospherica.block.entity;

import dev.dabrelity.atmospherica.Atmospherica;
import dev.dabrelity.atmospherica.block.ModBlocks;
import dev.dabrelity.atmospherica.event.GameBusClientEvents;
import dev.dabrelity.atmospherica.networking.ModNetworking;
import dev.dabrelity.atmospherica.weather.Storm;
import dev.dabrelity.atmospherica.weather.WeatherHandlerClient;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public class RadarBlockEntity extends BlockEntity {
   public List<Storm> storms = new ArrayList();
   public int tickCount;
   public int updateCount;
   public SimplexNoise noise;
   // Use primitive collections to avoid Long/Float boxing overhead
   public Long2FloatOpenHashMap reflectivityMap = new Long2FloatOpenHashMap();
   public Long2FloatOpenHashMap temperatureMap = new Long2FloatOpenHashMap();
   public Long2FloatOpenHashMap velocityMap = new Long2FloatOpenHashMap();
   public Long2ObjectOpenHashMap<Color> debugMap = new Long2ObjectOpenHashMap<>();
   public Long2ObjectOpenHashMap<Color> terrainMap = new Long2ObjectOpenHashMap<>();
   public List<RadarBlockEntity.BiomeData> biomeData = new ArrayList();
   public boolean init = false;
   public int lastUpdate = 0;
   public int ticksNoPacket = 0;
   public boolean hasRangeUpgrade = false;
   public Map<BlockPos, Holder<Biome>> biomeCache = new HashMap();
   
   // Spread initialization over multiple ticks to avoid lag spike
   private int initPhase = 0;
   private int initX = -2048;
   private int initZ = -2048;
   private static final int SAMPLES_PER_TICK = 64; // Process 64 biome samples per tick

   public RadarBlockEntity(BlockPos pos, BlockState blockState) {
      super((BlockEntityType)ModBlockEntities.RADAR_BE.get(), pos, blockState);
      this.noise = new SimplexNoise(new LegacyRandomSource(0L));
   }

   @Nullable
   public Holder<Biome> getNearestBiome(BlockPos pos) {
      double nearest = Double.MAX_VALUE;
      Holder<Biome> biome = null;
      if (this.biomeCache.containsKey(pos.atY(0))) {
         return (Holder<Biome>)this.biomeCache.get(pos.atY(0));
      } else {
         for (RadarBlockEntity.BiomeData bData : this.biomeData) {
            double dist = pos.distManhattan(bData.pos);
            if (dist < nearest) {
               nearest = dist;
               biome = bData.biome;
            }

            if (dist < 128.0) {
               break;
            }
         }

         if (biome != null) {
            this.biomeCache.put(pos.atY(0), biome);
         }

         return biome;
      }
   }

   public void clientInit(Level level, CompoundTag data) {
      if (!this.init) {
         Atmospherica.LOGGER.debug("Radar data received");
         this.init = true;
         CompoundTag list = data.getCompound("data");

         for (String key : list.getAllKeys()) {
            CompoundTag element = list.getCompound(key);
            BlockPos blockPos = NbtUtils.readBlockPos(element.getCompound("blockPos"));
            Holder<Biome> biome = level.registryAccess()
               .registryOrThrow(Registries.BIOME)
               .getHolderOrThrow(ResourceKey.create(Registries.BIOME, new ResourceLocation(element.getString("biome"))));
            this.biomeData.add(new RadarBlockEntity.BiomeData(blockPos, biome));
         }
      }
   }

   // Track sync progress for chunked syncing
   private int syncIndex = 0;
   private boolean needsSync = false;
   private static final int SYNC_CHUNK_SIZE = 256; // Send 256 biome entries per packet

   public void sync(@Nullable Player player, BlockPos blockPos) {
      if (this.init) {
         // Start chunked sync process
         this.syncIndex = 0;
         this.needsSync = true;
         syncChunk(player, blockPos);
      }
   }
   
   private void syncChunk(@Nullable Player player, BlockPos blockPos) {
      if (!this.needsSync || this.syncIndex >= this.biomeData.size()) {
         this.needsSync = false;
         return;
      }
      
      CompoundTag data = new CompoundTag();
      data.putString("packetCommand", "Radar");
      data.putString("command", "syncBiomes");
      data.putInt("chunkIndex", this.syncIndex / SYNC_CHUNK_SIZE);
      data.putInt("totalSize", this.biomeData.size());
      data.putBoolean("isComplete", this.syncIndex + SYNC_CHUNK_SIZE >= this.biomeData.size());
      
      CompoundTag map = new CompoundTag();
      int endIndex = Math.min(this.syncIndex + SYNC_CHUNK_SIZE, this.biomeData.size());
      
      for (int i = this.syncIndex; i < endIndex; i++) {
         RadarBlockEntity.BiomeData bData = this.biomeData.get(i);
         CompoundTag element = new CompoundTag();
         element.put("blockPos", NbtUtils.writeBlockPos(bData.pos()));
         element.putString("biome", bData.biome.unwrapKey().map(k -> k.location().toString()).orElse("minecraft:plains"));
         map.put(String.valueOf(i - this.syncIndex), element);
      }

      data.put("data", map);
      data.put("blockPos", NbtUtils.writeBlockPos(blockPos));
      
      if (player == null) {
         ModNetworking.serverSendToClientDimension(data, this.level);
      } else {
         ModNetworking.serverSendToClientPlayer(data, player);
      }
      
      this.syncIndex = endIndex;
      
      // Schedule next chunk if not complete
      if (this.syncIndex < this.biomeData.size()) {
         // Will be sent on next tick
      } else {
         this.needsSync = false;
         Atmospherica.LOGGER.debug("Radar sync complete, sent {} biome entries", this.biomeData.size());
      }
   }

   public static void playerRequestsSync(ServerPlayer player, BlockPos blockPos) {
      Level lvl = player.level();
      if (lvl.getBlockEntity(blockPos) instanceof RadarBlockEntity radarBlockEntity) {
         radarBlockEntity.sync(player, blockPos);
      }
   }

   public void tick(Level level, BlockPos blockPos, BlockState blockState) {
      this.tickCount++;
      if (level.isClientSide() && (level.getGameTime() % 100L == 0L || this.storms.isEmpty())) {
         WeatherHandlerClient weatherHandler = (WeatherHandlerClient)GameBusClientEvents.weatherHandler;
         if (weatherHandler == null) {
            return;
         }

         this.updateCount++;
         this.storms = weatherHandler.getStorms();
      }

      if (level.isClientSide() && level.getGameTime() % 10L == 0L) {
         boolean rangeUpgrade = false;

         for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
               for (int z = -1; z <= 1; z++) {
                  BlockPos pos = blockPos.offset(x, y, z);
                  BlockState state = level.getBlockState(pos);
                  if (state.is(ModBlocks.RANGE_UPGRADE_MODULE.get())) {
                     rangeUpgrade = true;
                  }
               }
            }
         }

         this.hasRangeUpgrade = rangeUpgrade;
      }

      if (!this.init) {
         if (!level.isClientSide()) {
            // Spread biome sampling over multiple ticks to avoid lag spike
            int samplesThisTick = 0;
            
            // Phase 0: Sample at 64-block intervals
            while (initPhase == 0 && samplesThisTick < SAMPLES_PER_TICK) {
               if (initX <= 2048) {
                  BlockPos pos = blockPos.offset(new Vec3i(initX, 0, initZ));
                  Holder<Biome> biome = level.getBiome(pos);
                  this.biomeData.add(new RadarBlockEntity.BiomeData(pos, biome));
                  samplesThisTick++;
                  
                  initZ += 64;
                  if (initZ > 2048) {
                     initZ = -2048;
                     initX += 64;
                  }
               } else {
                  // Move to phase 1
                  initPhase = 1;
                  initX = -2048;
                  initZ = -2048;
               }
            }
            
            // Phase 1: Sample at 128-block intervals (scaled by 4)
            while (initPhase == 1 && samplesThisTick < SAMPLES_PER_TICK) {
               if (initX <= 2048) {
                  BlockPos pos = blockPos.offset(new Vec3i(initX * 4, 0, initZ * 4));
                  Holder<Biome> biome = level.getBiome(pos);
                  RadarBlockEntity.BiomeData data = new RadarBlockEntity.BiomeData(pos, biome);
                  if (!this.biomeData.contains(data)) {
                     this.biomeData.add(data);
                  }
                  samplesThisTick++;
                  
                  initZ += 128;
                  if (initZ > 2048) {
                     initZ = -2048;
                     initX += 128;
                  }
               } else {
                  // Initialization complete
                  this.init = true;
                  this.sync(null, blockPos);
                  Atmospherica.LOGGER.info("Radar initialization complete with {} biome samples", this.biomeData.size());
                  break;
               }
            }
         } else {
            this.ticksNoPacket++;
            if (this.ticksNoPacket > 40) {
               Atmospherica.LOGGER.debug("Requesting data from server for radar at {}", blockPos);
               this.ticksNoPacket = 0;
               CompoundTag data = new CompoundTag();
               data.putString("packetCommand", "Radar");
               data.putString("command", "syncBiomes");
               data.put("blockPos", NbtUtils.writeBlockPos(blockPos));
               ModNetworking.clientSendToSever(data);
            }
         }
      } else {
         this.ticksNoPacket = 0;
         
         // Continue chunked sync if needed (server side only)
         if (!level.isClientSide() && this.needsSync) {
            syncChunk(null, blockPos);
         }
      }
   }

   public record BiomeData(BlockPos pos, Holder<Biome> biome) {
   }
}
