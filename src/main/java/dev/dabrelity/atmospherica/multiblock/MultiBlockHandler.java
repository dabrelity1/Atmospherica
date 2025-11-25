package dev.dabrelity.atmospherica.multiblock;

import dev.dabrelity.atmospherica.Atmospherica;
import dev.dabrelity.atmospherica.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent.NeighborNotifyEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Atmospherica.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MultiBlockHandler {
   public static int searchRange = 6;
   public static boolean isDirty = false;

   public static void update(BlockPos blockPos, LevelAccessor level) {
      if (!level.isClientSide()) {
         BlockState baseState = level.getBlockState(blockPos);
         if (baseState.is(ModBlocks.RADOME.get()) || baseState.getBlock() instanceof MultiBlock) {
            for (int x = -searchRange; x <= searchRange; x++) {
               for (int y = -searchRange; y <= searchRange; y++) {
                  for (int z = -searchRange; z <= searchRange; z++) {
                     BlockState state = level.getBlockState(blockPos.offset(x, y, z));
                     if (state.getBlock() instanceof MultiBlock multiblock) {
                        level.scheduleTick(blockPos.offset(x, y, z), multiblock, 0);
                        Atmospherica.LOGGER.debug("Scheduled a tick at {} for {}", blockPos.offset(x, y, z), state);
                        break;
                     }
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onUpdate(NeighborNotifyEvent event) {
      update(event.getPos(), event.getLevel());
   }
}
