
package dev.dabrelity.atmospherica.mixin;

import dev.dabrelity.atmospherica.interfaces.ParticleData;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({Particle.class})
public class ParticleMixin implements ParticleData {
   @Shadow
   protected double x;
   @Shadow
   protected double y;
   @Shadow
   protected double z;
   @Shadow
   protected double xd;
   @Shadow
   protected double yd;
   @Shadow
   protected double zd;

   @Override
   public Vec3 getVelocity() {
      return new Vec3(this.xd, this.yd, this.zd);
   }

   @Override
   public void addVelocity(Vec3 vec3) {
      this.xd = this.xd + vec3.x;
      this.yd = this.yd + vec3.y;
      this.zd = this.zd + vec3.z;
   }

   @Override
   public void setVelocity(Vec3 vec3) {
      this.xd = vec3.x;
      this.yd = vec3.y;
      this.zd = vec3.z;
   }

   @Override
   public Vec3 getPosition() {
      return new Vec3(this.x, this.y, this.z);
   }
}
