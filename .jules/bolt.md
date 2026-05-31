## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-24 - Primitive Bit-Packing Sort for Particles
**Learning:** `ParticleManager` rendering loops sort thousands of particles per frame back-to-front based on squared distance. The object-based `List.sort()` allocated multiple `Vec3` objects per comparison via `.distanceToSqr()`, causing immense CPU overhead and garbage collection pressure in an `O(N log N)` path.
**Action:** Replace `Comparator`-based sorts in hot loops with a bit-packing approach. Compute the distance once per object, pack it into the upper 32 bits of a `long` using `Float.floatToRawIntBits(distSq)`, pack the index into the lower 32 bits, and use `Arrays.sort()` on a pre-allocated reusable primitive `long[]` array. Extract the cached distance later using `Float.intBitsToFloat` to skip further calculations.
