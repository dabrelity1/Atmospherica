package dev.dabrelity.atmospherica.block;

import dev.dabrelity.atmospherica.block.entity.AnemometerBlockEntity;
import dev.dabrelity.atmospherica.block.entity.ModBlockEntities;
import dev.dabrelity.atmospherica.config.ClientConfig;
import dev.dabrelity.atmospherica.weather.WindEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class AnemometerBlock extends BaseEntityBlock {
   public static final VoxelShape SHAPE = box(6.0, 0.0, 6.0, 10.0, 14.0, 10.0);

   protected AnemometerBlock(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
      if (!player.getItemInHand(hand).isEmpty()) {
         return InteractionResult.PASS;
      }

      if (level.isClientSide()) {
         Vec3 wind = WindEngine.getWind(pos, level);
         double windspeed = wind.length();
         if (ClientConfig.metric) {
            player.sendSystemMessage(Component.literal(String.format("%s km/h", (int)Math.floor(windspeed * 1.609))));
         } else {
            player.sendSystemMessage(Component.literal(String.format("%s MPH", (int)Math.floor(windspeed))));
         }
      }

      return InteractionResult.sidedSuccess(level.isClientSide());
   }

   @Override
   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.INVISIBLE;
   }

   @Override
   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
      return new AnemometerBlockEntity(blockPos, blockState);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
      if (blockEntityType == ModBlockEntities.ANEMOMETER_BE.get()) {
         return (lvl, blockPos, blockState, blockEntity) -> {
            if (blockEntity instanceof AnemometerBlockEntity anemometer) {
               anemometer.tick(lvl, blockPos, blockState);
            }
         };
      }

      return null;
   }
}
