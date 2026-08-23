# Cubic Cadence（方律）

> A Minecraft Fabric client mod that signs into and plays NetEase Cloud Music in-game.

English | [简体中文](README.md)

[![Minecraft](https://img.shields.io/badge/Minecraft-26.2-62B47A?logo=minecraft)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.19.3-7F6DF2)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-CC0--1.0-blue.svg)](LICENSE)

Cubic Cadence is a Fabric client mod for Minecraft 26.2. Without leaving the game, you can sign in to NetEase Cloud Music with a QR code, browse your playlists, and play tracks online or from local MP3 files. Audio is routed through OpenAL and plays alongside vanilla game sound.

## Important Notice

This mod communicates with a third-party backend service, [api-enhanced](https://github.com/NeteaseCloudMusicApiEnhanced/api-enhanced), to fetch NetEase Cloud Music data.

- **For players**: the author has already deployed the `api-enhanced` backend on a server (`https://api.cubiccadence.top/`), and that address is the mod's built-in default. Install the mod and it works out of the box, with no service setup required.
- **For developers**: to develop, debug, or deploy on your own, you must deploy the `api-enhanced` backend yourself and override `apiEnhancedBaseUrl` in `config/cubic-cadence.json` with your service address.

Third-party library: <https://github.com/NeteaseCloudMusicApiEnhanced/api-enhanced>

## Features

- **QR-code sign-in**: generate a login QR code in-game and authorize with the NetEase Cloud Music app. The session credential is stored locally, encrypted with Windows DPAPI.
- **Music library & playlists**: browse "Created playlists", "Collected playlists", and "Liked songs" with pagination, on-demand loading, and cache-first sync.
- **Availability hints**: distinguish playable, copyright-restricted, membership-required, region-restricted, and quality-unavailable states.
- **Online streaming**: progressive playback with a 1 MiB PCM queue cap, without caching the whole song; tolerates short stream interruptions and guards against premature auto-skip.
- **Up-next preload**: while a track plays, pre-open the next audio stream and prefetch its lyrics in the background; natural or manual skip reuses the prepared result to reduce switch gaps.
- **Local MP3 playback**: bundled JavaSound MP3 decoder, decoding to PCM with no native dependency.
- **Playback controls**: play, pause, stop, previous, next, plus progress and volume adjustment.
- **Playback modes**: sequential, repeat-one, repeat-all, and shuffle.
- **Synchronized lyric HUD**: compact mode shows the current and next lines; lyrics mode hides cover art, metadata, progress, background, and border to present vertical bilingual context around the active line.
- **Custom lyric fonts**: choose the default or uniform font, regular or bold weight, or select a local TTF in-game. Custom fonts load through an isolated local resource pack with Unicode fallback for missing glyphs.
- **Configurable HUD**: disable the HUD or adjust its content, size, color, nine-point anchor, and offsets; choices persist across restarts.
- **Audio quality**: standard, higher, and highest; lossless streaming is not yet supported.
- **Independent volume**: adjust the mod volume and vanilla music volume separately, and optionally disable vanilla background music.
- **Cover art & cache**: remote cover decoding and local caching.

## Tech Stack

| Item | Version |
| --- | --- |
| Minecraft | 26.2 |
| Fabric Loader | 0.19.3 |
| Fabric API | 0.158.0+26.2 |
| Fabric Loom | 1.17-SNAPSHOT |
| Java | JDK 25 |
| Gradle | 9.7.0 |
| Mod ID | `cubic-cadence` |

## Getting Started

### Requirements

- JDK 25
- Gradle (the repository ships its own Gradle Wrapper)

### Build

```powershell
.\gradlew.bat build --no-daemon
```

The built artifact is placed in `build/libs/`.

### Run

```powershell
.\gradlew.bat runClient --no-daemon
```

For IDE setup instructions, see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) for your IDE.

## NetEase Data Source

NetEase Cloud Music does not offer its multi-user Open API to individual developers (individual accounts are limited to `ncm-cli`). The project therefore uses the third-party reverse-engineered library `NeteaseCloudMusicApiEnhanced/api-enhanced` as its data source.

That library relies on undocumented NetEase endpoints and is not officially authorized. Using it for a public release carries account, terms-of-service and legal compliance risk, and the endpoints may break without notice. This is a deliberate trade-off given the individual-developer constraint, not an official NetEase integration.

## Configuration

The mod talks to an `api-enhanced` service directly. Configure its origin in `config/cubic-cadence.json`:

- **For players**: the mod ships with the author-deployed server address built in, so it works out of the box with no configuration.

```json
{
  "apiEnhancedBaseUrl": "https://api.cubiccadence.top/"
}
```

- **For developers**: to connect to a self-hosted `api-enhanced` service, override it with your own address, for example:

```json
{
  "apiEnhancedBaseUrl": "http://localhost:3000"
}
```

The NetEase cookie returned after QR login is stored encrypted with Windows DPAPI in `config/cubic-cadence/auth-session.dpapi`.

## Project Structure

```text
src/main/java/com/cubiccadence/      # data models, provider interfaces and cross-layer contracts
src/client/java/com/cubiccadence/    # client implementation (auth, playback, UI, cache, sync)
src/client/resources/                # client resources (language files, mixin config)
docs/                                # design and development documentation
```

## License

This project is released under the [CC0-1.0](LICENSE) license. Feel free to learn from it and incorporate it into your own projects.
