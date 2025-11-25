package dev.protomanly.pmweather.render;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.protomanly.pmweather.config.ServerConfig;
import java.awt.Color;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fStack;

public class CustomLightningRenderer {
   public static void render(Vec3 pos, long seed, Camera camera, Color color) {
      if (Minecraft.getInstance().player != null) {
         Random rand = new Random(seed);
         Vec3 camPos = camera.getPosition();
         Vec3 offset = pos.subtract(camPos);
         Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
         matrix4fStack.pushMatrix();
         matrix4fStack.translate((float)offset.x, (float)offset.y, (float)offset.z);
         RenderSystem.applyModelViewMatrix();
         RenderSystem.enableBlend();
         RenderSystem.depthMask(true);
         RenderSystem.enableDepthTest();
         RenderSystem.setShader(GameRenderer::getPositionColorShader);
         RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
         Tesselator tesselator = Tesselator.getInstance();
         BufferBuilder bufferBuilder = tesselator.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
         Vec3 lastPos = new Vec3(0.0, ServerConfig.layer0Height * 2.0 - pos.y, 0.0);
         float r = 3.0F;

         while (lastPos.y > 0.0 && r > 0.1F) {
            Vec3 newPos = lastPos.add(rand.nextFloat(-6.0F, 6.0F), rand.nextFloat(-20.0F, 0.0F), rand.nextFloat(-6.0F, 6.0F));
            box(
               bufferBuilder,
               color,
               newPos.add(-1.0F * r, 0.0, -1.0F * r),
               newPos.add(-1.0F * r, 0.0, 1.0F * r),
               newPos.add(1.0F * r, 0.0, 1.0F * r),
               newPos.add(1.0F * r, 0.0, -1.0F * r),
               lastPos.add(-1.0F * r, 0.0, -1.0F * r),
               lastPos.add(-1.0F * r, 0.0, 1.0F * r),
               lastPos.add(1.0F * r, 0.0, 1.0F * r),
               lastPos.add(1.0F * r, 0.0, -1.0F * r)
            );
            lastPos = newPos;
            if (rand.nextInt(8) == 0) {
               float r2 = r * 0.8F;
               if (r2 > 0.1F) {
                  r *= 0.6F;
                  Vec3 lP = newPos;
                  int n = rand.nextInt(3, 10);

                  for (int i = 0; i < n; i++) {
                     Vec3 nP = lP.add(rand.nextFloat(-50.0F, 50.0F) / (i + 1), rand.nextFloat(-10.0F, 5.0F), rand.nextFloat(-50.0F, 50.0F) / (i + 1));
                     box(
                        bufferBuilder,
                        color,
                        nP.add(-1.0F * r2, 0.0, -1.0F * r2),
                        nP.add(-1.0F * r2, 0.0, 1.0F * r2),
                        nP.add(1.0F * r2, 0.0, 1.0F * r2),
                        nP.add(1.0F * r2, 0.0, -1.0F * r2),
                        lP.add(-1.0F * r2, 0.0, -1.0F * r2),
                        lP.add(-1.0F * r2, 0.0, 1.0F * r2),
                        lP.add(1.0F * r2, 0.0, 1.0F * r2),
                        lP.add(1.0F * r2, 0.0, -1.0F * r2)
                     );
                     lP = nP;
                  }
               }
            }
         }

         matrix4fStack.translate(-((float)offset.x), -((float)offset.y), -((float)offset.z));
         matrix4fStack.popMatrix();
         MeshData meshData = bufferBuilder.build();
         if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
         }

         RenderSystem.applyModelViewMatrix();
         RenderSystem.disableBlend();
         RenderSystem.defaultBlendFunc();
      }
   }

   public static void box(BufferBuilder bufferBuilder, Color color, Vec3 btl, Vec3 bbl, Vec3 bbr, Vec3 btr, Vec3 ttl, Vec3 tbl, Vec3 tbr, Vec3 ttr) {
      quad(bufferBuilder, color, btl, bbl, bbr, btr, true);
      quad(bufferBuilder, color, ttl, tbl, tbr, ttr, false);
      quad(bufferBuilder, color, ttl, btl, btr, ttr, false);
      quad(bufferBuilder, color, ttl, btl, bbl, tbl, false);
      quad(bufferBuilder, color, tbr, bbr, btr, ttr, false);
      quad(bufferBuilder, color, tbr, bbr, bbl, tbl, false);
   }

   public static void quad(BufferBuilder bufferBuilder, Color color, Vec3 tl, Vec3 bl, Vec3 br, Vec3 tr, boolean clockwise) {
      float r = color.getRed() / 255.0F;
      float g = color.getGreen() / 255.0F;
      float b = color.getBlue() / 255.0F;
      float a = color.getAlpha() / 255.0F;
      if (clockwise) {
         bufferBuilder.addVertex(tr.toVector3f()).setColor(r, g, b, a);
         bufferBuilder.addVertex(br.toVector3f()).setColor(r, g, b, a);
         bufferBuilder.addVertex(bl.toVector3f()).setColor(r, g, b, a);
         bufferBuilder.addVertex(tl.toVector3f()).setColor(r, g, b, a);
      } else {
         bufferBuilder.addVertex(tl.toVector3f()).setColor(r, g, b, a);
         bufferBuilder.addVertex(bl.toVector3f()).setColor(r, g, b, a);
         bufferBuilder.addVertex(br.toVector3f()).setColor(r, g, b, a);
         bufferBuilder.addVertex(tr.toVector3f()).setColor(r, g, b, a);
      }
   }
}
