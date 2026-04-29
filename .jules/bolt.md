## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-24 - Approximating Arbitrary Exponents Causes Logical Regressions
**Learning:** While optimizing `Math.pow(x, constant)` in hot loops using direct multiplication or `Math.sqrt` is highly effective for integers and clean fractions (0.5, 0.75), attempting to approximate arbitrary floating-point exponents (e.g., `0.85F`, `0.1F`) risks breaking complex algorithm behavior like weather banding or intensity calculations.
**Action:** When performing `Math.pow` optimizations, only target known, clean powers (e.g., `2.0`, `3.0`, `0.5`, `0.75`, `1.5`, `2.5`, `3.75`) that can be perfectly replicated using simple math. Leave arbitrary or imprecise floating-point exponents unoptimized to guarantee logic stability.
