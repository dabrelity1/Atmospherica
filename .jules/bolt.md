## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.
## 2024-05-31 - WindEngine and GameBusEvents Hot Path
**Learning:** `GameBusEvents.onLevelTick` runs a nested loop 260 times per player per tick (checking random positions for wind damage). Each iteration was allocating `BlockPos`, `Vec3i`, and `Vec3`. `WindEngine.getWind` was also heavily allocating `Vec3` for vector math.
**Action:** Replace `BlockPos` usage in hot loops with `BlockPos.MutableBlockPos`. Refactor heavy vector math methods (like `getWind`) to accept primitives (`double x, y, z`) and perform internal math using primitives to avoid `Vec3` allocation until absolutely necessary. This saves thousands of allocations per tick.
