## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-25 - Skip Costly Evaluations
**Learning:** `GameBusEvents` processed 260 damage evaluations per player per tick, even when storms weren't present. When base wind speeds never reach damage thresholds without storms, these checks are an extreme CPU and memory waste via `BlockPos` generation.
**Action:** Always wrap conditionally required loops (e.g. storm-driven logic) in a check verifying the conditional (e.g., `!weatherHandler.getStorms().isEmpty()`). Also, replace `distanceTo` with `distanceToSqr` and utilize primitive block coordinate queries via `Level.getHeight` to eliminate object allocations inside high-frequency evaluation loops.
