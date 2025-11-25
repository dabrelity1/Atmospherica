package dev.dabrelity.atmospherica.block;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum RadarBlock.Mode implements StringRepresentable {
   REFLECTIVITY("reflectivity"),
   VELOCITY("velocity"),
   IR("ir");

   private final String mode;

   private RadarBlock.Mode(String mode) {
      this.mode = mode;
   }

   @NotNull
   public String getSerializedName() {
      return this.mode;
   }
}
