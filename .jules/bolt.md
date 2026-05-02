## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.
## 2026-05-02 - Variable Hoisting and Scope Conflicts in the Render Loop
**Learning:** When hoisting variables to the top of a scope (like `camera.getPosition()`) inside a large method, there is a risk of naming collisions with existing local variables lower in the block (e.g., `camPos`). This can lead to compilation failures even if the math optimization is correct.
**Action:** Always verify variable declarations and potential naming overlaps when performing manual variable hoisting in large methods. Scan the whole scope for the intended variable name before applying.
