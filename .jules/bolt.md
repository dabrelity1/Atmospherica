## 2025-10-21 - Opaque Particle Sorting Overhead
**Learning:** Back-to-front sorting (Painter's Algorithm) is crucial for translucent objects but harmful for opaque ones. It wastes CPU cycles ($O(N \log N)$) and causes GPU overdraw by rendering distant objects before close ones (defeating Early-Z culling).
**Action:** Always verify if renderable objects are opaque before applying distance sorting. If opaque, rely on the depth buffer and skip sorting (or sort front-to-back if overdraw is severe).
