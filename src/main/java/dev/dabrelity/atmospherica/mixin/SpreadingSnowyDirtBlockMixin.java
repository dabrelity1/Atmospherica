package dev.dabrelity.atmospherica.mixin;

import dev.dabrelity.atmospherica.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SpreadingSnowyDirtBlock.class})
public class SpreadingSnowyDirtBlockMixin {
   @Inject(
      method = {"m_213898_"},
      at = {@At("HEAD")},
      remap = false
   )
   public void editRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo callbackInfo) {
      Util.checkLogs(state, level, pos, random.nextInt(4) - 1);
   }
}
