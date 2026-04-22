## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-11-20 - High-Frequency Shader Distance Optimizations
**Learning:** In `ModShaders.renderShaders` (a high-frequency hot path called every frame), checking distance between objects using `Vec3` methods like `storm.position.multiply(1.0, 0.0, 1.0).distanceTo(camera.getPosition().multiply(1.0, 0.0, 1.0))` is extremely inefficient. It performs multiple `Vec3` allocations per storm via `multiply()` and uses `Math.sqrt()` inside `distanceTo()`.
**Action:** When calculating 2D distance for thresholds in render paths, hoist variables (e.g. `camera.getPosition()`) and manually compute squared XZ distance inline `(dx * dx + dz * dz > limitSq)` to eliminate memory allocations and square root calculations completely.
