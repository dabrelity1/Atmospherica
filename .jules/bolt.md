## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-25 - Avoid `Vec3` garbage generation when doing 2D distance calculations
**Learning:** Calling `pos.multiply(1.0, 0.0, 1.0).distanceTo(storm.position.multiply(1.0, 0.0, 1.0))` creates 2 temporary `Vec3` instances per call (one for each `multiply`). When checking distances to hundreds of storms/particles inside ticking or rendering loops, this generates thousands of unnecessary objects per tick/frame.
**Action:** Replaced `.multiply(1.0, 0.0, 1.0).distanceTo(...)` and 2D-only `distanceTo` comparisons with a dedicated `Util.distance2D(Vec3 a, Vec3 b)` method. This uses primitive double arithmetic on the `x` and `z` components directly, resulting in zero object allocations during the computation.
