<#
.SYNOPSIS
    Prints or sets the version of the four bundled mods.

.DESCRIPTION
    Each mod carries its own `version = "..."` in its build.gradle.kts, so
    there is no single source of truth the way there is in the other
    repositories. This script prints all four and only proceeds when they
    already agree, on the assumption that they are being released together as
    a baseline. CLAUDE.md, self-check's README, verify.py, and Preview.java
    spell its jar name out by hand and move with it.

    Only two shapes are rewritten, and neither is "every number that looks like
    the current version": the `version = "..."` line in each build script, and
    the file name `self-check-<version>.jar` wherever it is spelled out. That
    precision is the point. verify.py also holds `CODERPACK = "..."`, the
    coderpack artifact version it looks api and zygote up by in the local Maven
    repository, and a blanket replacement moved that too, sending the script
    after a coderpack release that does not exist.

    gradle/libs.versions.toml's `coderpack` entry is that same number and is
    likewise left alone: it is the toolchain these mods build against, not a
    mod's own version.

    This does not rebuild the jars or touch sacred.mods.repository.json, and it
    no longer has to. CI writes the index from the jars it publishes.

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

# A mod's own version, on the one line that declares it.
$declaration = @{
    pattern = '(?m)^version = "' + [regex]::Escape($current) + '"$'
    replace = 'version = "' + $Version + '"'
}
# The jar name, wherever a document or a script spells it out.
$jarName = @{
    pattern = 'self-check-' + [regex]::Escape($current) + '\.jar'
    replace = 'self-check-' + $Version + '.jar'
}

$targets = @()
foreach ($mod in $mods) {
    $targets += @{ path = Join-Path $root "$mod/build.gradle.kts"; rules = @($declaration) }
}
$targets += @{ path = Join-Path $root 'CLAUDE.md'; rules = @($jarName) }
$targets += @{ path = Join-Path $root 'self-check/README.md'; rules = @($jarName) }
$targets += @{ path = Join-Path $root 'self-check/verify.py'; rules = @($jarName) }
$targets += @{
    path  = Join-Path $root 'self-check/src/main/java/dev/ancaria/selfcheck/view/Preview.java'
    rules = @($jarName)
}

$touched = 0
foreach ($target in $targets) {
    $text = Get-Content -Path $target.path -Raw
    $new = $text
    foreach ($rule in $target.rules) {
        $new = [regex]::Replace($new, $rule.pattern, $rule.replace)
    }
    if ($new -eq $text) {
        Write-Warning "$current not found in $($target.path), left untouched"
        continue
    }
    Set-Content -Path $target.path -Value $new -NoNewline
    $touched++
}

Write-Host "$current -> $Version in $touched file(s)"
Write-Host 'Rebuild the jars and push. CI writes the index and cuts the releases.'
