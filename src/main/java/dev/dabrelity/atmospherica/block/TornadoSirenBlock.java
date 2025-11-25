package dev.dabrelity.atmospherica.block;

import dev.dabrelity.atmospherica.block.entity.ModBlockEntities;
import dev.dabrelity.atmospherica.block.entity.TornadoSirenBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import org.jetbrains.annotations.Nullable;

public class TornadoSirenBlock extends BaseEntityBlock {

   public TornadoSirenBlock(Properties properties) {
      super(properties);
   }

   @Override
   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
      if (blockEntityType == ModBlockEntities.TORNADO_SIREN_BE.get()) {
         return (lvl, blockPos, blockState, blockEntity) -> {
            if (blockEntity instanceof TornadoSirenBlockEntity siren) {
               siren.tick(lvl, blockPos, blockState);
            }
         };
      }

      return null;
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
      return new TornadoSirenBlockEntity(blockPos, blockState);
   }
}
