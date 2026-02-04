## 2024-05-31 - Wind Damage Logic Optimization
**Learning:** The wind simulation logic runs expensive per-player checks (260 iterations with raycasts) for wind damage even when wind strength is insufficient (no storms). Base noise wind never exceeds ~36.0F, while damage requires >45.0F.
**Action:** Always check high-level conditions (like active storms list) before entering expensive simulation loops.
