## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-25 - Primitive Math Overrides Object Allocation
**Learning:** `Vec3.distanceTo()` creates temporary vectors and uses `Math.sqrt`, which can be a significant bottleneck in frequently ticked code paths like `BlockEntity` proximity checks. Even using custom 2D distance utility like `Util.distance2D(vec1, vec2)` requires vector instantiation.
**Action:** Replace `distanceTo` with inline primitive math (`dx * dx + dz * dz < rangeSq`) directly derived from `BlockPos` and entity positional components whenever calculating ranges. Hoist squared constants outside checking loops to eliminate allocations entirely.
