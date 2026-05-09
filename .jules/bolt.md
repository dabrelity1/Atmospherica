## 2024-10-24 - Coordinate Mapping Traps in Vector Optimization
**Learning:** The codebase constructs `Vec3(x, z, time)` in weather calculations, mapping the Z coordinate to the vector's Y component and Time to Z. When unpacking `Vec3` to primitives for optimization, `pos.y` does not always correspond to vertical position.
**Action:** Always trace `Vec3` constructor arguments `(x, y, z)` to their semantic meaning before replacing with primitives, especially when `Vec3` is used as a generic data container.

## 2024-10-25 - Replace HashMap with TreeMap for Ordered Map Data Lookups
**Learning:** In the `Sounding` and `Sounding.Parcel` data structures, iterating over `HashMap` entries using `.stream().sorted(Map.Entry.comparingByKey()).toList()` creates high object allocation and sorting overhead in paths executed frequently.
**Action:** Replace `HashMap` with `TreeMap` for maps that are inherently accessed in an ordered manner, using `TreeMap.entrySet()` to achieve `O(N)` traversal overhead instead of `O(N log N)` allocation-heavy iteration. Always declare data structure interfaces explicitly, using `Map<K, V> = new TreeMap<>()` over `TreeMap<K, V>`.
