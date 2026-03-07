## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-25 - Avoid Vec3 Allocations for 2D Distance
**Learning:** Using `Vec3.multiply(1.0, 0.0, 1.0).distanceTo(...)` implicitly creates multiple temporary `Vec3` objects. In hot paths like particle rendering, radar rendering or tick loops, this leads to immense GC pressure. However, attempting to change 3D euclidean distances to 2D euclidean distances in certain loops (`WindEngine`) caused logical regressions in weather calculations.
**Action:** Replace 2D distance calculations involving `Vec3` allocations with the primitive-based `Util.distance2D(Vec3 a, Vec3 b)` method everywhere EXCEPT `WindEngine.java`.
