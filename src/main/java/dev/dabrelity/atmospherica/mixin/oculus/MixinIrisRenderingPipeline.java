package dev.dabrelity.atmospherica.mixin.oculus;

import dev.dabrelity.atmospherica.Atmospherica;
import dev.dabrelity.atmospherica.compat.OculusShaderBridge;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into Oculus/Iris to inject Atmospherica's cloud/tornado rendering
 * into the shader pipeline at the right moment.
 * 
 * We inject AFTER the final pass completes, so we can render directly
 * to Minecraft's main framebuffer which is now the active target.
 */
@Mixin(value = IrisRenderingPipeline.class, remap = false)
public class MixinIrisRenderingPipeline {
    
    /**
     * Inject at the END of finalizeLevelRendering, AFTER Iris has completed
     * all its passes and written to the main framebuffer.
     * 
     * At this point, main framebuffer is bound and we can render our effects
     * on top of the shader pack's output.
     */
    @Inject(
        method = "finalizeLevelRendering",
        at = @At("TAIL")
    )
    private void atmospherica$injectCustomRenderingAfterFinal(CallbackInfo ci) {
        try {
            // Call our bridge to render Atmospherica effects
            OculusShaderBridge.renderAtmosphericaEffects((IrisRenderingPipeline)(Object)this);
        } catch (Exception e) {
            // Log but don't crash - gracefully degrade if something goes wrong
            Atmospherica.LOGGER.error("Error rendering Atmospherica effects in Oculus pipeline", e);
        }
    }
}
