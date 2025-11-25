package dev.dabrelity.atmospherica.interfaces;

import net.minecraft.world.phys.Vec3;

public interface ParticleData {
   Vec3 getVelocity();

   void addVelocity(Vec3 var1);

   void setVelocity(Vec3 var1);

   Vec3 getPosition();
}
