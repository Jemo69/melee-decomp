# -*- mode: python ; coding: utf-8 -*-
"""PyInstaller spec for building melee-agent.exe on Windows.

Build on a Windows machine (or the windows-latest GitHub Actions runner):

    pip install -r build/windows/requirements.txt
    pyinstaller build/windows/melee-agent.spec

Output: dist/melee-agent.exe  (single-file console executable)
"""
import os
from pathlib import Path

from PyInstaller.utils.hooks import collect_submodules

SPEC_DIR = Path(SPECPATH).resolve()
REPO_ROOT = SPEC_DIR.parent.parent  # build/windows -> repo root

# Entry point: tiny launcher that imports the Typer app.
# (kept as a real file so tracebacks show a sensible filename)
ENTRY = SPEC_DIR / "_entry.py"

# Collect every submodule of the `src` package so Typer sub-apps,
# extractor, commit workflow, hooks, etc. are all bundled even if
# some are imported lazily.
hiddenimports = collect_submodules("src")
# Rich/typer plugin entry points that PyInstaller can miss:
hiddenimports += [
    "tree_sitter",
    "tree_sitter_c",
]

block_cipher = None

a = Analysis(
    [str(ENTRY)],
    pathex=[str(REPO_ROOT)],
    binaries=[],
    datas=[],
    hiddenimports=hiddenimports,
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[
        # Keep the exe lean: dev/test-only deps never imported at runtime.
        "pytest",
        "pytest_asyncio",
        "pytest_cov",
        "respx",
        "ruff",
        "tkinter",
        "unittest",
    ],
    win_no_prefer_redirects=False,
    win_private_assemblies=False,
    cipher=block_cipher,
    noarchive=False,
)

pyz = PYZ(a.pure, a.zipped_data, cipher=block_cipher)

exe = EXE(
    pyz,
    a.scripts,
    a.binaries,
    a.zipfiles,
    a.datas,
    [],
    name="melee-agent",
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=False,  # UPX often trips Windows antivirus heuristics; keep it off.
    upx_exclude=[],
    runtime_tmpdir=None,
    console=True,  # CLI tool: keep the console window.
    disable_windowed_traceback=False,
    argv_emulation=False,
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
)
