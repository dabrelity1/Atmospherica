## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.
## 2024-10-24 - Zero-Allocation Particle Sorting
**Learning:** `ParticleManager` is a major bottleneck because it sorts hundreds of thousands of particles using `List.sort` and `particle.getPos()`, allocating a `Vec3` object per particle, per frame. The $O(N \log N)$ object creation is massive. Fabric/Forge Mixin interfaces can safely expose primitive variables without reflection.
**Action:** Always replace `List.sort(Comparator)` with primitive array bit-packing (`long[]`) for spatial sorting on the hot path. Extract coordinate fields `x, y, z` natively by extending the Mixin interface to `getPosX()`, `getPosY()`, `getPosZ()` to skip `Vec3` instantiation.
