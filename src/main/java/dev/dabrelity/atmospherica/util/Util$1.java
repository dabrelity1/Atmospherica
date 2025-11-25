package dev.dabrelity.atmospherica.util;

import java.util.HashMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

class Util$1 extends HashMap<Block, Block> {
   Util$1() {
      this.put(Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG);
      this.put(Blocks.ACACIA_LOG, Blocks.STRIPPED_ACACIA_LOG);
      this.put(Blocks.BIRCH_LOG, Blocks.STRIPPED_BIRCH_LOG);
      this.put(Blocks.CHERRY_LOG, Blocks.STRIPPED_CHERRY_LOG);
      this.put(Blocks.DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_LOG);
      this.put(Blocks.JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_LOG);
      this.put(Blocks.MANGROVE_LOG, Blocks.STRIPPED_MANGROVE_LOG);
      this.put(Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG);
   }
}
