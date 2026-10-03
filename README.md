# Tadiran Remote

A simple Android remote for **Tadiran** air conditioners (TAC 297 remote), for phones with a
built-in IR blaster.

<p>
  <img src="screenshots/cool.png" width="240" alt="Cool mode">
  <img src="screenshots/heat_dark.png" width="240" alt="Heat mode, dark theme">
  <img src="screenshots/off.png" width="240" alt="Off">
</p>

An AC remote sends its whole state (power, mode, fan speed and temperature) in every
button press. The app does the same. It keeps track of the state you've chosen, and each
change sends the matching code. Several quick +/− taps send only the temperature you end on.

- **Power** sends Off, or the current state when turning on. Long-press it to resend the
  current state, for example after someone used the original remote.
- **Modes:** Cool, Heat, Dry, Fan, Auto. Temperatures 16–30 °C. Fan speeds Low, Mid, High,
  Auto. In Dry mode the fan selector is disabled, because the Dry codes don't depend on fan
  speed.

## Install

Download the APK from the [latest release](../../releases/latest) on the phone and open it.
You'll need to allow installing apps from your browser.

## Codes

The 301 IR codes come from [tadiran-irdb](https://github.com/anadav/tadiran-irdb), which
converts them from [SmartIR](https://github.com/smartHomeHub/SmartIR) (MIT).
`app/src/main/assets/Tadiran_1345_full.ir` is a copy of that repo's Flipper file. Refresh it
with `tools/update-codes.sh`.

## Building

You need JDK 21 and an Android SDK with `platforms;android-37.2`.

```sh
./gradlew testDebugUnitTest assembleDebug          # unit tests + debug APK
adb install -r app/build/outputs/apk/debug/app-debug.apk
./gradlew recordRoborazziDebug                     # re-render screenshots/
```

Pushing a `v*` tag makes CI build a signed release APK and attach it to a GitHub release.
Signing uses the `SIGNING_*` repository secrets. Local release builds read the same
variables from the environment.

## License

MIT. See [LICENSE](LICENSE).
