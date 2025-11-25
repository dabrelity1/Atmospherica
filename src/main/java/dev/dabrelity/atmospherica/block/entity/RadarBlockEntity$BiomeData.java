package dev.dabrelity.atmospherica.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

public record RadarBlockEntity$BiomeData(BlockPos pos, Holder<Biome> biome) {
}
