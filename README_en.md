# Music Auto

[中文](README.md) · [Download 1.4.8 prerelease](https://github.com/baijinming97/Music-Auto/releases/tag/v1.4.8) · [Upstream AAMediaMate](https://github.com/gululu1235/AAMediaMate)

Music Auto is a community fork of **gululu1235/AAMediaMate**, maintained by baijinming97. It improves Spotify bridging, resuming playback after an Android Auto connection, and compact two-row lyrics. Spotify plays the audio; Music Auto supplies the car-facing media session and optional lyrics.

**1.4.8 is a prerelease.** Playback resumption and two-row lyrics have been tested in a car. Automatic selection of Music Auto remains device-dependent: earlier reconnects selected native Spotify, while the latest user report showed blue Music Auto, lyrics, and normal audio from startup. Consistency across reconnects and other devices is not established.

This fork retains the upstream Apache-2.0 license and attribution. It is not an official upstream, Spotify, or Google release.

## Changes

- Default-on Spotify source lock overrides the general native-AA-app filter while respecting per-app control opt-outs. Disable it to bridge other players.
- Metadata and controls follow the same source session; bridge installations cannot become sources themselves.
- A playable Spotify entry remains available without an active session. Startup can be requested through its exported media-button receiver.
- After AA connects and the bridge is ready, playback resumes once by default. Already-playing sources are skipped; manual transport requests cancel pending resumption. The setting can be disabled.
- The notification listener and browser service share session ownership across browser teardown.
- Long lyrics use both text rows; longer lines pause before timed static page changes. The option can be disabled.
- Notification access is checked for the exact listener, and the phone UI displays the installed version.

See [CHANGELOG](CHANGELOG.md) and [validation scope](docs/VALIDATION.md).

## Installation

1. Download `Music-Auto-1.4.8.apk` from this fork's [Releases](https://github.com/baijinming97/Music-Auto/releases). Android 10+ and an AA-compatible head unit are required.
2. Keep Spotify installed, sign in, and verify playback. Music Auto supplies no account, music files, or subscription.
3. Open Music Auto and grant notification access to this installation. Source lock and connection auto-resume are enabled by default.
4. If the app is missing in AA, tap the AA version entry ten times to enable developer settings, enable **Unknown sources**, and reconnect.
5. Select Music Auto on the head unit. Read and accept its lyrics notice if enabling lyrics and configure a provider. View lyrics only while parked or as a passenger.
6. If the phone restricts background operation, allow unrestricted background use and autostart for Music Auto where available.

The APK is the same file used in the local car tests: a **debuggable debug build**, package `com.gululu.aamediamate.dev`, versionName `1.4.8`, versionCode `22`, signed with a separate Android Debug certificate. It can coexist with upstream AAMediaMate. It includes no personal lyrics library, backups, accounts, API keys, or device logs.

Updates with the same signing certificate can preserve data. A build with the same package name but a different certificate cannot be installed over it: export a backup before considering removal. Locally compiled debug APKs may also use a different signing key.

## Lyrics and backup

Upstream LRCLib, Spotify via RapidAPI, and LrcApi support is retained. LRCLib needs no API key; supply your own credentials or endpoint for other providers when required. Availability depends on the song and provider.

The compact layout estimates about 12 CJK characters per row and was tested on one 800-pixel-wide display. Other displays, fonts, or languages may truncate text. The current line's continuation takes priority over next-line preview or song details. The two-row option can be disabled in display settings.

Use in-app export/import to migrate existing lyrics and settings. Do not attach credential-bearing configuration, full backups, or unreviewed device logs to public reports.

## Limitations

- **Source lock does not pin the AA-selected player.** This build neither takes permanent audio focus nor blocks Spotify's focus requests. More repeated starts and devices are needed to validate automatic UI selection.
- AA owns the Maps media strip and may collapse text into transport buttons. Persistent lyrics there are not guaranteed.
- Bluetooth auto-resume, AA autoplay, and OEM battery restrictions can affect startup order. Disabling AA autoplay alone did not guarantee MA selection. Spotify's similar-content autoplay is separate from connection resumption.
- A zero-PCM experiment exists only in explicitly opted-in instrumentation. **The published app does not run it.** It changed the system media-button target once but did not establish AA UI takeover.

## Build

Use JDK 17, Android SDK Platform 36, and the checked-in Gradle Wrapper. Configure `ANDROID_HOME` or the SDK path in local `local.properties`, then run:

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug --max-workers=1
```

On Windows use `gradlew.bat`. Output is `app/build/outputs/apk/debug/app-debug.apk`. Private signing keys are excluded. CI builds and checks source; it does not automatically publish update-compatible signed APKs.

This prerelease uses the `debug` variant. The inherited `release` variant still uses the upstream base application ID and is not a direct upgrade path for this APK. Device tests are opt-in and must be run while parked; see [testing](docs/VALIDATION.md).

## Attribution and contributions

Report issues in [this fork](https://github.com/baijinming97/Music-Auto/issues), including relevant versions, display mode, and expected/actual behavior after removing sensitive information.

Thanks to [gululu1235/AAMediaMate](https://github.com/gululu1235/AAMediaMate) and its contributors for the bridge, lyrics, backup, and settings implementation. Upstream history is preserved in Git. This fork was implemented and tested with ChatGPT/Codex assistance.

Distributed under [Apache License 2.0](LICENSE). See [NOTICE](NOTICE) for attribution and modifications.
