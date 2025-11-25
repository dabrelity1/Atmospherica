package dev.dabrelity.atmospherica.render;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.dabrelity.atmospherica.Atmospherica;
import dev.dabrelity.atmospherica.block.entity.AnemometerBlockEntity;
import dev.dabrelity.atmospherica.config.ClientConfig;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AnemometerRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
   private static Map<String, ResourceLocation> resLocMap = Maps.newHashMap();
   private static Map<String, Material> materialMap = Maps.newHashMap();
   protected final AnemometerModel model = new AnemometerModel(Minecraft.getInstance().getEntityModels().bakeLayer(AnemometerModel.LAYER_LOCATION));

   public static Material getMaterial(String path) {
      return (Material)materialMap.computeIfAbsent(path, m -> createMaterial(path));
   }

   public static Material createMaterial(String path) {
      return new Material(TextureAtlas.LOCATION_BLOCKS, getTexture(path));
   }

   public static ResourceLocation getTexture(String path) {
      return (ResourceLocation)resLocMap.computeIfAbsent(path, k -> Atmospherica.getPath(String.format("textures/block/%s.png", path)));
   }

   public static void renderModel(Material material, Model model, PoseStack poseStack, MultiBufferSource buffer, int combinedLightIn, int combinedOverlayIn) {
      model.renderToBuffer(poseStack, buffer.getBuffer(model.renderType(material.texture())), combinedLightIn, combinedOverlayIn, 1.0F, 1.0F, 1.0F, 1.0F);
   }

   public AnemometerRenderer(Context context) {
   }

   public void render(T blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource multiBufferSource, int combinedLightIn, int combinedOverlayIn) {
      this.model.tower.getAllParts().forEach(ModelPart::resetPose);
      this.model.shaft.getAllParts().forEach(ModelPart::resetPose);
      ModelPart mp = this.model.tower;
      mp.x += 8.0F;
      mp.z += 8.0F;
      mp.xRot = mp.xRot + (float)Math.toRadians(180.0);
      mp.yRot = mp.yRot + (float)Math.toRadians(180.0);
      mp.y += 2.0F;
      mp = this.model.shaft;
      mp.x += 8.0F;
      mp.z += 8.0F;
      mp.xRot = mp.xRot + (float)Math.toRadians(180.0);
      mp.yRot = mp.yRot + (float)Math.toRadians(180.0);
      mp.y += 2.0F;
      if (blockEntity instanceof AnemometerBlockEntity anemometerBlockEntity) {
         float lerpAngle = Mth.lerp(partialTicks, anemometerBlockEntity.prevSmoothAngle, anemometerBlockEntity.smoothAngle);
         this.model.shaft.yRot = -((float)Math.toRadians(lerpAngle));
         if (anemometerBlockEntity.smoothAngleRotationalVel > 25.0F && ClientConfig.funenometers) {
            this.model.shaft.xRot = this.model.shaft.xRot + (Atmospherica.RANDOM.nextFloat() - 0.5F) * 0.002F * anemometerBlockEntity.smoothAngleRotationalVel;
            this.model.shaft.zRot = this.model.shaft.zRot + (Atmospherica.RANDOM.nextFloat() - 0.5F) * 0.002F * anemometerBlockEntity.smoothAngleRotationalVel;
         }
      }

      renderModel(getMaterial("anemometer"), this.model, poseStack, multiBufferSource, combinedLightIn, combinedOverlayIn);
   }
}
