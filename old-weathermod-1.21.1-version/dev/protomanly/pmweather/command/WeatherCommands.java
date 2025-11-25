package dev.protomanly.pmweather.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.event.GameBusEvents;
import dev.protomanly.pmweather.util.Util;
import dev.protomanly.pmweather.weather.Sounding;
import dev.protomanly.pmweather.weather.Storm;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import dev.protomanly.pmweather.weather.WeatherHandlerServer;
import dev.protomanly.pmweather.weather.WindEngine;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;

public class WeatherCommands {
   public WeatherCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("pmweather")
            .then(
               ((LiteralArgumentBuilder)Commands.literal("report").requires(plr -> plr.hasPermission(2)))
                  .then(Commands.literal("generate").executes(this::generateReport))
            )
      );
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("pmweather")
            .then(
               ((LiteralArgumentBuilder)Commands.literal("atmosphere").requires(plr -> plr.hasPermission(2)))
                  .then(Commands.literal("sample").then(Commands.literal("column").executes(this::sampleColumn)))
            )
      );
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("pmweather")
            .then(
               ((LiteralArgumentBuilder)Commands.literal("spawn").requires(plr -> plr.hasPermission(2)))
                  .then(
                     Commands.literal("tornado")
                        .then(
                           Commands.argument("windspeed", IntegerArgumentType.integer(0, 400))
                              .then(Commands.argument("width", IntegerArgumentType.integer(5, 1000)).executes(this::newTornado))
                        )
                  )
            )
      );
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("pmweather")
            .then(
               ((LiteralArgumentBuilder)Commands.literal("spawn").requires(plr -> plr.hasPermission(2)))
                  .then(
                     Commands.literal("cyclone")
                        .then(
                           Commands.argument("windspeed", IntegerArgumentType.integer(0, 250))
                              .then(Commands.argument("width", IntegerArgumentType.integer(3000, 16000)).executes(this::newCyclone))
                        )
                  )
            )
      );
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("pmweather")
            .then(
               ((LiteralArgumentBuilder)Commands.literal("spawn").requires(plr -> plr.hasPermission(2)))
                  .then(Commands.literal("cyclone").then(Commands.literal("natural").executes(this::naturalCyclone)))
            )
      );
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("pmweather")
            .then(
               ((LiteralArgumentBuilder)Commands.literal("spawn").requires(plr -> plr.hasPermission(2)))
                  .then(
                     Commands.literal("tornado")
                        .then(
                           Commands.literal("buildto")
                              .then(
                                 Commands.argument("fromStage", IntegerArgumentType.integer(0, 2))
                                    .then(
                                       Commands.argument("fromEnergy", IntegerArgumentType.integer(0, 99))
                                          .then(
                                             Commands.argument("windspeed", IntegerArgumentType.integer(0, 400))
                                                .then(Commands.argument("width", IntegerArgumentType.integer(5, 1000)).executes(this::buildTornado))
                                          )
                                    )
                              )
                        )
                  )
            )
      );
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("pmweather")
            .then(((LiteralArgumentBuilder)Commands.literal("strike").requires(plr -> plr.hasPermission(2))).executes(this::strike))
      );
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("pmweather")
            .then(
               ((LiteralArgumentBuilder)Commands.literal("spawn").requires(plr -> plr.hasPermission(2)))
                  .then(
                     Commands.literal("supercell")
                        .then(
                           Commands.argument("stage", IntegerArgumentType.integer(0, 2))
                              .then(Commands.argument("energy", IntegerArgumentType.integer(1, 99)).executes(this::newStorm))
                        )
                  )
            )
      );
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("pmweather")
            .then(
               ((LiteralArgumentBuilder)Commands.literal("spawn").requires(plr -> plr.hasPermission(2)))
                  .then(
                     Commands.literal("supercell")
                        .then(
                           Commands.literal("buildto")
                              .then(
                                 Commands.argument("stage", IntegerArgumentType.integer(0, 2))
                                    .then(Commands.argument("energy", IntegerArgumentType.integer(1, 99)).executes(this::buildStorm))
                              )
                        )
                  )
            )
      );
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("pmweather")
            .then(
               ((LiteralArgumentBuilder)Commands.literal("spawn").requires(plr -> plr.hasPermission(2)))
                  .then(
                     Commands.literal("squall")
                        .then(
                           Commands.argument("stage", IntegerArgumentType.integer(0, 3))
                              .then(Commands.argument("energy", IntegerArgumentType.integer(1, 99)).executes(this::newSquall))
                        )
                  )
            )
      );
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("pmweather")
            .then(
               ((LiteralArgumentBuilder)Commands.literal("spawn").requires(plr -> plr.hasPermission(2)))
                  .then(
                     Commands.literal("squall")
                        .then(
                           Commands.literal("buildto")
                              .then(
                                 Commands.argument("stage", IntegerArgumentType.integer(0, 3))
                                    .then(Commands.argument("energy", IntegerArgumentType.integer(1, 99)).executes(this::buildSquall))
                              )
                        )
                  )
            )
      );
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("pmweather")
            .then(
               ((LiteralArgumentBuilder)Commands.literal("spawn").requires(plr -> plr.hasPermission(2)))
                  .then(Commands.literal("supercell").then(Commands.literal("natural").executes(this::naturalStorm)))
            )
      );
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("pmweather")
            .then(
               ((LiteralArgumentBuilder)Commands.literal("spawn").requires(plr -> plr.hasPermission(2)))
                  .then(Commands.literal("squall").then(Commands.literal("natural").executes(this::naturalSquall)))
            )
      );
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("pmweather")
            .then(
               ((LiteralArgumentBuilder)Commands.literal("clear").requires(plr -> plr.hasPermission(2))).then(Commands.literal("all").executes(this::clearAll))
            )
      );
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("pmweather")
            .then(
               ((LiteralArgumentBuilder)Commands.literal("aimtoplayer").requires(plr -> plr.hasPermission(2)))
                  .then(Commands.literal("all").executes(this::aimToPlayer))
            )
      );
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("pmweather")
            .then(Commands.literal("blockstrength").then(Commands.argument("block", ItemArgument.item(context)).executes(this::blockStrength)))
      );
   }

   private int generateReport(CommandContext<CommandSourceStack> context) {
      ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
      WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)GameBusEvents.MANAGERS.get(level.dimension());
      ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
      if (player == null) {
         return -1;
      } else {
         StringBuilder str = new StringBuilder();

         for (Storm storm : weatherHandlerServer.getStorms()) {
            str.append("@").append(storm.position.toString());
            str.append("\n");
            str.append("Type: ").append(storm.stormType).append("\n");
            str.append("Windspeed: ").append(storm.windspeed).append("\n");
            str.append("MaxWidth: ").append(storm.maxWidth);
            str.append("\n");
         }

         player.sendSystemMessage(Component.literal(str.toString()));
         return 1;
      }
   }

   private int sampleColumn(CommandContext<CommandSourceStack> context) {
      ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
      WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)GameBusEvents.MANAGERS.get(level.dimension());
      ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
      if (player != null) {
         Sounding sounding = new Sounding(weatherHandlerServer, player.position(), level, 1000, 16000);
         player.sendSystemMessage(Component.literal(sounding.toString()));
         return 1;
      } else {
         return -1;
      }
   }

   private int blockStrength(CommandContext<CommandSourceStack> context) {
      if (ItemArgument.getItem(context, "block").getItem() instanceof BlockItem blockItem) {
         Block block = blockItem.getBlock();
         float strength;
         if (ServerConfig.blockStrengths.containsKey(block)) {
            strength = ServerConfig.blockStrengths.get(block);
         } else {
            strength = Storm.getBlockStrength(block, ((CommandSourceStack)context.getSource()).getLevel(), null);
         }

         if (((CommandSourceStack)context.getSource()).isPlayer()) {
            ((CommandSourceStack)context.getSource())
               .getPlayer()
               .sendSystemMessage(Component.literal(String.format("%s Strength: Damaged at %s MPH", block.getName().getString(), Math.floor(strength))));
         }

         return 1;
      } else {
         ((CommandSourceStack)context.getSource()).sendFailure(Component.literal("Failed to get block from item, is item not a block?"));
         return -1;
      }
   }

   private int aimToPlayer(CommandContext<CommandSourceStack> context) {
      ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
      WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)GameBusEvents.MANAGERS.get(level.dimension());

      for (Storm storm : weatherHandlerServer.getStorms()) {
         storm.aimAtPlayer();
      }

      ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("Successfully aimed all storms and clouds at players"), true);
      return 1;
   }

   private int clearAll(CommandContext<CommandSourceStack> context) {
      ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
      WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)GameBusEvents.MANAGERS.get(level.dimension());
      weatherHandlerServer.clearAllStorms();
      ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("Successfully cleared all storms"), true);
      return 1;
   }

   private int buildTornado(CommandContext<CommandSourceStack> context) {
      ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
      ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
      int windspeed = IntegerArgumentType.getInteger(context, "windspeed");
      int width = IntegerArgumentType.getInteger(context, "width");
      int fromStage = IntegerArgumentType.getInteger(context, "fromStage");
      int fromEnergy = IntegerArgumentType.getInteger(context, "fromEnergy");
      WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)GameBusEvents.MANAGERS.get(level.dimension());
      Storm storm = new Storm(weatherHandlerServer, level, null, 0);
      storm.width = 15.0F;
      storm.windspeed = 0;
      storm.stormType = 0;
      storm.stage = fromStage;
      storm.position = player.position();
      storm.velocity = Vec3.ZERO;
      storm.energy = fromEnergy;
      storm.initFirstTime();
      storm.maxStage = 3;
      storm.maxProgress = 100;
      storm.maxWindspeed = windspeed;
      storm.maxWidth = width;
      weatherHandlerServer.addStorm(storm);
      weatherHandlerServer.syncStormNew(storm);
      ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("Successfully spawned storm"), true);
      return 1;
   }

   private int buildStorm(CommandContext<CommandSourceStack> context) {
      ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
      ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
      int fromStage = IntegerArgumentType.getInteger(context, "stage");
      int fromEnergy = IntegerArgumentType.getInteger(context, "energy");
      WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)GameBusEvents.MANAGERS.get(level.dimension());
      Vec3 pos = new Vec3(player.getX(), level.getMaxBuildHeight(), player.getZ())
         .add(
            PMWeather.RANDOM.nextInt(-ServerConfig.spawnRange, ServerConfig.spawnRange + 1),
            0.0,
            PMWeather.RANDOM.nextInt(-ServerConfig.spawnRange, ServerConfig.spawnRange + 1)
         );
      Storm storm = new Storm(weatherHandlerServer, level, null, 0);
      storm.width = 0.0F;
      storm.windspeed = 0;
      storm.stormType = 0;
      storm.stage = 0;
      Vec3 wind = WindEngine.getWind(new Vec3(player.getX(), level.getMaxBuildHeight() + 1, player.getZ()), level, true, true, false);
      float dist = PMWeather.RANDOM.nextFloat(256.0F, 512.0F) * 4.0F;
      pos = pos.add(wind.normalize().multiply(-dist, 0.0, -dist));
      storm.position = pos;
      storm.velocity = Vec3.ZERO;
      storm.energy = 0;
      storm.maxStage = fromStage;
      storm.maxProgress = fromEnergy;
      storm.initFirstTime();
      weatherHandlerServer.addStorm(storm);
      weatherHandlerServer.syncStormNew(storm);
      ((CommandSourceStack)context.getSource())
         .sendSuccess(
            () -> Component.literal(
               "Successfully spawned storm:\nMax Stage: "
                  + storm.maxStage
                  + " Max Energy: "
                  + storm.maxProgress
                  + " Max Windspeed: "
                  + storm.maxWindspeed
                  + " Max Width: "
                  + storm.maxWidth
            ),
            true
         );
      return 1;
   }

   private int buildSquall(CommandContext<CommandSourceStack> context) {
      ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
      ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
      int fromStage = IntegerArgumentType.getInteger(context, "stage");
      int fromEnergy = IntegerArgumentType.getInteger(context, "energy");
      WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)GameBusEvents.MANAGERS.get(level.dimension());
      Vec3 pos = new Vec3(player.getX(), level.getMaxBuildHeight(), player.getZ())
         .add(
            PMWeather.RANDOM.nextInt(-ServerConfig.spawnRange, ServerConfig.spawnRange + 1),
            0.0,
            PMWeather.RANDOM.nextInt(-ServerConfig.spawnRange, ServerConfig.spawnRange + 1)
         );
      Storm storm = new Storm(weatherHandlerServer, level, null, 1);
      storm.width = 0.0F;
      storm.windspeed = 0;
      storm.stage = 0;
      Vec3 wind = WindEngine.getWind(new Vec3(player.getX(), level.getMaxBuildHeight() + 1, player.getZ()), level, true, true, false);
      if (wind.length() < 10.0) {
         wind = wind.normalize().multiply(10.0, 0.0, 10.0);
      }

      float dist = PMWeather.RANDOM.nextFloat(256.0F, 512.0F) * 6.0F;
      pos = pos.add(wind.normalize().multiply(-dist, 0.0, -dist));
      storm.position = pos;
      storm.velocity = wind.multiply(0.1, 0.0, 0.1);
      storm.energy = 0;
      storm.stormType = 1;
      storm.maxStage = fromStage;
      storm.maxProgress = fromEnergy;
      storm.initFirstTime();
      weatherHandlerServer.addStorm(storm);
      weatherHandlerServer.syncStormNew(storm);
      ((CommandSourceStack)context.getSource())
         .sendSuccess(() -> Component.literal("Successfully spawned storm:\nMax Stage: " + storm.maxStage + " Max Energy: " + storm.maxProgress), true);
      return 1;
   }

   private int naturalCyclone(CommandContext<CommandSourceStack> context) {
      ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
      ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
      WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)GameBusEvents.MANAGERS.get(level.dimension());
      Vec3 pos = Util.getValidTropicalSystemSpawn(weatherHandlerServer, player.position(), 4000.0F);
      if (pos == null) {
         ((CommandSourceStack)context.getSource()).sendFailure(Component.literal("Failed to find valid spawn for tropical cyclone"));
         return -1;
      } else {
         Storm storm = new Storm(weatherHandlerServer, level, null, 2);
         storm.width = PMWeather.RANDOM.nextFloat(5000.0F, 16000.0F);
         storm.windspeed = 0;
         storm.stormType = 2;
         storm.position = pos;
         storm.velocity = Vec3.ZERO;
         storm.initFirstTime();
         storm.maxWindspeed = 0;
         storm.maxWidth = Math.round(storm.width);
         weatherHandlerServer.addStorm(storm);
         weatherHandlerServer.syncStormNew(storm);
         ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("Successfully spawned cyclone"), true);
         return 1;
      }
   }

   private int newCyclone(CommandContext<CommandSourceStack> context) {
      ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
      ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
      int windspeed = IntegerArgumentType.getInteger(context, "windspeed");
      int width = IntegerArgumentType.getInteger(context, "width");
      WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)GameBusEvents.MANAGERS.get(level.dimension());
      Storm storm = new Storm(weatherHandlerServer, level, null, 2);
      storm.width = width;
      storm.windspeed = windspeed;
      storm.cycloneWindspeed = windspeed;
      storm.stormType = 2;
      storm.position = player.position();
      storm.velocity = Vec3.ZERO;
      storm.initFirstTime();
      storm.maxStage = Math.max(storm.maxStage, 3);
      storm.maxProgress = Math.max(storm.maxProgress, 100);
      storm.maxWindspeed = windspeed;
      storm.maxWidth = width;
      weatherHandlerServer.addStorm(storm);
      weatherHandlerServer.syncStormNew(storm);
      ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("Successfully spawned cyclone"), true);
      return 1;
   }

   private int newTornado(CommandContext<CommandSourceStack> context) {
      ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
      ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
      int windspeed = IntegerArgumentType.getInteger(context, "windspeed");
      int width = IntegerArgumentType.getInteger(context, "width");
      WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)GameBusEvents.MANAGERS.get(level.dimension());
      Storm storm = new Storm(weatherHandlerServer, level, null, 0);
      storm.width = width;
      storm.windspeed = windspeed;
      storm.stormType = 0;
      storm.stage = 3;
      storm.position = player.position();
      storm.velocity = Vec3.ZERO;
      storm.energy = 0;
      storm.initFirstTime();
      storm.maxStage = Math.max(storm.maxStage, 3);
      storm.maxProgress = Math.max(storm.maxProgress, 100);
      storm.maxWindspeed = windspeed;
      storm.maxWidth = width;
      weatherHandlerServer.addStorm(storm);
      weatherHandlerServer.syncStormNew(storm);
      ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("Successfully spawned storm"), true);
      return 1;
   }

   private int newStorm(CommandContext<CommandSourceStack> context) {
      ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
      ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
      int stage = IntegerArgumentType.getInteger(context, "stage");
      int energy = IntegerArgumentType.getInteger(context, "energy");
      if (stage < 0 || stage >= 3) {
         ((CommandSourceStack)context.getSource()).sendFailure(Component.literal("stage must be within range 0-2"));
         return -1;
      } else if (energy >= 0 && energy <= 100) {
         WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)GameBusEvents.MANAGERS.get(level.dimension());
         Storm storm = new Storm(weatherHandlerServer, level, null, 0);
         storm.width = 0.0F;
         storm.windspeed = 0;
         storm.stormType = 0;
         storm.stage = stage;
         storm.position = player.position();
         storm.velocity = Vec3.ZERO;
         storm.energy = energy;
         storm.initFirstTime();
         storm.maxStage = Math.max(storm.maxStage, stage);
         storm.maxProgress = Math.max(storm.maxProgress, energy);
         weatherHandlerServer.addStorm(storm);
         weatherHandlerServer.syncStormNew(storm);
         ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("Successfully spawned storm"), true);
         return 1;
      } else {
         ((CommandSourceStack)context.getSource()).sendFailure(Component.literal("energy must be within range 0-100"));
         return -1;
      }
   }

   private int newSquall(CommandContext<CommandSourceStack> context) {
      ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
      ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
      int stage = IntegerArgumentType.getInteger(context, "stage");
      int energy = IntegerArgumentType.getInteger(context, "energy");
      if (stage < 0 || stage > 3) {
         ((CommandSourceStack)context.getSource()).sendFailure(Component.literal("stage must be within range 0-3"));
         return -1;
      } else if (energy >= 0 && energy <= 100) {
         WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)GameBusEvents.MANAGERS.get(level.dimension());
         Storm storm = new Storm(weatherHandlerServer, level, null, 1);
         Vec3 wind = WindEngine.getWind(new Vec3(player.getX(), level.getMaxBuildHeight() + 1, player.getZ()), level, true, true, false);
         if (wind.length() < 10.0) {
            wind = wind.normalize().multiply(10.0, 0.0, 10.0);
         }

         storm.width = 0.0F;
         storm.windspeed = 0;
         storm.stormType = 1;
         storm.stage = stage;
         storm.position = player.position();
         storm.velocity = wind.multiply(0.1, 0.0, 0.1);
         storm.energy = energy;
         storm.initFirstTime();
         storm.maxStage = Math.max(storm.maxStage, stage);
         storm.maxProgress = Math.max(storm.maxProgress, energy);
         storm.coldEnergy = stage * 100 + energy;
         weatherHandlerServer.addStorm(storm);
         weatherHandlerServer.syncStormNew(storm);
         ((CommandSourceStack)context.getSource()).sendSuccess(() -> Component.literal("Successfully spawned storm"), true);
         return 1;
      } else {
         ((CommandSourceStack)context.getSource()).sendFailure(Component.literal("energy must be within range 0-100"));
         return -1;
      }
   }

   private int naturalStorm(CommandContext<CommandSourceStack> context) {
      ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
      ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
      WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)GameBusEvents.MANAGERS.get(level.dimension());
      Vec3 pos = new Vec3(player.getX(), level.getMaxBuildHeight(), player.getZ())
         .add(
            PMWeather.RANDOM.nextInt(-ServerConfig.spawnRange, ServerConfig.spawnRange + 1),
            0.0,
            PMWeather.RANDOM.nextInt(-ServerConfig.spawnRange, ServerConfig.spawnRange + 1)
         );
      Vec3 sfcPos = weatherHandlerServer.getWorld().getHeightmapPos(Types.WORLD_SURFACE_WG, player.blockPosition()).getCenter();
      Sounding sounding = new Sounding(weatherHandlerServer, sfcPos, level, 250, 16000);
      ThermodynamicEngine.AtmosphericDataPoint sfc = ThermodynamicEngine.samplePoint(weatherHandlerServer, sfcPos, level, null, 0);
      float riskV = sounding.getRisk(0);
      Storm storm = new Storm(weatherHandlerServer, level, riskV, 0);
      storm.width = 0.0F;
      storm.windspeed = 0;
      storm.stormType = 0;
      storm.stage = 0;
      Vec3 wind = WindEngine.getWind(new Vec3(player.getX(), level.getMaxBuildHeight() + 1, player.getZ()), level, true, true, false);
      float dist = PMWeather.RANDOM.nextFloat(256.0F, 512.0F) * 4.0F;
      pos = pos.add(wind.normalize().multiply(-dist, 0.0, -dist));
      if (ServerConfig.environmentSystem) {
         if (PMWeather.RANDOM.nextFloat() <= riskV * 2.5F) {
            storm.maxStage = Math.max(storm.maxStage, 1);
         }

         if (PMWeather.RANDOM.nextFloat() <= riskV * 2.0F) {
            storm.maxStage = Math.max(storm.maxStage, 2);
         }

         if (PMWeather.RANDOM.nextFloat() <= riskV * 1.5F) {
            storm.maxStage = Math.max(storm.maxStage, 3);
         }

         storm.recalc(riskV);
      }

      storm.position = pos;
      storm.velocity = Vec3.ZERO;
      storm.energy = 0;
      storm.initFirstTime();
      weatherHandlerServer.addStorm(storm);
      weatherHandlerServer.syncStormNew(storm);
      ((CommandSourceStack)context.getSource())
         .sendSuccess(
            () -> Component.literal(
               "Successfully spawned storm:\nMax Stage: "
                  + storm.maxStage
                  + " Max Energy: "
                  + storm.maxProgress
                  + " Max Windspeed: "
                  + storm.maxWindspeed
                  + " Max Width: "
                  + storm.maxWidth
            ),
            true
         );
      return 1;
   }

   private int naturalSquall(CommandContext<CommandSourceStack> context) {
      ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
      ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
      WeatherHandlerServer weatherHandlerServer = (WeatherHandlerServer)GameBusEvents.MANAGERS.get(level.dimension());
      Vec3 pos = new Vec3(player.getX(), level.getMaxBuildHeight(), player.getZ())
         .add(
            PMWeather.RANDOM.nextInt(-ServerConfig.spawnRange, ServerConfig.spawnRange + 1),
            0.0,
            PMWeather.RANDOM.nextInt(-ServerConfig.spawnRange, ServerConfig.spawnRange + 1)
         );
      Vec3 sfcPos = weatherHandlerServer.getWorld().getHeightmapPos(Types.WORLD_SURFACE_WG, player.blockPosition()).getCenter();
      Sounding sounding = new Sounding(weatherHandlerServer, sfcPos, level, 250, 16000);
      ThermodynamicEngine.AtmosphericDataPoint sfc = ThermodynamicEngine.samplePoint(weatherHandlerServer, sfcPos, level, null, 0);
      float riskV = sounding.getRisk(0);
      if (sfc.temperature() < 3.0F) {
         riskV += Math.clamp((sfc.temperature() - 3.0F) / -6.0F, 0.0F, 1.0F) * 0.25F;
      }

      Storm storm = new Storm(weatherHandlerServer, level, riskV, 1);
      storm.width = 0.0F;
      storm.windspeed = 0;
      storm.stage = 0;
      Vec3 wind = WindEngine.getWind(new Vec3(player.getX(), level.getMaxBuildHeight() + 1, player.getZ()), level, true, true, false);
      if (wind.length() < 10.0) {
         wind = wind.normalize().multiply(10.0, 0.0, 10.0);
      }

      float dist = PMWeather.RANDOM.nextFloat(256.0F, 512.0F) * 6.0F;
      pos = pos.add(wind.normalize().multiply(-dist, 0.0, -dist));
      if (ServerConfig.environmentSystem) {
         if (PMWeather.RANDOM.nextFloat() <= riskV * 2.5F) {
            storm.maxStage = Math.max(storm.maxStage, 1);
         }

         if (PMWeather.RANDOM.nextFloat() <= riskV * 2.0F) {
            storm.maxStage = Math.max(storm.maxStage, 2);
         }

         if (PMWeather.RANDOM.nextFloat() <= riskV * 1.5F) {
            storm.maxStage = Math.max(storm.maxStage, 3);
         }

         storm.recalc(riskV);
      }

      storm.position = pos;
      storm.velocity = wind.multiply(0.1, 0.0, 0.1);
      storm.energy = 0;
      storm.stormType = 1;
      storm.initFirstTime();
      weatherHandlerServer.addStorm(storm);
      weatherHandlerServer.syncStormNew(storm);
      ((CommandSourceStack)context.getSource())
         .sendSuccess(() -> Component.literal("Successfully spawned storm:\nMax Stage: " + storm.maxStage + " Max Energy: " + storm.maxProgress), true);
      return 1;
   }

   private int strike(CommandContext<CommandSourceStack> context) {
      ServerLevel level = ((CommandSourceStack)context.getSource()).getLevel();
      if (((CommandSourceStack)context.getSource()).isPlayer()) {
         ServerPlayer player = ((CommandSourceStack)context.getSource()).getPlayer();
         Vec3 lPos = player.position()
            .add(
               PMWeather.RANDOM.nextFloat((float)(-ServerConfig.stormSize), (float)ServerConfig.stormSize) / 2.0F,
               0.0,
               PMWeather.RANDOM.nextFloat((float)(-ServerConfig.stormSize), (float)ServerConfig.stormSize) / 2.0F
            );
         int height = level.getHeightmapPos(Types.MOTION_BLOCKING, new BlockPos((int)lPos.x, (int)lPos.y, (int)lPos.z)).getY();
         ((WeatherHandlerServer)GameBusEvents.MANAGERS.get(level.dimension())).syncLightningNew(new Vec3(lPos.x, height, lPos.z));
         return 1;
      } else {
         return 0;
      }
   }
}
