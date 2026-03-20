## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-25 - Avoid Vector3f allocations in per-particle render loops
**Learning:** In high-frequency rendering methods, such as `EntityRotFX.render`, `ParticleTexExtraRender.render`, and `ParticleCube.render`, creating new array structures and instances of JOML `Vector3f` generates massive garbage collection pressure since these are instantiated for every particle on every frame.
**Action:** Extract vectors used for computing particle quad vertices to static pre-calculated arrays or final instance fields that act as mutable structures. Call `.set()` on existing vectors rather than making `new Vector3f()` to eliminate object allocations entirely during rendering.