package dev.dabrelity.atmospherica.particle;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import dev.dabrelity.atmospherica.Atmospherica;
import dev.dabrelity.atmospherica.config.ClientConfig;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleDescription;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.commons.compress.utils.Lists;
import org.joml.Matrix4fStack;

public class ParticleManager implements PreparableReloadListener {
   private static final FileToIdConverter PARTICLE_LISTER = FileToIdConverter.json("particles");
   private static final ResourceLocation PARTICLES_ATLAS_INFO = new ResourceLocation("minecraft", "particles");
   private static final List<ParticleRenderType> RENDER_ORDER = ImmutableList.of(
      ParticleRenderType.TERRAIN_SHEET,
      ParticleRenderType.PARTICLE_SHEET_OPAQUE,
      ParticleRenderType.PARTICLE_SHEET_LIT,
      ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT,
      ParticleRenderType.CUSTOM,
      EntityRotFX.SORTED_OPAQUE_BLOCK,
      EntityRotFX.SORTED_TRANSLUCENT
   );
   protected ClientLevel level;
   private final TextureAtlas textureAtlas;
   private final TextureManager textureManager;
   private final Map<ResourceLocation, ParticleProvider<?>> providers = new HashMap();
   private final Map<ResourceLocation, ParticleManager.MutableSpriteSet> spriteSets = Maps.newHashMap();
   private final Queue<Particle> particlesToAdd = Queues.newArrayDeque();
   private final Queue<TrackingEmitter> trackingEmitters = Queues.newArrayDeque();
   private final Map<ParticleRenderType, Queue<Particle>> particles = Maps.newTreeMap(makeParticleRenderTypeComparator(RENDER_ORDER));
   private final Object2IntOpenHashMap<ParticleGroup> trackedParticleCounts = new Object2IntOpenHashMap();
   
   // Reusable collections for render() to avoid allocations each frame
   private final Map<Integer, List<Particle>> sortedListCache = new HashMap();
   private final List<Particle> tempParticleList = new ArrayList(256);

   private static java.util.Comparator<ParticleRenderType> makeParticleRenderTypeComparator(List<ParticleRenderType> order) {
      return (a, b) -> {
         int ai = order.indexOf(a);
         int bi = order.indexOf(b);
         if (ai == -1 && bi == -1) return Integer.compare(System.identityHashCode(a), System.identityHashCode(b));
         if (ai == -1) return 1;
         if (bi == -1) return -1;
         return Integer.compare(ai, bi);
      };
   }

   public ParticleManager(ClientLevel level, TextureManager textureManager) {
      this.textureAtlas = new TextureAtlas(TextureAtlas.LOCATION_PARTICLES);
      this.level = level;
      this.textureManager = textureManager;
   }

   // Helper record to store particle definition data
   private record ParticleDefinitionData(ResourceLocation resourceLocation, Optional<List<ResourceLocation>> sprites) {}

   public CompletableFuture<Void> reload(
      PreparationBarrier preparationBarrier,
      ResourceManager resourceManager,
      ProfilerFiller profilerFiller,
      ProfilerFiller profilerFiller1,
      Executor executor,
      Executor executor1
   ) {
      CompletableFuture<List<ParticleDefinitionData>> completableFuture = CompletableFuture.supplyAsync(
            () -> PARTICLE_LISTER.listMatchingResources(resourceManager), executor
         )
         .thenCompose(
            locationResourceMap -> {
               List<CompletableFuture<ParticleDefinitionData>> list = new ArrayList(locationResourceMap.size());
               locationResourceMap.forEach(
                  (k, v) -> {
                     ResourceLocation resourceLocation = PARTICLE_LISTER.fileToId(k);
                     list.add(
                        CompletableFuture.supplyAsync(
                           () -> new ParticleDefinitionData(resourceLocation, this.loadParticleDescription(resourceLocation, v)), executor
                        )
                     );
                  }
               );
               return Util.sequence(list);
            }
         );
      CompletableFuture<SpriteLoader.Preparations> completableFuture1 = SpriteLoader.create(this.textureAtlas)
         .loadAndStitch(resourceManager, PARTICLES_ATLAS_INFO, 0, executor)
         .thenCompose(SpriteLoader.Preparations::waitForUpload);
      return CompletableFuture.allOf(completableFuture1, completableFuture).thenCompose(preparationBarrier::wait).thenAcceptAsync(v -> {
         this.clearParticles();
         profilerFiller1.startTick();
         profilerFiller1.push("upload");
         SpriteLoader.Preparations preparations = completableFuture1.join();
         this.textureAtlas.upload(preparations);
         profilerFiller1.popPush("bindSpriteSets");
         Set<ResourceLocation> set = new HashSet();
         TextureAtlasSprite textureAtlasSprite = preparations.missing();
         completableFuture.join().forEach(particleDefinition -> {
            Optional<List<ResourceLocation>> optionalResourceLocations = particleDefinition.sprites();
            if (optionalResourceLocations.isPresent()) {
               List<TextureAtlasSprite> textureAtlasSprites = new ArrayList();

               for (ResourceLocation resourceLocation : optionalResourceLocations.get()) {
                  TextureAtlasSprite textureAtlasSprite1 = preparations.regions().get(resourceLocation);
                  if (textureAtlasSprite1 == null) {
                     set.add(resourceLocation);
                     textureAtlasSprites.add(textureAtlasSprite);
                  } else {
                     textureAtlasSprites.add(textureAtlasSprite1);
                  }
               }

               if (textureAtlasSprites.isEmpty()) {
                  textureAtlasSprites.add(textureAtlasSprite);
               }

               ((ParticleManager.MutableSpriteSet)this.spriteSets.get(particleDefinition.resourceLocation())).rebind(textureAtlasSprites);
            }
         });
         if (!set.isEmpty()) {
            Atmospherica.LOGGER.warn("Missing particle sprites: {}", set.stream().sorted().map(ResourceLocation::toString).collect(Collectors.joining(",")));
         }

         profilerFiller1.pop();
         profilerFiller1.endTick();
      }, executor1);
   }

   private Optional<List<ResourceLocation>> loadParticleDescription(ResourceLocation resourceLocation, Resource resource) {
      if (!this.spriteSets.containsKey(resourceLocation)) {
         Atmospherica.LOGGER.debug("Redundant texture list for particle: {}", resourceLocation);
         return Optional.empty();
      } else {
         try {
            Reader reader = resource.openAsReader();

            Optional var5;
            try {
               ParticleDescription particleDescription = ParticleDescription.fromJson(GsonHelper.parse(reader));
               var5 = Optional.of(particleDescription.getTextures());
            } catch (Throwable var7) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (reader != null) {
               reader.close();
            }

            return var5;
         } catch (IOException var8) {
            throw new IllegalStateException("Failed to load description for particle " + resourceLocation, var8);
         }
      }
   }

   @Nullable
   private <T extends ParticleOptions> Particle makeParticle(T particleOptions, double x, double y, double z, double xMotion, double yMotion, double zMotion) {
      ParticleProvider<T> particleProvider = (ParticleProvider<T>)this.providers.get(BuiltInRegistries.PARTICLE_TYPE.getKey(particleOptions.getType()));
      return particleProvider == null ? null : particleProvider.createParticle(particleOptions, this.level, x, y, z, xMotion, yMotion, zMotion);
   }

   public void add(Particle particle) {
      Optional<ParticleGroup> optional = particle.getParticleGroup();
      if (optional.isPresent()) {
         if (this.hasSpaceInParticleLimit((ParticleGroup)optional.get())) {
            this.particlesToAdd.add(particle);
            this.updateCount((ParticleGroup)optional.get(), 1);
         }
      } else {
         this.particlesToAdd.add(particle);
      }
   }

   public void tick() {
      this.level.getProfiler().push("Atmospherica_particle_tick");
      this.particles.forEach((particleRenderType, particles1) -> {
         this.level.getProfiler().push("Atmospherica_particle_tick_" + particleRenderType.toString());
         this.tickParticleList(particles1);
         this.level.getProfiler().pop();
      });
      if (!this.trackingEmitters.isEmpty()) {
         List<TrackingEmitter> list = Lists.newArrayList();

         for (TrackingEmitter trackingEmitter : this.trackingEmitters) {
            trackingEmitter.tick();
            if (!trackingEmitter.isAlive()) {
               list.add(trackingEmitter);
            }
         }

         this.trackingEmitters.removeAll(list);
      }

      Particle particle;
      if (!this.particlesToAdd.isEmpty()) {
         while ((particle = (Particle)this.particlesToAdd.poll()) != null) {
            ((Queue)this.particles.computeIfAbsent(particle.getRenderType(), particleRenderType -> EvictingQueue.create(32768))).add(particle);
         }
      }

      this.level.getProfiler().pop();
   }

   private void tickParticleList(Collection<Particle> particles) {
      if (!particles.isEmpty()) {
         Iterator<Particle> iterator = particles.iterator();

         while (iterator.hasNext()) {
            Particle particle = (Particle)iterator.next();
            this.tickParticle(particle);
            if (!particle.isAlive()) {
               particle.getParticleGroup().ifPresent(particleGroup -> this.updateCount(particleGroup, -1));
               iterator.remove();
            }
         }
      }
   }

   private void updateCount(ParticleGroup particleGroup, int count) {
      this.trackedParticleCounts.addTo(particleGroup, count);
   }

   private void tickParticle(Particle particle) {
      try {
         particle.tick();
      } catch (Throwable var5) {
         CrashReport crashReport = CrashReport.forThrowable(var5, "Ticking Particle");
         CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being ticked");
         crashReportCategory.setDetail("Particle", particle::toString);
         crashReportCategory.setDetail("Particle Type", particle.getRenderType()::toString);
         throw new ReportedException(crashReport);
      }
   }

   public void render(PoseStack poseStack, BufferSource bufferSource, LightTexture lightTexture, Camera camera, float partialTicks, @Nullable Frustum frustum) {
      this.level.getProfiler().push("Atmospherica_particle_render");
      float fogStart = RenderSystem.getShaderFogStart();
      float fogEnd = RenderSystem.getShaderFogEnd();
      RenderSystem.setShaderFogStart(fogStart);
      RenderSystem.setShaderFogEnd(fogEnd * 2.0F);
      lightTexture.turnOnLightLayer();
      RenderSystem.enableDepthTest();
      RenderSystem.activeTexture(33986);
      RenderSystem.activeTexture(33984);
      PoseStack modelViewStack = RenderSystem.getModelViewStack();
      modelViewStack.pushPose();
      modelViewStack.mulPoseMatrix(poseStack.last().pose());
      RenderSystem.applyModelViewMatrix();
      RenderSystem.disableCull();
      int particleCount = 0;
      
      // Cache camera position to avoid repeated method calls
      final Vec3 cameraPos = camera.getPosition();
      final double maxDistSq = ClientConfig.maxParticleSpawnDistanceFromPlayer * ClientConfig.maxParticleSpawnDistanceFromPlayer;

      for (ParticleRenderType particleRenderType : this.particles.keySet()) {
         this.level.getProfiler().push(particleRenderType.toString());
         if (particleRenderType != ParticleRenderType.NO_RENDER) {
            Iterable<Particle> iterable = (Iterable<Particle>)this.particles.get(particleRenderType);
            if (iterable != null) {
               RenderSystem.setShader(GameRenderer::getParticleShader);
               Tesselator tesselator = Tesselator.getInstance();
               BufferBuilder bufferBuilder = tesselator.getBuilder();
               particleRenderType.begin(bufferBuilder, this.textureManager);
               
               // Reuse cached map and clear it instead of creating new one each frame
               for (List<Particle> list : sortedListCache.values()) {
                  list.clear();
               }
               int maxRenderOrder = 0;

               for (Particle particle : iterable) {
                  int renderOrder = 10;
                  if (particle instanceof EntityRotFX entityRotFX) {
                     renderOrder = entityRotFX.renderOrder;
                  }

                  if (renderOrder > maxRenderOrder) {
                     maxRenderOrder = renderOrder;
                  }

                  List<Particle> list = sortedListCache.get(renderOrder);
                  if (list == null) {
                     list = new ArrayList(64);
                     sortedListCache.put(renderOrder, list);
                  }
                  list.add(particle);
               }

               for (int i = 0; i <= maxRenderOrder; i++) {
                  List<Particle> particlesSorted = sortedListCache.get(i);
                  if (particlesSorted != null && !particlesSorted.isEmpty()) {
                     // Sort by distance to camera (back to front)
                     particlesSorted.sort((p1, p2) -> {
                        double d1 = p1.getPos().distanceToSqr(cameraPos);
                        double d2 = p2.getPos().distanceToSqr(cameraPos);
                        return Double.compare(d2, d1);
                     });

                     for (Particle particle : particlesSorted) {
                        double distSq = cameraPos.distanceToSqr(particle.getPos());
                        
                        // Skip particles that are too far away
                        if (distSq > maxDistSq) continue;
                        
                        boolean shouldSkip;
                        if (particle instanceof EntityRotFX entityRotFX) {
                           shouldSkip = distSq > entityRotFX.renderRange * entityRotFX.renderRange
                              || (frustum != null && !frustum.isVisible(entityRotFX.getBoundingBoxForRender()));
                        } else {
                           shouldSkip = distSq > 65536.0
                              || (frustum != null && !frustum.isVisible(particle.getBoundingBox()));
                        }
                        
                        if (!shouldSkip) {
                           try {
                              particle.render(bufferBuilder, camera, partialTicks);
                           } catch (Throwable var25) {
                              CrashReport crashReport = CrashReport.forThrowable(var25, "Rendering Particle");
                              CrashReportCategory crashReportCategory = crashReport.addCategory("Particle being rendered");
                              crashReportCategory.setDetail("Particle", particle::toString);
                              crashReportCategory.setDetail("Particle Type", particleRenderType::toString);
                              throw new ReportedException(crashReport);
                           }
                        }
                     }
                  }
               }

               particleRenderType.end(tesselator);
            }

            this.level.getProfiler().pop();
         }
      }

      modelViewStack.popPose();
      RenderSystem.applyModelViewMatrix();
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
      lightTexture.turnOffLightLayer();
      RenderSystem.setShaderFogStart(fogStart);
      RenderSystem.setShaderFogEnd(fogEnd);
      this.level.getProfiler().pop();
   }

   public void setLevel(@Nullable ClientLevel level) {
      this.level = level;
      this.clearParticles();
      this.trackingEmitters.clear();
   }

   private boolean hasSpaceInParticleLimit(ParticleGroup particleGroup) {
      return this.trackedParticleCounts.getInt(particleGroup) < particleGroup.getLimit();
   }

   public void clearParticles() {
      this.particles.clear();
      this.particlesToAdd.clear();
      this.trackingEmitters.clear();
      this.trackedParticleCounts.clear();
   }

   public Map<ParticleRenderType, Queue<Particle>> getParticles() {
      return this.particles;
   }

   static class MutableSpriteSet implements SpriteSet {
      private List<TextureAtlasSprite> sprites;

      public TextureAtlasSprite get(int i, int i1) {
         return (TextureAtlasSprite)this.sprites.get(i * (this.sprites.size() - 1) / i1);
      }

      public TextureAtlasSprite get(RandomSource randomSource) {
         return (TextureAtlasSprite)this.sprites.get(randomSource.nextInt(this.sprites.size()));
      }

      public void rebind(List<TextureAtlasSprite> textureAtlasSprites) {
         this.sprites = ImmutableList.copyOf(textureAtlasSprites);
      }
   }
}
