## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-24 - Math.pow Overhead in JNI Simulation
**Learning:** `Math.pow(x, 2.0)` and `Math.pow(x, 0.5)` are used heavily in simulation physics (`WindEngine`, `ThermodynamicEngine`, `GameBusEvents`). Java delegates these calls natively, incurring massive JNI overhead compared to basic operations (`x * x` or `Math.sqrt`). A microbenchmark demonstrated `Math.pow(..., 2)` was ~100x slower than `x * x` in these loops.
**Action:** Unroll power functions into `x * x` and `Math.sqrt(x)` when dealing with integer and half-step constants, especially when executed inside intensive loops iterating over entities, blocks, or weather parameters.
