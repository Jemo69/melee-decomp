# Windows build

Produces a single-file `melee-agent.exe` via [PyInstaller](https://pyinstaller.org/).

## Local build (on Windows)

```powershell
powershell -ExecutionPolicy Bypass -File build/windows/build-exe.ps1
```

This installs the project + PyInstaller, then runs
`build/windows/melee-agent.spec`. The output lands at `dist/melee-agent.exe`.

## CI build

`.github/workflows/release.yml` builds the exe on `windows-latest` and
attaches it to the GitHub Release automatically when a `v*` tag is pushed.

## Notes

- The app is a console (CLI) tool, so the exe keeps its console window.
- UPX packing is disabled: it frequently trips Windows antivirus heuristics.
- The exe is unsigned; Windows SmartScreen may show a warning on first run.
  Code-signing can be added later with `codesign_identity` in the spec.
- Runtime state (agent DB, cookies) lives under `%USERPROFILE%\.config\decomp-me`
  on Windows, same layout as other platforms.
