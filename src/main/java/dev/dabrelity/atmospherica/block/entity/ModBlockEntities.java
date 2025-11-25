package dev.dabrelity.atmospherica.block.entity;

import dev.dabrelity.atmospherica.Atmospherica;
import dev.dabrelity.atmospherica.block.ModBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
   public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Atmospherica.MOD_ID);
   public static final RegistryObject<BlockEntityType<AnemometerBlockEntity>> ANEMOMETER_BE = BLOCK_ENTITIES.register(
      "anemometer_be", () -> Builder.of(AnemometerBlockEntity::new, new Block[]{ModBlocks.ANEMOMETER.get()}).build(null)
   );
   public static final RegistryObject<BlockEntityType<RadarBlockEntity>> RADAR_BE = BLOCK_ENTITIES.register(
      "radar_be", () -> Builder.of(RadarBlockEntity::new, new Block[]{ModBlocks.RADAR.get()}).build(null)
   );
   public static final RegistryObject<BlockEntityType<TornadoSensorBlockEntity>> TORNADO_SENSOR_BE = BLOCK_ENTITIES.register(
      "tornado_sensor_be", () -> Builder.of(TornadoSensorBlockEntity::new, new Block[]{ModBlocks.TORNADO_SENSOR.get()}).build(null)
   );
   public static final RegistryObject<BlockEntityType<TornadoSirenBlockEntity>> TORNADO_SIREN_BE = BLOCK_ENTITIES.register(
      "tornado_siren_be", () -> Builder.of(TornadoSirenBlockEntity::new, new Block[]{ModBlocks.TORNADO_SIREN.get()}).build(null)
   );
   public static final RegistryObject<BlockEntityType<WeatherPlatformBlockEntity>> WEATHER_PLATFORM_BE = BLOCK_ENTITIES.register(
      "weather_platform_be", () -> Builder.of(WeatherPlatformBlockEntity::new, new Block[]{ModBlocks.WEATHER_PLATFORM.get()}).build(null)
   );
   public static final RegistryObject<BlockEntityType<SoundingViewerBlockEntity>> SOUNDING_VIEWER_BE = BLOCK_ENTITIES.register(
      "sounding_viewer_be", () -> Builder.of(SoundingViewerBlockEntity::new, new Block[]{ModBlocks.SOUNDING_VIEWER.get()}).build(null)
   );
}
