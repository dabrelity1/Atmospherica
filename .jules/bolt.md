## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-25 - Avoiding Vector Allocations in Render Loops
**Learning:** In high-frequency rendering paths (e.g., `ModShaders.onRenderLevelLast`), chaining `Vec3` operations like `storm.position.multiply().distanceTo()` creates excessive garbage collection overhead per frame. The `distanceTo` method also incurs an expensive `Math.sqrt` cost.
**Action:** Replace 3D vector `distanceTo` checks with manual 2D squared distance logic (`dx*dx + dz*dz < limitSq`) on the XZ plane, leveraging existing local variables like `camPos` to completely eliminate `Vec3` allocations and square root calculations in hot loops.
