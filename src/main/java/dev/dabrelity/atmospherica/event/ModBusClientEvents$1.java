package dev.dabrelity.atmospherica.event;

import dev.dabrelity.atmospherica.shaders.ModShaders;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

class ModBusClientEvents$1 extends SimplePreparableReloadListener<Object> {
   protected Object prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
      return null;
   }

   protected void apply(Object o, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
      ModShaders.reload();
   }
}
