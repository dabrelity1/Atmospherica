package dev.dabrelity.atmospherica.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dabrelity.atmospherica.Atmospherica;
import dev.dabrelity.atmospherica.config.ClientConfig;
import dev.dabrelity.atmospherica.event.GameBusClientEvents;
import dev.dabrelity.atmospherica.shaders.ModShaders;
import dev.dabrelity.atmospherica.weather.WeatherHandlerClient;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

/**
 * Bridge class that handles rendering Atmospherica's volumetric effects
 * within the Oculus/Iris shader pipeline.
 * 
 * This is called from our mixin into IrisRenderingPipeline after composite
 * passes but before the final pass.
 */
public class OculusShaderBridge {
    
    private static boolean initialized = false;
    private static long lastLogTime = 0;
    
    /**
     * Called from our mixin into IrisRenderingPipeline.finalizeLevelRendering()
     * to render Atmospherica's volumetric clouds and tornadoes.
     * 
     * @param pipeline The IrisRenderingPipeline instance (passed as Object to avoid hard dep)
     */
    public static void renderAtmosphericaEffects(Object pipeline) {
        // Check if we should render
        if (!shouldRender()) {
            return;
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        
        // Log occasionally for debugging
        if (System.currentTimeMillis() - lastLogTime > 10000) {
            Atmospherica.LOGGER.info("Atmospherica: Rendering in Oculus pipeline");
            lastLogTime = System.currentTimeMillis();
        }
        
        try {
            // Get the camera and matrices
            Camera camera = minecraft.gameRenderer.getMainCamera();
            
            // We need to create proper matrices for our shader
            // In the Iris pipeline context, we need to use the current GL state
            Matrix4f projMat = RenderSystem.getProjectionMatrix();
            Matrix4f modelMat = new Matrix4f(RenderSystem.getModelViewMatrix());
            
            float partialTicks = minecraft.getFrameTime();
            
            // Save current GL state that Iris uses
            saveIrisGLState();
            
            try {
                // Call our shader rendering
                // This is the same method used in normal rendering, but now we're
                // in the right place in the Iris pipeline
                ModShaders.renderShadersForOculus(partialTicks, camera, projMat, modelMat);
            } finally {
                // Restore Iris GL state
                restoreIrisGLState();
            }
            
        } catch (Exception e) {
            Atmospherica.LOGGER.error("Error in Atmospherica Oculus bridge", e);
        }
    }
    
    /**
     * Check if we should render our effects.
     */
    private static boolean shouldRender() {
        // Only render if force volumetric clouds is enabled
        // (otherwise, there's no point - user wants shader pack clouds)
        if (!ClientConfig.forceVolumetricClouds) {
            return false;
        }
        
        // Check if we have a weather handler
        if (GameBusClientEvents.weatherHandler == null) {
            return false;
        }
        
        return true;
    }
    
    // GL state preservation for Iris compatibility
    private static int savedFramebuffer = 0;
    private static int savedProgram = 0;
    private static boolean savedBlend = false;
    private static boolean savedDepthTest = false;
    private static boolean savedDepthMask = false;
    
    private static void saveIrisGLState() {
        // Save the current framebuffer and shader state
        // Iris uses custom framebuffers and we need to restore them
        savedFramebuffer = org.lwjgl.opengl.GL30.glGetInteger(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_BINDING);
        savedProgram = org.lwjgl.opengl.GL20.glGetInteger(org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM);
        savedBlend = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
        savedDepthTest = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        savedDepthMask = org.lwjgl.opengl.GL11.glGetBoolean(org.lwjgl.opengl.GL11.GL_DEPTH_WRITEMASK);
    }
    
    private static void restoreIrisGLState() {
        // Restore Iris's GL state
        org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER, savedFramebuffer);
        org.lwjgl.opengl.GL20.glUseProgram(savedProgram);
        
        if (savedBlend) {
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
        } else {
            org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_BLEND);
        }
        
        if (savedDepthTest) {
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        } else {
            org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        }
        
        org.lwjgl.opengl.GL11.glDepthMask(savedDepthMask);
    }
}
