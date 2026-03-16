## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-24 - Optimizing Hot Loops with MutableBlockPos
**Learning:** Hot loops in `GameBusEvents` perform thousands of `BlockPos` allocations per second due to random offset calculations and `below()` calls. Refactoring to use a single `MutableBlockPos` and updating utility methods (like `WindEngine.getWind`) to accept it can eliminate >80% of these allocations.
**Action:** Identify loops iterating over block positions and replace `offset`/`below`/`above` chains with `MutableBlockPos.set`/`move`. Ensure `immutable()` is called before passing the position to any method that might store it (e.g., entity creation).
