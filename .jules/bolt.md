## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-05-23 - Particle Rendering Allocations
**Learning:** `EntityRotFX.render` allocated `new Vector3f[]` and 4 `Vector3f` objects per frame per particle. This causes massive GC pressure. Also `Quaternionf.clone()` is used defensively but is slow and relies on try-catch; `new Quaternionf(other)` is the correct copy constructor.
**Action:** Unroll tight rendering loops and use a single reused "scratch" `Vector3f` variable to perform vertex calculations. Replace `clone()` with copy constructors for JOML objects.
