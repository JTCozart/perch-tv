# Perch

A camera wall for Google TV and Android TV. Perch shows up to 16 security cameras at once, opens any of them full screen
with sound, and reconnects dropped cameras by itself.

- **Wired RTSP cameras** play directly: mains-powered Tapo cameras through their camera account, or any other RTSP camera.
- **Tapo battery and solar cameras** that offer no RTSP (TP-Link lists the C410, C420, C425 and D230) play over TP-Link's
  own local protocol, signed in with the Tapo app password. Experimental: undocumented protocol, no sound yet.
- **Bridge streams**: any RTSP or HLS address another box re-publishes (go2rtc, Home Assistant, an NVR).

Website and install guide: https://jtcozart.github.io/perch-tv/

Perch is an independent app and is not made by, endorsed by or affiliated with TP-Link. Tapo is a trademark of TP-Link.

## Building

Needs JDK 21 and the Android SDK (compile SDK 37).

```bash
./gradlew test            # unit tests (kotest)
./gradlew assembleDebug   # app/build/outputs/apk/debug
```

## Layout

| Path | What it holds |
| --- | --- |
| `app/src/main/java/com/jtcozart/perch/camera` | Camera model, stream URL rules, form validation. Plain Kotlin, unit tested. |
| `app/src/main/java/com/jtcozart/perch/tapo` | Tapo local stream protocol: digest sign-in, key exchange, decryption of the MPEG-TS parts. Unit tested against a fake camera. |
| `app/src/main/java/com/jtcozart/perch/wall` | Grid planning and the device decoder budget. |
| `app/src/main/java/com/jtcozart/perch/storage` | Camera list, encrypted with an Android Keystore key. |
| `app/src/main/java/com/jtcozart/perch/ui` | Compose screens: wall, full screen, camera list, add and edit. |
| `docs/` | The GitHub Pages site. |

## Continuous integration

- `.github/workflows/tests.yaml` runs the unit tests and lint on every push to `master` and every pull request.
- `.github/workflows/build.yaml` builds a debug APK for the same triggers and attaches it to the run as the
  `perch-tv-debug` artifact for 14 days.

## Releases

Pushing a tag such as `v0.1.0` (or `v0.1.0-rc.1` for a pre-release) runs `.github/workflows/release.yaml`: it runs the
tests, builds a signed APK and publishes `perch-tv.apk` plus `SHA256SUMS` as a GitHub release. The website links to
`releases/latest/download/perch-tv.apk`. The workflow needs the `PERCH_KEYSTORE` (base64), `PERCH_KEYSTORE_PASSWORD`,
`PERCH_KEY_ALIAS` and `PERCH_KEY_PASSWORD` secrets.

## License

MIT, see `LICENSE`. Third-party notices are in `NOTICES.md`.
