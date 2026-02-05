## 2024-05-31 - Distance Check Optimization
**Learning:** `distanceTo` uses `Math.sqrt` which is expensive in hot loops (like checking all players against all players). `distanceToSqr` avoids this.
**Action:** Always prefer `distanceToSqr` for distance comparisons and square the threshold.

## 2024-05-31 - Gradle Toolchain Issues
**Learning:** ForgeGradle rigidly enforces Java versions (e.g. 17 for 1.20.1). If the environment only has Java 21, the build will fail with toolchain errors even if `java.toolchain.languageVersion` is changed, likely due to internal ForgeGradle checks or missing dependencies that can't be downloaded.
**Action:** When build environment is unstable, use standalone Java files (`Benchmark.java`) compiled with system `javac` to verify algorithmic changes.
