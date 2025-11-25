package dev.dabrelity.atmospherica.shaders;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.shaders.Uniform;
import java.util.Arrays;
import dev.dabrelity.atmospherica.Atmospherica;
import dev.dabrelity.atmospherica.compat.DistantHorizons;
import dev.dabrelity.atmospherica.compat.OculusCompat;
import dev.dabrelity.atmospherica.config.ClientConfig;
import dev.dabrelity.atmospherica.config.ServerConfig;
import dev.dabrelity.atmospherica.event.GameBusClientEvents;
import dev.dabrelity.atmospherica.mixin.PostChainMixin;
import dev.dabrelity.atmospherica.weather.Lightning;
import dev.dabrelity.atmospherica.weather.Storm;
import dev.dabrelity.atmospherica.weather.ThermodynamicEngine;
import dev.dabrelity.atmospherica.weather.WeatherHandler;
import dev.dabrelity.atmospherica.weather.WeatherHandlerClient;
import dev.dabrelity.atmospherica.weather.WindEngine;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL32;

public class ModShaders {

    private static PostChain clouds;
    private static Vec2 lastScroll = Vec2.ZERO;
    public static Vec2 scroll = Vec2.ZERO;
    private static int lastWidth = 0;
    private static int lastHeight = 0;
    private static float snow = 0.0F;
    private static float lastSnow = 0.0F;
    private static float gameTime = 0.0F;
    private static final float[] STORM_POSITIONS = new float[48];
    private static final float[] STORM_VELOCITIES = new float[32];
    private static final float[] STORM_STAGES = new float[16];
    private static final float[] STORM_ENERGIES = new float[16];
    private static final float[] STORM_TYPES = new float[16];
    private static final float[] STORM_OCCLUSIONS = new float[16];
    private static final float[] TORNADO_WINDSPEEDS = new float[16];
    private static final float[] TORNADO_WIDTHS = new float[16];
    private static final float[] TORNADO_TOUCHDOWN_SPEEDS = new float[16];
    private static final float[] VISUAL_ONLYS = new float[16];
    private static final float[] STORM_SPINS = new float[16];
    private static final float[] STORM_DYINGS = new float[16];
    private static final float[] TORNADO_SHAPES = new float[16];
    private static final float[] LIGHTNING_STRIKES = new float[192];
    private static final float[] LIGHTNING_BRIGHTNESS = new float[64];

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        WeatherHandler weatherHandler = GameBusClientEvents.weatherHandler;
        if (
            player != null && weatherHandler != null && minecraft.level != null
        ) {
            Vec3 wind = WindEngine.getWind(
                new Vec3(
                    player.position().x,
                    minecraft.level.getMaxBuildHeight(),
                    player.position().z
                ),
                minecraft.level,
                true,
                true,
                false
            );
            Vec3 windScaled = wind.multiply(0.03, 0.03, 0.03);
            lastScroll = scroll;
            scroll = scroll.add(
                new Vec2(-((float) windScaled.x), -((float) windScaled.z))
            );

            for (Storm storm : weatherHandler.getStorms()) {
                if (storm.lastPosition == null) {
                    storm.lastPosition = storm.position;
                } else {
                    storm.lastPosition = storm.lastPosition.lerp(
                        storm.position,
                        0.05F
                    );
                }

                storm.lastSpin = storm.spin;
                storm.spin =
                    storm.spin +
                    (storm.smoothWindspeed * 0.01F) /
                    Math.max(storm.smoothWidth, 20.0F);
            }

            ThermodynamicEngine.Precipitation precip =
                ThermodynamicEngine.getPrecipitationType(
                    weatherHandler,
                    player.position(),
                    minecraft.level,
                    0
                );
            lastSnow = snow;
            if (
                precip != ThermodynamicEngine.Precipitation.SNOW &&
                precip != ThermodynamicEngine.Precipitation.WINTRY_MIX
            ) {
                snow = Mth.lerp(0.05F, snow, 0.0F);
            } else {
                float rain = weatherHandler.getPrecipitation(player.position());
                float snowBlindness = (float) Mth.clamp(
                    Math.pow(wind.length() / 60.0, 2.0) * rain,
                    0.0,
                    1.0
                );
                snow = Mth.lerp(0.05F, snow, snowBlindness);
            }
        }
    }

    public static void renderShaders(
        float partialTicks,
        Camera camera,
        Matrix4f projMat,
        Matrix4f modelMat
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        WeatherHandler weatherHandler = GameBusClientEvents.weatherHandler;
        
        // Debug: Log shader pack status
        boolean shaderPackActive = OculusCompat.isShaderPackActive();
        
        // Skip volumetric cloud rendering if Oculus shader pack is active
        // (shader packs typically have their own cloud rendering)
        // Unless user has forced volumetric clouds via config
        // OR we're being called from within the Oculus pipeline (mixin injection)
        if (shaderPackActive && !ClientConfig.forceVolumetricClouds && !renderingFromOculusPipeline) {
            return;
        }
        
        // Log when we're attempting to render with shaders active
        if (shaderPackActive && (ClientConfig.forceVolumetricClouds || renderingFromOculusPipeline)) {
            // Only log occasionally to avoid spam
            if (minecraft.level != null && minecraft.level.getGameTime() % 200 == 0) {
                Atmospherica.LOGGER.info("Atmospherica: Attempting cloud render with shader pack active (forceVolumetricClouds=true)");
            }
        }
        
        if (
            clouds != null &&
            player != null &&
            weatherHandler != null &&
            minecraft.level != null &&
            ServerConfig.validDimensions != null &&
            ServerConfig.validDimensions.contains(minecraft.level.dimension())
        ) {
            int width = minecraft.getWindow().getWidth();
            int height = minecraft.getWindow().getHeight();
            if (width != lastWidth || height != lastHeight) {
                lastWidth = width;
                lastHeight = height;
                updateShaderGroupSize(clouds);
            }

            RenderSystem.enableDepthTest();
            RenderSystem.resetTextureMatrix();
            RenderSystem.disableBlend();
            RenderSystem.depthMask(false);
            if (clouds instanceof PostChainMixin mixin) {
                List<PostPass> passes = mixin.getPasses();
                if (passes.isEmpty()) {
                    return;
                }

                EffectInstance effect = passes.get(0).getEffect();
                Vec3 camPos = camera.getPosition();
                
                // Use optimized direct setters instead of lambda-based withUniform
                setUniformFloat2(effect, "OutSize", (float) width, (float) height);
                setUniformFloat3(effect, "pos", (float) camPos.x, (float) camPos.y, (float) camPos.z);
                setUniformFloat2(effect, "scroll", 
                    Mth.lerp(partialTicks, lastScroll.x, scroll.x),
                    Mth.lerp(partialTicks, lastScroll.y, scroll.y)
                );
                setUniformInt(effect, "maxSteps", 160);
                setUniformFloat(effect, "stepSize", 0.01F);
                setUniformFloat(effect, "fogStart", RenderSystem.getShaderFogStart() * 4.0F);
                setUniformFloat(effect, "fogEnd", RenderSystem.getShaderFogEnd() * 4.0F);
                setUniformMat4(effect, "proj", projMat.invert());
                setUniformMat4(effect, "viewmat", modelMat.invert());
                setUniformFloat(effect, "time", player.tickCount + partialTicks);
                
                long seed = GameBusClientEvents.weatherHandler != null
                    ? GameBusClientEvents.weatherHandler.seed
                    : 0L;
                float goal =
                    player.level().getGameTime() +
                    (float) seed / 1.0E14F +
                    partialTicks;
                gameTime = Mth.lerp(0.3F, gameTime, goal);
                if (Math.abs(gameTime - goal) > 30.0F) {
                    gameTime = goal;
                }

                setUniformFloat(effect, "worldTime", gameTime);
                setUniformFloat(effect, "layer0height", (float) ServerConfig.layer0Height);
                setUniformFloat(effect, "layerCheight", (float) ServerConfig.layerCHeight);
                setUniformFloat(effect, "stormSize", (float) ServerConfig.stormSize * 2.0F);
                
                float sunAngle = minecraft.level.getSunAngle(partialTicks);
                Vec3 sunDir = new Vec3(
                    -Math.sin(sunAngle),
                    Math.cos(sunAngle),
                    0.0
                );
                setUniformFloat3(effect, "sunDir", (float) sunDir.x, (float) sunDir.y, (float) sunDir.z);
                setUniformFloat(effect, "lightIntensity", 
                    (float) Math.pow((Math.cos(sunAngle) + 1.0) / 2.0, 3.0));
                setUniformFloat(effect, "downsample", (float) ClientConfig.volumetricsDownsample);
                
                if (passes.size() > 1) {
                    EffectInstance blur = passes.get(1).getEffect();
                    setUniformFloat(blur, "downsample", (float) ClientConfig.volumetricsDownsample);
                    setUniformFloat(blur, "glowFix", ClientConfig.glowFix ? 1.0F : 0.0F);
                    setUniformFloat(blur, "doBlur", ClientConfig.volumetricsBlur ? 1.0F : 0.0F);
                }
                setUniformFloat(effect, "simpleLighting", ClientConfig.simpleLighting ? 0.0F : 1.0F);

                try {
                    if (DistantHorizons.isAvailable()) {
                        int depthTextureId =
                            DistantHorizons.getDepthTextureId();
                        GL32.glActiveTexture(33991);
                        GL32.glBindTexture(3553, depthTextureId);
                        setUniformInt(effect, "dhDepthTex0", 7);
                        setUniformFloat(effect, "hasDHDepth", 1.0F);
                        Matrix4f dhProj = DistantHorizons.getDhProjectionMatrix();
                        Matrix4f dhProjInv = new Matrix4f(dhProj).invert();
                        setUniformMat4(effect, "dhProjection", dhProj);
                        setUniformMat4(effect, "dhProjectionInverse", dhProjInv);
                        setUniformMat4(effect, "dhViewmat", DistantHorizons.getDhModelViewMatrix());
                        setUniformFloat(effect, "dhNearPlane", DistantHorizons.getNearPlane());
                        setUniformFloat(effect, "dhFarPlane", DistantHorizons.getFarPlane());
                        setUniformFloat(effect, "dhRenderDistance", DistantHorizons.getChunkRenderDistance() * 16.0F);
                        GL32.glActiveTexture(33984);
                    } else {
                        setUniformFloat(effect, "hasDHDepth", 0.0F);
                    }
                } catch (Exception exception) {
                    Atmospherica.LOGGER.debug("DH Uniforms not available");
                    setUniformFloat(effect, "hasDHDepth", 0.0F);
                }

                if (weatherHandler instanceof WeatherHandlerClient whc) {
                    setUniformFloat(effect, "rain", whc.getPrecipitation());
                    setUniformFloat(effect, "snow", Mth.lerp(partialTicks, lastSnow, snow));
                }

                List<Storm> storms = weatherHandler.getStorms();
                float[] stormPositions = STORM_POSITIONS;
                float[] stormVelocities = STORM_VELOCITIES;
                float[] stormStages = STORM_STAGES;
                float[] stormEnergies = STORM_ENERGIES;
                float[] stormTypes = STORM_TYPES;
                float[] tornadoWindspeeds = TORNADO_WINDSPEEDS;
                float[] tornadoWidths = TORNADO_WIDTHS;
                float[] tornadoTouchdownSpeeds = TORNADO_TOUCHDOWN_SPEEDS;
                float[] visualOnlys = VISUAL_ONLYS;
                float[] stormSpins = STORM_SPINS;
                float[] stormDyings = STORM_DYINGS;
                float[] tornadoShapes = TORNADO_SHAPES;
                float[] stormOcclusions = STORM_OCCLUSIONS;
                float[] lightningStrikes = LIGHTNING_STRIKES;
                float[] lightningBrightness = LIGHTNING_BRIGHTNESS;

                Arrays.fill(stormPositions, 0.0F);
                Arrays.fill(stormVelocities, 0.0F);
                Arrays.fill(stormStages, 0.0F);
                Arrays.fill(stormEnergies, 0.0F);
                Arrays.fill(stormTypes, 0.0F);
                Arrays.fill(tornadoWindspeeds, 0.0F);
                Arrays.fill(tornadoWidths, 0.0F);
                Arrays.fill(tornadoTouchdownSpeeds, 0.0F);
                Arrays.fill(visualOnlys, 0.0F);
                Arrays.fill(stormSpins, 0.0F);
                Arrays.fill(stormDyings, 0.0F);
                Arrays.fill(tornadoShapes, 0.0F);
                Arrays.fill(stormOcclusions, 0.0F);
                Arrays.fill(lightningStrikes, 0.0F);
                Arrays.fill(lightningBrightness, 0.0F);

                if (
                    weatherHandler instanceof WeatherHandlerClient handlerClient
                ) {
                    List<Lightning> lightnings = handlerClient.lightnings;
                    int lightningCount = Math.min(lightnings.size(), 64);
                    for (int i = 0; i < lightningCount; i++) {
                        Lightning lightning = lightnings.get(i);
                        lightningStrikes[i * 3] = (float) lightning.position.x;
                        lightningStrikes[i * 3 + 1] =
                            (float) lightning.position.y;
                        lightningStrikes[i * 3 + 2] =
                            (float) lightning.position.z;
                        float p = Mth.clamp(
                            (lightning.ticks + partialTicks) /
                                lightning.lifetime,
                            0.0F,
                            1.0F
                        );
                        lightningBrightness[i] =
                            (float) Math.abs(
                                Math.cos(Math.sqrt(p) * Math.PI * 3.0)
                            ) *
                            (1.0F - p);
                    }
                    setUniformFloatArray(effect, "lightningStrikes", lightningStrikes);
                    setUniformInt(effect, "lightningCount", lightningCount);
                    setUniformFloatArray(effect, "lightningBrightness", lightningBrightness);
                } else {
                    setUniformInt(effect, "lightningCount", 0);
                }

                int count = 0;
                for (int i = 0; i < storms.size() && i < 16; i++) {
                    Storm storm = storms.get(i);
                    if (
                        storm.lastPosition == null ||
                        storm.position
                            .multiply(1.0, 0.0, 1.0)
                            .distanceTo(
                                camera.getPosition().multiply(1.0, 0.0, 1.0)
                            ) >
                        32000.0 ||
                        (storm.stage <= 0 &&
                            storm.energy <= 0 &&
                            storm.stormType != 2)
                    ) {
                        continue;
                    }

                    Vec3 pos = storm.lastPosition;
                    Vec3 vel = storm.velocity;
                    stormPositions[count * 3] = (float) pos.x;
                    stormPositions[count * 3 + 1] = (float) pos.y;
                    stormPositions[count * 3 + 2] = (float) pos.z;
                    stormVelocities[count * 2] = (float) vel.x;
                    stormVelocities[count * 2 + 1] = (float) vel.z;
                    stormStages[count] = storm.stage;
                    stormEnergies[count] = storm.energy;
                    tornadoWindspeeds[count] = storm.smoothWindspeed;
                    tornadoWidths[count] = storm.smoothWidth;
                    tornadoTouchdownSpeeds[count] = storm.touchdownSpeed;
                    stormSpins[count] = Mth.lerp(
                        partialTicks,
                        storm.lastSpin,
                        storm.spin
                    );
                    tornadoShapes[count] = storm.tornadoShape;
                    stormTypes[count] = storm.stormType;
                    stormOcclusions[count] = storm.occlusion;
                    visualOnlys[count] = storm.visualOnly ? 1.0F : -1.0F;
                    stormDyings[count] = storm.isDying ? 1.0F : -1.0F;
                    count++;
                }

                setUniformInt(effect, "stormCount", count);
                setUniformFloatArray(effect, "stormPositions", stormPositions);
                setUniformFloatArray(effect, "stormVelocities", stormVelocities);
                setUniformFloatArray(effect, "stormStages", stormStages);
                setUniformFloatArray(effect, "stormEnergies", stormEnergies);
                setUniformFloatArray(effect, "stormTypes", stormTypes);
                setUniformFloatArray(effect, "tornadoWindspeeds", tornadoWindspeeds);
                setUniformFloatArray(effect, "tornadoWidths", tornadoWidths);
                setUniformFloatArray(effect, "tornadoTouchdownSpeeds", tornadoTouchdownSpeeds);
                setUniformFloatArray(effect, "visualOnlys", visualOnlys);
                setUniformFloatArray(effect, "stormSpins", stormSpins);
                setUniformFloatArray(effect, "stormDyings", stormDyings);
                setUniformFloatArray(effect, "tornadoShapes", tornadoShapes);
                setUniformFloatArray(effect, "stormOcclusions", stormOcclusions);
                setUniformFloat(effect, "overcastPerc", (float) ServerConfig.overcastPercent);
                setUniformFloat(effect, "rainStrength", (float) ServerConfig.rainStrength);
                
                Vec3 samplePos = camera
                    .getPosition()
                    .multiply(1.0, 0.0, 1.0)
                    .add(0.0, ServerConfig.layer0Height, 0.0);
                Vec3 lightingColor = new Vec3(1.0, 1.0, 1.0);
                lightingColor = lightingColor.lerp(
                    new Vec3(0.741, 0.318, 0.227),
                    Math.pow(1.0 - sunDir.y, 2.5)
                );
                lightingColor = lightingColor.lerp(
                    new Vec3(0.314, 0.408, 0.525),
                    Mth.clamp((sunDir.y + 0.1) / -0.1, 0.0, 1.0)
                );
                Vec3 skyColor = minecraft.level.getSkyColor(
                    samplePos,
                    partialTicks
                );
                setUniformFloat3(effect, "lightingColor", 
                    (float) lightingColor.x, (float) lightingColor.y, (float) lightingColor.z);
                setUniformFloat3(effect, "skyColor", 
                    (float) skyColor.x, (float) skyColor.y, (float) skyColor.z);
                
                int quality = switch (ClientConfig.volumetricsQuality) {
                    case POTATO -> 0;
                    case LOW -> 1;
                    case MEDIUM -> 2;
                    case HIGH -> 3;
                    case PC_KILLER -> 4;
                };
                setUniformInt(effect, "quality", quality);
                setUniformFloat(effect, "nearPlane", 0.05F);
                setUniformFloat(effect, "farPlane", minecraft.options.renderDistance().get() * 4.0F * 16.0F);
                setUniformFloat(effect, "renderDistance", 6000.0F);
                
                clouds.process(partialTicks);
            }

            minecraft.getMainRenderTarget().bindWrite(false);
            RenderSystem.depthMask(true);
            projMat.invert();
            modelMat.invert();
        }
    }

    /**
     * Special rendering method called from within the Oculus/Iris pipeline.
     * This bypasses the normal Oculus check since we're already inside the pipeline
     * and rendering at the correct stage (after composites, before final pass).
     * 
     * @param partialTicks Frame partial ticks
     * @param camera The camera
     * @param projMat Projection matrix
     * @param modelMat Model view matrix
     */
    public static void renderShadersForOculus(
        float partialTicks,
        Camera camera,
        Matrix4f projMat,
        Matrix4f modelMat
    ) {
        // Use a thread-local flag to bypass the Oculus check in renderShaders
        renderingFromOculusPipeline = true;
        try {
            renderShaders(partialTicks, camera, projMat, modelMat);
        } finally {
            renderingFromOculusPipeline = false;
        }
    }
    
    // Flag to indicate we're being called from within Oculus pipeline
    private static boolean renderingFromOculusPipeline = false;
    
    /**
     * Check if we should skip Oculus detection (because we're already in the pipeline)
     */
    public static boolean isRenderingFromOculusPipeline() {
        return renderingFromOculusPipeline;
    }

    public static PostChain createShader(ResourceLocation resourceLocation) {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            return new PostChain(
                minecraft.getTextureManager(),
                minecraft.getResourceManager(),
                minecraft.getMainRenderTarget(),
                resourceLocation
            );
        } catch (IOException exception) {
            Atmospherica.LOGGER.error(
                "Failed to load shader: {}",
                resourceLocation,
                exception
            );
        } catch (JsonSyntaxException exception) {
            Atmospherica.LOGGER.error(
                "Failed to parse shader: {}",
                resourceLocation,
                exception
            );
        }

        return null;
    }

    public static void createShaders() {
        if (clouds == null) {
            Atmospherica.LOGGER.info("Creating cloud shader from: {}", Atmospherica.getPath("shaders/post/clouds.json"));
            clouds = createShader(
                Atmospherica.getPath("shaders/post/clouds.json")
            );
            if (clouds != null) {
                Atmospherica.LOGGER.info("Cloud shader created successfully");
            } else {
                Atmospherica.LOGGER.error("Failed to create cloud shader!");
            }
        }
    }

    public static void reload() {
        if (clouds != null) {
            clouds.close();
        }

        clouds = null;
        createShaders();
        updateShaderGroupSize(clouds);
        Atmospherica.LOGGER.info("Loaded Atmospherica Shaders");
    }

    private static void updateShaderGroupSize(PostChain shaderGroup) {
        if (shaderGroup != null) {
            Minecraft minecraft = Minecraft.getInstance();
            int width = minecraft.getWindow().getWidth();
            int height = minecraft.getWindow().getHeight();
            shaderGroup.resize(width, height);
        }
    }

    // Optimized uniform setters that avoid lambda allocation overhead
    // Called every frame, so eliminating lambda allocation is important
    
    private static void setUniformFloat(EffectInstance effect, String name, float value) {
        if (effect == null) return;
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) uniform.set(value);
    }
    
    private static void setUniformFloat2(EffectInstance effect, String name, float v1, float v2) {
        if (effect == null) return;
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) uniform.set(v1, v2);
    }
    
    private static void setUniformFloat3(EffectInstance effect, String name, float v1, float v2, float v3) {
        if (effect == null) return;
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) uniform.set(v1, v2, v3);
    }
    
    private static void setUniformInt(EffectInstance effect, String name, int value) {
        if (effect == null) return;
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) uniform.set(value);
    }
    
    private static void setUniformMat4(EffectInstance effect, String name, Matrix4f mat) {
        if (effect == null) return;
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) uniform.set(mat);
    }
    
    private static void setUniformFloatArray(EffectInstance effect, String name, float[] arr) {
        if (effect == null) return;
        Uniform uniform = effect.getUniform(name);
        if (uniform != null) uniform.set(arr);
    }

    public enum Quality {
        POTATO,
        LOW,
        MEDIUM,
        HIGH,
        PC_KILLER,
    }
}
