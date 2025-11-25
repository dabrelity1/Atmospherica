package dev.dabrelity.atmospherica.event;

import dev.dabrelity.atmospherica.Atmospherica;
import dev.dabrelity.atmospherica.particle.ParticleRegistry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Atmospherica.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModBusEvents {
   private ModBusEvents() {
   }

   @SubscribeEvent
   public static void gatherData(GatherDataEvent event) {
      if (event.includeClient()) {
         DataGenerator gen = event.getGenerator();
         ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
         PackOutput output = gen.getPackOutput();
         gen.addProvider(event.includeClient(), new ParticleRegistry(output, existingFileHelper));
      }
   }
}
