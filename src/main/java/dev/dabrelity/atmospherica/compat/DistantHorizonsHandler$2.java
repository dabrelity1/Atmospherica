package dev.dabrelity.atmospherica.compat;

import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeRenderPassEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;

class DistantHorizonsHandler$2 extends DhApiBeforeRenderPassEvent {
   DistantHorizonsHandler$2(final DistantHorizonsHandler this$0) {
      this.this$0 = this$0;
   }

   public void beforeRender(DhApiEventParam<DhApiRenderParam> event) {
      this.this$0.captureRenderState((DhApiRenderParam)event.value);
   }
}
