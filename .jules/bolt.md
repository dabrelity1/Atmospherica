## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-06-01 - Block Entity Proximity Checks
**Learning:** `BlockEntity.tick` methods running proximity checks against centralized dynamic lists (like active storms) suffer unnecessary memory allocations when calling helper functions like `blockPos.getCenter()` (allocates `Vec3`) or `Util.distance2D` (calls `Math.sqrt` and accepts `Vec3` arguments). Short-circuiting evaluation using fast integer comparisons (e.g., `storm.stage >= 3`) before spatial checks prevents these allocations.
**Action:** When refactoring proximity checks inside BlockEntities, unroll helpers into local primitive XZ squared distance calculations (`dx*dx + dz*dz < rangeSq`), hoist stationary block coordinates outside the loop, and move cheap boolean/integer checks before the spatial math.
