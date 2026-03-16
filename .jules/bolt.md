## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-25 - High-Frequency Object Allocation in Mixins/Renderers
**Learning:** High-frequency rendering loops (like `EntityRotFX.render`, `ParticleCube.render`, and `ParticleTexExtraRender.render`) were redundantly allocating `new Vector3f[]` arrays on every frame, generating massive garbage collection (GC) pressure.
**Action:** For rendering geometry loops, replace `new Vector3f[]` array allocations with pre-defined static `float[]` arrays for base vertices, and use reused `Vector3f` instances and primitive math local variables to execute position translations/rotations without triggering object allocation.
