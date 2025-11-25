package dev.dabrelity.atmospherica.entity;

import dev.dabrelity.atmospherica.Atmospherica;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
   public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Atmospherica.MOD_ID);
   public static final RegistryObject<EntityType<MovingBlock>> MOVING_BLOCK = ENTITY_TYPES.register(
      "moving_block", () -> Builder.<MovingBlock>of(MovingBlock::new, MobCategory.MISC).sized(1.0F, 1.0F).build(Atmospherica.getPath("moving_block").toString())
   );
}
