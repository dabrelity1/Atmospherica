package dev.dabrelity.atmospherica.compat;

import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeRenderSetupEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;

class DistantHorizonsHandler$3 extends DhApiBeforeRenderSetupEvent {
   DistantHorizonsHandler$3(final DistantHorizonsHandler this$0) {
      this.this$0 = this$0;
   }

   public void beforeSetup(DhApiEventParam<DhApiRenderParam> event) {
      this.this$0.captureRenderState((DhApiRenderParam)event.value);
   }
}
