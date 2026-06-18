## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2026-06-18 - Redundant Trigonometric and Vector Operations in Shaders
**Learning:** Operations like `Math.pow(wind.length() / 60.0, 2.0)` calculate a square root intrinsically (via `length()`) just to immediately square it again, which is highly inefficient. Similarly, recalculating `Math.cos(sunAngle)` multiple times and using `Math.pow(val, 3.0)` for small integer exponents in hot paths adds unnecessary overhead.
**Action:** Always replace `pow(length(), 2)` with `lengthSqr()`. Cache repeated trigonometric calls into local variables, and unroll small integer powers (e.g., `x * x * x`) to use explicit multiplication instead of `Math.pow()`.
