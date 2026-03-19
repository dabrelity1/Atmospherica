## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-25 - Expensive distance calculations in tick/render loops
**Learning:** `Vec3.distanceTo` implicitly calls `Math.sqrt`. In very hot loops like `GameBusEvents.onLevelTick`, `ModShaders.renderShaders` (per-frame, per-storm distance check), and `WindEngine.getWind`, using `distanceTo` imposes continuous CPU overhead.
**Action:** When calculating distance simply to compare it against a range constant, always square the range constant (or calculate it manually) and use `distanceToSqr` (or `dx*dx + dy*dy + dz*dz`) instead to eliminate `Math.sqrt` overhead in critical paths.
