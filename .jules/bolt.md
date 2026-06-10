## 2024-11-20 - [Math.pow to lengthSqr in Hot Shader Paths]
**Learning:** `Math.pow(vec.length(), 2.0)` is a common anti-pattern in game modding. It forces a `Math.sqrt` inside `length()`, and then undoes it with `Math.pow`. Using `vec.lengthSqr()` entirely bypasses both operations and is functionally equivalent for ratio calculations like `Math.pow(vec.length() / 60.0, 2.0)` -> `vec.lengthSqr() / 3600.0`.
**Action:** Always check `Math.pow` calls involving `.length()`, `.distanceTo()`, or similar vector magnitude functions. Replace them with `.lengthSqr()` and manually squared constants where possible.

## 2024-11-20 - [Unrolling integer exponents vs Math.pow]
**Learning:** Fractional constants like `2.5` can also be unrolled efficiently as `x * x * Math.sqrt(x)`. Hardware-accelerated `Math.sqrt` combined with inline multiplication is significantly faster than generalized CPU `.pow()` functions for fixed known fractional exponents like 0.5, 1.5, 2.5.
**Action:** Unroll integer exponents `x^3 -> x*x*x`, and unroll `0.5` fractionals to `Math.sqrt(x)`. Avoid doing this for arbitrary fractionals (e.g. `0.75`, `0.85`), as approximations introduce logic regressions.
