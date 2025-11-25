package dev.protomanly.pmweather.block;

import dev.protomanly.pmweather.item.ModItems;
import dev.protomanly.pmweather.sound.ModSounds;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Blocks;

public class ModBlocks {
   public static final Blocks BLOCKS = DeferredRegister.createBlocks("pmweather");
   public static final DeferredBlock<Block> ANEMOMETER = registerBlock(
      "anemometer",
      () -> new AnemometerBlock(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.OAK_PLANKS).strength(1.25F).noOcclusion().sound(SoundType.METAL))
   );
   public static final DeferredBlock<Block> RADAR = registerBlock(
      "radar", () -> new RadarBlock(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.OAK_PLANKS).strength(1.0F).sound(SoundType.METAL))
   );
   public static final DeferredBlock<Block> TORNADO_SENSOR = registerBlock(
      "tornado_sensor",
      () -> new TornadoSensorBlock(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.OAK_PLANKS).strength(1.0F).sound(SoundType.METAL))
   );
   public static final DeferredBlock<Block> TORNADO_SIREN = registerBlock(
      "tornado_siren",
      () -> new TornadoSirenBlock(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.OAK_PLANKS).strength(1.0F).sound(SoundType.METAL))
   );
   public static final DeferredBlock<Block> METAR = registerBlock(
      "metar", () -> new MetarBlock(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.OAK_PLANKS).strength(1.0F).sound(SoundType.METAL))
   );
   public static final DeferredBlock<Block> RADOME = registerBlock(
      "radome", () -> new Block(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WHITE_WOOL).strength(0.5F).sound(SoundType.STONE))
   );
   public static final DeferredBlock<Block> SCOURED_GRASS = registerBlock(
      "scoured_grass", () -> new ScouredGrassBlock(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.GRASS_BLOCK).randomTicks())
   );
   public static final DeferredBlock<Block> MEDIUM_SCOURING = registerBlock(
      "medium_scouring", () -> new MediumScourBlock(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.DIRT).randomTicks())
   );
   public static final DeferredBlock<Block> HEAVY_SCOURING = registerBlock(
      "heavy_scouring", () -> new HeavyScourBlock(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.DIRT).randomTicks())
   );
   public static final DeferredBlock<Block> WEATHER_PLATFORM = registerBlock(
      "balloon_platform",
      () -> new WeatherPlatformBlock(
         Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.OAK_PLANKS).strength(1.0F).sound(SoundType.METAL).noOcclusion()
      )
   );
   public static final DeferredBlock<Block> SOUNDING_VIEWER = registerBlock(
      "sounding_viewer",
      () -> new SoundingViewerBlock(
         Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.OAK_PLANKS).strength(1.0F).sound(SoundType.METAL).noOcclusion()
      )
   );
   public static final DeferredBlock<Block> ICE_LAYER = registerBlock(
      "ice_layer",
      () -> new SnowLayerBlock(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.SNOW).sound(SoundType.GLASS).strength(0.85F).noOcclusion())
   );
   public static final DeferredBlock<Block> SLEET_LAYER = registerBlock(
      "sleet_layer", () -> new SnowLayerBlock(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.SNOW).sound(ModSounds.SLEET_BLOCK).noOcclusion())
   );
   public static final DeferredBlock<Block> REINFORCED_GLASS = registerBlock(
      "reinforced_glass",
      () -> new TransparentBlock(
         Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.GLASS).strength(3.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()
      )
   );
   public static final DeferredBlock<Block> REINFORCED_GLASS_PANE = registerBlock(
      "reinforced_glass_pane",
      () -> new IronBarsBlock(
         Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.GLASS).strength(1.5F).sound(SoundType.STONE).requiresCorrectToolForDrops()
      )
   );
   public static final DeferredBlock<Block> ROTTED_LOG = registerBlock(
      "rotted_log", () -> new RottedLogBlock(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.OAK_LOG).sound(SoundType.GRAVEL).instabreak())
   );
   public static final DeferredBlock<Block> RANGE_UPGRADE_MODULE = registerBlock(
      "range_upgrade_module",
      () -> new DirectionalBlock(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.OAK_PLANKS).strength(1.0F).sound(SoundType.METAL).noOcclusion())
   );

   private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
      DeferredBlock<T> returnBlock = BLOCKS.register(name, block);
      registerBlockItem(name, returnBlock);
      return returnBlock;
   }

   private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
      ModItems.ITEMS.register(name, () -> new BlockItem((Block)block.get(), new net.minecraft.world.item.Item.Properties()));
   }
}
