## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-25 - Use distance2D for 2D distance calculations
**Learning:** The codebase repeatedly used `.multiply(1.0, 0.0, 1.0).distanceTo(...)` to compute 2D distances using 3D vectors. This created unnecessary temporary `Vec3` objects.
**Action:** Introduced a `distance2D(Vec3 a, Vec3 b)` method in `Util.java` and replaced the `.multiply(1.0, 0.0, 1.0).distanceTo(...)` calls with it to avoid object allocation.
