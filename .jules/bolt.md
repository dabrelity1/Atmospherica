## 2024-05-23 - Exception-based Cloning in Render Loops
**Learning:** Legacy code in `EntityRotFX` used `clone()` wrapped in `try-catch` inside the particle render loop. This is extremely expensive (exceptions for flow control + allocation). Replacing it with a copy constructor (`new Quaternionf(other)`) and unrolling allocation-heavy loops (removing `new Vector3f[]`) yields significant gains.
**Action:** Always check render methods for `try-catch` blocks and array allocations. Use copy constructors for JOML objects.
