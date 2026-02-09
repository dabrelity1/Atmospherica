## 2024-05-31 - Wind Logic Optimization
**Learning:** Logic dependent on environmental conditions (like wind speed) often has implicit bounds. Verifying these bounds (e.g., max Simplex noise output) allows skipping entire logic blocks.
**Action:** When optimizing "if (value > threshold)" checks, determine the theoretical max/min of "value" in common states (e.g. no storms) to add early returns.

**Learning:** `BlockPos.offset(Vec3i)` allocates a new object. `BlockPos.offset(x, y, z)` does not. In tight loops (260 iter * players), this matters.
**Action:** Use primitive overloads for vector math in loops.
