## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.
## 2024-05-23 - Optimize Tornado Proximity Checks
**Learning:** Checking proximity via generic 2D distance methods (`distance2D`) and instantiating positions inside high-frequency entity tick loops causes unnecessary `Vec3` allocations and expensive `Math.sqrt` operations per block, per storm.
**Action:** Always hoist scalar coordinates (`getX() + 0.5D`) and square the range (`rangeSq = range * range`) outside loops. Inside the loop, perform early-exit integer checks first, then calculate manual 2D squared distance (`dx*dx + dz*dz < rangeSq`) to completely eliminate allocations and square root math in proximity checks.
