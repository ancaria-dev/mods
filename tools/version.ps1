<#
.SYNOPSIS
    Prints or sets the version of the four bundled mods.

.DESCRIPTION
    Each mod carries its own `version = "..."` in its build.gradle.kts, so
    there is no single source of truth the way there is in the other
    repositories. This script prints all four and only proceeds when they
    already agree, on the assumption that they are being released together as
    a baseline. self-check's README, verify.py, and Preview.java spell its jar
    name out by hand and move with it.

    This does not rebuild the jars or touch sacred.mods.repository.json.
    Run `coderpack index` afterward and commit the regenerated file, the
    same step every mod version bump has always needed.

    gradle/libs.versions.toml's `coderpack` entry is a different number: the
    coderpack toolchain these mods build against, not a mod's own version.
    This script does not touch it.

.EXAMPLE
    pwsh tools/version.ps1
    pwsh tools/version.ps1 0.99.1
#>
param(
    [string]$Version
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$mods = 'all-my-runes', 'old-huge-potions', 'self-check', 'tracer'

$versions = @{}
foreach ($mod in $mods) {
    $path = Join-Path $root "$mod/build.gradle.kts"
    $match = Select-String -Path $path -Pattern '^version = "(.+)"$'
    if (-not $match) { throw "No version = line in $path" }
    $versions[$mod] = $match.Matches[0].Groups[1].Value
}

if (-not $Version) {
    foreach ($mod in $mods) { Write-Host "$mod $($versions[$mod])" }
    return
}

$distinct = @($versions.Values | Select-Object -Unique)
if ($distinct.Count -gt 1) {
    foreach ($mod in $mods) { Write-Host "$mod $($versions[$mod])" }
    throw 'The four mods are not on the same version. Resolve that by hand before bumping them together.'
}
$current = $distinct[0]

$targets = foreach ($mod in $mods) { Join-Path $root "$mod/build.gradle.kts" }
$targets += Join-Path $root 'self-check/README.md'
$targets += Join-Path $root 'self-check/verify.py'
$targets += Join-Path $root 'self-check/src/main/java/dev/ancaria/selfcheck/view/Preview.java'

$pattern = "(?<!\d)$([regex]::Escape($current))(?!\d)"
$touched = 0
foreach ($path in $targets) {
    $text = Get-Content -Path $path -Raw
    $new = [regex]::Replace($text, $pattern, $Version)
    if ($new -eq $text) {
        Write-Warning "$current not found in $path, left untouched"
        continue
    }
    Set-Content -Path $path -Value $new -NoNewline
    $touched++
}

Write-Host "$current -> $Version in $touched file(s)"
Write-Host 'Rebuild the jars and run coderpack index before committing.'
