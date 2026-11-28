# GATE-smolder-3 R0b (2026-11-28, feat/ultimates-3 from master 2386162e): the deployed jar carries Smolder's nine arms
# (V1), Solar Flare's all-orange burst and its one deep boom (V2, V3), and both -3 gates. One script for both gates.
# Prediction: before '--- absent', 9 PRESENT and 0 ABSENT; between '--- absent' and '--- controls', 5 ABSENT and 0
# PRESENT; after '--- controls', 2 PRESENT and 0 ABSENT. Every PRESENT needle was checked ABSENT in the same file at
# master 2386162e first (git show, an exact line match for each ValueLine; the two Entry needles are new files). The
# key and pitch needles read solar_flare_burst.yml ONLY: master has 'key: entity.generic.explode' in 5 other main files
# and 'pitch: 0.7' in 2, none of them this one.
# Run: powershell -File <this> [-JarPath <jar>]
param([string]$JarPath = 'run\plugins\rpg-0.1.0-SNAPSHOT.jar')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $JarPath))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function Entry($name) { if ($zip.GetEntry($name) -ne $null) { "PRESENT Entry '$name'" } else { "ABSENT  Entry '$name'" } }
function ValueLine($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + '([ \t]*\r?$|[ \t]+#)')) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Has($text, $s, $label) { if ($text -ne $null -and $text.Contains($s)) { "PRESENT $label '$s'" } else { "ABSENT  $label '$s'" } }
$sm = Bytes 'content/abilities/smolder.yml'
$bu = Bytes 'content/visuals/solar_flare_burst.yml'
$ix = Bytes 'selftest/index.yml'
Entry 'selftest/GATE-smolder-3.yml'
Entry 'selftest/GATE-solar-flare-3.yml'
ValueLine $sm '    arms: 9' 'smolder.yml (V1: nine arms)'
ValueLine $bu '    key: entity.generic.explode' 'solar_flare_burst.yml (V3: the deep boom)'
ValueLine $bu '    pitch: 0.7' 'solar_flare_burst.yml (V3: low)'
ValueLine $bu '    duration_ticks: 20' 'solar_flare_burst.yml (V2: the orange sparks)'
ValueLine $bu '    burst_radius: 5.5' 'solar_flare_burst.yml (V2: the sparks thrown out)'
ValueLine $ix '  - smolder-3' 'selftest/index.yml'
ValueLine $ix '  - solar-flare-3' 'selftest/index.yml'
'--- absent'
ValueLine $sm '    arms: 3' 'smolder.yml (the old three arms)'
Has $bu 'firework_rocket' 'solar_flare_burst.yml (the old firework sounds)'
ValueLine $bu '    color: [255, 220, 40]' 'solar_flare_burst.yml (the old yellow ball; a value line: the comment names the old colour)'
ValueLine $bu '    particle: FIREWORK' 'solar_flare_burst.yml (the old white sparks)'
ValueLine $bu '    pitch: 1.0' 'solar_flare_burst.yml (the old pitch)'
'--- controls'
ValueLine $sm 'id: smolder' 'smolder.yml (an anchored needle finds a key that is there)'
Has $bu 'burst_radius' 'solar_flare_burst.yml (the instrument reads the file)'
$zip.Dispose()
