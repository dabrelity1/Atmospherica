## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-25 - Primitive Packing for Particle Sorting
**Learning:** High-frequency path `ParticleManager.render` was generating thousands of `Vec3` objects per frame when computing sorting distances for back-to-front rendering of overlapping transparent particles (via `particle.getPos()`). Additionally, calling `List.sort` allocated sorting overhead arrays. Replacing it with bit-packed `long[]` elements (storing float distance in high 32 bits and list index in low 32 bits) drastically reduced frame-time memory overhead.
**Action:** When sorting hundreds of elements in the render loop by a computed float criteria, use an explicit `long[]` with primitive sorting (`Arrays.sort`) instead of object collections to avoid excessive GC thrashing. Unpack values from mixin interfaces to skip `Vec3` allocations.
