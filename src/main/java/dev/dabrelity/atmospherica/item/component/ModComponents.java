package dev.dabrelity.atmospherica.item.component;

import dev.dabrelity.atmospherica.Atmospherica;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ModComponents {
   private ModComponents() {
   }

   public static final WeatherBalloonPlatformComponent WEATHER_BALLOON_PLATFORM = new WeatherBalloonPlatformComponent();

   public static final class WeatherBalloonPlatformComponent {
      private static final String TAG = Atmospherica.MOD_ID + ":weather_balloon_platform";

      public void set(ItemStack stack, BlockPos pos) {
          stack.getOrCreateTag().putLong(TAG, pos.asLong());
      }

      public boolean has(ItemStack stack) {
         return this.get(stack) != null;
      }

      public void remove(ItemStack stack) {
         CompoundTag tag = stack.getTag();
         if (tag != null) {
            tag.remove(TAG);
            if (tag.isEmpty()) {
               stack.setTag(null);
            }
         }
      }

      @Nullable
      public BlockPos get(ItemStack stack) {
         CompoundTag tag = stack.getTag();
         if (tag != null && tag.contains(TAG)) {
            return BlockPos.of(tag.getLong(TAG));
         }

         return null;
      }
   }
}
