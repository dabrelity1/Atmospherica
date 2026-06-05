## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.
## 2024-10-24 - Unrolling Math.pow() in ThermodynamicEngine.samplePoint
**Learning:** `Math.pow()` is extensively used in the hot path `ThermodynamicEngine.samplePoint` for basic squaring (`x^2`) and square roots (`x^0.5`), introducing unnecessary JNI boundaries and generalized algorithm overhead compared to direct primitive operations.
**Action:** When working in high-frequency engine calculations, aggressively unroll `Math.pow(x, 2)` to `x * x` and `Math.pow(x, 0.5)` to `Math.sqrt(x)` to minimize CPU time spent on boundary traversals and float-to-double implicit casts.
