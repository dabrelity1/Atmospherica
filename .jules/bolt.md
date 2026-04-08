## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-05-18 - Configuration lists to HashSets for fast lookups
**Learning:** Changing configuration-based whitelists/blacklists (e.g., `ServerConfig.validDimensions`) from `ArrayList` to `HashSet` reduces membership lookup complexity from O(n) to O(1). This is critical for high-frequency paths like `ModShaders.renderShaders` (called every frame) and `GameBusEvents.onLevelTick` (called every tick).
**Action:** Always prefer `HashSet` over `ArrayList` for configuration lists when the main usage is membership testing (`contains()`).
