package dev.dabrelity.atmospherica.util;

import dev.dabrelity.atmospherica.Atmospherica;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import net.minecraft.util.Mth;

public class Sampler2D {
   private final float[] data;
   private final int width;
   private final int height;
   // Optimization: Store masks for power-of-two wrapping
   private final int widthMask;
   private final int heightMask;
   // Optimization: Store shift for power-of-two index calculation
   private final int widthShift;
   private boolean bilinear = false;

   public Sampler2D(String path) {
      // Try multiple ways to load the resource
      URL url = Thread.currentThread().getContextClassLoader().getResource(path);
      if (url == null) {
         url = Sampler2D.class.getClassLoader().getResource(path);
      }
      if (url == null) {
         url = Sampler2D.class.getResource("/" + path);
      }
      if (url == null) {
         Atmospherica.LOGGER.error("Could not find texture resource: {}", path);
      }

      BufferedImage bufferedImage;
      try {
         bufferedImage = ImageIO.read(url);
      } catch (IOException | IllegalArgumentException var6) {
         Atmospherica.LOGGER.error("Failed to load texture {}: {}", path, var6.getMessage());
         bufferedImage = new BufferedImage(1, 1, 2);
      }

      this.width = bufferedImage.getWidth();
      this.height = bufferedImage.getHeight();
      this.data = new float[this.width * this.height];
      int[] pixels = new int[this.data.length];
      bufferedImage.getRGB(0, 0, this.width, this.height, pixels, 0, this.width);

      for (int i = 0; i < this.data.length; i++) {
         this.data[i] = (pixels[i] & 0xFF) / 255.0F;
      }

      // Initialize optimization fields
      this.widthMask = (this.width > 0 && (this.width & (this.width - 1)) == 0) ? this.width - 1 : -1;
      this.heightMask = (this.height > 0 && (this.height & (this.height - 1)) == 0) ? this.height - 1 : -1;
      this.widthShift = (this.widthMask != -1) ? Integer.numberOfTrailingZeros(this.width) : -1;
   }

   private static float interpolate2D(float x, float y, float v1, float v2, float v3, float v4) {
      return Mth.lerp(y, Mth.lerp(x, v1, v2), Mth.lerp(x, v3, v4));
   }

   public float sample(float x, float y) {
      x *= this.width;
      y *= this.height;
      int x1 = Mth.floor(x);
      int y1 = Mth.floor(y);

      float dx = x - (float)x1;
      float dy = y - (float)y1;

      int x2 = x1 + 1;
      int y2 = y1 + 1;

      // Wrap x
      if (widthMask != -1) {
         x1 &= widthMask;
         x2 &= widthMask;
      } else {
         x1 %= width; if (x1 < 0) x1 += width;
         x2 %= width; if (x2 < 0) x2 += width;
      }

      // Wrap y
      if (heightMask != -1) {
         y1 &= heightMask;
         y2 &= heightMask;
      } else {
         y1 %= height; if (y1 < 0) y1 += height;
         y2 %= height; if (y2 < 0) y2 += height;
      }

      float a = this.data[this.getIndex(x1, y1)];
      float b = this.data[this.getIndex(x2, y1)];
      float c = this.data[this.getIndex(x1, y2)];
      float d = this.data[this.getIndex(x2, y2)];

      if (this.bilinear) {
         dx = this.smoothStep(dx);
         dy = this.smoothStep(dy);
      }

      return interpolate2D(dx, dy, a, b, c, d);
   }

   public void setBilinear(boolean bilinear) {
      this.bilinear = bilinear;
   }

   private int getIndex(int x, int y) {
      if (widthShift != -1) {
         return (y << widthShift) + x;
      }
      return y * this.width + x;
   }

   private float smoothStep(float x) {
      return x * x * x * (x * (x * 6.0F - 15.0F) + 10.0F);
   }
}
