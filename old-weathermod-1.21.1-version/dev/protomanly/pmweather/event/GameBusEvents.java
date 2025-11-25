package dev.protomanly.pmweather.event;

import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.command.WeatherCommands;
import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.entity.ModEntities;
import dev.protomanly.pmweather.entity.MovingBlock;
import dev.protomanly.pmweather.util.Util;
import dev.protomanly.pmweather.weather.Sounding;
import dev.protomanly.pmweather.weather.Storm;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import dev.protomanly.pmweather.weather.WeatherHandler;
import dev.protomanly.pmweather.weather.WeatherHandlerServer;
import dev.protomanly.pmweather.weather.WindEngine;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.common.Tags.Blocks;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.LevelEvent.Load;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.tick.EntityTickEvent.Post;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Pre;
import net.neoforged.neoforge.server.command.ConfigCommand;

@EventBusSubscriber(
   modid = "pmweather",
   bus = Bus.GAME
)
public class GameBusEvents {
   public static final Map<ResourceKey<Level>, WeatherHandler> MANAGERS = new Reference2ObjectOpenHashMap();
   public static final Map<String, WeatherHandler> MANAGERSLOOKUP = new HashMap<>();

   @SubscribeEvent
   public static void onEntityTick(Post event) {
      Entity entity = event.getEntity();
      Level level = entity.level();
      if (!level.isClientSide()) {
         if (entity instanceof MovingBlock) {
            Vec3 wind = WindEngine.getWind(entity.getPosition(1.0F), level, false, true, false);
            entity.addDeltaMovement(wind.multiply(0.05F, 0.0, 0.05F).multiply(0.01F, 0.0, 0.01F));
         }

         if (entity instanceof Player player && !player.isCreative() && !player.isSpectator()) {
            Vec3 wind = WindEngine.getWind(entity.getPosition(1.0F), level, false, true, false);
            if (wind.length() > 60.0) {
               double factor = Mth.lerp(Math.clamp(wind.length() / 125.0, 0.0, 1.0), 0.005, 0.02);
               float mult = 0.65F;
               if (!entity.onGround()) {
                  mult = 0.15F;
               }

               entity.addDeltaMovement(wind.multiply(0.05F, 0.0, 0.05F).multiply(factor, 0.0, factor).multiply(mult, mult, mult));
            }
         }

         if (entity instanceof LivingEntity && !(entity instanceof Player)) {
            Vec3 wind = WindEngine.getWind(entity.getPosition(1.0F), level, false, true, false);
            if (wind.length() > 60.0) {
               double factor = Mth.lerp(Math.clamp(wind.length() / 125.0, 0.0, 1.0), 0.005, 0.02);
               float mult = 0.65F;
               if (!entity.onGround()) {
                  mult = 0.15F;
               }

               entity.addDeltaMovement(wind.multiply(0.05F, 0.0, 0.05F).multiply(factor, 0.0, factor).multiply(mult, mult, mult));
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLevelTick(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event) {
      Level level = event.getLevel();
      if (!level.isClientSide()
         && ServerConfig.validDimensions != null
         && level instanceof ServerLevel serverLevel
         && ServerConfig.validDimensions.contains(level.dimension())) {
         WeatherHandlerServer weatherHandler = (WeatherHandlerServer)MANAGERS.get(level.dimension());
         if (PMWeather.RANDOM.nextInt(2) == 0) {
            List<ServerPlayer> validPlayers = new ArrayList<>();
            List<ServerPlayer> plrs = serverLevel.players();
            Collections.shuffle(plrs);

            for (ServerPlayer player : plrs) {
               boolean isTooNear = false;

               for (ServerPlayer existing : validPlayers) {
                  if (existing.distanceTo(player) <= 64.0F) {
                     isTooNear = true;
                     break;
                  }
               }

               if (!isTooNear) {
                  validPlayers.add(player);
               }
            }

            for (ServerPlayer player : validPlayers) {
               for (int i = 0; i < 260; i++) {
                  BlockPos check = player.blockPosition().offset(new Vec3i(PMWeather.RANDOM.nextInt(-64, 65), 50, PMWeather.RANDOM.nextInt(-64, 65)));
                  check = level.getHeightmapPos(Types.MOTION_BLOCKING, check).below();
                  float wind = (float)WindEngine.getWind(new Vec3(check.getX(), check.getY(), check.getZ()), level, false, true, false, true).length();
                  float chance = wind / 140.0F;
                  if (wind > 45.0F && PMWeather.RANDOM.nextFloat() <= chance) {
                     check = check.below(PMWeather.RANDOM.nextInt(3));
                     BlockState state = level.getBlockState(check);
                     Block block = state.getBlock();
                     float blockStrength = Storm.getBlockStrength(block, level, check);
                     if (ServerConfig.blockStrengths.containsKey(block)) {
                        blockStrength = ServerConfig.blockStrengths.get(block);
                     }

                     blockStrength *= 0.9F;
                     boolean blacklisted = false;

                     for (TagKey<Block> tag : ServerConfig.blacklistedBlockTags) {
                        if (block.defaultBlockState().is(tag)) {
                           blacklisted = true;
                           break;
                        }
                     }

                     if (!blacklisted && !ServerConfig.blacklistedBlocks.contains(block) && Util.canWindAffect(check.getCenter(), level)) {
                        if (!state.is(Blocks.GLASS_BLOCKS) && !state.is(Blocks.GLASS_PANES)) {
                           double percChance = Math.clamp(
                              Math.pow(Math.clamp(Math.max((double)(wind - blockStrength), 0.0) / 20.0, 0.0, 1.0), 2.0) + 0.02, 0.0, 1.0
                           );
                           if (wind < blockStrength) {
                              percChance = 0.0;
                           }

                           if (block.defaultDestroyTime() < 0.05F
                              && block.defaultDestroyTime() >= 0.0F
                              && PMWeather.RANDOM.nextFloat() <= percChance
                              && Util.canWindAffect(check.getCenter(), level)) {
                              level.removeBlock(check, false);
                           } else if (PMWeather.RANDOM.nextFloat() <= percChance && Util.canWindAffect(check.getCenter(), level)) {
                              MovingBlock movingBlock = (MovingBlock)ModEntities.MOVING_BLOCK.get().create(level);
                              if (movingBlock != null) {
                                 movingBlock.setStartPos(check);
                                 movingBlock.setBlockState(state);
                                 movingBlock.setPos(check.getX(), check.getY(), check.getZ());
                                 level.removeBlock(check, false);
                                 if (level.isLoaded(check) && PMWeather.RANDOM.nextFloat() <= Math.clamp(1.0F - chance, 0.0F, 1.0F) * 0.4F) {
                                    level.addFreshEntity(movingBlock);
                                 } else {
                                    movingBlock.discard();
                                 }
                              }
                           }
                        } else {
                           double percChancex = Math.clamp((wind - 55.0F) / 15.0F, 0.0F, 1.0F);
                           if (PMWeather.RANDOM.nextFloat() <= percChancex && Util.canWindAffect(check.getCenter(), level)) {
                              level.removeBlock(check, false);
                              level.playSound(null, check, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, PMWeather.RANDOM.nextFloat(0.8F, 1.2F));
                           }
                        }
                     }
                  }
               }
            }
         }

         int storms = weatherHandler.getStorms().size();
         int cyclones = 0;

         for (Storm storm : weatherHandler.getStorms()) {
            if (storm.stormType == 2) {
               cyclones++;
            }
         }

         if (level.getGameTime() % 1200L == 0L) {
            PMWeather.LOGGER.debug("Checking for storm/cloud spawns");
            List<ServerPlayer> validPlayers = new ArrayList<>();
            List<ServerPlayer> plrs = serverLevel.players();
            Collections.shuffle(plrs);

            for (ServerPlayer player : plrs) {
               boolean isTooNear = false;

               for (ServerPlayer existingx : validPlayers) {
                  if (existingx.distanceTo(player) <= ServerConfig.spawnRange / 2.0F) {
                     isTooNear = true;
                     break;
                  }
               }

               if (!isTooNear) {
                  validPlayers.add(player);
               }
            }

            PMWeather.LOGGER.debug("{} players available to spawn around", validPlayers.size());

            for (ServerPlayer player : validPlayers) {
               Vec3 pos = new Vec3(player.getX(), level.getMaxBuildHeight(), player.getZ())
                  .add(
                     PMWeather.RANDOM.nextInt(-ServerConfig.spawnRange, ServerConfig.spawnRange + 1),
                     0.0,
                     PMWeather.RANDOM.nextInt(-ServerConfig.spawnRange, ServerConfig.spawnRange + 1)
                  );
               Vec3 wind = WindEngine.getWind(new Vec3(player.getX(), level.getMaxBuildHeight() + 1, player.getZ()), level, true, true, false);
               boolean squall = PMWeather.RANDOM.nextInt(ServerConfig.chanceInOneSquall) == 0 && ServerConfig.doSqualls;
               boolean cyclone = PMWeather.RANDOM.nextInt(ServerConfig.chanceInOneCyclone) == 0 && cyclones <= 0 && ServerConfig.doCyclones;
               if (cyclone) {
                  PMWeather.LOGGER.debug("Checking for cyclone spawn");
                  pos = Util.getValidTropicalSystemSpawn(weatherHandler, pos, ServerConfig.spawnRange + 4000.0F);
               } else if (squall) {
                  PMWeather.LOGGER.debug("Checking for squall spawn");
                  float dist = PMWeather.RANDOM.nextFloat(256.0F, 512.0F) * 6.0F;
                  pos = pos.add(wind.normalize().multiply(-dist, 0.0, -dist));
               } else {
                  float dist = PMWeather.RANDOM.nextFloat(256.0F, 512.0F) * 4.0F;
                  pos = pos.add(wind.normalize().multiply(-dist, 0.0, -dist));
               }

               if (pos != null) {
                  for (Storm stormx : weatherHandler.getStorms()) {
                     double dist = pos.distanceTo(stormx.position);
                     if (stormx.stormType == 2 && dist < stormx.maxWidth / 1.5F) {
                        pos = null;
                        break;
                     }
                  }
               }

               if (pos != null) {
                  PMWeather.LOGGER
                     .debug("Checking storm spawns around {} at {}, {}", new Object[]{player.getDisplayName().getString(), (int)pos.x, (int)pos.z});
               } else {
                  PMWeather.LOGGER.debug("Cyclone position check failed or position too near to cyclone");
               }

               double spawnChance = ServerConfig.stormSpawnChancePerMinute;
               Vec3 sfcPos = serverLevel.getHeightmapPos(Types.WORLD_SURFACE_WG, player.blockPosition()).getCenter();
               Sounding sounding = new Sounding(weatherHandler, sfcPos, level, 250, 16000);
               ThermodynamicEngine.AtmosphericDataPoint sfc = ThermodynamicEngine.samplePoint(weatherHandler, sfcPos, level, null, 0);
               float riskV = sounding.getRisk(0);
               if (ServerConfig.environmentSystem) {
                  if (cyclone) {
                     if (pos != null) {
                        Float sst = ThermodynamicEngine.GetSST(weatherHandler, pos, weatherHandler.getWorld(), null, 0);
                        if (sst != null) {
                           spawnChance *= Math.clamp((sst - 22.0F) / 4.0F, 0.0F, 2.0F);
                        } else {
                           spawnChance = 0.0;
                        }
                     } else {
                        spawnChance = 0.0;
                     }
                  } else if (squall) {
                     spawnChance *= Math.pow(riskV, 0.75) + 0.035F;
                  } else {
                     spawnChance *= Math.pow(riskV, 0.75) * 1.25 + 0.02;
                  }

                  if (sfc.temperature() < 3.0F) {
                     spawnChance += Math.clamp((sfc.temperature() - 3.0F) / -6.0F, 0.0F, 1.0F) * 0.035F;
                  }

                  PMWeather.LOGGER.debug("W/ spawn chance: {}%\nRisk: {}", (int)(spawnChance * 100.0), (int)(riskV * 100.0F));
               }

               if (PMWeather.RANDOM.nextFloat() <= spawnChance && storms < ServerConfig.maxStorms && pos != null) {
                  if (cyclone) {
                     Storm stormxx = new Storm(weatherHandler, level, riskV, 2);
                     stormxx.width = PMWeather.RANDOM.nextFloat(7500.0F, 16000.0F);
                     stormxx.windspeed = 0;
                     stormxx.stormType = 2;
                     stormxx.position = pos;
                     stormxx.velocity = Vec3.ZERO;
                     stormxx.initFirstTime();
                     stormxx.maxWindspeed = 0;
                     stormxx.maxWidth = Math.round(stormxx.width);
                     weatherHandler.addStorm(stormxx);
                     weatherHandler.syncStormNew(stormxx);
                     storms++;
                     cyclones++;
                  } else if (squall) {
                     if (sfc.temperature() < 3.0F) {
                        riskV += Math.clamp((sfc.temperature() - 3.0F) / -6.0F, 0.0F, 1.0F) * 0.25F;
                     }

                     Storm stormxx = new Storm(weatherHandler, level, riskV, 1);
                     stormxx.width = 0.0F;
                     stormxx.windspeed = 0;
                     stormxx.stormType = 1;
                     stormxx.stage = 0;
                     if (ServerConfig.environmentSystem) {
                        if (PMWeather.RANDOM.nextFloat() <= riskV * 2.5F) {
                           stormxx.maxStage = Math.max(stormxx.maxStage, 1);
                        }

                        if (PMWeather.RANDOM.nextFloat() <= riskV * 2.0F) {
                           stormxx.maxStage = Math.max(stormxx.maxStage, 2);
                        }

                        if (PMWeather.RANDOM.nextFloat() <= riskV * 1.5F) {
                           stormxx.maxStage = Math.max(stormxx.maxStage, 3);
                        }

                        stormxx.recalc(riskV);
                     }

                     if (wind.length() < 6.0) {
                        wind = wind.normalize().multiply(6.0, 0.0, 6.0);
                     }

                     stormxx.position = pos;
                     stormxx.velocity = wind.multiply(0.1, 0.0, 0.1);
                     stormxx.energy = 0;
                     stormxx.initFirstTime();
                     weatherHandler.addStorm(stormxx);
                     weatherHandler.syncStormNew(stormxx);
                     storms++;
                  } else {
                     Storm stormxxx = new Storm(weatherHandler, level, riskV, 0);
                     stormxxx.width = 0.0F;
                     stormxxx.windspeed = 0;
                     stormxxx.stormType = 0;
                     stormxxx.stage = 0;
                     if (ServerConfig.environmentSystem) {
                        if (PMWeather.RANDOM.nextFloat() <= riskV * 2.5F) {
                           stormxxx.maxStage = Math.max(stormxxx.maxStage, 1);
                        }

                        if (PMWeather.RANDOM.nextFloat() <= riskV * 2.0F) {
                           stormxxx.maxStage = Math.max(stormxxx.maxStage, 2);
                        }

                        if (PMWeather.RANDOM.nextFloat() <= riskV * 1.5F) {
                           stormxxx.maxStage = Math.max(stormxxx.maxStage, 3);
                        }

                        stormxxx.recalc(riskV);
                     }

                     stormxxx.position = pos;
                     stormxxx.velocity = Vec3.ZERO;
                     stormxxx.energy = 0;
                     stormxxx.initFirstTime();
                     if (!ServerConfig.doTornadoes && stormxxx.maxStage >= 3) {
                        stormxxx.maxStage = 2;
                     }

                     weatherHandler.addStorm(stormxxx);
                     weatherHandler.syncStormNew(stormxxx);
                     storms++;
                  }

                  PMWeather.LOGGER.debug("Spawned storm at {}, {}", (int)pos.x, (int)pos.z);
               } else {
                  PMWeather.LOGGER.debug("Storm spawn failed, rolled bad number or too many storms");
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLevelLoad(Load event) {
      LevelAccessor level = event.getLevel();
      if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
         ResourceKey<Level> dimension = serverLevel.dimension();
         WeatherHandler weatherHandler = new WeatherHandlerServer(serverLevel);
         weatherHandler.read();
         MANAGERS.put(dimension, weatherHandler);
         MANAGERSLOOKUP.put(dimension.location().toString(), weatherHandler);
         if (WindEngine.simplexNoise == null) {
            WindEngine.init(weatherHandler);
         }
      }
   }

   @SubscribeEvent
   public static void onLevelUnload(Unload event) {
      LevelAccessor level = event.getLevel();
      if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
         MANAGERS.remove(serverLevel.dimension());
         MANAGERSLOOKUP.remove(serverLevel.dimension().toString());
      }
   }

   @SubscribeEvent
   public static void onServerTick(Pre event) {
      if (!event.getServer().isPaused()) {
         for (WeatherHandler weatherHandler : MANAGERS.values()) {
            weatherHandler.tick();
         }
      }
   }

   @SubscribeEvent
   public static void onCommandsRegister(RegisterCommandsEvent event) {
      new WeatherCommands(event.getDispatcher(), event.getBuildContext());
      ConfigCommand.register(event.getDispatcher());
   }

   public static void playerRequestsFullSync(ServerPlayer player) {
      WeatherHandler weatherHandler = MANAGERS.get(player.level().dimension());
      if (weatherHandler instanceof WeatherHandlerServer weatherHandlerServer) {
         weatherHandlerServer.playerJoinedWorldSyncFull(player);
      }
   }
}
