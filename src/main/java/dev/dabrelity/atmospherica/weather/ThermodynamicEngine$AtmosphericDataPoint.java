package dev.dabrelity.atmospherica.weather;

import net.minecraft.nbt.CompoundTag;

public record ThermodynamicEngine.AtmosphericDataPoint(float temperature, float dewpoint, float pressure, float virtualTemperature) {
   public String toString() {
      return String.format(
         "Temperature: %s, DewPoint: %s, Pressure: %s, Virtual Temperature: %s",
         Math.floor(this.temperature * 10.0F) / 10.0,
         Math.floor(this.dewpoint * 10.0F) / 10.0,
         Math.floor(this.pressure * 10.0F) / 10.0,
         Math.floor(this.virtualTemperature * 10.0F) / 10.0
      );
   }

   public CompoundTag serializeNBT() {
      CompoundTag data = new CompoundTag();
      data.putFloat("temperature", this.temperature);
      data.putFloat("dewpoint", this.dewpoint);
      data.putFloat("pressure", this.pressure);
      data.putFloat("virtualTemperature", this.virtualTemperature);
      return data;
   }
}
