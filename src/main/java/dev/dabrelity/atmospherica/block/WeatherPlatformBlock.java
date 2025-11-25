package dev.dabrelity.atmospherica.block;

import dev.dabrelity.atmospherica.block.entity.ModBlockEntities;
import dev.dabrelity.atmospherica.block.entity.WeatherPlatformBlockEntity;
import dev.dabrelity.atmospherica.item.ModItems;
import dev.dabrelity.atmospherica.item.component.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class WeatherPlatformBlock extends BaseEntityBlock {

   protected WeatherPlatformBlock(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
      ItemStack stack = player.getItemInHand(hand);
      if (stack.is((Item)ModItems.CONNECTOR.get())) {
         if (!level.isClientSide()) {
            ModComponents.WEATHER_BALLOON_PLATFORM.set(stack, pos);
            player.sendSystemMessage(Component.literal(String.format("Connector is configured to %s, %s, %s", pos.getX(), pos.getY(), pos.getZ())));
         }

         return InteractionResult.sidedSuccess(level.isClientSide());
      }

      if (stack.is((Item)ModItems.WEATHER_BALLOON.get())) {
         if (level.getBlockEntity(pos) instanceof WeatherPlatformBlockEntity weatherPlatformBlockEntity
            && !weatherPlatformBlockEntity.active
            && level.canSeeSky(pos.above())) {
            if (!level.isClientSide()) {
               if (!player.getAbilities().instabuild) {
                  stack.shrink(1);
               }

               weatherPlatformBlockEntity.activate(level, pos, state);
            }

            return InteractionResult.sidedSuccess(level.isClientSide());
         }

         if (!level.canSeeSky(pos.above()) && !level.isClientSide()) {
            player.sendSystemMessage(Component.literal("Platform cannot see sky!"));
         }

         return InteractionResult.PASS;
      }

      return InteractionResult.PASS;
   }

   @Override
   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
      return new WeatherPlatformBlockEntity(blockPos, blockState);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
      if (blockEntityType == ModBlockEntities.WEATHER_PLATFORM_BE.get()) {
         return (lvl, blockPos, blockState, blockEntity) -> {
            if (blockEntity instanceof WeatherPlatformBlockEntity platform) {
               platform.tick(lvl, blockPos, blockState);
            }
         };
      }

      return null;
   }
}
