## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.
## 2024-10-25 - Avoid Object Allocations with MutableBlockPos
**Learning:** Re-instantiating `BlockPos` objects within tight loops creates significant garbage collection overhead. Using `BlockPos.MutableBlockPos` avoids allocations by reusing the object via `.set()` and `.setY()`. However, you must always call `.immutable()` when storing the reference, such as when passing the position to `setStartPos()`.
**Action:** Identify tight simulation or loop-heavy components and replace standard `BlockPos` object creations with shared `MutableBlockPos` references.
