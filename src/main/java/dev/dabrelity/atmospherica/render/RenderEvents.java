package dev.dabrelity.atmospherica.render;

import dev.dabrelity.atmospherica.Atmospherica;
import dev.dabrelity.atmospherica.event.GameBusClientEvents;
import dev.dabrelity.atmospherica.particle.ParticleManager;
import dev.dabrelity.atmospherica.shaders.ModShaders;
import dev.dabrelity.atmospherica.weather.Lightning;
import dev.dabrelity.atmospherica.weather.WeatherHandlerClient;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Handles rendering for Atmospherica mod.
 * Registered via @EventBusSubscriber annotation.
 * Uses AFTER_WEATHER stage for shader rendering.
 */
@Mod.EventBusSubscriber(modid = Atmospherica.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class RenderEvents {

    public static List<Color> lightningColors = new ArrayList<Color>() {
        {
            this.add(new Color(16777215));
            this.add(new Color(15587698));
            this.add(new Color(13041578));
            this.add(new Color(10778102));
            this.add(new Color(15106690));
            this.add(new Color(7203548));
        }
    };

    /**
     * RenderLevelStageEvent - Main rendering hook for shaders and effects
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        RenderLevelStageEvent.Stage stage = event.getStage();

        // Only render once the vanilla weather has finished drawing so our pass is last
        if (stage != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        RadarRenderer.RenderedRadars = 0;
        float partialTicks = event.getPartialTick();
        WeatherHandlerClient weatherHandlerClient =
            (WeatherHandlerClient) GameBusClientEvents.weatherHandler;

        if (weatherHandlerClient != null) {
            // Log storm rendering occasionally

            // Render lightning
            List<Lightning> lightnings = weatherHandlerClient.lightnings;
            for (int i = 0; i < lightnings.size(); i++) {
                Lightning lightning = lightnings.get(i);
                if (lightning != null) {
                    Random rand = new Random(lightning.seed);
                    Color color = lightningColors.get(
                        rand.nextInt(lightningColors.size())
                    );
                    float p = Mth.clamp(
                        (lightning.ticks + partialTicks) / lightning.lifetime,
                        0.0F,
                        1.0F
                    );
                    float alpha =
                        (float) Math.abs(
                            Math.cos(Math.sqrt(p) * Math.PI * 3.0)
                        ) *
                        (1.0F - p);
                    int alphaInt = Mth.clamp((int) (alpha * 255.0F), 0, 255);
                    color = new Color(
                        color.getRed(),
                        color.getGreen(),
                        color.getBlue(),
                        alphaInt
                    );
                    CustomLightningRenderer.render(
                        lightning.position,
                        lightning.seed,
                        event.getCamera(),
                        color
                    );
                }
            }

            // Render volumetric shaders (clouds/tornados)
            Matrix4f modelViewMatrix = new Matrix4f(
                event.getPoseStack().last().pose()
            );
            Matrix4f projectionMatrix = event.getProjectionMatrix();

            ModShaders.renderShaders(
                partialTicks,
                event.getCamera(),
                projectionMatrix,
                modelViewMatrix
            );

            // Render custom particles
            ParticleManager pm = GameBusClientEvents.particleManager;
            if (pm != null) {
                pm.render(
                    event.getPoseStack(),
                    null,
                    minecraft.gameRenderer.lightTexture(),
                    event.getCamera(),
                    partialTicks,
                    event.getFrustum()
                );
            }
        }
    }
}
