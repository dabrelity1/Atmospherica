package dev.protomanly.pmweather.util;

import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import dev.protomanly.pmweather.weather.WeatherHandler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;

public class Util {
   public static Vec3[] RAIN_POSITIONS = new Vec3[Util.MAX_RAIN_DROPS];
   public static int MAX_RAIN_DROPS = 2000;
   public static Map<Block, Block> STRIPPED_VARIANTS = new HashMap<Block, Block>() {
      {
         this.put(Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG);
         this.put(Blocks.ACACIA_LOG, Blocks.STRIPPED_ACACIA_LOG);
         this.put(Blocks.BIRCH_LOG, Blocks.STRIPPED_BIRCH_LOG);
         this.put(Blocks.CHERRY_LOG, Blocks.STRIPPED_CHERRY_LOG);
         this.put(Blocks.DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_LOG);
         this.put(Blocks.JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_LOG);
         this.put(Blocks.MANGROVE_LOG, Blocks.STRIPPED_MANGROVE_LOG);
         this.put(Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG);
      }
   };
   public static float ROCP;

   public static void checkLogs(BlockState state, ServerLevel level, BlockPos pos) {
   }

   public static void checkLogs(BlockState state, ServerLevel level, BlockPos pos, int y) {
   }

   public static boolean canLogSurvive(BlockState state, ServerLevel level, BlockPos pos, List<BlockPos> checked) {
      return true;
   }

   public static boolean canWindAffect(Vec3 pos, Level level) {
      BlockHitResult upRay = level.clip(
         new ClipContext(
            pos.add(0.0, 0.55, 0.0), pos.add(0.0, 128.0, 0.0), net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, CollisionContext.empty()
         )
      );
      BlockHitResult pxRay = level.clip(
         new ClipContext(
            pos.add(1.0, 0.55, 0.0), pos.add(64.0, 128.0, 0.0), net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, CollisionContext.empty()
         )
      );
      BlockHitResult nxRay = level.clip(
         new ClipContext(
            pos.add(-1.0, 0.55, 0.0), pos.add(-64.0, 128.0, 0.0), net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, CollisionContext.empty()
         )
      );
      BlockHitResult pzRay = level.clip(
         new ClipContext(
            pos.add(0.0, 0.55, 1.0), pos.add(0.0, 128.0, 64.0), net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, CollisionContext.empty()
         )
      );
      BlockHitResult nzRay = level.clip(
         new ClipContext(
            pos.add(0.0, 0.55, -1.0), pos.add(0.0, 128.0, -64.0), net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, CollisionContext.empty()
         )
      );
      return upRay.getType() == Type.MISS
         || pxRay.getType() == Type.MISS
         || nxRay.getType() == Type.MISS
         || pzRay.getType() == Type.MISS
         || nzRay.getType() == Type.MISS;
   }

   public static Vec2 mulVec2(Vec2 a, Vec2 b) {
      return new Vec2(a.x * b.x, a.y * b.y);
   }

   public static Vec2 mulVec2(Vec2 a, float b) {
      return new Vec2(a.x * b, a.y * b);
   }

   public static Vec2 nearestPoint(Vec2 v, Vec2 w, Vec2 p) {
      float l2 = v.distanceToSqr(w);
      float t = Mth.clamp(p.add(v.negated()).dot(w.add(v.negated())) / l2, 0.0F, 1.0F);
      return v.add(mulVec2(w.add(v.negated()), t));
   }

   public static float minimumDistance(Vec2 v, Vec2 w, Vec2 p) {
      float l2 = v.distanceToSqr(w);
      if (l2 == 0.0F) {
         return Mth.sqrt(p.distanceToSqr(v));
      } else {
         Vec2 proj = nearestPoint(v, w, p);
         return Mth.sqrt(p.distanceToSqr(proj));
      }
   }

   public static boolean isInteger(String string) {
      try {
         Integer.parseInt(string);
         return true;
      } catch (NumberFormatException var2) {
         return false;
      }
   }

   public static float celsiusToFahrenheit(float t) {
      return t * 1.8F + 32.0F;
   }

   public static float fahrenheitToCelsius(float t) {
      return (t - 32.0F) * 0.5555556F;
   }

   public static float celsiusToKelvin(float t) {
      return t + 273.15F;
   }

   public static float kelvinToCelsius(float t) {
      return t - 273.15F;
   }

   public static float MixingRatio(float vapprs, float prs, @Nullable Float molWeight) {
      if (molWeight == null) {
         molWeight = 0.62197F;
      }

      return molWeight * (vapprs / (prs - vapprs));
   }

   public static float SaturationVaporPressure(float t) {
      return 6.112F * (float)Math.exp(17.67F * t / (t + 243.5F));
   }

   public static String riskToString(float riskV) {
      String risk = "NONE (0/6)";
      if (riskV > 1.5F) {
         risk = "HIGH (6/6)";
      } else if (riskV > 1.2F) {
         risk = "MDT (5/6)";
      } else if (riskV > 0.8F) {
         risk = "ENH (4/6)";
      } else if (riskV > 0.6F) {
         risk = "SLGT (3/6)";
      } else if (riskV > 0.3F) {
         risk = "MRGL (2/6)";
      } else if (riskV > 0.15F) {
         risk = "TSTM (1/6)";
      }

      return risk;
   }

   public static float SaturationMixingRatio(float tp, float t) {
      return MixingRatio(SaturationVaporPressure(t), tp, null);
   }

   public static Vec3 rotatePoint(Vec3 point, Vec3 origin, double angle) {
      point = point.subtract(origin);
      double x = point.x * Math.cos(angle) - point.z * Math.sin(angle);
      double z = point.z * Math.cos(angle) + point.x * Math.sin(angle);
      return new Vec3(x + origin.x, point.y, z + origin.z);
   }

   @Nullable
   public static Vec3 getValidTropicalSystemSpawn(WeatherHandler weatherHandler, Vec3 origin, float area) {
      for (int i = 0; i < 35; i++) {
         Vec3 check = origin.add(PMWeather.RANDOM.nextFloat(-area, area), 0.0, PMWeather.RANDOM.nextFloat(-area, area));
         Float sst = ThermodynamicEngine.GetSST(weatherHandler, check, weatherHandler.getWorld(), null, 0);
         if (sst != null && sst > 25.0F) {
            return check;
         }
      }

      return null;
   }

   static {
      float range = 10.0F;

      for (int i = 0; i < MAX_RAIN_DROPS; i++) {
         RAIN_POSITIONS[i] = new Vec3(
            PMWeather.RANDOM.nextFloat() * range - range / 2.0F,
            PMWeather.RANDOM.nextFloat() * range - range / 2.0F,
            PMWeather.RANDOM.nextFloat() * range - range / 2.0F
         );
      }

      ROCP = 0.28571427F;
   }
}
