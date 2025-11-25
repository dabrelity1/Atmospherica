package dev.dabrelity.atmospherica.compat;

import dev.dabrelity.atmospherica.Atmospherica;

/**
 * Handler for Oculus (Iris) API interactions.
 * This class is only loaded when Oculus is present to avoid ClassNotFoundException.
 */
public class OculusHandler {
    
    private boolean irisApiAvailable = false;
    
    public OculusHandler() {
        try {
            // Check for Iris/Oculus API class
            Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            irisApiAvailable = true;
            Atmospherica.LOGGER.debug("Iris/Oculus API found");
        } catch (ClassNotFoundException e) {
            // Try alternate package location for some Oculus versions
            try {
                Class.forName("net.coderbot.iris.apiimpl.IrisApiV0Impl");
                irisApiAvailable = true;
                Atmospherica.LOGGER.debug("Iris/Oculus legacy API found");
            } catch (ClassNotFoundException e2) {
                Atmospherica.LOGGER.debug("Iris/Oculus API not available");
                irisApiAvailable = false;
            }
        }
    }
    
    /**
     * Check if a shader pack is currently in use.
     */
    public boolean isShaderPackInUse() {
        if (!irisApiAvailable) {
            return false;
        }
        
        try {
            // Try the standard Iris API first
            Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = irisApiClass.getMethod("getInstance").invoke(null);
            Boolean result = (Boolean) irisApiClass.getMethod("isShaderPackInUse").invoke(instance);
            return result != null && result;
        } catch (Exception e) {
            // Try alternate method for older versions
            try {
                Class<?> irisClass = Class.forName("net.coderbot.iris.Iris");
                Object currentPack = irisClass.getMethod("getCurrentPack").invoke(null);
                return currentPack != null;
            } catch (Exception e2) {
                // If all else fails, assume no shader pack
                return false;
            }
        }
    }
    
    /**
     * Get the name of the current shader pack, if any.
     */
    public String getCurrentShaderPackName() {
        if (!irisApiAvailable) {
            return null;
        }
        
        try {
            Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = irisApiClass.getMethod("getInstance").invoke(null);
            
            // getConfig returns Optional<Path>
            Object configOptional = irisApiClass.getMethod("getConfig").invoke(instance);
            if (configOptional != null) {
                Class<?> optionalClass = configOptional.getClass();
                Boolean isPresent = (Boolean) optionalClass.getMethod("isPresent").invoke(configOptional);
                if (isPresent != null && isPresent) {
                    Object path = optionalClass.getMethod("get").invoke(configOptional);
                    if (path != null) {
                        return path.toString();
                    }
                }
            }
        } catch (Exception e) {
            // Ignore - not critical
        }
        return null;
    }
}
