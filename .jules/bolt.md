## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.
## 2026-05-21 - [HashSet Config Lookup Optimization]
**Learning:** In highly recursive or tight loops (e.g., weather event processing checking multiple blocks per tick), config properties backed by `ArrayList` (like `blacklistedBlocks`) introduce an O(N) hidden cost on every  call.
**Action:** Always verify the underlying data structure of frequently queried config collections. Convert them to `HashSet` if they are used primarily for membership checks (O(1)).
## 2024-05-21 - [HashSet Config Lookup Optimization]
**Learning:** In highly recursive or tight loops (e.g., weather event processing checking multiple blocks per tick), config properties backed by `ArrayList` (like `blacklistedBlocks`) introduce an O(N) hidden cost on every `.contains()` call.
**Action:** Always verify the underlying data structure of frequently queried config collections. Convert them to `HashSet` if they are used primarily for membership checks (O(1)).
