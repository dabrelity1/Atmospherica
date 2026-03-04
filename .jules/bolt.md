## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-25 - Implicit Allocation in 2D Distance Checks
**Learning:** Using `Vec3.multiply(1.0, 0.0, 1.0).distanceTo(...)` to calculate 2D Euclidean distance on the XZ plane allocates two temporary `Vec3` objects per call. In hot loops like `WindEngine` (tick loop) and `RadarRenderer` (frame loop), this leads to massive garbage generation and GC stalls.
**Action:** Created `Util.distance2D(Vec3, Vec3)` which calculates Euclidean distance using strictly primitive math `Math.sqrt(dx * dx + dz * dz)`, resulting in zero allocations. When performing vector math in tight loops, always favor primitive unwrapping over chaining immutable vector methods.
