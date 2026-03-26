## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-25 - Object Allocation in 3D Math operations
**Learning:** Minecraft's math functions (`distanceTo`) implicitly perform expensive operations (`Math.sqrt()`). When performing many distance checks in tight loops (e.g. checking storm spawns against player distance), doing raw squared distance checks (`distanceToSqr`) significantly reduces calculation overhead.
**Action:** In high frequency execution paths like `TickEvent.LevelTickEvent`, check if distance formulas can be rewritten to use squared distances to eliminate expensive square roots and memory allocations.
