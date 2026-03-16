package dev.dabrelity.atmospherica.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dabrelity.atmospherica.util.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ParticleTexExtraRender extends ParticleTexFX {
   public int extraParticlesBaseAmount = 5;
   public boolean noExtraParticles = false;
   public float extraRandomSecondaryYawRotation = 360.0F;

   public ParticleTexExtraRender(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, TextureAtlasSprite sprite) {
      super(level, x, y, z, xSpeed, ySpeed, zSpeed, sprite);
   }

   @Override
   public void tickExtraRotations() {
      if (this.slantParticleToWind) {
         double speed = this.xd * this.xd + this.zd * this.zd;
         this.rotationYaw = -((float)Math.toDegrees(Math.atan2(this.zd, this.xd))) - 90.0F;
         this.rotationPitch = Math.min(45.0F, (float)(speed * 120.0));
         this.rotationPitch = this.rotationPitch + (this.entityID % 10 - 5);
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
         quaternion.mul(Axis.YP.rotationDegrees(this.rotationYaw));
         quaternion.mul(Axis.XP.rotationDegrees(this.rotationPitch));
         if (this.extraRandomSecondaryYawRotation > 0.0F) {
            quaternion.mul(Axis.YP.rotationDegrees(this.entityID % this.extraRandomSecondaryYawRotation));
         }
      } else {
         quaternion = renderInfo.rotation();
      }

      float u0 = this.getU0();
      float u1 = this.getU1();
      float v0_uv = this.getV0();
      float v1_uv = this.getV1();
      int renderAmount;
      if (this.noExtraParticles) {
         renderAmount = 1;
      } else {
         renderAmount = Math.min(1 + this.extraParticlesBaseAmount, Util.MAX_RAIN_DROPS);
      }

      int i = this.getLightColor(partialTicks);
      if (i > 0) {
         this.lastNonZeroBrightness = i;
      } else {
         i = this.lastNonZeroBrightness;
      }

      float scale = this.getQuadSize(partialTicks);

      float x0 = -1.0F; float y0 = -1.0F; float z0 = 0.0F;
      float x1 = -1.0F; float y1 =  1.0F; float z1 = 0.0F;
      float x2 =  1.0F; float y2 =  1.0F; float z2 = 0.0F;
      float x3 =  1.0F; float y3 = -1.0F; float z3 = 0.0F;

      Vector3f v0_vec = new Vector3f(x0, y0, z0);
      v0_vec.rotate(quaternion);
      v0_vec.mul(scale);
      v0_vec.add(f, f1, f2);

      Vector3f v1_vec = new Vector3f(x1, y1, z1);
      v1_vec.rotate(quaternion);
      v1_vec.mul(scale);
      v1_vec.add(f, f1, f2);

      Vector3f v2_vec = new Vector3f(x2, y2, z2);
      v2_vec.rotate(quaternion);
      v2_vec.mul(scale);
      v2_vec.add(f, f1, f2);

      Vector3f v3_vec = new Vector3f(x3, y3, z3);
      v3_vec.rotate(quaternion);
      v3_vec.mul(scale);
      v3_vec.add(f, f1, f2);

      for (int ii = 0; ii < renderAmount; ii++) {
         double xx = 0.0;
         double zz = 0.0;
         double yy = 0.0;
         if (ii != 0) {
            xx = Util.RAIN_POSITIONS[ii].x;
            yy = Util.RAIN_POSITIONS[ii].y;
            zz = Util.RAIN_POSITIONS[ii].z;
         }

         if (this.dontRenderUnderTopmostBlock) {
            int height2 = this.level.getHeightmapPos(Types.MOTION_BLOCKING, new BlockPos((int)(this.x + xx), (int)this.y, (int)(this.z + zz))).getY();
            if (this.y + yy < height2) {
               continue;
            }
         }

         buffer.vertex((float)(xx + v0_vec.x), (float)(yy + v0_vec.y), (float)(zz + v0_vec.z))
            .uv(u1, v1_uv)
            .color(this.rCol, this.gCol, this.bCol, this.alpha)
            .uv2(i).endVertex();
         buffer.vertex((float)(xx + v1_vec.x), (float)(yy + v1_vec.y), (float)(zz + v1_vec.z))
            .uv(u1, v0_uv)
            .color(this.rCol, this.gCol, this.bCol, this.alpha)
            .uv2(i).endVertex();
         buffer.vertex((float)(xx + v2_vec.x), (float)(yy + v2_vec.y), (float)(zz + v2_vec.z))
            .uv(u0, v0_uv)
            .color(this.rCol, this.gCol, this.bCol, this.alpha)
            .uv2(i).endVertex();
         buffer.vertex((float)(xx + v3_vec.x), (float)(yy + v3_vec.y), (float)(zz + v3_vec.z))
            .uv(u0, v1_uv)
            .color(this.rCol, this.gCol, this.bCol, this.alpha)
            .uv2(i).endVertex();
      }
   }
}
