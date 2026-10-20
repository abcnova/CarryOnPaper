$ErrorActionPreference = 'Stop'
$Maven = Join-Path $PSScriptRoot '..\work\apache-maven-3.9.11\bin\mvn.cmd'
if (-not (Test-Path $Maven)) { $Maven = 'mvn' }
$versions = @(
  '1.21-R0.1-SNAPSHOT','1.21.1-R0.1-SNAPSHOT','1.21.2-R0.1-SNAPSHOT','1.21.3-R0.1-SNAPSHOT',
  '1.21.4-R0.1-SNAPSHOT','1.21.5-R0.1-SNAPSHOT','1.21.6-R0.1-SNAPSHOT','1.21.7-R0.1-SNAPSHOT',
  '1.21.8-R0.1-SNAPSHOT','1.21.9-R0.1-SNAPSHOT','1.21.10-R0.1-SNAPSHOT','1.21.11-R0.1-SNAPSHOT',
  '26.1-R0.1-SNAPSHOT','26.1.1-R0.1-SNAPSHOT','26.1.2-R0.1-SNAPSHOT','26.2-R0.1-SNAPSHOT','26.3-R0.1-SNAPSHOT'
)
$results = [System.Collections.Generic.List[string]]::new()
foreach ($version in $versions) {
  $adventure = if ($version.StartsWith('26.')) { '5.2.0' } else { '4.17.0' }
  Write-Host "Checking Spigot API $version"
  & $Maven "-Dspigot.api.version=$version" "-Dadventure.version=$adventure" -Pspigot -q clean compile
  if ($LASTEXITCODE -ne 0) { $results.Add("| $version | FAIL | API compilation failed |"); continue }
  $results.Add("| $version | PASS | Spigot API compile, Java 21 bytecode |")
}
$results | Set-Content (Join-Path $PSScriptRoot '..\verification\spigot-api-matrix.md') -Encoding utf8
if ($results.Where({ $_ -match '\| FAIL \|' }).Count -gt 0) { exit 1 }
