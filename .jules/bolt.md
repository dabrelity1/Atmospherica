## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-11-20 - distanceTo vs distanceToSqr loops
**Learning:** Distance threshold checks in high-frequency loops (like checking all valid storm spawning players or checking cyclone distance thresholds in `GameBusEvents`) were using `.distanceTo`, causing unnecessary `Math.sqrt` calculations.
**Action:** Use `.distanceToSqr` and compare against a squared threshold value to eliminate the expensive square root operations when checking if an object is within a certain radius.
