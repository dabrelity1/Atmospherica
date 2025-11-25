package dev.dabrelity.atmospherica.multiblock;

import dev.dabrelity.atmospherica.block.ModBlocks;
import dev.dabrelity.atmospherica.item.ModItems;
import dev.dabrelity.atmospherica.multiblock.wsr88d.WSR88DCore;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraftforge.registries.RegistryObject;

public class MultiBlocks {
   public static final RegistryObject<Block> WSR88D_CORE = registerBlock(
      "wsr88d_core", () -> new WSR88DCore(Properties.copy(Blocks.IRON_BLOCK).noOcclusion())
   );

   private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
      RegistryObject<T> returnBlock = ModBlocks.BLOCKS.register(name, block);
      registerBlockItem(name, returnBlock);
      return returnBlock;
   }

   private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block) {
      ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new net.minecraft.world.item.Item.Properties()));
   }

   public static void register() {
   }
}
