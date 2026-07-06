## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-24 - Efficient Particle Sorting via Bit-Packing
**Learning:** `ParticleManager.render` was creating significant overhead per frame by repeatedly sorting `Particle` objects using a `Comparator`. Since distance calculations (`getPos()`, `distanceToSqr`) allocated new `Vec3` instances on every call, the $O(N \log N)$ sorting phase became a massive memory allocation bottleneck.
**Action:** By extracting the primitive properties directly (via a Mixin over `Particle`), and packing the `distSq` as a `float` into the upper 32 bits of a `long[]` with the index in the lower 32 bits, we can sort primitive values with `Arrays.sort`. Iterating backward yields the identical back-to-front rendering order with near-zero allocation and much faster execution.
