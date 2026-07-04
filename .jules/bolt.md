## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.
## 2024-10-24 - ModShaders High-Frequency Optimizations
**Learning:** In Minecraft `renderShaders`, heavy operations like `Math.pow` and `Vec3` allocations are executed every frame. `Vec3.length()` allocates nothing but uses expensive `Math.sqrt()`. Using `Vec3.lengthSqr()` avoids `Math.sqrt()`, and unpacking `Math.pow(..., 3.0)` avoids JNI overhead.
**Action:** Always replace `Math.pow` with unrolled math and `length()` with `lengthSqr()` in high-frequency GL loops.
