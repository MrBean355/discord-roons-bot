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

## Local Development

### Prerequisites
- **Java 25**: The project uses modern JVM features.
- **PostgreSQL**: A local database instance is required.

### Database Setup
1. Create a database named `roons_bot` in PostgreSQL.
2. Run the [schema.sql](schema.sql) script against your database to create the necessary tables.

### Environment Variables
Configure the following environment variables (e.g., in an `.env` file or your IDE's run configuration):

| Variable | Description |
| :--- | :--- |
| `JDBC_DATABASE_URL` | e.g. `jdbc:postgresql://localhost:5432/roons_bot` |
| `JDBC_DATABASE_USERNAME` | Database username |
| `JDBC_DATABASE_PASSWORD` | Database password |
| `DISCORD_BOT_TOKEN` | Token for your Discord bot application |

### Running the Application
Run the following command to start the server with a stubbed Telegram client (logs messages to the console instead of sending them):
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### Analytics Dashboard
Once running, you can access the analytics dashboard at:
`http://localhost:8090/dashboard.html`
