package dev.dabrelity.atmospherica.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import dev.dabrelity.atmospherica.block.SoundingViewerBlock;
import dev.dabrelity.atmospherica.block.entity.SoundingViewerBlockEntity;
import dev.dabrelity.atmospherica.block.entity.WeatherPlatformBlockEntity;
import dev.dabrelity.atmospherica.weather.Sounding;
import dev.dabrelity.atmospherica.weather.Sounding.Parcel;
import dev.dabrelity.atmospherica.weather.ThermodynamicEngine.AtmosphericDataPoint;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SoundingViewerRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
   public int maxDisplayHeight = 16000;

   public SoundingViewerRenderer(Context context) {
   }

   public void render(T blockEntity, float v, PoseStack poseStack, MultiBufferSource multiBufferSource, int j, int j1) {
      if (blockEntity instanceof SoundingViewerBlockEntity soundingViewerBlockEntity) {
         if (Minecraft.getInstance().player.position().distanceTo(blockEntity.getBlockPos().getCenter()) > 25.0) {
            return;
         }

         if (soundingViewerBlockEntity.isConnected
            && blockEntity.getLevel().getBlockEntity(soundingViewerBlockEntity.connectedTo) instanceof WeatherPlatformBlockEntity weatherPlatformBlockEntity
            && weatherPlatformBlockEntity.sounding != null) {
            BlockState state = blockEntity.getBlockState();
            Direction direction = (Direction)state.getValue(SoundingViewerBlock.FACING);
            PoseStack modelViewStack = RenderSystem.getModelViewStack();
            modelViewStack.pushPose();
            modelViewStack.mulPoseMatrix(poseStack.last().pose());
            modelViewStack.translate(0.5F, 0.5F, 0.5F);

            Quaternionf rotation = switch (direction) {
               case NORTH -> Axis.YP.rotationDegrees(180.0F);
               case EAST -> Axis.YP.rotationDegrees(90.0F);
               case WEST -> Axis.YP.rotationDegrees(270.0F);
               default -> Axis.YP.rotationDegrees(0.0F);
            };
            modelViewStack.mulPose(rotation);
            modelViewStack.translate(0.0F, 0.0F, 0.55F);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.enableBlend();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.defaultBlendFunc();
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder bufferBuilder = tesselator.getBuilder();
            bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            float r = 0.0F;
            float g = 0.0F;
            float b = 0.0F;
            float a = 1.0F;
            Vector3f topLeft = new Vector3f(-1.0F, 1.0F, 0.0F).mul(1.0F);
            Vector3f bottomLeft = new Vector3f(-1.0F, -1.0F, 0.0F).mul(1.0F);
            Vector3f bottomRight = new Vector3f(1.0F, -1.0F, 0.0F).mul(1.0F);
            Vector3f topRight = new Vector3f(1.0F, 1.0F, 0.0F).mul(1.0F);
            bufferBuilder.vertex(topLeft.x(), topLeft.y(), topLeft.z())
               .color(r, g, b, a)
               .endVertex();
            bufferBuilder.vertex(bottomLeft.x(), bottomLeft.y(), bottomLeft.z())
               .color(r, g, b, a)
               .endVertex();
            bufferBuilder.vertex(bottomRight.x(), bottomRight.y(), bottomRight.z())
               .color(r, g, b, a)
               .endVertex();
            bufferBuilder.vertex(topRight.x(), topRight.y(), topRight.z())
               .color(r, g, b, a)
               .endVertex();
            BufferUploader.drawWithShader(bufferBuilder.end());

            Sounding sounding = weatherPlatformBlockEntity.sounding;
            Sounding.Parcel parcel = sounding.getSBParcel();
            List<Map.Entry<Integer, dev.dabrelity.atmospherica.weather.ThermodynamicEngine.AtmosphericDataPoint>> set = sounding.data
               .entrySet()
               .stream()
               .sorted(Map.Entry.comparingByKey())
               .toList();
            Vec2 lastTempPoint = null;
            Vec2 lastVTempPoint = null;
            Vec2 lastDewPoint = null;
            Vec2 lastParcelPoint = null;
            BufferBuilder lineBuilder = tesselator.getBuilder();
            lineBuilder.begin(Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            float minPressure = 100.0F;
            float maxPressure = 1050.0F;

            for (int i = 100; i <= 1000; i += 100) {
               Vec2 p = Sounding.getPosition(0.0F, i, minPressure, maxPressure, 40.0F);
               lineBuilder.vertex(-1.0F, p.y, 0.005F).color(0.4F, 0.4F, 0.4F, 1.0F).endVertex();
               lineBuilder.vertex(1.0F, p.y, 0.005F).color(0.4F, 0.4F, 0.4F, 1.0F).endVertex();
            }

            for (Map.Entry<Integer, dev.dabrelity.atmospherica.weather.ThermodynamicEngine.AtmosphericDataPoint> entry : set) {
               int height = entry.getKey();
               dev.dabrelity.atmospherica.weather.ThermodynamicEngine.AtmosphericDataPoint atmosphericDataPoint = entry.getValue();
               if (atmosphericDataPoint.pressure() < minPressure) {
                  break;
               }

               Vec2 p = Sounding.getPosition(atmosphericDataPoint.temperature(), atmosphericDataPoint.pressure(), minPressure, maxPressure, 40.0F);
               if (lastTempPoint != null) {
                  lineBuilder.vertex(lastTempPoint.x, lastTempPoint.y, 0.01F).color(1.0F, 0.0F, 0.0F, 1.0F).endVertex();
                  lineBuilder.vertex(p.x, p.y, 0.01F).color(1.0F, 0.0F, 0.0F, 1.0F).endVertex();
               }

               lastTempPoint = p;
               p = Sounding.getPosition(atmosphericDataPoint.virtualTemperature(), atmosphericDataPoint.pressure(), minPressure, maxPressure, 40.0F);
               if (lastVTempPoint != null) {
                  lineBuilder.vertex(lastVTempPoint.x, lastVTempPoint.y, 0.005F).color(0.3F, 0.0F, 0.0F, 1.0F).endVertex();
                  lineBuilder.vertex(p.x, p.y, 0.005F).color(0.3F, 0.0F, 0.0F, 1.0F).endVertex();
               }

               lastVTempPoint = p;
               p = Sounding.getPosition(atmosphericDataPoint.dewpoint(), atmosphericDataPoint.pressure(), minPressure, maxPressure, 40.0F);
               if (lastDewPoint != null) {
                  lineBuilder.vertex(lastDewPoint.x, lastDewPoint.y, 0.01F).color(0.0F, 1.0F, 0.0F, 1.0F).endVertex();
                  lineBuilder.vertex(p.x, p.y, 0.01F).color(0.0F, 1.0F, 0.0F, 1.0F).endVertex();
               }

               lastDewPoint = p;
               if (parcel != null) {
                  Float t = (Float)parcel.profile.getOrDefault(atmosphericDataPoint.pressure(), null);
                  p = Sounding.getPosition(t, atmosphericDataPoint.pressure(), minPressure, maxPressure, 40.0F);
                  if (lastParcelPoint != null && p.x > -1.0F) {
                     lineBuilder.vertex(lastParcelPoint.x, lastParcelPoint.y, 0.02F).color(0.8F, 0.8F, 0.8F, 1.0F).endVertex();
                     lineBuilder.vertex(p.x, p.y, 0.02F).color(0.8F, 0.8F, 0.8F, 1.0F).endVertex();
                  }

                  lastParcelPoint = p;
               }
            }

            if (parcel != null) {
               if (parcel.lclP > 0.0F) {
                  Vec2 px = Sounding.getPosition(0.0F, parcel.lclP, minPressure, maxPressure, 40.0F);
                  lineBuilder.vertex(0.95F, px.y, 0.01F).color(0.0F, 1.0F, 0.0F, 1.0F).endVertex();
                  lineBuilder.vertex(1.0F, px.y, 0.01F).color(0.0F, 1.0F, 0.0F, 1.0F).endVertex();
               }

               if (parcel.lfcP > 0.0F) {
                  Vec2 px = Sounding.getPosition(0.0F, parcel.lfcP, minPressure, maxPressure, 40.0F);
                  lineBuilder.vertex(0.95F, px.y, 0.01F).color(1.0F, 1.0F, 0.0F, 1.0F).endVertex();
                  lineBuilder.vertex(1.0F, px.y, 0.01F).color(1.0F, 1.0F, 0.0F, 1.0F).endVertex();
               }

               if (parcel.elP > 0.0F) {
                  Vec2 px = Sounding.getPosition(0.0F, parcel.elP, minPressure, maxPressure, 40.0F);
                  lineBuilder.vertex(0.95F, px.y, 0.01F).color(1.0F, 0.0F, 1.0F, 1.0F).endVertex();
                  lineBuilder.vertex(1.0F, px.y, 0.01F).color(1.0F, 0.0F, 1.0F, 1.0F).endVertex();
               }
            }

            modelViewStack.mulPoseMatrix(poseStack.last().pose().invert());
            modelViewStack.translate(-0.5F, -0.5F, -0.5F);
            modelViewStack.mulPose(rotation.invert());
            modelViewStack.translate(0.0F, 0.0F, -0.55F);
            modelViewStack.popPose();
            BufferUploader.drawWithShader(lineBuilder.end());

            RenderSystem.applyModelViewMatrix();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
         }
      }
   }
}
