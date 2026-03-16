## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-25 - Particle Rendering Allocations
**Learning:** `EntityRotFX.render` allocated a `Vector3f[]` and 4 `Vector3f` instances per particle per frame, causing significant GC pressure in high-particle scenarios. Replacing with a single mutable `Vector3f` and unrolling the loop eliminates these allocations.
**Action:** In high-frequency rendering paths, prefer reusing a single mutable vector instance and unrolling small loops over allocating new objects or arrays.

## 2024-10-25 - Quaternion Cloning Overhead
**Learning:** `Quaternionf.clone()` usage wrapped in `try-catch` adds unnecessary overhead in hot paths.
**Action:** Use copy constructors (e.g., `new Quaternionf(other)`) instead of `clone()` for JOML objects to avoid checked exceptions and improve performance.
