## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-25 - Expensive Vector Operations in Distance Calculations
**Learning:** Chaining `Vec3.multiply(1.0, 0.0, 1.0).distanceTo(...)` implicitly creates multiple temporary `Vec3` objects per call, creating unnecessary garbage collection pressure, especially in high-frequency loops.
**Action:** Replace `multiply(1.0, 0.0, 1.0).distanceTo(...)` with `Util.distance2D(vec1, vec2)` to perform XZ plane calculations efficiently using primitive variables. However, always verify that a true 3D spherical check wasn't intended before refactoring.
