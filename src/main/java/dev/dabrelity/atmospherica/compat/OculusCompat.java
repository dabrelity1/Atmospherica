package dev.dabrelity.atmospherica.compat;

import dev.dabrelity.atmospherica.Atmospherica;
import net.minecraftforge.fml.ModList;

/**
 * Compatibility layer for Oculus (Iris shader mod for Forge).
 * 
 * Oculus replaces vanilla rendering with its own shader pipeline.
 * When shader packs are active, we need to disable our volumetric
 * cloud shader to avoid conflicts, but keep particle/lightning effects.
 */
public class OculusCompat {
    private static boolean initialized = false;
    private static boolean oculusPresent = false;
    private static OculusHandler handler = null;
    
    /**
     * Initialize Oculus compatibility.
     * Should be called during mod initialization.
     */
    public static void initialize() {
        if (initialized) return;
        initialized = true;
        
        try {
            // Check if Oculus is loaded via ModList first (safer)
            if (ModList.get().isLoaded("oculus")) {
                // Try to load the handler class which will access Oculus internals
                handler = new OculusHandler();
                oculusPresent = true;
                Atmospherica.LOGGER.info("Oculus detected - shader compatibility mode enabled");
            } else {
                Atmospherica.LOGGER.debug("Oculus not present");
                oculusPresent = false;
            }
        } catch (NoClassDefFoundError | Exception e) {
            Atmospherica.LOGGER.debug("Oculus compatibility initialization failed: {}", e.getMessage());
            oculusPresent = false;
            handler = null;
        }
    }
    
    /**
     * Check if Oculus is installed and loaded.
     */
    public static boolean isOculusPresent() {
        if (!initialized) initialize();
        return oculusPresent;
    }
    
    /**
     * Check if an Oculus shader pack is currently active.
     * When active, we should disable our PostChain volumetric shader
     * to avoid visual conflicts.
     */
    public static boolean isShaderPackActive() {
        if (!isOculusPresent() || handler == null) {
            return false;
        }
        try {
            return handler.isShaderPackInUse();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if we should render our custom volumetric clouds.
     * Returns false if Oculus shaders are active (they typically have their own clouds).
     */
    public static boolean shouldRenderVolumetricClouds() {
        return !isShaderPackActive();
    }
    
    /**
     * Check if we should render our custom sky effects.
     * Particles and lightning should always render.
     */
    public static boolean shouldRenderParticlesAndLightning() {
        // Always render particles and lightning - they work fine with shader packs
        return true;
    }
}
