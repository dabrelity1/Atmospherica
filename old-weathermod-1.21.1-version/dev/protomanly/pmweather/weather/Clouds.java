package dev.protomanly.pmweather.weather;

import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.util.ShaderCompatibleNoise;
import net.minecraft.util.Mth;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Clouds {
   public static float getCloudDensity(WeatherHandler weatherHandler, Vector2f location, float partialTicks) {
      float c = 0.0F;
      long seed = weatherHandler.seed;
      float worldTime = (float)weatherHandler.getWorld().getGameTime() + (float)seed / 1.0E14F + partialTicks;
      Vector3f noisePos = new Vector3f(location.x + worldTime, (float)ServerConfig.layer0Height, location.y + worldTime);
      Vector3f cloudNoisePos = new Vector3f(location.x + worldTime * 0.5F, (float)ServerConfig.layer0Height, location.y + worldTime * 0.5F);
      float detailNoise = ShaderCompatibleNoise.fbm(noisePos.div(90.0F), 3, 2.0F, 0.5F, 1.0F);
      float densityNoise = Math.min(ShaderCompatibleNoise.noise2D(new Vector2f(cloudNoisePos.x, cloudNoisePos.z).div(400.0F)) + detailNoise * 0.02F, 1.0F);
      float cloudNoise = Math.min(ShaderCompatibleNoise.noise2D(new Vector2f(noisePos.x, noisePos.z).div(30.0F)) + detailNoise * 0.05F, 1.0F);
      float heightNoise = ShaderCompatibleNoise.noise2D(new Vector2f(noisePos.x, noisePos.z).div(90.0F));
      float bgCloudHeight = Mth.lerp(Math.clamp((heightNoise + 1.0F) * 0.5F, 0.0F, 1.0F), 300.0F, 850.0F);
      float v = Math.clamp(densityNoise - (1.0F - (float)ServerConfig.overcastPercent), 0.0F, 1.0F);
      c += (float)Math.max(Math.pow(v, 0.25), 0.0);
      c *= Mth.lerp(v, Math.clamp(cloudNoise - 0.1F + v, 0.0F, 1.0F), 1.0F);
      c *= 0.9F + detailNoise * 0.1F;
      c = (float)Math.pow(c, 0.5) * 0.5F;
      c *= Mth.lerp((float)Math.sqrt(v), bgCloudHeight / 850.0F, 1.0F);
      double stormSize = ServerConfig.stormSize;

      for (Storm storm : weatherHandler.getStorms()) {
         Vector2f stormPos = new Vector2f((float)storm.position.x, (float)storm.position.z);
         float dist = stormPos.distance(location);
         float smoothStage = storm.stage + storm.energy / 100.0F;
         if (storm.stormType == 2) {
            c *= Mth.lerp(
               Math.clamp((storm.windspeed - 65) / 60.0F, 0.0F, 1.0F), 1.0F, (float)Math.pow(Math.clamp(dist / (storm.maxWidth * 0.1), 0.0, 1.0), 2.0)
            );
         }

         if (storm.stormType == 0) {
            c *= Mth.lerp(Math.clamp(smoothStage, 0.0F, 1.0F), 1.0F, (float)Math.pow(Math.clamp(dist / (stormSize * 4.0), 0.0, 1.0), 1.5));
         }
      }

      return c * (float)ServerConfig.overcastPercent;
   }
}
