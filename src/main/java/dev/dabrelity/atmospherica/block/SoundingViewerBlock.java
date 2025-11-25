package dev.dabrelity.atmospherica.block;

import dev.dabrelity.atmospherica.block.entity.ModBlockEntities;
import dev.dabrelity.atmospherica.block.entity.SoundingViewerBlockEntity;
import dev.dabrelity.atmospherica.item.ModItems;
import dev.dabrelity.atmospherica.item.component.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class SoundingViewerBlock extends BaseEntityBlock {
   public static DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

   protected SoundingViewerBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(FACING, Direction.NORTH));
   }

   @Override
   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
      ItemStack stack = player.getItemInHand(hand);
      if (stack.is((Item)ModItems.CONNECTOR.get()) && ModComponents.WEATHER_BALLOON_PLATFORM.has(stack)) {
         if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SoundingViewerBlockEntity soundingViewerBlockEntity) {
            BlockPos target = ModComponents.WEATHER_BALLOON_PLATFORM.get(stack);
            if (target != null) {
               soundingViewerBlockEntity.connect(target);
               player.sendSystemMessage(Component.literal("Connected sounding viewer!"));
            }
         }

         return InteractionResult.sidedSuccess(level.isClientSide());
      }

      return InteractionResult.PASS;
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(new Property[]{FACING});
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState blockstate = super.getStateForPlacement(context);
      if (blockstate == null) {
         blockstate = this.defaultBlockState();
      }

      return (BlockState)blockstate.setValue(FACING, context.getHorizontalDirection().getOpposite());
   }

   @Override
   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
      return new SoundingViewerBlockEntity(blockPos, blockState);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
      if (blockEntityType == ModBlockEntities.SOUNDING_VIEWER_BE.get()) {
         return (lvl, blockPos, blockState, blockEntity) -> {
            if (blockEntity instanceof SoundingViewerBlockEntity soundingViewer) {
               soundingViewer.tick(lvl, blockPos, blockState);
            }
         };
      }

      return null;
   }
}
