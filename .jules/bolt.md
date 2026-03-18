## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2026-03-18 - Avoiding Temporary Vector Allocations in High-Frequency Loops
**Learning:** In Minecraft, `Vec3` objects are immutable and methods like `multiply` create new object instances. Chaining these methods in a loop that runs every frame (e.g., iterating over storms in `ModShaders.renderShaders`) causes substantial object allocation and garbage collection pressure, leading to micro-stutters. Relying on generic distance functions instead of inline calculations can also result in unneeded method overhead or missed opportunities like using squared distance to avoid `Math.sqrt`.
**Action:** Replace 3D vector math chains (e.g., `position.multiply(1.0, 0.0, 1.0).distanceTo(camera.multiply(1.0, 0.0, 1.0))`) with inline squared distance checks, caching any loop-invariant constants (like `camera.getPosition()`), to avoid GC overhead and expensive operations.
