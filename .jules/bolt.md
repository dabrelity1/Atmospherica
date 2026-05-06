## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.
## 2024-11-20 - Avoid Math and Object Allocation Overhead in Renderer Tick Loop
**Learning:** In Minecraft rendering loops, calling `Vec3.multiply().distanceTo(Vec3)` continuously instantiates several objects (e.g. `Vector3d`) and calls `Math.sqrt` internally inside hot loops for every tick, per storm.
**Action:** When computing geometric properties strictly for distance comparisons in high-frequency events (like storm render limits inside `ModShaders`), hoist reused properties and calculate squared 2D/3D offsets manually (e.g. `(dx*dx + dz*dz) > squared_dist`) to bypass object GC cycles.
