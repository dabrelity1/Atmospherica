package dev.dabrelity.atmospherica.creative;

import dev.dabrelity.atmospherica.Atmospherica;
import dev.dabrelity.atmospherica.block.ModBlocks;
import dev.dabrelity.atmospherica.item.ModItems;
import dev.dabrelity.atmospherica.multiblock.MultiBlocks;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.DeferredRegister;

public class ModCreativeTabs {
   public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Atmospherica.MOD_ID);
   public static final Supplier<CreativeModeTab> ATMOSPHERICA_TAB = CREATIVE_MODE_TABS.register(
      "atmospherica_tab",
      () -> CreativeModeTab.builder()
         .icon(() -> new ItemStack((ItemLike)ModBlocks.RADAR.get()))
         .title(Component.translatable("creativetab.atmospherica.main"))
         .displayItems((itemDisplayParameters, output) -> {
            output.accept(ModBlocks.REINFORCED_GLASS.get());
            output.accept(ModBlocks.REINFORCED_GLASS_PANE.get());
            output.accept(ModBlocks.RADAR.get());
            output.accept(ModBlocks.RANGE_UPGRADE_MODULE.get());
            output.accept(MultiBlocks.WSR88D_CORE.get());
            output.accept(ModBlocks.RADOME.get());
            output.accept(ModBlocks.TORNADO_SENSOR.get());
            output.accept(ModBlocks.TORNADO_SIREN.get());
            output.accept(ModBlocks.ANEMOMETER.get());
            output.accept(ModBlocks.METAR.get());
            output.accept(ModBlocks.SOUNDING_VIEWER.get());
            output.accept(ModBlocks.WEATHER_PLATFORM.get());
            output.accept(ModItems.CONNECTOR.get());
            output.accept(ModItems.WEATHER_BALLOON.get());
            output.accept(ModBlocks.ICE_LAYER.get());
            output.accept(ModBlocks.SLEET_LAYER.get());
         })
         .build()
   );
}
