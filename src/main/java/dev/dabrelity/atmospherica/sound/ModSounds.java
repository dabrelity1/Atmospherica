package dev.dabrelity.atmospherica.sound;

import dev.dabrelity.atmospherica.Atmospherica;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.ForgeSoundType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
   public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Atmospherica.MOD_ID);
   public static final RegistryObject<SoundEvent> SIREN = register("siren");
   public static final RegistryObject<SoundEvent> HAIL = register("hail");
   public static final RegistryObject<SoundEvent> CALM_AMBIENCE = register("calm_ambience");
   public static final RegistryObject<SoundEvent> WIND_STRONG = register("wind_strong");
   public static final RegistryObject<SoundEvent> WIND_MED = register("wind_med");
   public static final RegistryObject<SoundEvent> WIND_CALM = register("wind_calm");
   public static final RegistryObject<SoundEvent> TORNADIC_WIND = register("tornadic_wind");
   public static final RegistryObject<SoundEvent> TORNADIC_DAMAGE = register("tornadic_damage");
   public static final RegistryObject<SoundEvent> SUPERCELL_WIND = register("supercell_wind");
   public static final RegistryObject<SoundEvent> EYEWALL_WIND = register("eyewall_wind");
   public static final RegistryObject<SoundEvent> UNDERGROUND_WIND = register("underground_wind");
   public static final RegistryObject<SoundEvent> WSR88D_COMPLETED = register("wsr88d_completed");
   public static final RegistryObject<SoundEvent> WSR88D_DISMANTLED = register("wsr88d_dismantled");
   public static final RegistryObject<SoundEvent> RAIN = register("rain");
   public static final RegistryObject<SoundEvent> SLEET = register("sleet");
   public static final RegistryObject<SoundEvent> THUNDER_NEAR = register("thunder_near");
   public static final RegistryObject<SoundEvent> THUNDER_FAR = register("thunder_far");
   public static final RegistryObject<SoundEvent> SLEET_BREAK = register("sleet_break");
   public static final RegistryObject<SoundEvent> SLEET_STEP = register("sleet_step");
   public static final RegistryObject<SoundEvent> SLEET_PLACE = register("sleet_place");
   public static final RegistryObject<SoundEvent> SLEET_HIT = register("sleet_hit");
   public static final RegistryObject<SoundEvent> SLEET_FALL = register("sleet_fall");
   public static final SoundType SLEET_BLOCK = new ForgeSoundType(
      1.0F,
      1.0F,
      () -> SLEET_BREAK.get(),
      () -> SLEET_STEP.get(),
      () -> SLEET_PLACE.get(),
      () -> SLEET_HIT.get(),
      () -> SLEET_FALL.get()
   );

   private static RegistryObject<SoundEvent> register(String name) {
      return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(Atmospherica.getPath(name)));
   }

   @OnlyIn(Dist.CLIENT)
   public static void playBlockSound(Level level, BlockState block, BlockPos blockPos, SoundEvent soundEvent, float volume, float pitch, float cutOffRange) {
      MovingSoundStreamingSource sound = new MovingSoundStreamingSource(level, block, blockPos, soundEvent, SoundSource.WEATHER, volume, pitch, cutOffRange);
      Minecraft.getInstance().getSoundManager().play(sound);
   }

   @OnlyIn(Dist.CLIENT)
   public static void playPlayerLockedSound(Vec3 pos, SoundEvent soundEvent, float volume, float pitch) {
      MovingSoundStreamingSource sound = new MovingSoundStreamingSource(pos, soundEvent, SoundSource.WEATHER, volume, pitch, true);
      Minecraft.getInstance().getSoundManager().play(sound);
   }
}
