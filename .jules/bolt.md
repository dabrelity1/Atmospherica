## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-25 - Block Entity Distance Check Optimizations
**Learning:** Proximity checks in block entities like `TornadoSirenBlockEntity` and `TornadoSensorBlockEntity` were performing expensive `Math.sqrt` calculations and object allocations (`blockPos.getCenter()`) inside a loop for every storm, even when the storm wasn't a valid type (e.g. stage < 3).
**Action:** Optimize radial distance checks by hoisting static calculations (like `rangeSq` and `blockPos` coordinates) outside the loop. Use squared distance comparisons (`dx*dx + dz*dz < rangeSq`) to avoid `Math.sqrt`. Always evaluate cheap integer conditions (`storm.stage`, `storm.stormType`) before performing expensive math or distance calculations to allow for early exits.
