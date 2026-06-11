## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.
## 2024-05-28 - Avoid Math.sqrt inside Vector length methods in shaders
**Learning:** In Minecraft shader rendering loops (like `ModShaders.tick()`), `wind.length()` calls native `Math.sqrt` which becomes a massive bottleneck. When the result is immediately squared (e.g. `Math.pow(wind.length() / 60.0, 2.0)`), using `.lengthSqr()` entirely bypasses the square root and avoids `Math.pow` object allocation overhead.
**Action:** Always check if a Vector length is squared nearby and replace it with `lengthSqr()` divided by the squared constant.
