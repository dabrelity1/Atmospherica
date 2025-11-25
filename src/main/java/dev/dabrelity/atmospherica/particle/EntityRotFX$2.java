package dev.dabrelity.atmospherica.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferBuilder.RenderedBuffer;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import org.jetbrains.annotations.NotNull;

class EntityRotFX$2 implements ParticleRenderType {
   public void begin(BufferBuilder bufferBuilder, @NotNull TextureManager textureManager) {
      RenderSystem.disableBlend();
      RenderSystem.depthMask(true);
      RenderSystem.setShader(GameRenderer::getParticleShader);
      RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
      bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.PARTICLE);
   }

   public void end(Tesselator tesselator) {
      RenderedBuffer renderedBuffer = tesselator.end();
      BufferUploader.drawWithShader(renderedBuffer);
   }

   public String toString() {
      return "PARTICLE_BLOCK_SHEET_SORTED_OPAQUE";
   }
}
