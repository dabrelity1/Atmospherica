## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-11-20 - 2D Distance Calculation Overhead
**Learning:** Using `Vec3.multiply(1.0, 0.0, 1.0).distanceTo(...)` to perform 2D calculations implicitly creates multiple temporary `Vec3` objects. This introduces massive garbage collection overhead when used in high-frequency loops like `RadarRenderer` or weather checking logic.
**Action:** Always use a primitive-based 2D distance calculation like `Math.sqrt(dx*dx + dz*dz)` via a utility method (`Util.distance2D`) to perform 2D distances without memory allocations.
