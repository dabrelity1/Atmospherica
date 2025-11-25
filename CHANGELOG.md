# Changelog

All notable changes to Atmospherica will be documented in this file.

## [0.15.3-1.20.1] - 2025-11-25

### Added
- Initial release as Atmospherica (rebranded from PMWeather)
- Oculus (Iris) shader pack compatibility
- Embeddium/Rubidium compatibility
- Distant Horizons integration
- Config option to force volumetric clouds with shader packs

### Changed
- Package renamed to `dev.dabrelity.atmospherica`
- Mod ID changed to `atmospherica`
- Display name changed to "Atmospherica"

### Performance Improvements
- Optimized particle sorting with cached collections
- Reduced ambient sound block checks from 32,768 to ~512
- Replaced boxed collections with primitive fastutil collections
- Eliminated lambda allocations in shader uniform setting
- Optimized stream operations in atmospheric calculations

### Fixed
- Various performance optimizations to reduce GC pressure
