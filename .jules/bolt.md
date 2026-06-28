## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.
## 2024-05-18 - Optimize Particle Sorting Allocations
**Learning:** In Minecraft/Forge codebases, using `List.sort()` with a Comparator that calls `getPos()` on particles generates massive GC overhead because `Particle.getPos()` allocates a new `Vec3` on every call, leading to O(N log N) allocations per frame.
**Action:** Replace `List.sort()` in hot rendering paths with a primitive bit-packed array sort (`long[]` where high 32 bits are float distance, low 32 bits are index). Expose primitive coordinates (`getPosX()`, `getPosY()`, `getPosZ()`) via mixins to bypass `Vec3` allocations entirely.
