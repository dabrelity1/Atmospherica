## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.
## 2024-10-25 - Particle Render Optimization Bit-Packing
**Learning:** In high-frequency render loops (like ParticleManager.render), sorting particles by allocating `Vec3`s inside the comparator via `getPos()` creates massive GC pressure. Bit-packing a primitive distance (high 32 bits) and index (low 32 bits) into a `long[]` array and using `Arrays.sort` eliminates allocations and reduces sorting overhead significantly.
**Action:** Always prefer primitive array sorting over object `List.sort()` with complex comparators in hot render paths, and use bit-packing for safe float/int sorting. Ensure primitive coordinate getters are exposed via Mixins to avoid Vec3 unpacking.
