## 2026-02-10 - Skip Heavy Logic when Threshold Unreachable
**Learning:** When a heavy calculation (like wind simulation with noise) feeds into a logic block that requires a high threshold (e.g., wind > 45), always check if the maximum possible value of the calculation can even reach that threshold under current conditions (e.g., no storms). If not, skip the entire block.
**Action:** Before running expensive loops or simulations, check global state (like active storms list) to see if the expensive operation can be short-circuited.
