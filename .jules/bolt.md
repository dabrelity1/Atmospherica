## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-24 - Overcoming O(N log N) Sorting Overheads in Map Iteration
**Learning:** Utilizing `.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()` on `HashMap`s inside high-frequency loops (like checking atmospheric data bounds) introduces severe performance overhead due to stream instantiation and repeated array sorting.
**Action:** When a `Map` is frequently iterated in sorted order by key, use a `TreeMap` instead of a `HashMap` to maintain natural order incrementally, eliminating the need to re-sort it on every query.
