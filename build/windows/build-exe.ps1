<#
.SYNOPSIS
    Build melee-agent.exe on Windows.
.DESCRIPTION
    Installs dependencies and runs PyInstaller with build/windows/melee-agent.spec.
    Produces dist/melee-agent.exe (single-file console executable).
.EXAMPLE
    powershell -ExecutionPolicy Bypass -File build/windows/build-exe.ps1
#>
$ErrorActionPreference = "Stop"

$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
Set-Location $RepoRoot

Write-Host "==> Installing build dependencies..." -ForegroundColor Cyan
python -m pip install --upgrade pip
# Install from inside build/windows: the `-e ../..` line in requirements.txt
# is resolved relative to the current directory, not the file's location.
Push-Location (Join-Path $RepoRoot "build/windows")
python -m pip install -r requirements.txt
Pop-Location

Write-Host "==> Building melee-agent.exe with PyInstaller..." -ForegroundColor Cyan
python -m PyInstaller build/windows/melee-agent.spec --noconfirm --clean

$exe = Join-Path $RepoRoot "dist/melee-agent.exe"
if (Test-Path $exe) {
    Write-Host "==> Build OK: $exe" -ForegroundColor Green
    Write-Host "==> Smoke test:" -ForegroundColor Cyan
    & $exe --help | Select-Object -First 8
} else {
    Write-Error "Build failed: $exe not produced"
}
