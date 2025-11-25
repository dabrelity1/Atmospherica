package dev.protomanly.pmweather.mixin;

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
      method = {"randomTick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void editRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo callbackInfo) {
      callbackInfo.cancel();
   }
}
