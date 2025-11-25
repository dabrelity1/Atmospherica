package dev.protomanly.pmweather.creative;

import dev.protomanly.pmweather.block.ModBlocks;
import dev.protomanly.pmweather.item.ModItems;
import dev.protomanly.pmweather.multiblock.MultiBlocks;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
   public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "pmweather");
   public static final Supplier<CreativeModeTab> PMWEATHER_TAB = CREATIVE_MODE_TABS.register(
      "pmweather_tab",
      () -> CreativeModeTab.builder()
         .icon(() -> new ItemStack((ItemLike)ModBlocks.RADAR.get()))
         .title(Component.translatable("creativetab.pmweather.main"))
         .displayItems((itemDisplayParameters, output) -> {
            output.accept(ModBlocks.REINFORCED_GLASS);
            output.accept(ModBlocks.REINFORCED_GLASS_PANE);
            output.accept(ModBlocks.RADAR);
            output.accept(ModBlocks.RANGE_UPGRADE_MODULE);
            output.accept(MultiBlocks.WSR88D_CORE);
            output.accept(ModBlocks.RADOME);
            output.accept(ModBlocks.TORNADO_SENSOR);
            output.accept(ModBlocks.TORNADO_SIREN);
            output.accept(ModBlocks.ANEMOMETER);
            output.accept(ModBlocks.METAR);
            output.accept(ModBlocks.SOUNDING_VIEWER);
            output.accept(ModBlocks.WEATHER_PLATFORM);
            output.accept(ModItems.CONNECTOR);
            output.accept(ModItems.WEATHER_BALLOON);
            output.accept(ModBlocks.ICE_LAYER);
            output.accept(ModBlocks.SLEET_LAYER);
         })
         .build()
   );
}
