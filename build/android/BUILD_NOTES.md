# Android build notes (Chaquopy)

Minimal Android wrapper around the melee-decomp Python CLI. Chaquopy embeds a
CPython interpreter in the APK; `MainActivity` calls `main.py::run_command(args)`
and shows the returned string in a TextView.

## Versions used (verified against https://chaquo.com/chaquopy/doc/current/, Chaquopy 17.0)

| Component | Version |
|---|---|
| Chaquopy Gradle plugin (`com.chaquo.python`) | **17.0.0** |
| Android Gradle Plugin (`com.android.application`) | **8.7.2** (Chaquopy 17 supports AGP 7.3–9.2) |
| Gradle wrapper | **8.9** (AGP 8.7 requires Gradle 8.9+) |
| JDK to run Gradle | **17** (required by AGP 8.x) |
| Python inside the app (`chaquopy { version }`) | **3.11** (available: 3.10–3.14; 3.11 is the newest that still supports 32-bit ABIs) |
| Python on the **build** machine | **3.11.x** — must match the app's major.minor (needed for the `pip` block and bytecode compilation) |
| compileSdk / targetSdk | 35 |
| minSdk | 24 (Chaquopy 17 minimum) |
| ABIs | `arm64-v8a`, `x86_64` (current devices + emulators) |

## CI build

`.github/workflows/release.yml` builds the APK on `ubuntu-latest` (JDK 17,
Python 3.11, Gradle 8.9, Android SDK 35) via `gradle :app:assembleRelease` in
`build/android`, signs it with an ephemeral self-signed key, and attaches it
to the GitHub Release automatically when a `v*` tag is pushed.

Note: there is no `gradlew` wrapper script checked in. CI installs Gradle 8.9
via `gradle/actions/setup-gradle`; for local builds, install Gradle 8.9 (or
run `gradle wrapper` once) and use `gradle` directly.

## Gotchas

- **tree-sitter is excluded on purpose.** It has native components with no
  Android wheels and Chaquopy can't build it. The CLI already treats
  tree-sitter as optional (`src/hooks/c_analyzer.py` and
  `src/cli/extract.py` catch `ImportError` and fall back to regex-based
  parsing), so everything still works on Android, just with slightly less
  accurate C parsing in a few commands.
- **Wheels come from Chaquopy's repo.** `httpx`, `pydantic` (has a Rust core),
  `pyyaml` (has a C ext) etc. install from Chaquopy's prebuilt wheel index
  (https://chaquo.com/pypi-13.1/) automatically — no extra config needed.
  If a version you pin has no wheel there, loosen the pin.
- **No real HOME / cwd.** Android has no home directory; Chaquopy sets
  `os.environ["HOME"]` to the app's internal files dir (writable — use it for
  any files the CLI writes). `main.py` chdirs there at startup so relative
  paths and `load_dotenv()` behave sanely. `python-dotenv` still only loads a
  `.env` file if one actually exists; there is no `.env` bundled by default.
- **Python startup is slow-ish** (a second or two). `MainActivity` starts
  Python on a background thread and disables the Run button while a command
  runs — never call `run_command` on the UI thread.
- **Bytecode:** Chaquopy compiles `.py` → `.pyc` at build time by default, so
  tracebacks in the app won't show source lines. During development you can set
  `pyc { src = false }` in the `chaquopy` block to keep readable tracebacks.
- **Duplicate filenames:** `copyPythonSrc` copies the whole `../../src` tree;
  make sure it doesn't contain a second `main.py` at its top level or other
  files colliding with `app/src/main/python/` contents.
- **APK size:** the interpreter + stdlib + pip deps add tens of MB per ABI.
  If size matters, ship `arm64-v8a` only (drop `x86_64`, which is just for
  emulators) or split ABIs via product flavors.
- **INTERNET permission** is declared in the manifest (the CLI calls the
  Anthropic API over HTTPS via httpx).
