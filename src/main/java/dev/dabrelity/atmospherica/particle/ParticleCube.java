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

      List<Vector3f[]> faces = new ArrayList();
      Vector3f[] face = new Vector3f[]{
         new Vector3f(-1.0F, -1.0F, -1.0F), new Vector3f(-1.0F, 1.0F, -1.0F), new Vector3f(1.0F, 1.0F, -1.0F), new Vector3f(1.0F, -1.0F, -1.0F)
      };
      faces.add(face);
      face = new Vector3f[]{new Vector3f(-1.0F, -1.0F, 1.0F), new Vector3f(-1.0F, 1.0F, 1.0F), new Vector3f(1.0F, 1.0F, 1.0F), new Vector3f(1.0F, -1.0F, 1.0F)};
      faces.add(face);
      face = new Vector3f[]{
         new Vector3f(-1.0F, -1.0F, -1.0F), new Vector3f(-1.0F, 1.0F, -1.0F), new Vector3f(-1.0F, 1.0F, 1.0F), new Vector3f(-1.0F, -1.0F, 1.0F)
      };
      faces.add(face);
      face = new Vector3f[]{new Vector3f(1.0F, -1.0F, -1.0F), new Vector3f(1.0F, 1.0F, -1.0F), new Vector3f(1.0F, 1.0F, 1.0F), new Vector3f(1.0F, -1.0F, 1.0F)};
      faces.add(face);
      face = new Vector3f[]{
         new Vector3f(-1.0F, -1.0F, -1.0F), new Vector3f(-1.0F, -1.0F, 1.0F), new Vector3f(1.0F, -1.0F, 1.0F), new Vector3f(1.0F, -1.0F, -1.0F)
      };
      faces.add(face);
      face = new Vector3f[]{new Vector3f(-1.0F, 1.0F, -1.0F), new Vector3f(-1.0F, 1.0F, 1.0F), new Vector3f(1.0F, 1.0F, 1.0F), new Vector3f(1.0F, 1.0F, -1.0F)};
      faces.add(face);
      float f4 = this.getQuadSize(partialTicks);

      for (Vector3f[] entryFace : faces) {
         for (int i = 0; i < 4; i++) {
            entryFace[i].rotate(quaternion);
            entryFace[i].mul(f4);
            entryFace[i].add(f, f1, f2);
         }
      }

      float u0 = this.getU0();
      float u1 = this.getU1();
      float v0 = this.getV0();
      float v1 = this.getV1();
      int j = this.getLightColor(partialTicks);
      if (j > 0) {
         this.lastNonZeroBrightness = j;
      } else {
         j = this.lastNonZeroBrightness;
      }

      for (Vector3f[] entryFace : faces) {
         buffer.vertex(entryFace[0].x(), entryFace[0].y(), entryFace[0].z()).uv(u1, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
         buffer.vertex(entryFace[1].x(), entryFace[1].y(), entryFace[1].z()).uv(u1, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
         buffer.vertex(entryFace[2].x(), entryFace[2].y(), entryFace[2].z()).uv(u0, v0).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
         buffer.vertex(entryFace[3].x(), entryFace[3].y(), entryFace[3].z()).uv(u0, v1).color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(j).endVertex();
      }
   }

   @Override
   public ParticleRenderType getRenderType() {
      return SORTED_OPAQUE_BLOCK;
   }
}
