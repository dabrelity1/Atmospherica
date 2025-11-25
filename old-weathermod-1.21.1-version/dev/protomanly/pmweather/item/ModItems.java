package dev.protomanly.pmweather.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredRegister.Items;

public class ModItems {
   public static final Items ITEMS = DeferredRegister.createItems("pmweather");
   public static final DeferredItem<Item> CONNECTOR = ITEMS.register("connector", () -> new ConnectorItem(new Properties().stacksTo(1)));
   public static final DeferredItem<Item> WEATHER_BALLOON = ITEMS.register("weather_balloon", () -> new Item(new Properties().stacksTo(8)));
}
