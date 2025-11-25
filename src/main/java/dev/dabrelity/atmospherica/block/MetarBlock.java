package dev.dabrelity.atmospherica.block;

import dev.dabrelity.atmospherica.config.ClientConfig;
import dev.dabrelity.atmospherica.event.GameBusEvents;
import dev.dabrelity.atmospherica.networking.ModNetworking;
import dev.dabrelity.atmospherica.util.Util;
import dev.dabrelity.atmospherica.weather.Sounding;
import dev.dabrelity.atmospherica.weather.ThermodynamicEngine;
import dev.dabrelity.atmospherica.weather.ThermodynamicEngine.AtmosphericDataPoint;
import dev.dabrelity.atmospherica.weather.WeatherHandler;
import dev.dabrelity.atmospherica.weather.WindEngine;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public class MetarBlock extends Block {
   public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
   private Map<UUID, Long> lastInteractions = new HashMap();

   protected MetarBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(FACING, Direction.NORTH));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(new Property[]{FACING});
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState blockstate = super.getStateForPlacement(context);
      if (blockstate == null) {
         blockstate = this.defaultBlockState();
      }

      return (BlockState)blockstate.setValue(FACING, context.getHorizontalDirection().getOpposite());
   }

   @Override
   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
      if (!player.getItemInHand(hand).isEmpty()) {
         return InteractionResult.PASS;
      }

      if (!level.isClientSide) {
         UUID uuid = player.getUUID();
         long lastInteration = (Long)this.lastInteractions.getOrDefault(uuid, -10000L);
         long curTime = level.getGameTime();
         if (curTime - lastInteration > 40L) {
            this.lastInteractions.put(uuid, curTime);
            WeatherHandler weatherHandler = (WeatherHandler)GameBusEvents.MANAGERS.get(level.dimension());
            Vec3 wind = WindEngine.getWind(pos, level);
            int windAngle = Math.floorMod((int)Math.toDegrees(Math.atan2(wind.x, -wind.z)), 360);
            double windspeed = wind.length();
            ThermodynamicEngine.AtmosphericDataPoint sfc = ThermodynamicEngine.samplePoint(weatherHandler, pos.getCenter(), level, null, 0);
            float riskV = 0.0F;
            float peakRiskOffset = 0.0F;
            float risk2 = 0.0F;
            float risk3 = 0.0F;

            for (int i = 0; i < 24000; i += 200) {
               Sounding sounding = new Sounding(weatherHandler, pos.getCenter(), level, 250, 16000, i);
               float r = sounding.getRisk(i);
               if (r > riskV) {
                  riskV = r;
                  peakRiskOffset = i;
               }
            }

            for (int ix = 24000; ix < 48000; ix += 400) {
               Sounding sounding = new Sounding(weatherHandler, pos.getCenter(), level, 250, 16000, ix);
               float r = sounding.getRisk(ix);
               if (r > risk2) {
                  risk2 = r;
               }
            }

            for (int ixx = 48000; ixx < 72000; ixx += 800) {
               Sounding sounding = new Sounding(weatherHandler, pos.getCenter(), level, 250, 16000, ixx);
               float r = sounding.getRisk(ixx);
               if (r > risk3) {
                  risk3 = r;
               }
            }

            float temperature = sfc.temperature();
            float dew = sfc.dewpoint();
            CompoundTag data = new CompoundTag();
            data.putString("packetCommand", "Metar");
            data.putString("command", "sendData");
            data.putFloat("temp", temperature);
            data.putFloat("dew", dew);
            data.putFloat("day1", riskV);
            data.putFloat("day2", risk2);
            data.putFloat("day3", risk3);
            data.putFloat("peakOffset", peakRiskOffset);
            data.putFloat("windAngle", windAngle);
            data.putDouble("windspeed", windspeed);
            ModNetworking.serverSendToClientPlayer(data, player);
         }
      }

      return InteractionResult.sidedSuccess(level.isClientSide());
   }

   @OnlyIn(Dist.CLIENT)
   public static void sendMessage(CompoundTag data) {
      if (Minecraft.getInstance().player != null) {
         String strForFormat = "Wind: %s° @ %s MPH\nTemp: %s°F\nDew: %s°F\n0-24hr Risk: %s\n24-48hr Risk: %s\n48-72hr Risk: %s\n0-24hr Risk Peak: %s";
         double windspeed = data.getDouble("windspeed");
         float windAngle = data.getFloat("windAngle");
         float temperature = data.getFloat("temp");
         float dew = data.getFloat("dew");
         if (ClientConfig.metric) {
            strForFormat = "Wind: %s° @ %s km/h\nTemp: %s°C\nDew: %s°C\n0-24hr Risk: %s\n24-48hr Risk: %s\n48-72hr Risk: %s\n0-24hr Risk Peak: %s";
            windspeed *= 1.609;
         } else {
            temperature = Util.celsiusToFahrenheit(temperature);
            dew = Util.celsiusToFahrenheit(dew);
         }

         float riskV = data.getFloat("day1");
         float risk2 = data.getFloat("day2");
         float risk3 = data.getFloat("day3");
         float peakRiskOffset = data.getFloat("peakOffset");
         String str = String.format(
            strForFormat,
            windAngle,
            (int)windspeed,
            (int)temperature,
            (int)dew,
            Util.riskToString(riskV),
            Util.riskToString(risk2),
            Util.riskToString(risk3),
            peakRiskOffset < 1200.0F ? "Now" : String.format("In %s minutes", (int)Math.floor(peakRiskOffset / 20.0F / 60.0F))
         );
         Minecraft.getInstance().player.sendSystemMessage(Component.literal(str));
      }
   }
}
