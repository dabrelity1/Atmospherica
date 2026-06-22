## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-25 - Redundant Trigonometry in Render Loops
**Learning:** High-frequency rendering pipelines (e.g., `ModShaders`) sometimes re-evaluate expensive trig functions like `Math.cos(sunAngle)` directly in shader uniform setters, even when the exact result was just calculated and stored in a direction vector (like `sunDir.y`).
**Action:** Always cross-reference existing vector components before calculating or unrolling new trigonometric functions in render loops to reuse already-computed values.
