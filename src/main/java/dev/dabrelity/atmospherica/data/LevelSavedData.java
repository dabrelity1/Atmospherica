package dev.dabrelity.atmospherica.data;

import dev.dabrelity.atmospherica.interfaces.IWorldData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public class LevelSavedData extends SavedData {
   private CompoundTag compoundTag;
   private IWorldData dataHandler;

   public LevelSavedData() {
      this.compoundTag = new CompoundTag();
   }

   public LevelSavedData(CompoundTag compoundTag) {
      this.compoundTag = compoundTag;
   }

   public void setDataHandler(IWorldData dataHandler) {
      this.dataHandler = dataHandler;
   }

   public static LevelSavedData load(CompoundTag compoundTag) {
      return new LevelSavedData(compoundTag);
   }

   @Override
   public CompoundTag save(CompoundTag compoundTag) {
      if (this.dataHandler != null) {
         this.dataHandler.save(compoundTag);
      }
      return compoundTag;
   }

   public CompoundTag getData() {
      return this.compoundTag;
   }

   @Override
   public boolean isDirty() {
      return true;
   }
}
