package dev.dabrelity.atmospherica.compat;

import com.seibel.distanthorizons.api.DhApi.Delayed;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiAfterDhInitEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import dev.dabrelity.atmospherica.Atmospherica;

class DistantHorizonsHandler$1 extends DhApiAfterDhInitEvent {
   DistantHorizonsHandler$1(final DistantHorizonsHandler this$0) {
      this.this$0 = this$0;
   }

   public void afterDistantHorizonsInit(DhApiEventParam<Void> event) {
      Atmospherica.LOGGER.info("Distant Horizons initialized");
      this.this$0.dhReady = true;

      try {
         this.this$0.dhRenderDistance = (Integer)Delayed.configs.graphics().chunkRenderDistance().getValue();
      } catch (Exception var3) {
         Atmospherica.LOGGER.warn("Failed to get DH render distance, using default", var3);
         this.this$0.dhRenderDistance = 256;
      }
   }
}
