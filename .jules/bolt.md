## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.
## 2024-10-24 - Math.pow vs Multiplication in Java
**Learning:** When unrolling `Math.pow(x, c)` to `x * x` patterns for performance, copying an expression containing method calls directly (e.g. `Mth.clamp(...) * Mth.clamp(...)`) results in repeated method execution, which often degrades performance instead of improving it.
**Action:** Always extract inner expressions of `Math.pow` into local primitive variables before multiplying them out, and explicitly cast the variable/result back to `(float)` to prevent `double to float` lossy conversion compile errors.
