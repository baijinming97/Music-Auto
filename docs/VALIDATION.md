# Validation and testing

## Release scope

Music Auto 1.4.8 is based on upstream AAMediaMate 1.4.5. The first public release shares the previously tested debug APK, package `com.gululu.aamediamate.dev`, versionCode `22`. Public preparation adds documentation and modification notices without changing app behavior.

The APK is debuggable and uses an Android Debug signing certificate. It is a prerelease for community testing. Signing keys and device configuration are not in Git.

## Recorded checks

- 139 local unit tests passed with no failures or errors for 1.4.8.
- Debug assembly and Android lint completed; lint reported zero errors.
- Spotify warm-session resume and cold-start fallback were exercised on a phone in earlier integration tests.
- A user confirmed two-row long lyrics were readable without ellipsis on the tested display.
- Session retention across AA browser disconnect/reconnect was observed in diagnostics.
- Testing used one Android 16 phone and a wireless AA car setup; it is not a compatibility matrix.

## Automatic player selection

Several earlier reconnects resumed music but displayed native Spotify while the bridge published lyric metadata. Session retention and background settings did not by themselves guarantee AA selected Music Auto.

User feedback received 2026-11-10 reported blue Music Auto from startup, lyrics, and normal audio. The second automated reconnect probe did not run because AA and then ADB disconnected; no synchronized startup trace exists for that successful report. Consistent automatic UI selection remains unverified.

The app does not modify Spotify's focus permission, Bluetooth profiles, or AA settings. Temporary device diagnostics are not shipped as app behavior.

## Zero-PCM experiment

`CarAudioOwnershipProbeTest` is disabled unless `audioOwnershipProbe=true` is explicitly supplied. It requires projected AA and already-playing Spotify, then observes ten seconds without PCM, at most sixty seconds of zero PCM, and ten seconds after stopping. It releases its AudioTrack and bridge ownership on exit and requests no audio focus.

In the completed experiment, the media-button target changed from Spotify to Music Auto during PCM and reverted afterward. Spotify retained audio focus. AA already showed Music Auto before the experiment, so this does **not** establish AA UI takeover. There is no production implementation of this approach.

## Automated checks

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug --max-workers=1
```

Use JDK 17 and SDK Platform 36. Unit tests need no Spotify account or car. CI does not run device instrumentation or publish APKs.

## Optional device tests

Tests can start playback or restart the bridge. Use a parked setup with Spotify signed in, notification access granted, and matching app/test APKs installed.

```sh
./gradlew assembleDebugAndroidTest --max-workers=1
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

adb shell am instrument -w -e class com.gululu.aamediamate.SpotifyDeviceTest -e spotifyIntegration true com.gululu.aamediamate.dev.test/androidx.test.runner.AndroidJUnitRunner

# Separate experiment: AA connected and Spotify already playing.
adb shell am instrument -w -e class com.gululu.aamediamate.CarAudioOwnershipProbeTest -e audioOwnershipProbe true com.gululu.aamediamate.dev.test/androidx.test.runner.AndroidJUnitRunner
```

An instrumentation pass is not a car compatibility result. Observe audio, badge, and lyrics; distinguish the system media-button target from AA's player UI. Test pause/resume, reconnect, navigation, and calls before relying on a changed playback design.

## Reporting results

Include app/Android/AA/Spotify versions, whether audio played before connection, display mode, and badge. Share minimal redacted diagnostics, excluding accounts, API credentials, backups, device identifiers, and unrelated logs.
