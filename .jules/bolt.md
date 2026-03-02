## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-25 - Avoid `Vec3` 2D Distance Workarounds
**Learning:** Using patterns like `position.multiply(1.0, 0.0, 1.0).distanceTo(target.multiply(1.0, 0.0, 1.0))` to calculate 2D distance implicitly allocates multiple throwaway `Vec3` objects. In high-frequency paths like tick loops (`WindEngine.java`), this generates excessive GC pressure and degrades performance.
**Action:** Replace `multiply(1.0, 0.0, 1.0).distanceTo(...)` with manual primitive math `Math.sqrt(dx * dx + dz * dz)` to completely eliminate object allocation during distance checks.