## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.
## 2024-10-24 - Optimization Pattern: Unrolling Math.pow(x, N) for integer powers
**Learning:** `Math.pow(x, N)` is significantly slower than direct floating-point multiplication (e.g., `x * x` or `x * x * x`) and `Math.pow(x, 0.5)` is slower than `Math.sqrt(x)`. In game ticks, these micro-optimizations compound to provide measurable benefits.
**Action:** When replacing `Math.pow(x, N)`, if `x` is a complex expression (e.g., `Math.abs(bands)` or `Mth.clamp(...)`), extract the expression into a hoisted local variable first, then multiply the local variable. This prevents redundant re-evaluation of the complex logic.
