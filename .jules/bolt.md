## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.
## 2024-05-23 - WindEngine Vector Optimization
**Learning:** Minecraft's `Vec3` class is immutable and lacks a mutable counterpart in the standard library (unlike JOML's `Vector3f`). Chaining operations like `new Vec3(...).normalize().multiply(...)` creates multiple short-lived objects. In high-frequency paths (like `WindEngine.getWind` called per-particle), this causes significant GC pressure.
**Action:** Replace  chains with primitive  arithmetic. Construct the final  only once at the end. Use  to generate unit vectors directly to avoid . Use  instead of  to avoid allocating the argument.
## 2024-05-23 - WindEngine Vector Optimization
**Learning:** Minecraft's `Vec3` class is immutable and lacks a mutable counterpart in the standard library (unlike JOML's `Vector3f`). Chaining operations like `new Vec3(...).normalize().multiply(...)` creates multiple short-lived objects. In high-frequency paths (like `WindEngine.getWind` called per-particle), this causes significant GC pressure.
**Action:** Replace `Vec3` chains with primitive `double` arithmetic. Construct the final `Vec3` only once at the end. Use `Math.cos/sin` to generate unit vectors directly to avoid `normalize()`. Use `vec.add(x, y, z)` instead of `vec.add(new Vec3(x, y, z))` to avoid allocating the argument.
