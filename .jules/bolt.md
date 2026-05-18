## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.
## 2024-10-24 - O(1) Configurations
**Learning:** High-frequency configuration lookups (e.g. valid dimension checks in tick loops or block checks during storm events) that use `ArrayList.contains()` introduce unnecessary O(N) overhead.
**Action:** Replace `ArrayList` with `HashSet` for public static configuration whitelists/blacklists (like `ServerConfig.validDimensions`) to achieve O(1) membership lookup complexity without changing downstream consumer logic, as both implement `Collection`.
