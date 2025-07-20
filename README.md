# AdmiralBulldog Sounds - Backend

Welcome! Please visit the [app's page](https://github.com/MrBean355/admiralbulldog-sounds/wiki) to get started.

---

[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=discord-roons-bot&metric=ncloc)](https://sonarcloud.io/dashboard?id=discord-roons-bot)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=discord-roons-bot&metric=coverage)](https://sonarcloud.io/dashboard?id=discord-roons-bot)

## Overview

This application serves as the backend for the
[AdmiralBulldog desktop application](https://github.com/MrBean355/admiralbulldog-sounds). It is composed of the
following core components:

- **Sound Bite Management** – Stores and organises sound bites for use across the application.
- **REST API** – Provides an interface for the desktop application.
- **Discord Bot** – Integrates with Discord to play sound bites in voice channels.

### Sound Bite Management

The backend includes a fixed set of sound bites sourced from the official Play Sounds page. To update this collection
with the latest sound bites, run the `./gradlew updateSoundBites` command. See the [buildSrc](buildSrc) project for
more details.

### REST API

The REST API exposes endpoints used by the desktop application, including:

- Downloading sound bites
- Interacting with the Discord bot

For a complete overview, see the classes in the [controller](src/main/kotlin/com/github/mrbean355/roons/controller)
package.

### Discord Bot

This component manages the Discord bot using the official Discord API. It handles user-issued slash commands (e.g.
/join), connects to voice channels, and plays sound bites.

Refer to the [DiscordBot](src/main/kotlin/com/github/mrbean355/roons/discord/DiscordBot.kt) class for implementation
details.
