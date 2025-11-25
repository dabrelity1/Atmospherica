package dev.dabrelity.atmospherica.compat;

import dev.dabrelity.atmospherica.Atmospherica;
import net.minecraftforge.fml.ModList;

/**
 * Compatibility layer for Embeddium (Sodium port for Forge).
 * 
 * Embeddium optimizes chunk rendering significantly. While most of our
 * rendering is unaffected (particles, shaders), we need to be aware of
 * potential conflicts with custom rendering.
 */
public class EmbeddiumCompat {
    private static boolean initialized = false;
    private static boolean embeddiumPresent = false;
    private static boolean rubidiumPresent = false;  // Rubidium is another Sodium port
    
    /**
     * Initialize Embeddium/Rubidium compatibility.
     * Should be called during mod initialization.
     */
    public static void initialize() {
        if (initialized) return;
        initialized = true;
        
        try {
            // Check for Embeddium
            if (ModList.get().isLoaded("embeddium")) {
                embeddiumPresent = true;
                Atmospherica.LOGGER.info("Embeddium detected - optimized rendering compatibility enabled");
            }
            
            // Check for Rubidium (older Sodium fork)
            if (ModList.get().isLoaded("rubidium")) {
                rubidiumPresent = true;
                Atmospherica.LOGGER.info("Rubidium detected - optimized rendering compatibility enabled");
            }
            
            if (!embeddiumPresent && !rubidiumPresent) {
                Atmospherica.LOGGER.debug("No Sodium-like mod detected");
            }
        } catch (Exception e) {
            Atmospherica.LOGGER.debug("Embeddium/Rubidium compatibility check failed: {}", e.getMessage());
        }
    }
    
    /**
     * Check if Embeddium is installed and loaded.
     */
    public static boolean isEmbeddiumPresent() {
        if (!initialized) initialize();
        return embeddiumPresent;
    }
    
    /**
     * Check if Rubidium is installed and loaded.
     */
    public static boolean isRubidiumPresent() {
        if (!initialized) initialize();
        return rubidiumPresent;
    }
    
    /**
     * Check if any Sodium-like mod is present.
     */
    public static boolean isSodiumLikeModPresent() {
        if (!initialized) initialize();
        return embeddiumPresent || rubidiumPresent;
    }
    
    /**
     * Get render pass hints for Embeddium.
     * Embeddium uses deferred rendering which can affect when our
     * effects are visible.
     */
    public static boolean shouldDeferParticleRendering() {
        // Currently we render in AFTER_WEATHER which should be fine
        // But this hook allows us to adjust if needed
        return false;
    }
    
    /**
     * Check if we need to use alternative fog handling.
     * Embeddium may modify fog rendering.
     */
    public static boolean needsAlternateFogHandling() {
        return isSodiumLikeModPresent();
    }
}
