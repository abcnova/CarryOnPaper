$ErrorActionPreference = 'Stop'
$Maven = Join-Path $PSScriptRoot '..\work\apache-maven-3.9.11\bin\mvn.cmd'
if (-not (Test-Path $Maven)) { $Maven = 'mvn' }
$Root = Split-Path $PSScriptRoot -Parent
$Release = Join-Path $Root 'release'
New-Item -ItemType Directory -Force $Release | Out-Null

& $Maven -q clean package
if ($LASTEXITCODE -ne 0) { throw 'Paper 1.21 baseline build failed.' }
Copy-Item (Join-Path $Root 'target\CarryOnPaper-1.0.2.jar') (Join-Path $Release 'CarryOnPaper-1.0.2.jar') -Force

& $Maven -Pspigot -q clean package
if ($LASTEXITCODE -ne 0) { throw 'Spigot 1.21 baseline build failed.' }
Copy-Item (Join-Path $Root 'target\CarryOnSpigot-1.0.2.jar') (Join-Path $Release 'CarryOnSpigot-1.0.2.jar') -Force

& $Maven -Ppaper26 -q clean package
if ($LASTEXITCODE -ne 0) { throw 'Paper 26.3 API compile failed.' }
& $Maven -Pspigot26 -q clean package
if ($LASTEXITCODE -ne 0) { throw 'Spigot 26.3 API compile failed.' }

Write-Host 'Release artifacts:'
Get-ChildItem $Release -Filter '*.jar' | Select-Object Name, Length
