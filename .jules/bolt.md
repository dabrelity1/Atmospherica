## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-24 - Optimizing Configuration Lookups
**Learning:** Changing configuration-based collections (like `ServerConfig.validDimensions` or `blacklistedBlocks`) from `ArrayList` to `HashSet` reduces membership lookup complexity from O(N) to O(1). This is especially beneficial when these collections are queried frequently (e.g., every tick or during rendering).
**Action:** When collections are primarily used for fast lookups (`.contains()`) rather than ordered iteration, initialize them as `HashSet` instead of `ArrayList`.
