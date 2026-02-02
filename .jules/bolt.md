## 2024-05-31 - Unnecessary Sorting of Opaque Particles
**Learning:** The custom `ParticleManager` was sorting ALL particles by distance every frame, including opaque ones (`PARTICLE_SHEET_OPAQUE`, `TERRAIN_SHEET`). Opaque particles do not require sorting as they rely on the depth buffer. This introduced O(N log N) overhead for particle types that don't need it.
**Action:** Always filter render operations based on whether sorting is visually required (translucency) before applying expensive sort algorithms.
