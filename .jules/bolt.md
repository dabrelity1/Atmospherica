## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-25 - Math.sqrt Instead of Vec3.distanceTo for Hot Loop Performance
**Learning:** `Vec3.distanceTo` implicitly creates new `Vec3` objects if vectors are multiplied/manipulated on the fly (e.g., `pos.multiply(1.0, 0.0, 1.0).distanceTo(...)`). This occurs frequently in `WindEngine`, a class executed 20+ times per tick for all nearby blocks and entities, generating enormous garbage collection overhead.
**Action:** Unpack distance calculations into primitive local variables (e.g., `dx = x1 - x2`, `Math.sqrt(dx*dx + dz*dz)`) to eliminate the `Vec3` allocations during hot weather loops.
