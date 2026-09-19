# Packaging: Windows and Android builds

This directory holds everything needed to ship `melee-agent` as a native
binary, so users don't need a Python install.

| Target | Config | Output |
|---|---|---|
| Windows (x64) | [`windows/`](windows/) — PyInstaller spec + build script | `melee-agent.exe` (single file) |
| Android (arm64/x86_64) | [`android/`](android/) — Chaquopy Gradle project | `melee-agent.apk` |

## Releases (CI)

`.github/workflows/release.yml` builds both artifacts and publishes them on a
GitHub Release whenever a `v*` tag is pushed:

```bash
git tag v1.0.0
git push origin v1.0.0
```

- **Windows job** (`windows-latest`): installs the project + PyInstaller, runs
  `build/windows/melee-agent.spec`, smoke-tests `dist/melee-agent.exe --help`.
  The spec logic is platform-independent and was validated with a Linux build.
- **Android job** (`ubuntu-latest`): JDK 17 + Python 3.11 + Gradle 8.9 +
  Android SDK 35, then `gradle :app:assembleRelease` in `build/android`.
  The APK is signed with an ephemeral self-signed key generated per release.
- **Publish job**: downloads both artifacts and creates the GitHub Release
  with `melee-agent-windows.exe` and `melee-agent-android.apk` attached.

## Local builds

- Windows: on a Windows machine, run
  `powershell -ExecutionPolicy Bypass -File build/windows/build-exe.ps1`
  (see [windows/README.md](windows/README.md)).
- Android: install JDK 17, Python 3.11, Gradle 8.9 and the Android SDK 35,
  then `gradle :app:assembleRelease` in `build/android`
  (see [android/BUILD_NOTES.md](android/BUILD_NOTES.md)).

## Android app design

The APK is a thin wrapper: Chaquopy embeds CPython 3.11, `MainActivity`
shows a command input + output view, and `main.py::run_command(args)` runs
the real Typer CLI in-process and returns its output as a string. tree-sitter
is deliberately excluded on Android (no native wheels); the CLI already falls
back to regex-based parsing without it.
