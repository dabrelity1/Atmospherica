## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-25 - Efficient 2D Distance Checks
**Learning:** Using `Vec3.multiply(1.0, 0.0, 1.0).distanceTo()` for 2D distance calculations instantiates multiple temporary `Vec3` objects, causing high garbage collection overhead in hot loops like `WindEngine` and `RadarRenderer`.
**Action:** Use primitive arithmetic via `Util.distance2D(Vec3 a, Vec3 b)` specifically designed to calculate 2D Euclidean distance on the X and Z axes without instantiating new objects.
