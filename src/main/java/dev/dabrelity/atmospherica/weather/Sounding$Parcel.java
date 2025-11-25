package dev.dabrelity.atmospherica.weather;

import dev.dabrelity.atmospherica.util.Util;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;

public class Sounding.Parcel {
   public Sounding sounding;
   public ThermodynamicEngine.AtmosphericDataPoint parcel;
   public Map<Float, Float> profile = new HashMap();
   public float lclP;
   public float lfcP = -1.0F;
   public float elP = -1.0F;

   public Sounding.Parcel(Sounding sounding, ThermodynamicEngine.AtmosphericDataPoint parcel) {
      this.sounding = sounding;
      this.parcel = parcel;
      Sounding.Parcel.LCL lcl = DryLift(parcel.pressure(), parcel.temperature(), parcel.dewpoint());
      this.lclP = lcl.pressure();
      List<Map.Entry<Integer, ThermodynamicEngine.AtmosphericDataPoint>> set = sounding.data.entrySet().stream().toList();
      Map<Integer, ThermodynamicEngine.AtmosphericDataPoint> lower = new HashMap();
      Map<Integer, ThermodynamicEngine.AtmosphericDataPoint> upper = new HashMap();
      float lowMinP = 10000.0F;

      for (Map.Entry<Integer, ThermodynamicEngine.AtmosphericDataPoint> entry : set) {
         int height = entry.getKey();
         ThermodynamicEngine.AtmosphericDataPoint dataPoint = entry.getValue();
         if (!(dataPoint.pressure() > parcel.pressure())) {
            if (dataPoint.pressure() >= lcl.pressure()) {
               lower.put(height, dataPoint);
               lowMinP = Math.min(dataPoint.pressure(), lowMinP);
            } else {
               upper.put(height, dataPoint);
            }
         }
      }

      Map<Float, Float> tLower = this.GetDryLapse(lower, parcel.virtualTemperature(), parcel.dewpoint(), parcel.pressure());
      Map<Float, Float> tUpper = this.GetMoistLapse(upper, (Float)tLower.getOrDefault(lowMinP, lcl.temp()), lowMinP);
      this.profile.putAll(tLower);
      this.profile.putAll(tUpper);
      List<Map.Entry<Float, Float>> profileSetAsc = this.profile.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
      List<Map.Entry<Float, Float>> profileSetDesc = profileSetAsc.reversed();
      List<Map.Entry<Integer, ThermodynamicEngine.AtmosphericDataPoint>> soundingSetAsc = sounding.data
         .entrySet()
         .stream()
         .sorted(Map.Entry.comparingByKey())
         .toList();
      List<Map.Entry<Integer, ThermodynamicEngine.AtmosphericDataPoint>> soundingSetDesc = soundingSetAsc.reversed();

      for (Map.Entry<Integer, ThermodynamicEngine.AtmosphericDataPoint> entryx : soundingSetAsc) {
         ThermodynamicEngine.AtmosphericDataPoint dataPoint = entryx.getValue();
         float p = dataPoint.pressure();
         float t = (Float)this.profile.getOrDefault(p, -100.0F);
         if (!(p > this.lclP) && t >= dataPoint.virtualTemperature()) {
            this.lfcP = p;
            break;
         }
      }

      for (Map.Entry<Integer, ThermodynamicEngine.AtmosphericDataPoint> entryxx : soundingSetDesc) {
         ThermodynamicEngine.AtmosphericDataPoint dataPoint = entryxx.getValue();
         float p = dataPoint.pressure();
         float t = (Float)this.profile.getOrDefault(p, -100.0F);
         if (p >= this.lclP || this.lfcP > 0.0F && p > this.lfcP) {
            break;
         }

         if (t >= dataPoint.virtualTemperature()) {
            this.elP = p;
            break;
         }
      }
   }

   public Map<Float, Float> GetDryLapse(Map<Integer, ThermodynamicEngine.AtmosphericDataPoint> l, float t, float dp, float p) {
      Sounding.Parcel.LCL lcl = DryLift(p, t, dp);
      Map<Float, Float> r = new HashMap();

      for (Map.Entry<Integer, ThermodynamicEngine.AtmosphericDataPoint> entry : l.entrySet().stream().toList()) {
         ThermodynamicEngine.AtmosphericDataPoint dataPoint = entry.getValue();
         r.put(dataPoint.pressure(), Mth.lerp(1.0F - (dataPoint.pressure() - lcl.pressure()) / (p - lcl.pressure()), t, lcl.temp()));
      }

      return r;
   }

   public Map<Float, Float> GetMoistLapse(Map<Integer, ThermodynamicEngine.AtmosphericDataPoint> l, float t, float p) {
      Map<Float, Float> r = new HashMap();

      for (Map.Entry<Integer, ThermodynamicEngine.AtmosphericDataPoint> entry : l.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
         ThermodynamicEngine.AtmosphericDataPoint dataPoint = entry.getValue();
         r.put(dataPoint.pressure(), WetLift(p, t, dataPoint.pressure()));
      }

      return r;
   }

   public static float LCLTemp(float t, float dp) {
      float s = t - dp;
      float dlt = s * (1.2185F + 0.001278F * t + s * (-0.00219F + 1.173E-5F * s - 5.2E-6F * t));
      return t - dlt * 0.6F;
   }

   public static float Theta(float p, float t, @Nullable Float p2) {
      if (p2 == null) {
         p2 = 1000.0F;
      }

      return Util.kelvinToCelsius(Util.celsiusToKelvin(t) * (float)Math.pow(p2 / p, Util.ROCP));
   }

   public static float ThaLVL(float theta, float t) {
      return 1000.0F / (float)Math.pow(Util.celsiusToKelvin(theta) / Util.celsiusToKelvin(t), 1.0F / Util.ROCP);
   }

   public static float SatLift(float p, float thetam, @Nullable Float conv) {
      if (conv == null) {
         conv = 100.0F;
      }

      if (Math.abs(p - 1000.0F) - 0.001F <= 0.0F) {
         return thetam;
      } else {
         float eor = 999.0F;
         float pwrp = 0.0F;
         float t1 = 0.0F;
         float t2 = 0.0F;
         float e1 = 0.0F;
         float e2 = 0.0F;

         while (Math.abs(eor) - conv > 0.0F) {
            float rate;
            if (eor == 999.0F) {
               pwrp = (float)Math.pow(p / 1000.0F, Util.ROCP);
               t1 = Util.kelvinToCelsius(Util.celsiusToKelvin(thetam) * pwrp);
               e1 = Wobf(t1) - Wobf(thetam);
               rate = 1.0F;
            } else {
               rate = (t2 - t1) / (e2 - e1);
               t1 = t2;
               e1 = e2;
            }

            t2 = t1 - e1 * rate;
            float var10 = Util.kelvinToCelsius(Util.celsiusToKelvin(t2) / pwrp);
            e2 = var10 + (Wobf(t2) - Wobf(var10) - thetam);
            eor = e2 * rate;
         }

         return t2 - eor;
      }
   }

   public static float Wobf(float t) {
      t -= 20.0F;
      if (t <= 0.0F) {
         float npol = 1.0F + t * (-0.008841661F + t * (1.4714143E-4F + t * (-9.671988E-7F + t * (-3.260722E-8F + t * -3.8598072E-10F))));
         return 15.13F / (float)Math.pow(npol, 4.0);
      } else {
         float ppol = t * (4.961892E-7F + t * (-6.1059366E-9F + t * (3.940155E-11F + t * (-1.258813E-13F + t * 1.668828E-16F))));
         ppol = 1.0F + t * (0.0036182988F + t * (-1.3603273E-5F + ppol));
         return 29.93F / (float)Math.pow(ppol, 4.0) + 0.96F * t - 14.8F;
      }
   }

   public static float WetLift(float p, float t, float p2) {
      float thta = Theta(p, t, null);
      float thetam = thta - Wobf(thta) + Wobf(t);
      return SatLift(p2, thetam, null);
   }

   public static Sounding.Parcel.LCL DryLift(float p, float t, float dp) {
      float t2 = LCLTemp(t, dp);
      float p2 = ThaLVL(Theta(p, t, null), t2);
      return new Sounding.Parcel.LCL(p2, t2);
   }

   public record LCL(float pressure, float temp) {
   }
}
