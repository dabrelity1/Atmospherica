package dev.protomanly.pmweather.block;

import dev.protomanly.pmweather.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class RottedLogBlock extends RotatedPillarBlock {
   public RottedLogBlock(Properties properties) {
      super(properties);
   }

   protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      Util.checkLogs(state, level, pos);
      if (random.nextInt(2) == 0) {
         BlockPos below = pos.below();
         BlockState belowState = level.getBlockState(below);
         if (belowState.is(BlockTags.DIRT)) {
            level.setBlockAndUpdate(pos, Blocks.OAK_SAPLING.defaultBlockState());
         } else {
            level.removeBlock(pos, false);
         }
      }
   }
}
