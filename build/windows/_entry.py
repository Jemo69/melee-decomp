"""Launcher entry point for the PyInstaller Windows build.

The installed package is a top-level package literally named ``src``
(see ``[tool.hatch.build.targets.wheel] packages = ["src"]`` in
pyproject.toml), so the repo root must be on ``sys.path``.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from src.cli import main

if __name__ == "__main__":
    main()
