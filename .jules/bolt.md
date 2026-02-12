## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-24 - Particle Rendering Allocations
**Learning:** Particle rendering methods like `EntityRotFX.render` are extremely hot paths. Even small object allocations like `new Vector3f[]` or `new Vector3f()` inside loops accumulate significant GC pressure.
**Action:** Unroll fixed-size loops for vertex calculations and use local variables or mutable reusable objects instead of allocating arrays. Use copy constructors `new Quaternionf(other)` instead of `clone()` to avoid overhead and exception handling.
