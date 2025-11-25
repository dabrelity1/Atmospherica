package dev.protomanly.pmweather.config;

import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.util.Util;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.config.ModConfigEvent.Unloading;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

@EventBusSubscriber(
   modid = "pmweather",
   bus = Bus.MOD
)
public class ServerConfig {
   private static final Builder BUILDER = new Builder();
   private static final BooleanValue DOTORNADOES = BUILDER.comment("Whether tornadoes should be possible").define("dotornadoes", true);
   public static boolean doTornadoes;
   private static final BooleanValue DOCYCLONES = BUILDER.comment("Whether cyclones should be possible").define("docyclones", true);
   public static boolean doCyclones;
   private static final BooleanValue DOSQUALLS = BUILDER.comment("Whether squalls should be possible").define("dosqualls", true);
   public static boolean doSqualls;
   private static final BooleanValue REQUIRE_WSR88D = BUILDER.comment("Whether radar blocks require a completed WSR-88D to be nearby for display")
      .define("requirewsr88d", false);
   public static boolean requireWSR88D;
   private static final IntValue SPAWN_RANGE = BUILDER.comment("Range within which clouds and storms will spawn from players")
      .defineInRange("spawnrange", 384, 256, 2048);
   public static int spawnRange;
   private static final IntValue CHANCE_IN_ONE_SQUALL = BUILDER.comment("1 in x chance that a spawning storm will spawn as a squall")
      .defineInRange("chanceinonesquall", 10, 1, 100);
   public static int chanceInOneSquall;
   private static final IntValue CHANCE_IN_ONE_CYCLONE = BUILDER.comment("1 in x chance that a spawning storm will spawn as a cyclone")
      .defineInRange("chanceinonecyclone", 16, 1, 100);
   public static int chanceInOneCyclone;
   private static final IntValue CHANCE_IN_ONE_STAGE_1 = BUILDER.comment("1 in x chance that a storm will progress to stage 1")
      .defineInRange("chanceinonestage1", 2, 1, 100);
   public static int chanceInOneStage1;
   private static final IntValue CHANCE_IN_ONE_STAGE_2 = BUILDER.comment("1 in x chance that a storm will progress to stage 2")
      .defineInRange("chanceinonestage2", 3, 1, 100);
   public static int chanceInOneStage2;
   private static final IntValue CHANCE_IN_ONE_STAGE_3 = BUILDER.comment("1 in x chance that a storm will progress to stage 3 / tornado")
      .defineInRange("chanceinonestage3", 5, 1, 100);
   public static int chanceInOneStage3;
   private static final DoubleValue RISK_CURVE = BUILDER.comment("Risk curve, higher is rarer").defineInRange("riskcurve", 1.0, 0.5, 2.0);
   public static double riskCurve;
   private static final DoubleValue SQUALL_STRENGTH_MULTIPLIER = BUILDER.comment("Multiplier of squall windspeeds")
      .defineInRange("squallstrengthmultiplier", 1.25, 0.0, 2.0);
   public static double squallStrengthMultiplier;
   private static final DoubleValue STORM_SPAWN_CHANCE_PER_MINUTE = BUILDER.comment("Chance a storm will spawn each minute")
      .defineInRange("stormspawnchanceperminute", 0.2, 0.0, 1.0);
   public static double stormSpawnChancePerMinute;
   private static final BooleanValue ENVIRONMENT_SYSTEM = BUILDER.comment(
         "Whether chance to spawn storms and chance for storms to progress is affected by the game environment"
      )
      .define("environmentsystem", true);
   public static boolean environmentSystem;
   private static final IntValue MAX_STORMS = BUILDER.comment("Maximum number of active storms allowed to spawn").defineInRange("maxstorms", 5, 1, 10);
   public static int maxStorms;
   private static final IntValue SNOW_ACCUMULATION_HEIGHT = BUILDER.comment("Maximum precipitation accumulation, 0 = off")
      .defineInRange("snowaccumulationheight", 6, 0, 8);
   public static int snowAccumulationHeight;
   public static int maxClouds = 0;
   private static final ConfigValue<List<? extends String>> BLOCK_STENGTHS = BUILDER.comment(
         "List of blocks and respective windspeed at which they get damaged"
      )
      .defineListAllowEmpty(
         "blockstrengths",
         () -> new ArrayList<String>() {
            {
               this.add("minecraft:acacia_leaves=55");
               this.add("minecraft:azalea_leaves=55");
               this.add("minecraft:birch_leaves=50");
               this.add("minecraft:dark_oak_leaves=55");
               this.add("minecraft:cherry_leaves=55");
               this.add("minecraft:flowering_azalea_leaves=55");
               this.add("minecraft:mangrove_leaves=65");
               this.add("minecraft:oak_leaves=55");
               this.add("minecraft:jungle_leaves=55");
               this.add("minecraft:chain=75");
               this.add("minecraft:lantern=75");
               this.add("minecraft:soul_lantern=75");
               this.add("minecraft:white_wool=60");
               this.add("minecraft:orange_wool=60");
               this.add("minecraft:magenta_wool=60");
               this.add("minecraft:light_blue_wool=60");
               this.add("minecraft:yellow_wool=60");
               this.add("minecraft:lime_wool=60");
               this.add("minecraft:pink_wool=60");
               this.add("minecraft:gray_wool=60");
               this.add("minecraft:light_gray_wool=60");
               this.add("minecraft:cyan_wool=60");
               this.add("minecraft:purple_wool=60");
               this.add("minecraft:blue_wool=60");
               this.add("minecraft:brown_wool=60");
               this.add("minecraft:green_wool=60");
               this.add("minecraft:red_wool=60");
               this.add("minecraft:black_wool=60");
            }
         },
         () -> "pmweather",
         e -> e instanceof String string
            && string.contains("=")
            && string.split("=").length == 2
            && Objects.nonNull(ResourceLocation.tryParse(string.split("=")[0]))
            && BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse(string.split("=")[0]))
            && Util.isInteger(string.split("=")[1])
      );
   public static Map<Block, Float> blockStrengths;
   private static final ConfigValue<List<? extends String>> BLACKLISTED_BLOCKS = BUILDER.comment("List of blocks not allowed to be damaged")
      .defineListAllowEmpty(
         "blacklistedblocks",
         () -> new ArrayList<String>() {
            {
               this.add("minecraft:gravel");
               this.add("minecraft:farmland");
               this.add("minecraft:dirt_path");
            }
         },
         () -> "pmweather",
         e -> e instanceof String string
            && Objects.nonNull(ResourceLocation.tryParse(string))
            && BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse(string))
      );
   public static List<Block> blacklistedBlocks;
   private static final ConfigValue<List<? extends String>> BLACKLISTED_BLOCKTAGS = BUILDER.comment("List of blocktags not allowed to be damaged")
      .defineListAllowEmpty("blacklistedblocktags", () -> new ArrayList<String>() {
         {
            this.add("minecraft:dirt");
            this.add("minecraft:base_stone_overworld");
            this.add("minecraft:terracotta");
            this.add("minecraft:badlands_terracotta");
            this.add("minecraft:ice");
            this.add("minecraft:sand");
         }
      }, () -> "pmweather", e -> {
         if (e instanceof String string) {
            ResourceLocation path = ResourceLocation.tryParse(string);
            if (Objects.isNull(path)) {
               return false;
            } else {
               TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, path);
               BuiltInRegistries.BLOCK.getTagOrEmpty(tagKey);
               return BuiltInRegistries.BLOCK.getTagOrEmpty(tagKey).iterator().hasNext();
            }
         } else {
            return false;
         }
      });
   public static List<TagKey<Block>> blacklistedBlockTags;
   private static final ConfigValue<List<? extends String>> VALID_DIMENSIONS = BUILDER.comment("List of valid dimensions for spawning weather")
      .defineListAllowEmpty("validdimensions", () -> new ArrayList<String>() {
         {
            this.add("minecraft:overworld");
         }
      }, () -> "pmweather", e -> e instanceof String);
   public static List<ResourceKey<Level>> validDimensions;
   private static final DoubleValue STORM_SIZE = BUILDER.comment("Size of storms").defineInRange("stormsize", 300.0, 128.0, 512.0);
   public static double stormSize;
   private static final DoubleValue OVERCAST_PERCENT = BUILDER.comment("Overcast Modifier").defineInRange("overcastpercent", 0.85, 0.0, 0.9F);
   public static double overcastPercent;
   private static final DoubleValue RAIN_STRENGTH = BUILDER.comment("Rain Modifier").defineInRange("rainstrength", 0.8, 0.0, 1.25);
   public static double rainStrength;
   private static final DoubleValue LAYER_0_HEIGHT = BUILDER.comment("Height of first cloud layer").defineInRange("layer0height", 315.0, 150.0, 350.0);
   public static double layer0Height;
   private static final DoubleValue LAYER_C_HEIGHT = BUILDER.comment("Height of cirrus cloud layer").defineInRange("layerCheight", 2000.0, 1000.0, 3000.0);
   public static double layerCHeight;
   private static final IntValue MAX_TORNADO_WIDTH = BUILDER.comment("Maximum width of tornadoes").defineInRange("maxtornadowidth", 225, 100, 800);
   public static double maxTornadoWidth;
   private static final BooleanValue AIM_AT_PLAYER = BUILDER.comment("Whether storms will aim at the player whenever strengthening into a tornado")
      .define("aimatplayer", false);
   public static boolean aimAtPlayer;
   private static final DoubleValue AIM_AT_PLAYER_OFFSET = BUILDER.comment("Random range of blocks that storms will aim at around players")
      .defineInRange("aimatplayeroffset", 248.0, 0.0, 1024.0);
   public static double aimAtPlayerOffset;
   private static final IntValue MAX_BLOCKS_DAMAGED_PER_TICK = BUILDER.comment("Maximum number of blocks allowed to be damaged by a tornado per tick")
      .defineInRange("maxblocksdamagedpertick", 12500, 5000, 40000);
   public static int maxBlocksDamagedPerTick;
   private static final BooleanValue DAMAGE_EVERY_5TH_TICK = BUILDER.comment("Whether damage should run every tick (off) or every 5th tick (on)")
      .define("damageevery5thtick", true);
   public static boolean damageEvery5thTick;
   private static final BooleanValue DO_DEBARKING = BUILDER.comment("Whether debarking will be applied to logs").define("dodebarking", true);
   public static boolean doDebarking;
   public static final ModConfigSpec SPEC = BUILDER.build();

   @SubscribeEvent
   private static void onLoad(ModConfigEvent event) {
      if (event.getConfig().getSpec() == SPEC && !(event instanceof Unloading)) {
         PMWeather.LOGGER.info("Loading Server PMWeather Configs");
         requireWSR88D = (Boolean)REQUIRE_WSR88D.get();
         chanceInOneSquall = (Integer)CHANCE_IN_ONE_SQUALL.get();
         chanceInOneCyclone = (Integer)CHANCE_IN_ONE_CYCLONE.get();
         chanceInOneStage1 = (Integer)CHANCE_IN_ONE_STAGE_1.get();
         chanceInOneStage2 = (Integer)CHANCE_IN_ONE_STAGE_2.get();
         chanceInOneStage3 = (Integer)CHANCE_IN_ONE_STAGE_3.get();
         environmentSystem = (Boolean)ENVIRONMENT_SYSTEM.get();
         aimAtPlayer = (Boolean)AIM_AT_PLAYER.get();
         aimAtPlayerOffset = (Double)AIM_AT_PLAYER_OFFSET.get();
         spawnRange = (Integer)SPAWN_RANGE.get();
         stormSpawnChancePerMinute = (Double)STORM_SPAWN_CHANCE_PER_MINUTE.get();
         maxStorms = (Integer)MAX_STORMS.get();
         overcastPercent = (Double)OVERCAST_PERCENT.get();
         rainStrength = (Double)RAIN_STRENGTH.get();
         riskCurve = (Double)RISK_CURVE.get();
         squallStrengthMultiplier = (Double)SQUALL_STRENGTH_MULTIPLIER.get();
         maxTornadoWidth = ((Integer)MAX_TORNADO_WIDTH.get()).intValue();
         maxBlocksDamagedPerTick = (Integer)MAX_BLOCKS_DAMAGED_PER_TICK.get();
         damageEvery5thTick = (Boolean)DAMAGE_EVERY_5TH_TICK.get();
         doDebarking = (Boolean)DO_DEBARKING.get();
         blockStrengths = new HashMap<>();

         for (String string : (List)BLOCK_STENGTHS.get()) {
            String[] args = string.split("=");
            if (args.length == 2) {
               ResourceLocation resourceLocation = ResourceLocation.parse(args[0]);
               if (BuiltInRegistries.BLOCK.containsKey(resourceLocation) && Util.isInteger(args[1])) {
                  Block block = (Block)BuiltInRegistries.BLOCK.get(resourceLocation);
                  int strength = Integer.parseInt(args[1]);
                  PMWeather.LOGGER.debug("Setup Block {} with strength {} mph", block, strength);
                  blockStrengths.put(block, (float)strength);
               } else {
                  PMWeather.LOGGER.warn("Invalid blockstrengths config: {}", string);
               }
            } else {
               PMWeather.LOGGER.warn("Invalid blockstrengths config: {}", string);
            }
         }

         blacklistedBlocks = new ArrayList<>();

         for (String stringx : (List)BLACKLISTED_BLOCKS.get()) {
            ResourceLocation resourceLocation = ResourceLocation.parse(stringx);
            if (BuiltInRegistries.BLOCK.containsKey(resourceLocation)) {
               Block block = (Block)BuiltInRegistries.BLOCK.get(resourceLocation);
               PMWeather.LOGGER.debug("Inserted Block {}", block);
               blacklistedBlocks.add(block);
            } else {
               PMWeather.LOGGER.warn("Invalid block within config blacklistedblocks: {}", stringx);
            }
         }

         blacklistedBlockTags = new ArrayList<>();

         for (String stringxx : (List)BLACKLISTED_BLOCKTAGS.get()) {
            ResourceLocation resourceLocation = ResourceLocation.parse(stringxx);
            TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, resourceLocation);
            PMWeather.LOGGER.debug("Inserted BlockTag {}", tagKey);
            blacklistedBlockTags.add(tagKey);
         }

         validDimensions = new ArrayList<>();

         for (String stringxx : (List)VALID_DIMENSIONS.get()) {
            ResourceLocation resourceLocation = ResourceLocation.parse(stringxx);
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, resourceLocation);
            validDimensions.add(dimension);
         }

         stormSize = (Double)STORM_SIZE.get();
         layer0Height = (Double)LAYER_0_HEIGHT.get();
         layerCHeight = (Double)LAYER_C_HEIGHT.get();
         snowAccumulationHeight = (Integer)SNOW_ACCUMULATION_HEIGHT.get();
         doTornadoes = (Boolean)DOTORNADOES.get();
         doCyclones = (Boolean)DOCYCLONES.get();
         doSqualls = (Boolean)DOSQUALLS.get();
      }
   }
}
