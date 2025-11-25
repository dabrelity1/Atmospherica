package dev.dabrelity.atmospherica.compat;

import dev.dabrelity.atmospherica.Atmospherica;
import org.joml.Matrix4f;

/**
 * Distant Horizons compatibility handler.
 * This class is a stub that does nothing when DH is not present.
 * If DH API is available at runtime, it can be extended.
 */
public class DistantHorizonsHandler {
   private boolean dhReady = false;
   private int dhDepthTextureId = -1;
   private Matrix4f dhProjectionMatrix = new Matrix4f();
   private Matrix4f dhModelViewMatrix = new Matrix4f();
   private float dhNearPlane = 0.05F;
   private float dhFarPlane = 1024.0F;
   private int dhRenderDistance = 256;

   public void initialize() {
      // DH API not available in 1.20.1 Forge port - stub implementation
      Atmospherica.LOGGER.debug("DistantHorizons integration disabled (API not available)");
   }

   public boolean isDhReady() {
      return dhReady;
   }

   public int getDhDepthTextureId() {
      return dhDepthTextureId;
   }

   public Matrix4f getDhProjectionMatrix() {
      return dhProjectionMatrix;
   }

   public Matrix4f getDhModelViewMatrix() {
      return dhModelViewMatrix;
   }

   public float getDhNearPlane() {
      return dhNearPlane;
   }

   public float getDhFarPlane() {
      return dhFarPlane;
   }

   public int getDhRenderDistance() {
      return dhRenderDistance;
   }
}
