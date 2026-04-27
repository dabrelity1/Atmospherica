## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.
## 2024-10-24 - Coordinate Primitive Breakdowns Can Regress
**Learning:** Adding a primitive coordinate overload (e.g., `double posX, double posY, double posZ`) to avoid `Vec3` allocations is a common optimization, but if the caller *already* has an allocated object, unpacking it into primitives only to force the internal method to re-allocate a `new Vec3(posX, posY, posZ)` introduces a performance regression by increasing object allocations in a hot loop.
**Action:** When adding primitive overloads, verify the caller's allocation state. Only rewrite to primitives if the math inside the target method can also be fully converted to primitives without creating temporary objects.
