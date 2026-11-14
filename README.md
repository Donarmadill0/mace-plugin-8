# Plutus Mace Plugin V3 (Paper 1.21.11)

Advanced blood-themed mace plugin with zero-config resource pack delivery.

## Features
- **Auto-Hosted Resource Pack**: Automatically uploads assets to Catbox.moe on startup. No IP config needed.
- **Blood Toll (Passive)**: Attacker takes 1.0 damage per hit.
- **Blood Dash (F)**: Push forward and damage nearby players.
- **Blood Pact (Shift + F)**: Link up to 3 players; damage dealt to one is shared by all.
- **Custom Texture**: Red spiked mace model included.

## Installation
1. Take the `build/libs/plutus-mace-v3-3.0.0.jar` after building.
2. Place it in your server's `plugins/` folder.
3. Restart your server.

## Building
Use Gradle with Java 21:
```bash
./gradlew build
```
Or push to GitHub to use the included GitHub Actions workflow.
