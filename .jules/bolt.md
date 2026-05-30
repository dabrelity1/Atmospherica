## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-24 - Optimizing Configuration Whitelists Safely
**Learning:** O(N) list lookups (`contains()`) in server tick paths for configuration fields (like `validDimensions` or `blacklistedBlocks`) can create significant performance drag as modpacks scale. However, simply changing the field type from `List` to `Set` breaks the public API, and anonymous subclasses that only override `contains` and `add` are unsafe against other mutating methods (e.g., `addAll`, `remove`).
**Action:** When converting static `List` configs to O(1) lookups, wrap the populated list in an `AbstractList` that implements `contains()` via a backing `HashSet`, but delegates `get()` and `size()` to the original list. This yields O(1) lookups, respects the `List` contract, and inherently makes the config read-only (safe from desynchronization bugs).
