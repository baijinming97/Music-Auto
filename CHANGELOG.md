# Music Auto changes

Community fork of [AAMediaMate](https://github.com/gululu1235/AAMediaMate), based on upstream `v1.4.5` / `288cca9b54891a250b3d1a7f8717ff8b73b22da6`.

## 1.4.8 — first public prerelease, 2026-11-10

- Retain the same bridge session while either the notification listener or car browser owns it. Releasing one service no longer destroys the other's session.
- Render lyrics while AA is projected even if the browser has unbound; stop listener-only lyric work when disconnected.
- Include the earlier Spotify source, startup, auto-resume, and two-row lyric changes below.
- Publish the previously car-tested debug APK, source, checksums, attribution, and validation notes.

Automatic AA UI selection remains a compatibility limitation. Earlier reconnects chose native Spotify; the latest user report described Music Auto from startup with lyrics and normal audio. Universal or permanent player selection is not claimed.

## 1.4.7 — local test build

- Split long lyrics between the title/subtitle rows, prioritizing the current line's continuation.
- Page extra-long lines within their lyric interval after an initial hold, preserving seek, speed, and offset behavior.
- Add a default-on two-row setting included in backup/restore.

## 1.4.6 — local test build

- Use the Music Auto name and ordinary numeric versions.
- Resume Spotify once per projected connection after the bridge is ready; skip an already-playing source and cancel pending resumption on manual transport requests.

## Initial local changes on upstream 1.4.5

- Add a Spotify source lock that bypasses the native-player filter without overriding per-app opt-outs.
- Exclude bridge installations as sources and align metadata/control session selection.
- Keep a playable Spotify entry when its session is missing and use its exported media-button receiver for startup.
- Check the exact notification-listener component and show the installed version.

The opt-in zero-PCM experiment is instrumentation-only and not normal app behavior. Private diagnostics and backups are not included.
