package dev.dabrelity.atmospherica.item;

import dev.dabrelity.atmospherica.Atmospherica;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
   public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Atmospherica.MOD_ID);
   public static final RegistryObject<Item> CONNECTOR = ITEMS.register("connector", () -> new ConnectorItem(new Properties().stacksTo(1)));
   public static final RegistryObject<Item> WEATHER_BALLOON = ITEMS.register("weather_balloon", () -> new Item(new Properties().stacksTo(8)));
}
