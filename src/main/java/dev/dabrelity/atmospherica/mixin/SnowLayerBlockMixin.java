package dev.dabrelity.atmospherica.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SnowLayerBlock.class})
public class SnowLayerBlockMixin {
   @Inject(
      method = {"m_213898_"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   public void editRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo callbackInfo) {
      callbackInfo.cancel();
   }
}
