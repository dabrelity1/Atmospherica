package dev.dabrelity.atmospherica.block;

import dev.dabrelity.atmospherica.Atmospherica;
import dev.dabrelity.atmospherica.item.ModItems;
import dev.dabrelity.atmospherica.sound.ModSounds;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
   public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Atmospherica.MOD_ID);
   public static final RegistryObject<Block> ANEMOMETER = registerBlock(
      "anemometer",
      () -> new AnemometerBlock(Properties.copy(net.minecraft.world.level.block.Blocks.OAK_PLANKS).strength(1.25F).noOcclusion().sound(SoundType.METAL))
   );
   public static final RegistryObject<Block> RADAR = registerBlock(
      "radar", () -> new RadarBlock(Properties.copy(net.minecraft.world.level.block.Blocks.OAK_PLANKS).strength(1.0F).sound(SoundType.METAL))
   );
   public static final RegistryObject<Block> TORNADO_SENSOR = registerBlock(
      "tornado_sensor",
      () -> new TornadoSensorBlock(Properties.copy(net.minecraft.world.level.block.Blocks.OAK_PLANKS).strength(1.0F).sound(SoundType.METAL))
   );
   public static final RegistryObject<Block> TORNADO_SIREN = registerBlock(
      "tornado_siren",
      () -> new TornadoSirenBlock(Properties.copy(net.minecraft.world.level.block.Blocks.OAK_PLANKS).strength(1.0F).sound(SoundType.METAL))
   );
   public static final RegistryObject<Block> METAR = registerBlock(
      "metar", () -> new MetarBlock(Properties.copy(net.minecraft.world.level.block.Blocks.OAK_PLANKS).strength(1.0F).sound(SoundType.METAL))
   );
   public static final RegistryObject<Block> RADOME = registerBlock(
      "radome", () -> new Block(Properties.copy(net.minecraft.world.level.block.Blocks.WHITE_WOOL).strength(0.5F).sound(SoundType.STONE))
   );
   public static final RegistryObject<Block> SCOURED_GRASS = registerBlock(
      "scoured_grass", () -> new ScouredGrassBlock(Properties.copy(net.minecraft.world.level.block.Blocks.GRASS_BLOCK).randomTicks())
   );
   public static final RegistryObject<Block> MEDIUM_SCOURING = registerBlock(
      "medium_scouring", () -> new MediumScourBlock(Properties.copy(net.minecraft.world.level.block.Blocks.DIRT).randomTicks())
   );
   public static final RegistryObject<Block> HEAVY_SCOURING = registerBlock(
      "heavy_scouring", () -> new HeavyScourBlock(Properties.copy(net.minecraft.world.level.block.Blocks.DIRT).randomTicks())
   );
   public static final RegistryObject<Block> WEATHER_PLATFORM = registerBlock(
      "balloon_platform",
      () -> new WeatherPlatformBlock(
         Properties.copy(net.minecraft.world.level.block.Blocks.OAK_PLANKS).strength(1.0F).sound(SoundType.METAL).noOcclusion()
      )
   );
   public static final RegistryObject<Block> SOUNDING_VIEWER = registerBlock(
      "sounding_viewer",
      () -> new SoundingViewerBlock(
         Properties.copy(net.minecraft.world.level.block.Blocks.OAK_PLANKS).strength(1.0F).sound(SoundType.METAL).noOcclusion()
      )
   );
   public static final RegistryObject<Block> ICE_LAYER = registerBlock(
      "ice_layer",
      () -> new SnowLayerBlock(Properties.copy(net.minecraft.world.level.block.Blocks.SNOW).sound(SoundType.GLASS).strength(0.85F).noOcclusion())
   );
   public static final RegistryObject<Block> SLEET_LAYER = registerBlock(
      "sleet_layer", () -> new SnowLayerBlock(Properties.copy(net.minecraft.world.level.block.Blocks.SNOW).sound(ModSounds.SLEET_BLOCK).noOcclusion())
   );
   public static final RegistryObject<Block> REINFORCED_GLASS = registerBlock(
      "reinforced_glass",
      () -> new GlassBlock(
         Properties.copy(net.minecraft.world.level.block.Blocks.GLASS).strength(3.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()
      )
   );
   public static final RegistryObject<Block> REINFORCED_GLASS_PANE = registerBlock(
      "reinforced_glass_pane",
      () -> new IronBarsBlock(
         Properties.copy(net.minecraft.world.level.block.Blocks.GLASS).strength(1.5F).sound(SoundType.STONE).requiresCorrectToolForDrops()
      )
   );
   public static final RegistryObject<Block> ROTTED_LOG = registerBlock(
      "rotted_log", () -> new RottedLogBlock(Properties.copy(net.minecraft.world.level.block.Blocks.OAK_LOG).sound(SoundType.GRAVEL).instabreak())
   );
   public static final RegistryObject<Block> RANGE_UPGRADE_MODULE = registerBlock(
      "range_upgrade_module",
      () -> new DirectionalBlock(Properties.copy(net.minecraft.world.level.block.Blocks.OAK_PLANKS).strength(1.0F).sound(SoundType.METAL).noOcclusion())
   );

   private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
      RegistryObject<T> returnBlock = BLOCKS.register(name, block);
      registerBlockItem(name, returnBlock);
      return returnBlock;
   }

   private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block) {
      ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new net.minecraft.world.item.Item.Properties()));
   }
}
