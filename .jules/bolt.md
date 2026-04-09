## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-24 - Proximity Check Optimization
**Learning:** Checking distance against dynamic objects frequently causes `Vec3` allocations via `distance2D()` or `getCenter()`. In block entities checking for storms every second, this creates minor but steady GC pressure.
**Action:** Always compute 2D squared distance manually `(dx*dx + dz*dz) < rangeSq` to avoid `Math.sqrt` and `Vec3` garbage, and always perform cheap integer condition checks (`storm.stage`) before computing geometric distance.
