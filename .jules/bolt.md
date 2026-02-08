## 2025-10-23 - Initial Bolt Journal

**Learning:** Particle rendering loops are extremely sensitive to object allocation.
**Action:** When working on particle systems, always verify if `Vector3f` arrays or instances are being allocated per-frame. Unroll loops and reuse objects or use local variables to avoid allocation overhead.

**Learning:** `Quaternionf.clone()` is inefficient due to exception handling and object overhead.
**Action:** Prefer `new Quaternionf(other)` copy constructor.
