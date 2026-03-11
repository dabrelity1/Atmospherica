## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-25 - Avoid using multiply(1.0, 0.0, 1.0) and distanceTo for 2D distances
**Learning:** In weather calculations, checking distance on the XZ plane is done by zeroing out the Y coordinate with `.multiply(1.0, 0.0, 1.0)` and calling `.distanceTo()`. This is highly inefficient because it allocates intermediate temporary `Vec3` objects on every tick calculation, adding significant garbage collection pressure.
**Action:** Use primitive arithmetic like `Util.distance2D(Vec3 a, Vec3 b)` to avoid object allocation entirely when checking distances only on the XZ plane.
