package dev.dabrelity.atmospherica.mixin.oculus;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin plugin that conditionally loads Oculus mixins only when Oculus is present.
 * This prevents class loading errors when Oculus is not installed.
 * 
 * IMPORTANT: We must NOT use Class.forName() here as it would load classes too early
 * and cause mixin application failures. Instead, we check if Oculus mod is in the mod list.
 */
public class OculusMixinPlugin implements IMixinConfigPlugin {
    
    private static Boolean oculusPresent = null;
    
    /**
     * Check if Oculus is present using classloader resource check.
     * This doesn't load the class, just checks if the resource exists.
     */
    private static boolean checkOculusPresent() {
        if (oculusPresent == null) {
            try {
                // Check if Oculus class file exists without loading it
                // This is safe to do during mixin bootstrap
                ClassLoader cl = OculusMixinPlugin.class.getClassLoader();
                oculusPresent = cl.getResource("net/irisshaders/iris/pipeline/IrisRenderingPipeline.class") != null;
                
                if (oculusPresent) {
                    System.out.println("[Atmospherica] Oculus detected - enabling Oculus compatibility mixins");
                } else {
                    System.out.println("[Atmospherica] Oculus not detected - Oculus mixins will be skipped");
                }
            } catch (Exception e) {
                System.out.println("[Atmospherica] Error checking for Oculus: " + e.getMessage());
                oculusPresent = false;
            }
        }
        return oculusPresent;
    }
    
    @Override
    public void onLoad(String mixinPackage) {
        // Called when the mixin config is loaded
        // Trigger the check here
        checkOculusPresent();
    }
    
    @Override
    public String getRefMapperConfig() {
        return null;
    }
    
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Only apply Oculus mixins if Oculus is present
        if (mixinClassName.contains(".oculus.")) {
            return checkOculusPresent();
        }
        return true;
    }
    
    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // Not used
    }
    
    @Override
    public List<String> getMixins() {
        // Return null - mixins are defined in the JSON
        return null;
    }
    
    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // Not used
    }
    
    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // Not used
    }
    
    public static boolean isOculusPresent() {
        return checkOculusPresent();
    }
}
