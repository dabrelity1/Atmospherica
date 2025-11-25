package dev.dabrelity.atmospherica.item;

import dev.dabrelity.atmospherica.block.entity.WeatherPlatformBlockEntity;
import dev.dabrelity.atmospherica.item.component.ModComponents;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public class ConnectorItem extends Item {
   public ConnectorItem(Item.Properties properties) {
      super(properties);
   }

   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
      super.inventoryTick(stack, level, entity, slotId, isSelected);
      if (!level.isClientSide() && ModComponents.WEATHER_BALLOON_PLATFORM.has(stack)) {
         BlockPos pos = ModComponents.WEATHER_BALLOON_PLATFORM.get(stack);
         if (pos != null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof WeatherPlatformBlockEntity)) {
               ModComponents.WEATHER_BALLOON_PLATFORM.remove(stack);
            }
         }
      }
   }

   @Override
   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
      super.appendHoverText(stack, level, tooltipComponents, tooltipFlag);
      if (ModComponents.WEATHER_BALLOON_PLATFORM.has(stack)) {
         BlockPos pos = ModComponents.WEATHER_BALLOON_PLATFORM.get(stack);
         if (pos != null) {
            tooltipComponents.add(Component.literal(String.format("Connected to: %s, %s, %s", pos.getX(), pos.getY(), pos.getZ())));
         }
      } else {
         tooltipComponents.add(Component.literal("Unset"));
      }
   }
}
