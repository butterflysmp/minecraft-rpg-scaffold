# GATE-smolder-2 R0b (2026-11-28, feat/ultimates-2-gate on feat/solar-flare-volley on feat/smolder-spiral on
# feat/x6-clock): the deployed jar carries X6 (a) (the settle hop), Smolder retuned (M1-M4), Solar Flare changed (N1-N7),
# X6 (b) (the view lint and lines) and both gates. One script for the one command.
# Prediction: before '--- absent', 21 PRESENT and 0 ABSENT; between '--- absent' and '--- controls', 4 ABSENT and 0 PRESENT;
# after '--- controls', 2 PRESENT and 0 ABSENT. Every Has and ValueLine needle was checked ABSENT in master 88858c40's main
# sources first (git grep -F, 0 files each; the ValueLines by an exact line match on the file). 'duration_ticks' was
# dropped as a needle: master has it in 20 files.
# Run: powershell -File <this> [-JarPath <jar>]
param([string]$JarPath = 'run\plugins\rpg-0.1.0-SNAPSHOT.jar')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $JarPath))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function Entry($name) { if ($zip.GetEntry($name) -ne $null) { "PRESENT Entry '$name'" } else { "ABSENT  Entry '$name'" } }
function ValueLine($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + '([ \t]*\r?$|[ \t]+#)')) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Has($text, $s, $label) { if ($text -ne $null -and $text.Contains($s)) { "PRESENT $label '$s'" } else { "ABSENT  $label '$s'" } }
$sm = Bytes 'content/abilities/smolder.yml'
$sf = Bytes 'content/abilities/solar_flare.yml'
$bu = Bytes 'content/visuals/solar_flare_burst.yml'
$ix = Bytes 'selftest/index.yml'
$ce = Bytes 'io/github/butterflysmp/rpg/core/ability/CastExecutor.class'
$pf = Bytes 'io/github/butterflysmp/rpg/core/combat/PulseFront.class'
$vl = Bytes 'io/github/butterflysmp/rpg/paper/content/VisualLoader.class'
$sl = Bytes 'io/github/butterflysmp/rpg/paper/selftest/ScenarioLint.class'
$sd = Bytes 'io/github/butterflysmp/rpg/paper/selftest/SelfTestDrivers.class'
Entry 'content/visuals/smolder_spiral.yml'
Entry 'content/visuals/solar_flare_burst.yml'
Entry 'selftest/GATE-smolder-2.yml'
Entry 'selftest/GATE-solar-flare-2.yml'
Entry 'io/github/butterflysmp/rpg/core/shape/SphereDirections.class'
ValueLine $sm '  interval_ticks: 30' 'smolder.yml (M1: 1.5 s apart)'
ValueLine $sm '  travel_ticks: 15' 'smolder.yml (M1: 0.75 s to the edge)'
ValueLine $sm '    arms: 3' 'smolder.yml (M2: the spiral)'
ValueLine $sf '  speed: 2.0' 'solar_flare.yml (N2: 32 blocks in 16 ticks)'
ValueLine $sf '    interval_ticks: 10' 'solar_flare.yml (N1: the salvo, 0.5 s apart)'
ValueLine $sf '    travel_ticks: 0' 'solar_flare.yml (N3: the burst is instant)'
ValueLine $ix '  - smolder-2' 'selftest/index.yml'
ValueLine $ix '  - solar-flare-2' 'selftest/index.yml'
Has $ce 'bookOnCaster' 'CastExecutor.class (X6 (a): the settle hop; N1: the salvo)'
Has $pf 'spiralPoints' 'PulseFront.class (M2: the spiral drawn with the front)'
Has $vl 'burst_radius' 'VisualLoader.class (N4: the firework ball)'
Has $sl 'VIEW_RULE_PREDATES' 'ScenarioLint.class (X6 (b): the view lint)'
Has $sd 'DRIVE look yaw=' 'SelfTestDrivers.class (X6 (b): the look line)'
Has $sm 'Four pulses, 1.5s apart, burst from your feet.' 'smolder.yml (N7, SEAT PLACEHOLDER)'
Has $sf 'Fire three great fireballs, one after another.' 'solar_flare.yml (N7, SEAT PLACEHOLDER)'
Has $bu 'entity.firework_rocket.large_blast' 'solar_flare_burst.yml (N4, Route B: the blast sound)'
'--- absent'
Entry 'content/visuals/smolder_ring.yml'
Entry 'content/visuals/solar_flare_ring.yml'
ValueLine $sm '  interval_ticks: 60' 'smolder.yml (the old 3 s)'
ValueLine $sf '  speed: 0.9375' 'solar_flare.yml (the old 15 blocks)'
'--- controls'
ValueLine $sm 'id: smolder' 'smolder.yml (an anchored needle finds a key that is there)'
Has $sf 'salvo' 'solar_flare.yml (the instrument reads the file)'
$zip.Dispose()
