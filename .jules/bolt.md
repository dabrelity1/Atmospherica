## 2025-10-21 - [Initial Rendering Optimization]
**Learning:** High-frequency particle rendering loops are critical for performance. Allocating arrays or unnecessary objects inside `render()` methods causes measurable GC pressure.
**Action:** Always unroll loops and use local primitives or reused objects in rendering code. Avoid `clone()` on math objects like `Quaternionf` which can be slow and use copy constructors instead.
