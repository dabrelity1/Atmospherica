# Atmospherica

A realistic weather simulation mod for Minecraft Forge 1.20.1 featuring volumetric clouds, severe weather events, and atmospheric physics.

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green)
![Forge](https://img.shields.io/badge/Forge-47.0+-orange)
![License](https://img.shields.io/badge/License-All%20Rights%20Reserved-red)

## Features

### Severe Weather
- **Tornadoes** - Fully simulated with debris, damage, and realistic vortex physics
- **Thunderstorms** - Dynamic lightning with custom rendering
- **Supercells** - Rotating thunderstorms with mesocyclones

### Volumetric Clouds
- Raymarched volumetric cloud rendering
- Dynamic cloud formation based on atmospheric conditions
- Multiple cloud layers with realistic lighting

### Thermodynamic Engine
- Real atmospheric physics simulation
- Temperature, humidity, and pressure calculations
- CAPE (Convective Available Potential Energy) modeling
- Lifted Condensation Level (LCL) calculations

### Precipitation
- Custom particle system for rain, snow, and sleet
- Ground accumulation (ice layers, sleet)
- Dynamic precipitation based on atmospheric conditions

### 📡 Weather Equipment
- **Radar** - Track storms and precipitation
- **METAR Station** - Monitor local weather conditions
- **Tornado Siren** - Warning system for severe weather
- **Tornado Sensor** - Redstone output when tornadoes are nearby
- **Weather Balloon** - Launch to gather atmospheric data
- **Sounding Viewer** - Display atmospheric profiles
- **WSR-88D Radar** - Professional weather radar (multiblock)

## Compatibility

Atmospherica is compatible with:
- **Distant Horizons** - Extended render distance support
- **Oculus** (Iris for Forge) - Shader pack compatibility
- **Embeddium/Rubidium** (Sodium for Forge) - Performance mod compatibility
- **Serene Seasons** - Seasonal weather integration

## Installation

1. Install [Minecraft Forge](https://files.minecraftforge.net/) for 1.20.1
2. Download the latest Atmospherica release
3. Place the `.jar` file in your `mods` folder
4. Launch Minecraft

## Configuration

Configuration files are located in:
- `config/atmospherica-client.toml` - Client-side settings (graphics, particles)
- `config/atmospherica-server.toml` - Server-side settings (weather behavior)

### Client Options
- Volumetric cloud quality (Low/Medium/High/PC Killer)
- Particle density
- Radar resolution
- Sound volumes

### Server Options
- Storm spawn rates
- Tornado damage
- Valid dimensions
- Weather intensity

## Commands

- `/atmospherica storm spawn <x> <z>` - Spawn a storm at coordinates
- `/atmospherica storm clear` - Clear all storms
- `/atmospherica debug` - Toggle debug information

## Building from Source

```bash
git clone https://github.com/dabrelity1/Atmospherica.git
cd Atmospherica
./gradlew build
```

The built jar will be in `build/libs/`

## Credits

- **Author:** dabrelity1
- **Original Concept:** ProtoManly

## License

All Rights Reserved - See [LICENSE](LICENSE) for details.

---

*Atmospherica - Experience weather like never before in Minecraft*
