package dev.dabrelity.atmospherica.particle;

import com.google.common.collect.ImmutableList;
import java.util.List;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.RandomSource;

class ParticleManager$MutableSpriteSet implements SpriteSet {
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
