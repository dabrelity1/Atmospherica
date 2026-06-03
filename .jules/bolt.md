## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-05-18 - Bit-packed Sorting Array for Render Loops
**Learning:** In Minecraft mixin environments, methods like `Particle.getPos()` often implicitly instantiate new `Vec3` objects on every call. Using them inside standard Java object comparators (`List.sort((a,b) -> ...)`) inside the high-frequency rendering loop scales allocations to $O(N \log N)$ per frame, causing massive GC spikes.
**Action:** Replace `List.sort(Comparator)` with a pre-allocated primitive array sort. Pack the required comparison data (e.g., distance) into the upper 32 bits of a `long` using `Float.floatToRawIntBits`, and the object's index into the lower 32 bits. Call `Arrays.sort()` and iterate backward (for painter's algorithm rendering), recovering the object index via bitmasking. This eliminates all allocation overhead during high-frequency sorting.
