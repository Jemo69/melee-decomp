plugins {
    id("com.android.application")
    id("com.chaquo.python")
}

android {
    namespace = "com.jemo.meleeagent"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jemo.meleeagent"
        minSdk = 24 // Chaquopy 17 minimum
        targetSdk = 35
        versionCode = (project.findProperty("versionCode") as String? ?: "1").toInt()
        // Pass -PversionName=v1.2.3 (e.g. from the CI release workflow) to stamp
        // the APK with the release tag.
        versionName = project.findProperty("versionName") as String? ?: "1.0"

        ndk {
            // Python 3.11 supports 32-bit ABIs too, but arm64-v8a + x86_64
            // covers current devices and emulators while keeping the APK smaller.
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    // Release signing is configured entirely from environment variables so the
    // CI workflow can generate an ephemeral keystore per release build.
    // Local release builds without these variables produce an unsigned APK.
    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("MELEE_KEYSTORE_PATH")
            if (!keystorePath.isNullOrEmpty() && file(keystorePath).exists()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("MELEE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("MELEE_KEY_ALIAS")
                keyPassword = System.getenv("MELEE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            val keystorePath = System.getenv("MELEE_KEYSTORE_PATH")
            if (!keystorePath.isNullOrEmpty() && file(keystorePath).exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

chaquopy {
    defaultConfig {
        // App's Python version. Available: 3.10, 3.11, 3.12, 3.13, 3.14
        // (3.10 is Chaquopy's default). 3.11 is the newest that still supports
        // 32-bit ABIs, and it satisfies the package's requires-python >= 3.11.
        version = "3.11"

        // Python on the BUILD machine must be the same major.minor version
        // (3.11.x). Chaquopy auto-detects `python3.11` / `python3` / `python`
        // on PATH; override with buildPython("...") if needed.
        // buildPython("python3.11")

        pip {
            // Pure-Python (or Chaquopy-wheel-provided) dependencies of the
            // melee-decomp CLI. tree-sitter is DELIBERATELY excluded: it has
            // native components with no Android wheels, and Chaquopy cannot
            // build it. The CLI treats tree-sitter as optional and falls back
            // to regex-based parsing when it is unavailable, so everything
            // still works (see src/hooks/c_analyzer.py, src/cli/extract.py).
            install("httpx")
            install("pydantic")
            install("typer")
            install("rich")
            install("pyyaml")
            install("anthropic")
            install("python-dotenv")
        }
    }
}

// The melee-decomp CLI lives in ../../src (a top-level package literally
// named `src`, entry point `from src.cli import app`). Chaquopy bundles
// everything under app/src/main/python/, so this task copies the package
// there before every build. It runs on every build so edits to ../../src
// are always picked up.
tasks.register<Copy>("copyPythonSrc") {
    from("../../src")
    into("src/main/python/src")
    // Skip anything that should never ship inside the APK:
    exclude("**/__pycache__/**")
    exclude("**/*.pyc")
}

tasks.named("preBuild") {
    dependsOn("copyPythonSrc")
}
