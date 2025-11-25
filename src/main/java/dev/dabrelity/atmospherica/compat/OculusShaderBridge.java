package dev.dabrelity.atmospherica.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dabrelity.atmospherica.Atmospherica;
import dev.dabrelity.atmospherica.config.ClientConfig;
import dev.dabrelity.atmospherica.event.GameBusClientEvents;
import dev.dabrelity.atmospherica.shaders.ModShaders;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

/**
 * Bridge class that handles rendering Atmospherica's volumetric effects
 * within the Oculus/Iris shader pipeline.
 * 
 * This is called from our mixin into IrisRenderingPipeline AFTER all
 * Iris passes complete, so we render directly on top of the final output.
 * 
 * IMPORTANT: We cache the projection and modelview matrices during normal
 * rendering because by the time Iris's final pass completes, those matrices
 * have been reset/modified.
 */
public class OculusShaderBridge {
    
    private static long lastLogTime = 0;
    
    // Cached matrices from the normal render call
    // These are captured when renderShaders is called normally (even if it returns early due to Oculus)
    private static Matrix4f cachedProjectionMatrix = new Matrix4f();
    private static Matrix4f cachedModelViewMatrix = new Matrix4f();
    private static float cachedPartialTicks = 0;
    private static boolean matricesCaptured = false;
    
    /**
     * Called from the normal rendering path to capture matrices.
     * Even when Oculus is active and we skip rendering, we still capture
     * the matrices so we can use them later in the Oculus pipeline.
     */
    public static void captureMatrices(Matrix4f projMat, Matrix4f modelMat, float partialTicks) {
        cachedProjectionMatrix.set(projMat);
        cachedModelViewMatrix.set(modelMat);
        cachedPartialTicks = partialTicks;
        matricesCaptured = true;
    }
    
    /**
     * Called from our mixin into IrisRenderingPipeline.finalizeLevelRendering()
     * AFTER Iris has finished all its passes.
     * 
     * At this point, the main Minecraft framebuffer is bound and contains
     * the shader pack's final output. We render our effects on top.
     * 
     * @param pipeline The IrisRenderingPipeline instance (passed as Object to avoid hard dep)
     */
    public static void renderAtmosphericaEffects(Object pipeline) {
        // Check if we should render
        if (!shouldRender()) {
            return;
        }
        
        // Make sure we have captured matrices
        if (!matricesCaptured) {
            // Matrices weren't captured this frame, skip rendering
            return;
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        
        // Log occasionally for debugging
        if (System.currentTimeMillis() - lastLogTime > 10000) {
            Atmospherica.LOGGER.info("Atmospherica: Rendering in Oculus pipeline (after final pass)");
            lastLogTime = System.currentTimeMillis();
        }
        
        try {
            // Get the camera
            Camera camera = minecraft.gameRenderer.getMainCamera();
            
            // Use the CACHED matrices from the normal render path
            // These are the correct world-space matrices
            Matrix4f projMat = new Matrix4f(cachedProjectionMatrix);
            Matrix4f modelMat = new Matrix4f(cachedModelViewMatrix);
            
            // Make sure main render target is bound
            minecraft.getMainRenderTarget().bindWrite(false);
            
            // Call our shader rendering with the correct matrices
            ModShaders.renderShadersForOculus(cachedPartialTicks, camera, projMat, modelMat);
            
            // Reset the flag - matrices need to be captured each frame
            matricesCaptured = false;
            
        } catch (Exception e) {
            Atmospherica.LOGGER.error("Error in Atmospherica Oculus bridge", e);
        }
    }
    
    /**
     * Check if we should render our effects.
     */
    private static boolean shouldRender() {
        // Only render if force volumetric clouds is enabled
        if (!ClientConfig.forceVolumetricClouds) {
            return false;
        }
        
        // Check if we have a weather handler
        if (GameBusClientEvents.weatherHandler == null) {
            return false;
        }
        
        return true;
    }
}
