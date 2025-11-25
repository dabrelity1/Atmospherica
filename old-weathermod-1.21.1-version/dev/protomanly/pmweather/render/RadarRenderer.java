package dev.protomanly.pmweather.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.block.RadarBlock;
import dev.protomanly.pmweather.block.entity.RadarBlockEntity;
import dev.protomanly.pmweather.config.ClientConfig;
import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.event.GameBusClientEvents;
import dev.protomanly.pmweather.multiblock.wsr88d.WSR88DCore;
import dev.protomanly.pmweather.util.ColorTables;
import dev.protomanly.pmweather.util.Util;
import dev.protomanly.pmweather.weather.Clouds;
import dev.protomanly.pmweather.weather.Sounding;
import dev.protomanly.pmweather.weather.Storm;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import dev.protomanly.pmweather.weather.Vorticy;
import dev.protomanly.pmweather.weather.WindEngine;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fStack;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class RadarRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
   public static int RenderedRadars = 0;

   public static float FBM(SimplexNoise noise, Vec3 pos, int octaves, float lacunarity, float gain, float amplitude) {
      double y = 0.0;

      for (int i = 0; i < Math.max(octaves, 1); i++) {
         y += amplitude * noise.getValue(pos.x, pos.y, pos.z);
         pos = pos.multiply(lacunarity, lacunarity, lacunarity);
         amplitude *= gain;
      }

      return (float)y;
   }

   public RadarRenderer(Context context) {
   }

   public void render(T blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource multiBufferSource, int combinedLightIn, int combinedOverlayIn) {
      if (blockEntity instanceof RadarBlockEntity radarBlockEntity) {
         if (Minecraft.getInstance().player.position().distanceTo(blockEntity.getBlockPos().getCenter()) > 20.0 || RenderedRadars > 2) {
            return;
         }

         if (!(Boolean)radarBlockEntity.getBlockState().getValue(RadarBlock.ON)) {
            return;
         }

         RenderedRadars++;
         boolean canRender = true;
         BlockPos pos = radarBlockEntity.getBlockPos();
         float sizeRenderDiameter = 3.0F;
         float simSize = 2048.0F;
         int resolution = ClientConfig.radarResolution;
         if (radarBlockEntity.hasRangeUpgrade) {
            simSize *= 4.0F;
            if (!ClientConfig._3X3Radar) {
               sizeRenderDiameter = 6.0F;
            }
         }

         Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
         matrix4fStack.pushMatrix();
         matrix4fStack.mul(poseStack.last().pose());
         matrix4fStack.translate(0.5F, 1.05F, 0.5F);
         RenderSystem.applyModelViewMatrix();
         RenderSystem.enableBlend();
         RenderSystem.depthMask(true);
         RenderSystem.enableDepthTest();
         RenderSystem.setShader(GameRenderer::getPositionColorShader);
         RenderSystem.defaultBlendFunc();
         Tesselator tesselator = Tesselator.getInstance();
         BufferBuilder bufferBuilder = tesselator.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
         List<Storm> storms = new ArrayList<>(radarBlockEntity.storms);
         boolean update = false;
         ClientConfig.RadarMode radarMode = ClientConfig.radarMode;
         if (radarBlockEntity.lastUpdate < radarBlockEntity.tickCount) {
            radarBlockEntity.lastUpdate = radarBlockEntity.tickCount + 60;
            update = true;
         }

         if (ServerConfig.requireWSR88D && update) {
            canRender = false;
            int searchrange = 64;
            Level level = blockEntity.getLevel();

            for (int x = -searchrange; x <= searchrange && !canRender; x++) {
               for (int y = -searchrange; y <= searchrange && !canRender; y++) {
                  for (int z = -searchrange * 2; z <= searchrange * 2; z++) {
                     BlockState state = level.getBlockState(pos.offset(x, y, z));
                     if (state.getBlock() instanceof WSR88DCore core && core.isComplete(state)) {
                        canRender = true;
                        break;
                     }
                  }
               }
            }
         }

         float size = sizeRenderDiameter / resolution;

         for (int x = -resolution; x <= resolution; x++) {
            for (int zx = -resolution; zx <= resolution; zx++) {
               float r = 0.0F;
               float g = 0.0F;
               float b = 0.0F;
               float a = 0.0F;
               long id = x + resolution + 1 + (zx + resolution + 1) * (resolution * 2L + 1L);
               float dbz = radarBlockEntity.reflectivityMap.getOrDefault(id, 0.0F);
               float temp = radarBlockEntity.temperatureMap.getOrDefault(id, 15.0F);
               float vel = radarBlockEntity.velocityMap.getOrDefault(id, 0.0F);
               Color dbg = radarBlockEntity.debugMap.getOrDefault(id, new Color(0, 0, 0));
               Vector3f pixelPos = new Vector3f(x, 0.0F, zx).mul(1.0F / resolution).mul(sizeRenderDiameter / 2.0F);
               Vec3 worldPos = new Vec3(x, 0.0, zx).multiply(1.0F / resolution, 0.0, 1.0F / resolution).multiply(simSize, 0.0, simSize).add(pos.getCenter());
               if (update) {
                  float clouds = Clouds.getCloudDensity(GameBusClientEvents.weatherHandler, new Vector2f((float)worldPos.x, (float)worldPos.z), 0.0F);
                  dbz = 0.0F;
                  temp = 0.0F;
                  Vec2 f = new Vec2(x, zx).normalized();
                  Vec3 wind = WindEngine.getWind(
                     new Vec3(worldPos.x, blockEntity.getLevel().getMaxBuildHeight() + 1, worldPos.z), blockEntity.getLevel(), false, false, false
                  );
                  Vec2 w = new Vec2((float)wind.x, (float)wind.z);
                  vel = f.dot(w);

                  for (Storm storm : storms) {
                     if (!storm.visualOnly) {
                        double stormSize = ServerConfig.stormSize * 2.0;
                        if (storm.stormType == 0) {
                           stormSize *= 1.5;
                        }

                        double scale = stormSize / 1200.0;
                        if (storm.stormType == 2) {
                           scale = storm.maxWidth / 3000.0F;
                           scale *= 0.5;
                        }

                        double shapeNoise = radarBlockEntity.noise
                           .getValue(radarBlockEntity.tickCount / 8000.0F, worldPos.x / (750.0 * scale), worldPos.z / (750.0 * scale));
                        float fineShapeNoise = FBM(
                           radarBlockEntity.noise,
                           new Vec3(radarBlockEntity.tickCount / 8000.0F, worldPos.x / (500.0 * scale), worldPos.z / (500.0 * scale)),
                           10,
                           2.0F,
                           0.75F,
                           1.0F
                        );
                        double shapeNoise2 = radarBlockEntity.noise
                           .getValue(radarBlockEntity.tickCount / 8000.0F, worldPos.z / (750.0 * scale), worldPos.x / (750.0 * scale));
                        double shapeNoise3 = radarBlockEntity.noise
                           .getValue(radarBlockEntity.tickCount / 16000.0F, worldPos.x / (4000.0 * scale), worldPos.z / (4000.0 * scale));
                        double shapeNoise4 = radarBlockEntity.noise
                           .getValue(radarBlockEntity.tickCount / 8000.0F, worldPos.z / (250.0 * scale), worldPos.x / (250.0 * scale));
                        shapeNoise *= 0.5;
                        shapeNoise2 *= 0.5;
                        shapeNoise4 *= 0.5;
                        shapeNoise += 0.5;
                        shapeNoise2 += 0.5;
                        shapeNoise4 += 0.5;
                        float localDBZ = 0.0F;
                        float smoothStage = storm.stage + storm.energy / 100.0F;
                        if (storm.stormType == 2) {
                           Vec3 wPos = worldPos;
                           Vec3 cPos = storm.position.multiply(1.0, 0.0, 1.0);

                           for (Vorticy vorticy : storm.vorticies) {
                              Vec3 vPos = vorticy.getPosition();
                              float width = vorticy.getWidth() * 0.35F;
                              double d = wPos.multiply(1.0, 0.0, 1.0).distanceTo(vPos.multiply(1.0, 0.0, 1.0));
                              if (d < width) {
                                 double angle = Math.pow(1.0 - Math.clamp(d / width, 0.0, 1.0), 3.75);
                                 angle *= (float) (Math.PI / 10);
                                 angle *= Math.min(vorticy.windspeedMult * storm.windspeed, 6.0F);
                                 wPos = Util.rotatePoint(wPos, vPos, angle);
                              }
                           }

                           double rawDist = wPos.multiply(1.0, 0.0, 1.0).distanceTo(storm.position.multiply(1.0, 0.0, 1.0));
                           rawDist *= 1.0 + shapeNoise3 * 0.2F;
                           float intensity = (float)Math.pow(Math.clamp(storm.windspeed / 65.0F, 0.0F, 1.0F), 0.25);
                           Vec3 relPos = cPos.subtract(wPos).multiply(scale, 0.0, scale);
                           double d = storm.maxWidth / (3.0F + storm.windspeed / 12.0F);
                           double d2 = storm.maxWidth / (1.15F + storm.windspeed / 12.0F);
                           double dE = storm.maxWidth * 0.65F / (1.75F + storm.windspeed / 12.0F);
                           double fac = 1.0 + Math.max((rawDist - storm.maxWidth * 0.2F) / storm.maxWidth, 0.0) * 2.0;
                           d *= fac;
                           d2 *= fac;
                           double angle = Math.atan2(relPos.z, relPos.x) - rawDist / d;
                           double angle2 = Math.atan2(relPos.z, relPos.x) - rawDist / d2;
                           double angleE = Math.atan2(relPos.z, relPos.x) - rawDist / dE;
                           float weak = 0.0F;
                           float strong = 0.0F;
                           float intense = 0.0F;
                           float staticBands = (float)Math.sin(angle - (Math.PI / 2));
                           staticBands *= (float)Math.pow(Math.clamp(rawDist / (storm.maxWidth * 0.25F), 0.0, 1.0), 0.1F);
                           staticBands *= 1.25F * (float)Math.pow(intensity, 0.75);
                           if (staticBands < 0.0F) {
                              weak += Math.abs(staticBands);
                           } else {
                              weak += Math.abs(staticBands) * (float)Math.pow(1.0 - Math.clamp(rawDist / (storm.maxWidth * 0.65F), 0.0, 1.0), 0.5);
                              weak *= Math.clamp((storm.windspeed - 70.0F) / 40.0F, 0.0F, 1.0F);
                           }

                           float rotatingBands = (float)Math.sin((angle2 + Math.toRadians(storm.tickCount / 8.0F)) * 6.0);
                           rotatingBands *= (float)Math.pow(Math.clamp(rawDist / (storm.maxWidth * 0.25F), 0.0, 1.0), 0.1F);
                           rotatingBands *= 1.25F * (float)Math.pow(intensity, 0.75);
                           strong += Mth.lerp(0.45F, Math.abs(rotatingBands) * 0.3F + 0.7F, weak);
                           intense += Mth.lerp(0.3F, Math.abs(rotatingBands) * 0.2F + 0.8F, weak);
                           weak = (Math.abs(rotatingBands) * 0.3F + 0.6F) * weak;
                           localDBZ += Mth.lerp(
                              Math.clamp((storm.windspeed - 120.0F) / 60.0F, 0.0F, 1.0F),
                              Mth.lerp(Math.clamp((storm.windspeed - 40.0F) / 90.0F, 0.0F, 1.0F), weak, strong),
                              intense
                           );
                           float eye = (float)Math.sin((angleE + Math.toRadians(storm.tickCount / 4.0F)) * 2.0);
                           float efc = Mth.lerp(Math.clamp((storm.windspeed - 100.0F) / 50.0F, 0.0F, 1.0F), 0.15F, 0.4F);
                           localDBZ = Math.max(
                              (float)Math.pow(1.0 - Math.clamp(rawDist / (storm.maxWidth * efc), 0.0, 1.0), 0.5)
                                 * (Math.abs(eye * 0.1F) + 0.9F)
                                 * 1.35F
                                 * intensity,
                              localDBZ
                           );
                           localDBZ *= (float)Math.pow(1.0 - Math.clamp(rawDist / storm.maxWidth, 0.0, 1.0), 0.5);
                           localDBZ *= Mth.lerp(
                              0.5F + Math.clamp((storm.windspeed - 65.0F) / 40.0F, 0.0F, 1.0F) * 0.5F,
                              1.0F,
                              (float)Math.pow(Math.clamp(rawDist / (storm.maxWidth * 0.1F), 0.0, 1.0), 2.0)
                           );
                           localDBZ *= Mth.lerp(Math.clamp((storm.windspeed - 75.0F) / 50.0F, 0.0F, 1.0F), 0.8F + (float)shapeNoise2 * 0.4F, 1.0F);
                           localDBZ *= 0.8F + (float)shapeNoise * 0.4F;
                           localDBZ *= 1.0F + fineShapeNoise * Mth.lerp((float)Math.pow(Math.clamp(rawDist / storm.maxWidth, 0.0, 1.0), 1.5), 0.05F, 0.15F);
                           localDBZ = (float)Math.pow(localDBZ, 1.75);
                           if (localDBZ > 0.8F) {
                              float dif = (localDBZ - 0.8F) / 1.25F;
                              localDBZ -= dif;
                           }
                        }

                        if (storm.stormType == 1) {
                           double rawDist = worldPos.multiply(1.0, 0.0, 1.0).distanceTo(storm.position.multiply(1.0, 0.0, 1.0));
                           Vec2 v2fWorldPos = new Vec2((float)worldPos.x, (float)worldPos.z);
                           Vec2 stormVel = new Vec2((float)storm.velocity.x, (float)storm.velocity.z);
                           Vec2 v2fStormPos = new Vec2((float)storm.position.x, (float)storm.position.z);
                           Vec2 right = new Vec2(stormVel.y, -stormVel.x).normalized();
                           Vec2 fwd = stormVel.normalized();
                           Vec2 le = Util.mulVec2(right, -3000.0F * (float)scale);
                           Vec2 ri = Util.mulVec2(right, 3000.0F * (float)scale);
                           Vec2 off = Util.mulVec2(fwd, -((float)Math.pow(Mth.clamp(rawDist / (3000.0 * scale), 0.0, 1.0), 2.0)) * (900.0F * (float)scale));
                           le = le.add(off);
                           ri = ri.add(off);
                           le = le.add(v2fStormPos);
                           ri = ri.add(v2fStormPos);
                           float dist = Util.minimumDistance(le, ri, v2fWorldPos);

                           float intensityx = switch (storm.stage) {
                              case 1 -> 0.1F + storm.energy / 100.0F * 0.7F;
                              case 2 -> 0.8F + storm.energy / 100.0F * 0.4F;
                              case 3 -> 1.2F + storm.energy / 100.0F;
                              default -> storm.energy / 100.0F * 0.1F;
                           };
                           if (intensityx > 0.8F) {
                              intensityx = 0.8F + (intensityx - 0.8F) / 1.5F;
                           }

                           Vec2 nearPoint = Util.nearestPoint(le, ri, v2fWorldPos);
                           Vec2 facing = v2fWorldPos.add(nearPoint.negated());
                           float behind = -facing.dot(fwd);
                           behind += (float)shapeNoise * 600.0F * (float)scale * 0.2F;
                           float sze = 600.0F * (float)scale * 1.5F * 3.0F;
                           behind += (float)stormSize / 2.0F;
                           if (behind > 0.0F) {
                              sze *= Mth.lerp(Mth.clamp(smoothStage - 1.0F, 0.0F, 1.0F), 1.0F, 4.0F);
                              float p = Mth.clamp(Math.abs(behind) / sze, 0.0F, 1.0F);
                              float start = 0.06F;
                              if (p <= start) {
                                 p /= start;
                                 localDBZ += (float)Math.pow(p, 2.0);
                              } else {
                                 p = 1.0F - (p - start) / (1.0F - start);
                                 localDBZ += (float)Math.pow(p, 4.0);
                              }
                           }

                           localDBZ *= Mth.sqrt(1.0F - Mth.clamp(dist / sze, 0.0F, 1.0F));
                           if (smoothStage > 3.0F) {
                              float p = Mth.clamp((smoothStage - 3.0F) / 2.0F, 0.0F, 0.5F);
                              localDBZ *= 0.8F + (float)shapeNoise2 * 0.4F * (1.0F - p);
                              localDBZ *= 0.8F + (float)shapeNoise * 0.4F * (1.0F - p);
                              localDBZ *= 1.0F + p * 0.25F;
                           } else {
                              localDBZ *= 0.8F + (float)shapeNoise2 * 0.4F;
                              localDBZ *= 0.8F + (float)shapeNoise * 0.4F;
                           }

                           localDBZ *= Mth.sqrt(intensityx);
                        }

                        if (storm.stormType == 0) {
                           double dist = worldPos.multiply(1.0, 0.0, 1.0).distanceTo(storm.position.multiply(1.0, 0.0, 1.0));
                           if (dist > stormSize * 4.0) {
                              continue;
                           }
                           float intensityxx = switch (storm.stage) {
                              case 1 -> 0.1F + storm.energy / 100.0F * 0.7F;
                              case 2 -> 0.8F + storm.energy / 100.0F * 0.4F;
                              case 3 -> 1.2F + storm.windspeed / 100.0F;
                              default -> (float)Math.pow(storm.energy / 100.0F, 2.0) * 0.1F;
                           };
                           if (intensityxx > 0.8F) {
                              intensityxx = 0.8F + (intensityxx - 0.8F) / 4.0F;
                           }
                           float windspeed = switch (storm.stage) {
                              case 2 -> storm.energy / 100.0F * 40.0F;
                              case 3 -> 40.0F + storm.windspeed;
                              default -> 0.0F;
                           };
                           if (windspeed > 60.0F) {
                              windspeed -= (windspeed - 60.0F) * 0.2F;
                           }

                           Vec3 torPos = storm.position.multiply(1.0, 0.0, 1.0);
                           Vec3 corePos = torPos.add(
                              100.0 * scale * 2.5 * Math.clamp(intensityxx * 1.5F, 0.0F, 1.0F),
                              0.0,
                              -350.0 * scale * 2.5 * Math.clamp(intensityxx * 1.5F, 0.0F, 1.0F)
                           );
                           float xM = 1.75F;
                           if (worldPos.x > corePos.x) {
                              xM = 1.0F;
                           }

                           double coreDist = Math.sqrt(Math.pow((worldPos.x - corePos.x) * xM, 2.0) + Math.pow((worldPos.z - corePos.z) * 1.5, 2.0)) / scale;
                           dist /= scale;
                           coreDist *= 0.9 + shapeNoise * 0.3;
                           Vec3 relPosx = torPos.subtract(worldPos).multiply(scale, 0.0, scale);
                           double dx = 150.0 + dist / 3.0;
                           double d2x = 75.0 + dist / 3.0;
                           double anglex = Math.atan2(relPosx.z, relPosx.x) - dist / dx;
                           double angle2x = Math.atan2(relPosx.z, relPosx.x) - dist / d2x;
                           double angle3 = Math.atan2(relPosx.z, relPosx.x) - dist / d2x / 2.0;
                           anglex += Math.toRadians(180.0);
                           angle2x += Math.toRadians(180.0);
                           angle3 += Math.toRadians(180.0);
                           double angleMod = Math.toRadians(40.0) * (1.0 - Math.clamp(Math.pow(windspeed / 100.0, 2.0), 0.0, 0.9));
                           double noise = (shapeNoise4 - 0.5) * Math.toRadians(10.0);
                           anglex += angleMod + noise;
                           angle2x += angleMod + noise;
                           angle3 += angleMod + noise;
                           double inflow = Math.sin(anglex - Math.toRadians(15.0));
                           inflow = Math.pow(Math.abs(inflow), 0.5) * Math.sin(inflow);
                           inflow *= 1.0 - Math.clamp(dist / 2400.0, 0.0, 1.0);
                           if (inflow < 0.0) {
                              localDBZ += (float)(inflow * 2.0 * Math.pow(Math.clamp((windspeed - 15.0F) / 50.0, 0.0, 1.0), 2.0));
                           }

                           double surge = Math.sin(angle2x - Math.toRadians(60.0));
                           surge = Math.abs(surge) * Math.sin(surge);
                           surge *= (1.0 - Math.pow(Math.clamp(dist / 1200.0, 0.0, 1.0), 1.5)) * (1.0 - Math.clamp(dist / 200.0, 0.0, 0.3));
                           if (surge > 0.0) {
                              double n = 0.8 * (1.0 - Math.clamp(Math.pow(windspeed / 80.0, 2.0), 0.0, 1.0));
                              double m = 1.0 - shapeNoise4 * n;
                              localDBZ += (float)(
                                 surge * 1.5 * Math.clamp(dist / 500.0, 0.0, 1.0) * Math.sqrt(Math.clamp((windspeed - 20.0F) / 50.0, 0.0, 1.0)) * m
                              );
                           }

                           double shield = Math.sin(angle3 - Math.toRadians(60.0));
                           shield = Math.abs(shield) * Math.sin(shield);
                           shield *= 1.0 - Math.pow(Math.clamp(dist / 2400.0, 0.0, 1.0), 2.0);
                           if (shield > 0.0) {
                              localDBZ -= (float)(
                                 shield * 2.0 * Math.clamp(dist / 1000.0, 0.0, 1.0) * Math.sqrt(Math.clamp((windspeed - 30.0F) / 80.0, 0.0, 1.0))
                              );
                           }

                           double coreIntensity = (1.0 - Math.clamp(coreDist / 1800.0, 0.0, 1.0))
                              * (1.5 - shapeNoise2 * 0.5)
                              * Math.sqrt(Math.clamp(intensityxx / 2.0, 0.0, 1.0))
                              * Math.clamp(dist / 300.0, 0.5, 1.0)
                              * 1.2;
                           localDBZ += (float)Math.pow(coreIntensity, 0.65);
                        }

                        dbz = Math.max(dbz, localDBZ);
                     }
                  }

                  float v = Math.max(clouds - 0.15F, 0.0F) * 4.0F;
                  if (v > 0.4F) {
                     float dif = (v - 0.4F) / 2.0F;
                     v -= dif;
                  }

                  dbz = Math.max(dbz, v);
                  dbz += (PMWeather.RANDOM.nextFloat() - 0.5F) * 5.0F / 60.0F;
                  vel += (PMWeather.RANDOM.nextFloat() - 0.5F) * 3.0F;
                  if (dbz > 1.0F) {
                     dbz = (dbz - 1.0F) / 3.0F + 1.0F;
                  }

                  if (!canRender) {
                     dbz = PMWeather.RANDOM.nextFloat() * 1.2F;
                     vel = (PMWeather.RANDOM.nextFloat() - 0.5F) * 300.0F;
                     temp = 15.0F;
                  } else {
                     temp = ThermodynamicEngine.samplePoint(GameBusClientEvents.weatherHandler, worldPos, blockEntity.getLevel(), radarBlockEntity, 0)
                        .temperature();
                  }

                  radarBlockEntity.reflectivityMap.put(id, dbz);
                  radarBlockEntity.temperatureMap.put(id, temp);
                  radarBlockEntity.velocityMap.put(id, vel);
               }

               float rdbz = dbz * 60.0F;
               Color startColor = radarBlockEntity.terrainMap.getOrDefault(id, new Color(0, 0, 0));
               if (radarBlockEntity.init && update) {
                  Holder<Biome> biome = radarBlockEntity.getNearestBiome(new BlockPos((int)worldPos.x, (int)worldPos.y, (int)worldPos.z));
                  String rn = biome.getRegisteredName().toLowerCase();
                  if (rn.contains("ocean") || rn.contains("river")) {
                     startColor = new Color(((Biome)biome.value()).getWaterColor());
                  } else if (rn.contains("beach") || rn.contains("desert")) {
                     startColor = new Color(227, 198, 150);
                  } else if (rn.contains("badlands")) {
                     startColor = new Color(214, 111, 42);
                  } else {
                     startColor = new Color(((Biome)biome.value()).getGrassColor(worldPos.x, worldPos.z));
                  }

                  startColor = ColorTables.lerp(0.5F, startColor, new Color(0, 0, 0));
                  radarBlockEntity.terrainMap.put(id, startColor);
               }

               Color color = ColorTables.getReflectivity(rdbz, startColor);
               if (rdbz > 5.0F && !radarBlockEntity.hasRangeUpgrade) {
                  if (temp < 3.0F && temp > -1.0F) {
                     color = ColorTables.getMixedReflectivity(rdbz);
                  } else if (temp <= -1.0F) {
                     color = ColorTables.getSnowReflectivity(rdbz);
                  }
               }

               RadarBlock.Mode mode = (RadarBlock.Mode)blockEntity.getBlockState().getValue(RadarBlock.RADAR_MODE);
               if (mode == RadarBlock.Mode.VELOCITY) {
                  color = new Color(0, 0, 0);
                  vel /= 1.75F;
                  color = ColorTables.lerp(Mth.clamp(Math.max(rdbz, (Mth.abs(vel) - 18.0F) / 0.65F) / 12.0F, 0.0F, 1.0F), color, ColorTables.getVelocity(vel));
               }

               if (mode == RadarBlock.Mode.IR) {
                  float ir = rdbz * 10.0F;
                  if (rdbz > 10.0F) {
                     ir = 100.0F + (rdbz - 10.0F) * 2.5F;
                  }

                  if (rdbz > 50.0F) {
                     ir += (rdbz - 50.0F) * 5.0F;
                  }

                  color = ColorTables.getIR(ir);
               }

               if (ClientConfig.radarDebugging && update) {
                  if (radarMode == ClientConfig.RadarMode.TEMPERATURE) {
                     float t = ThermodynamicEngine.samplePoint(GameBusClientEvents.weatherHandler, worldPos, blockEntity.getLevel(), radarBlockEntity, 0)
                        .temperature();
                     if (t <= 0.0F) {
                        dbg = ColorTables.lerp(Math.clamp(t / -40.0F, 0.0F, 1.0F), new Color(153, 226, 251, 255), new Color(29, 53, 221, 255));
                     } else if (t < 15.0F) {
                        dbg = ColorTables.lerp(Math.clamp(t / 15.0F, 0.0F, 1.0F), new Color(255, 255, 255, 255), new Color(225, 174, 46, 255));
                     } else {
                        dbg = ColorTables.lerp(Math.clamp((t - 15.0F) / 25.0F, 0.0F, 1.0F), new Color(225, 174, 46, 255), new Color(232, 53, 14, 255));
                     }
                  }

                  if (radarMode == ClientConfig.RadarMode.SST) {
                     Float t = ThermodynamicEngine.GetSST(GameBusClientEvents.weatherHandler, worldPos, blockEntity.getLevel(), radarBlockEntity, 0);
                     if (t == null) {
                        dbg = new Color(0, 0, 0);
                     } else {
                        dbg = ColorTables.getSST(t);
                     }
                  }

                  if (radarMode == ClientConfig.RadarMode.CLOUDS) {
                     float clouds = Clouds.getCloudDensity(GameBusClientEvents.weatherHandler, new Vector2f((float)worldPos.x, (float)worldPos.z), 0.0F);
                     clouds = Math.clamp(clouds, 0.0F, 1.0F);
                     dbg = new Color(clouds, clouds, clouds);
                  }

                  if (radarMode == ClientConfig.RadarMode.WINDFIELDS && GameBusClientEvents.weatherHandler != null) {
                     Vec3 wP = new Vec3(x, 0.0, zx).multiply(1.0F / resolution, 0.0, 1.0F / resolution).multiply(256.0, 0.0, 256.0).add(pos.getCenter());
                     float wind = 0.0F;

                     for (Storm stormx : storms) {
                        wind += stormx.getWind(wP);
                     }

                     dbg = ColorTables.getWindspeed(wind);
                  }

                  if (radarMode == ClientConfig.RadarMode.GLOBALWINDS && GameBusClientEvents.weatherHandler != null) {
                     int height = GameBusClientEvents.weatherHandler.getWorld().getHeight(Types.MOTION_BLOCKING, (int)worldPos.x, (int)worldPos.z);
                     float wind = (float)WindEngine.getWind(
                           new Vec3(worldPos.x, height, worldPos.z), GameBusClientEvents.weatherHandler.getWorld(), false, false, false, true
                        )
                        .length();
                     dbg = ColorTables.getHurricaneWindspeed(wind);
                  }

                  if (radarMode == ClientConfig.RadarMode.CAPE) {
                     Sounding sounding = new Sounding(GameBusClientEvents.weatherHandler, worldPos, blockEntity.getLevel(), 500, 12000, radarBlockEntity);
                     Sounding.CAPE CAPE = sounding.getCAPE(sounding.getSBParcel());
                     dbg = ColorTables.lerp(Mth.clamp(CAPE.CAPE() / 6000.0F, 0.0F, 1.0F), new Color(0, 0, 0), new Color(255, 0, 0));
                  }

                  if (radarMode == ClientConfig.RadarMode.CAPE3KM) {
                     Sounding sounding = new Sounding(GameBusClientEvents.weatherHandler, worldPos, blockEntity.getLevel(), 250, 4000, radarBlockEntity);
                     Sounding.CAPE CAPE = sounding.getCAPE(sounding.getSBParcel());
                     dbg = ColorTables.lerp(Mth.clamp(CAPE.CAPE3() / 1000.0F, 0.0F, 1.0F), new Color(0, 0, 0), new Color(255, 0, 0));
                  }

                  if (radarMode == ClientConfig.RadarMode.CINH) {
                     Sounding sounding = new Sounding(GameBusClientEvents.weatherHandler, worldPos, blockEntity.getLevel(), 500, 12000, radarBlockEntity);
                     Sounding.CAPE CAPE = sounding.getCAPE(sounding.getSBParcel());
                     dbg = ColorTables.lerp(Mth.clamp(CAPE.CINH() / -250.0F, 0.0F, 1.0F), new Color(0, 0, 0), new Color(0, 0, 255));
                  }

                  if (radarMode == ClientConfig.RadarMode.LAPSERATE03) {
                     Sounding sounding = new Sounding(GameBusClientEvents.weatherHandler, worldPos, blockEntity.getLevel(), 250, 4000, radarBlockEntity);
                     float lapse = (float)Math.floor(sounding.getLapseRate(0, 3000) * 2.0F) / 2.0F;
                     if (lapse > 5.0F) {
                        dbg = ColorTables.lerp(Mth.clamp((lapse - 5.0F) / 5.0F, 0.0F, 1.0F), new Color(255, 255, 0), new Color(255, 0, 0));
                     } else {
                        dbg = ColorTables.lerp(Mth.clamp(lapse / 5.0F, 0.0F, 1.0F), new Color(0, 255, 0), new Color(255, 255, 0));
                     }
                  }

                  if (radarMode == ClientConfig.RadarMode.LAPSERATE36) {
                     Sounding sounding = new Sounding(GameBusClientEvents.weatherHandler, worldPos, blockEntity.getLevel(), 250, 7000, radarBlockEntity);
                     float lapse = (float)Math.floor(sounding.getLapseRate(3000, 6000) * 2.0F) / 2.0F;
                     if (lapse > 5.0F) {
                        dbg = ColorTables.lerp(Mth.clamp((lapse - 5.0F) / 5.0F, 0.0F, 1.0F), new Color(255, 255, 0), new Color(255, 0, 0));
                     } else {
                        dbg = ColorTables.lerp(Mth.clamp(lapse / 5.0F, 0.0F, 1.0F), new Color(0, 255, 0), new Color(255, 255, 0));
                     }
                  }

                  radarBlockEntity.debugMap.put(id, dbg);
               }

               if (ClientConfig.radarDebugging) {
                  color = dbg;
               }

               r = color.getRed() / 255.0F;
               g = color.getGreen() / 255.0F;
               b = color.getBlue() / 255.0F;
               a = color.getAlpha() / 255.0F * 0.75F + 0.25F;
               Vector3f topLeft = new Vector3f(-1.0F, 0.0F, -1.0F).mul(size / 4.0F).add(pixelPos);
               Vector3f bottomLeft = new Vector3f(-1.0F, 0.0F, 1.0F).mul(size / 4.0F).add(pixelPos);
               Vector3f bottomRight = new Vector3f(1.0F, 0.0F, 1.0F).mul(size / 4.0F).add(pixelPos);
               Vector3f topRight = new Vector3f(1.0F, 0.0F, -1.0F).mul(size / 4.0F).add(pixelPos);
               bufferBuilder.addVertex(topLeft)
                  .setColor(r, g, b, a)
                  .addVertex(bottomLeft)
                  .setColor(r, g, b, a)
                  .addVertex(bottomRight)
                  .setColor(r, g, b, a)
                  .addVertex(topRight)
                  .setColor(r, g, b, a);
            }
         }

         float rx = 1.0F;
         float gx = 0.0F;
         float bx = 0.0F;
         float ax = 1.0F;
         Vector3f topLeft = new Vector3f(-1.0F, 0.0F, -1.0F).mul(0.015F).add(0.0F, 0.01F, 0.0F);
         Vector3f bottomLeft = new Vector3f(-1.0F, 0.0F, 1.0F).mul(0.015F).add(0.0F, 0.01F, 0.0F);
         Vector3f bottomRight = new Vector3f(1.0F, 0.0F, 1.0F).mul(0.015F).add(0.0F, 0.01F, 0.0F);
         Vector3f topRight = new Vector3f(1.0F, 0.0F, -1.0F).mul(0.015F).add(0.0F, 0.01F, 0.0F);
         bufferBuilder.addVertex(topLeft)
            .setColor(rx, gx, bx, ax)
            .addVertex(bottomLeft)
            .setColor(rx, gx, bx, ax)
            .addVertex(bottomRight)
            .setColor(rx, gx, bx, ax)
            .addVertex(topRight)
            .setColor(rx, gx, bx, ax);
         matrix4fStack.mul(poseStack.last().pose().invert());
         matrix4fStack.translate(-0.5F, -1.05F, -0.5F);
         matrix4fStack.popMatrix();
         MeshData meshData = bufferBuilder.build();
         if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
         }

         RenderSystem.applyModelViewMatrix();
         RenderSystem.disableBlend();
         RenderSystem.defaultBlendFunc();
      }
   }
}
