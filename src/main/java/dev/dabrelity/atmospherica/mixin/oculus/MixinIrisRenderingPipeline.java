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
 * This injects AFTER composite passes but BEFORE the final pass,
 * allowing our volumetric effects to blend properly with shader pack output.
 */
@Mixin(value = IrisRenderingPipeline.class, remap = false)
public class MixinIrisRenderingPipeline {
    
    /**
     * Inject after compositeRenderer.renderAll() to render our custom effects
     * before the final pass blits to screen.
     * 
     * The finalizeLevelRendering method looks like:
     *   compositeRenderer.renderAll();     <- We inject AFTER this
     *   finalPassRenderer.renderFinalPass(); <- Before this
     */
    @Inject(
        method = "finalizeLevelRendering",
        at = @At(
            value = "INVOKE",
            target = "Lnet/irisshaders/iris/pipeline/FinalPassRenderer;renderFinalPass()V"
        )
    )
    private void atmospherica$injectCustomRendering(CallbackInfo ci) {
        try {
            // Call our bridge to render Atmospherica effects
            OculusShaderBridge.renderAtmosphericaEffects((IrisRenderingPipeline)(Object)this);
        } catch (Exception e) {
            // Log but don't crash - gracefully degrade if something goes wrong
            Atmospherica.LOGGER.error("Error rendering Atmospherica effects in Oculus pipeline", e);
        }
    }
}
