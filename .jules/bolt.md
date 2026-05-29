## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.
## 2024-11-20 - Distance Calculation Overhead in Entity Ticks
**Learning:** Checking distance using `distanceTo` or `Math.sqrt` on every tick for multiple entities creates unnecessary CPU overhead and garbage collection pressure due to temporary `Vec3` creation.
**Action:** Always prefer hoisted squared distance logic (`dx*dx + dz*dz < rangeSq`), moving conditional early-exits (e.g., checking storm stage) before math operations, and using primitive components instead of allocating new vector objects.
