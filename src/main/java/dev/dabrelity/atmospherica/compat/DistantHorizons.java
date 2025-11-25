package dev.dabrelity.atmospherica.compat;

import dev.dabrelity.atmospherica.Atmospherica;
import org.joml.Matrix4f;

public class DistantHorizons {
   private static boolean initialized = false;
   private static boolean dhPresent = false;
   private static DistantHorizonsHandler handler = null;
   private static final int DEFAULT_DEPTH_TEXTURE_ID = -1;
   private static final Matrix4f DEFAULT_MATRIX = new Matrix4f();
   private static final float DEFAULT_NEAR_PLANE = 0.05F;
   private static final float DEFAULT_FAR_PLANE = 1024.0F;
   private static final int DEFAULT_RENDER_DISTANCE = 256;

   public static void initialize() {
      if (!initialized) {
         initialized = true;

         try {
            Class.forName("com.seibel.distanthorizons.api.DhApi");
            handler = new DistantHorizonsHandler();
            handler.initialize();
            dhPresent = true;
            Atmospherica.LOGGER.info("Distant Horizons compatibility initialized");
         } catch (NoClassDefFoundError | ClassNotFoundException var1) {
            Atmospherica.LOGGER.info("Distant Horizons not found, skipping integration");
            dhPresent = false;
            handler = null;
         } catch (Exception var2) {
            Atmospherica.LOGGER.error("Failed to initialize Distant Horizons compatibility", var2);
            dhPresent = false;
            handler = null;
         }
      }
   }

   public static boolean isAvailable() {
      return dhPresent && handler != null && handler.isDhReady();
   }

   public static int getDepthTextureId() {
      return isAvailable() ? handler.getDhDepthTextureId() : -1;
   }

   public static Matrix4f getDhProjectionMatrix() {
      return isAvailable() ? handler.getDhProjectionMatrix() : new Matrix4f(DEFAULT_MATRIX);
   }

   public static Matrix4f getDhModelViewMatrix() {
      return isAvailable() ? handler.getDhModelViewMatrix() : new Matrix4f(DEFAULT_MATRIX);
   }

   public static float getNearPlane() {
      return isAvailable() ? handler.getDhNearPlane() : 0.05F;
   }

   public static float getFarPlane() {
      return isAvailable() ? handler.getDhFarPlane() : 1024.0F;
   }

   public static int getChunkRenderDistance() {
      return isAvailable() ? handler.getDhRenderDistance() : 256;
   }
}
