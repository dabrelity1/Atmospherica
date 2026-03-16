package dev.dabrelity.atmospherica.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dabrelity.atmospherica.Atmospherica;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ParticleCube extends ParticleTexFX {
   // 6 faces * 4 vertices = 24 total vertices, pre-define raw coordinates
   private static final float[] RAW_VERTICES = new float[]{
      // Face 0
      -1.0F, -1.0F, -1.0F,
      -1.0F, 1.0F, -1.0F,
      1.0F, 1.0F, -1.0F,
      1.0F, -1.0F, -1.0F,
      // Face 1
      -1.0F, -1.0F, 1.0F,
      -1.0F, 1.0F, 1.0F,
      1.0F, 1.0F, 1.0F,
      1.0F, -1.0F, 1.0F,
      // Face 2
      -1.0F, -1.0F, -1.0F,
      -1.0F, 1.0F, -1.0F,
      -1.0F, 1.0F, 1.0F,
      -1.0F, -1.0F, 1.0F,
      // Face 3
      1.0F, -1.0F, -1.0F,
      1.0F, 1.0F, -1.0F,
      1.0F, 1.0F, 1.0F,
      1.0F, -1.0F, 1.0F,
      // Face 4
      -1.0F, -1.0F, -1.0F,
      -1.0F, -1.0F, 1.0F,
      1.0F, -1.0F, 1.0F,
      1.0F, -1.0F, -1.0F,
      // Face 5
      -1.0F, 1.0F, -1.0F,
      -1.0F, 1.0F, 1.0F,
      1.0F, 1.0F, 1.0F,
      1.0F, 1.0F, -1.0F
   };

   public ParticleCube(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, BlockState state) {
      super(level, x, y, z, xSpeed, ySpeed, zSpeed, ParticleRegistry.rain);
      TextureAtlasSprite sprite1 = this.getSpriteFromState(state);
      if (sprite1 != null) {
         this.setSprite(sprite1);
      } else {
         Atmospherica.LOGGER.warn("Unable to find sprite from block {}", state);
         sprite1 = this.getSpriteFromState(Blocks.OAK_PLANKS.defaultBlockState());
         if (sprite1 != null) {
            this.setSprite(sprite1);
         }
      }

      int multiplier = Minecraft.getInstance().getBlockColors().getColor(state, this.level, new BlockPos((int)x, (int)y, (int)z), 0);
      float mr = (multiplier >>> 16 & 0xFF) / 255.0F;
      float mg = (multiplier >>> 8 & 0xFF) / 255.0F;
      float mb = (multiplier & 0xFF) / 255.0F;
      this.setColor(mr, mg, mb);
   }

   public TextureAtlasSprite getSpriteFromState(BlockState state) {
      BlockRenderDispatcher blockRenderDispatcher = Minecraft.getInstance().getBlockRenderer();
      BakedModel model = blockRenderDispatcher.getBlockModel(state);
      Direction[] var4 = Direction.values();
      int var5 = var4.length;
      byte var6 = 0;
      if (var6 < var5) {
         Direction direction = var4[var6];
         List<BakedQuad> list = model.getQuads(state, direction, RandomSource.create());
         return !list.isEmpty() ? ((BakedQuad)list.get(0)).getSprite() : model.getParticleIcon();
      } else {
         return null;
      }
   }

   @Override
   public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
      Vec3 vec3d = renderInfo.getPosition();
      float f = (float)(Mth.lerp(partialTicks, this.xo, this.x) - vec3d.x());
      float f1 = (float)(Mth.lerp(partialTicks, this.yo, this.y) - vec3d.y());
      float f2 = (float)(Mth.lerp(partialTicks, this.zo, this.z) - vec3d.z());
      Quaternionf quaternion;
      if (!this.facePlayer && (this.rotationPitch != 0.0F || this.rotationYaw != 0.0F)) {
         quaternion = new Quaternionf(0.0F, 0.0F, 0.0F, 1.0F);
         if (this.facePlayerYaw) {
            quaternion.mul(Axis.YP.rotationDegrees(-renderInfo.getYRot()));
         } else {
            quaternion.mul(Axis.YP.rotationDegrees(Mth.lerp(this.rotationSpeedAroundCenter, this.prevRotationYaw, this.rotationYaw)));
         }

         quaternion.mul(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, this.prevRotationPitch, this.rotationPitch)));
      } else {
         quaternion = renderInfo.rotation();
      }

      float f4 = this.getQuadSize(partialTicks);

      float[] transformedVertices = new float[RAW_VERTICES.length];
      Vector3f tempVec = new Vector3f();

      for (int i = 0; i < RAW_VERTICES.length; i += 3) {
         tempVec.set(RAW_VERTICES[i], RAW_VERTICES[i + 1], RAW_VERTICES[i + 2]);
         tempVec.rotate(quaternion);
         tempVec.mul(f4);
         tempVec.add(f, f1, f2);
         transformedVertices[i] = tempVec.x();
         transformedVertices[i + 1] = tempVec.y();
         transformedVertices[i + 2] = tempVec.z();
      }

      float u0 = this.getU0();
      float u1 = this.getU1();
      float v0_uv = this.getV0();
      float v1_uv = this.getV1();
      int j = this.getLightColor(partialTicks);
      if (j > 0) {
         this.lastNonZeroBrightness = j;
      } else {
         j = this.lastNonZeroBrightness;
      }

      for (int i = 0; i < 6; i++) {
         int idx = i * 12;
         buffer.vertex(transformedVertices[idx], transformedVertices[idx + 1], transformedVertices[idx + 2]).uv(u1, v1_uv).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
         buffer.vertex(transformedVertices[idx + 3], transformedVertices[idx + 4], transformedVertices[idx + 5]).uv(u1, v0_uv).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
         buffer.vertex(transformedVertices[idx + 6], transformedVertices[idx + 7], transformedVertices[idx + 8]).uv(u0, v0_uv).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
         buffer.vertex(transformedVertices[idx + 9], transformedVertices[idx + 10], transformedVertices[idx + 11]).uv(u0, v1_uv).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
      }
   }

   @Override
   public ParticleRenderType getRenderType() {
      return SORTED_OPAQUE_BLOCK;
   }
}
