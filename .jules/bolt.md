## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-25 - Optimizing Euclidean distance calculation in XZ plane
**Learning:** `v1.multiply(1.0, 0.0, 1.0).distanceTo(v2.multiply(1.0, 0.0, 1.0))` creates up to 3 intermediate `Vec3` objects just to calculate 2D distance. This causes massive garbage collection overhead in hot loops like rendering or tick processing.
**Action:** Created `Util.distance2D(Vec3, Vec3)` which calculates the Euclidean distance directly using the X and Z primitives, entirely avoiding object allocations. Replaced 10+ instances across the codebase while avoiding changes to 3D spherical checks like `WindEngine`.
