package dev.dabrelity.atmospherica.block;

import dev.dabrelity.atmospherica.block.entity.ModBlockEntities;
import dev.dabrelity.atmospherica.block.entity.RadarBlockEntity;
import dev.dabrelity.atmospherica.config.ServerConfig;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RadarBlock extends BaseEntityBlock {
   public static EnumProperty<RadarBlock.Mode> RADAR_MODE = EnumProperty.create("mode", RadarBlock.Mode.class);
   public static BooleanProperty ON = BooleanProperty.create("on");

   protected RadarBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.defaultBlockState().setValue(RADAR_MODE, RadarBlock.Mode.REFLECTIVITY)).setValue(ON, true));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(new Property[]{RADAR_MODE});
      builder.add(new Property[]{ON});
   }

   @Override
   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
      if (blockEntityType == ModBlockEntities.RADAR_BE.get()) {
         return (lvl, blockPos, blockState, blockEntity) -> {
            if (blockEntity instanceof RadarBlockEntity radar) {
               radar.tick(lvl, blockPos, blockState);
            }
         };
      }

      return null;
   }

   @Override
   public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
      super.appendHoverText(stack, level, tooltipComponents, tooltipFlag);
      if (ServerConfig.requireWSR88D) {
         tooltipComponents.add(Component.literal("Requires a completed WSR-88D nearby to function"));
      }
   }

   @Override
   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
      if (!player.getItemInHand(hand).isEmpty()) {
         return InteractionResult.PASS;
      }

      if (!level.isClientSide()) {
         RadarBlock.Mode newMode = (RadarBlock.Mode)state.getValue(RADAR_MODE);

         newMode = switch (newMode) {
            case REFLECTIVITY -> RadarBlock.Mode.VELOCITY;
            case VELOCITY -> RadarBlock.Mode.IR;
            default -> RadarBlock.Mode.REFLECTIVITY;
         };
         level.setBlockAndUpdate(pos, (BlockState)state.setValue(RADAR_MODE, newMode));
         if (player.isCrouching()) {
            level.setBlockAndUpdate(pos, (BlockState)state.setValue(ON, !(Boolean)state.getValue(ON)));
         }
      }

      return InteractionResult.sidedSuccess(level.isClientSide());
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
      return new RadarBlockEntity(blockPos, blockState);
   }

   public static enum Mode implements StringRepresentable {
      REFLECTIVITY("reflectivity"),
      VELOCITY("velocity"),
      IR("ir");

      private final String mode;

      private Mode(String mode) {
         this.mode = mode;
      }

      @NotNull
      public String getSerializedName() {
         return this.mode;
      }
   }
}
