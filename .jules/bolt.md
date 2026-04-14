## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-04-14 - HashSet over ArrayList in High-Frequency Paths
**Learning:** Checking membership with `ArrayList.contains` scales O(n) which kills performance when repeated 260 times per player tick (`GameBusEvents.onLevelTick`) or per-frame (`ModShaders.renderShaders`). Switching configuration block/dimension lists to `HashSet` makes membership lookup O(1).
**Action:** Replace `ArrayList` with `HashSet` for configuration lists used in high-frequency conditionals.
