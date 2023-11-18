# AdmiralBulldog Sounds - Backend

Welcome! Please visit the [app's page](https://github.com/MrBean355/admiralbulldog-sounds/wiki/Discord-Bot) to get
started with the Discord bot.

[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=discord-roons-bot&metric=ncloc)](https://sonarcloud.io/dashboard?id=discord-roons-bot)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=discord-roons-bot&metric=coverage)](https://sonarcloud.io/dashboard?id=discord-roons-bot)

---

## About

This application is designed to be the backend for the
[AdmiralBulldog desktop application](https://github.com/MrBean355/admiralbulldog-sounds). It is composed of a few
different components:

- **Sound bites** - downloading sound bites to use in the other components.
- **REST API** - servicing the desktop app.
- **Discord bot** - playing sound bites in voice channels.

### Sound Bites

The app will check the official [Play Sounds page](https://chatbot.admiralbulldog.live/playsounds) once per day, to see
what the latest sound bites are. It will then download each of them to disk, and make them available to download via the
REST API. See the [`SoundStore`](src/main/kotlin/com/github/mrbean355/roons/discord/SoundStore.kt) class for a starting
point.

After being downloaded, the files will be converted to MP3 format (using FFMPEG), to ensure consistent format
across all files. Without this step, some sounds were unable to be played by the desktop app. The sound's volume will
also be adjusted according to how it was configured on the Play Sounds page. See the
[`SoundBiteConverter`](src/main/kotlin/com/github/mrbean355/roons/component/SoundBiteConverter.kt) class for more
details.

### REST API

This is a set of APIs that is consumed by the desktop app, ranging from downloading sound bites, to interacting with the
Discord bot. Check out the [`controller`](src/main/kotlin/com/github/mrbean355/roons/controller) package to browse the
full list.

### Discord Bot

This component is responsible for starting up and controlling the Discord bot, through the official Discord APIs. It
will parse slash commands from users (e.g. `/join`), join voice channels and play sound bites. Check out the
[`DiscordBot`](src/main/kotlin/com/github/mrbean355/roons/discord/DiscordBot.kt) class for more details.

---

*Powered by a [JetBrains Open Source license](https://www.jetbrains.com/opensource/) ❤️*

[![Jetbrains](jetbrains-logo.png)](https://www.jetbrains.com/?from=AdmiralBulldogDota2app)