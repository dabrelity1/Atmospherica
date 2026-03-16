## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-25 - Zero-Allocation Geometric Rendering
**Learning:** `EntityRotFX.render` allocated `Vector3f[]` arrays and `Vector3f` objects per particle per frame, causing significant GC pressure. Additionally, `Quaternionf.clone()` used exception handling for control flow.
**Action:** In high-frequency render loops, replace small object allocations (like `Vector3f`) with local `float` primitives and manual math (e.g., unrolled matrix multiplication) to achieve zero-allocation rendering. Use copy constructors instead of `clone()` for JOML objects.
