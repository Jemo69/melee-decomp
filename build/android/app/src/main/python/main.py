"""Android entry point for the melee-decomp Typer CLI (Chaquopy).

The melee-decomp CLI lives in the top-level ``src`` package, which the
``copyPythonSrc`` Gradle task (see app/build.gradle) copies to
``app/src/main/python/src`` before every build. Chaquopy then bundles it into
the APK, so ``from src.cli import app`` works here at runtime.

Java calls :func:`run_command` with a single string of CLI arguments. The
function runs the Typer app in-process, captures stdout/stderr, and returns
the combined output as a string (truncated to ~200k chars).
"""

import io
import os
import shlex
import traceback
from contextlib import redirect_stderr, redirect_stdout

# Keeps the Java-side TextView responsive when a command is very chatty.
MAX_OUTPUT_CHARS = 200_000


def run_command(args_str):
    """Run the melee-decomp CLI with the given argument string.

    Args:
        args_str: e.g. ``"--help"`` or ``"extract game.iso --out out/"``.

    Returns:
        The combined stdout/stderr of the CLI run as a string.
    """
    args = shlex.split(args_str or "")

    # Chaquopy sets os.environ["HOME"] to the app's internal files directory,
    # which is writable. There is no real home dir or useful cwd on Android,
    # so run from HOME: relative paths and load_dotenv() then behave sanely.
    # (python-dotenv still only loads a .env file if one exists there.)
    home = os.environ.get("HOME")
    if home:
        try:
            os.chdir(home)
        except OSError:
            pass

    stdout_buf = io.StringIO()
    stderr_buf = io.StringIO()
    exit_code = 0

    with redirect_stdout(stdout_buf), redirect_stderr(stderr_buf):
        try:
            from src.cli import app
            from typer.main import get_command

            command = get_command(app)
            # standalone_mode=True (the default): click prints --help and
            # error messages itself, then raises SystemExit with the exit code.
            command.main(args=args, prog_name="melee-decomp")
        except SystemExit as e:
            code = e.code
            exit_code = code if isinstance(code, int) else (0 if code is None else 1)
        except BaseException:
            # Anything unexpected (bad args type, etc.): surface the
            # traceback as command output instead of crashing. Note that
            # tree-sitter is deliberately not bundled on Android; the CLI
            # already falls back to regex-based parsing without it.
            traceback.print_exc()
            exit_code = 1

    output = stdout_buf.getvalue()
    err = stderr_buf.getvalue()
    if err:
        if output and not output.endswith("\n"):
            output += "\n"
        output += err
    if exit_code != 0:
        output += "\n[exit code: {}]".format(exit_code)

    if len(output) > MAX_OUTPUT_CHARS:
        output = output[:MAX_OUTPUT_CHARS] + (
            "\n…[truncated: output exceeded {} chars]".format(MAX_OUTPUT_CHARS)
        )
    return output
