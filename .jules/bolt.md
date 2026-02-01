## 2024-05-22 - Avoid Vector Objects in Tight Loops (Minecraft)
**Learning:** Minecraft's `Vec3` is immutable, so operations like `multiply` create new objects. In tight loops like noise generation (FBM), this causes massive object churn (millions per second).
**Action:** Refactor tight math loops to use primitive doubles instead of vector objects.
