## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-24 - Heightmap Position Allocation Overhead
**Learning:** The method `level.getHeightmapPos(...)` instantiates and returns a new `BlockPos` object. When only the Y-coordinate is needed (`.getY()`), this causes unnecessary garbage collection pressure, especially in frequently called paths like rendering and ticking.
**Action:** Use `level.getHeight(Types.*, x, z)` instead to directly retrieve the primitive Y-coordinate integer without allocating a `BlockPos` object.
