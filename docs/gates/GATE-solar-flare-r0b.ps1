# GATE-solar-flare R0b (2026-11-27, feat/smolder-gate on feat/solar-flare on feat/water-douses-scorch): the deployed jar
# carries Solar Flare (#340), the builds restore (#341, D2) and this gate. Run beside GATE-smolder-r0b.ps1, which covers #336, #337, #338.
# Prediction: before '--- absent', 15 PRESENT and 0 ABSENT; between '--- absent' and '--- controls', 2 ABSENT and 0 PRESENT;
# after '--- controls', 2 PRESENT and 0 ABSENT.
# Run: powershell -File <this> [-JarPath <jar>]
param([string]$JarPath = 'run\plugins\rpg-0.1.0-SNAPSHOT.jar')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $JarPath))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function Entry($name) { if ($zip.GetEntry($name) -ne $null) { "PRESENT Entry '$name'" } else { "ABSENT  Entry '$name'" } }
function ValueLine($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + '([ \t]*\r?$|[ \t]+#)')) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Has($text, $s, $label) { if ($text -ne $null -and $text.Contains($s)) { "PRESENT $label '$s'" } else { "ABSENT  $label '$s'" } }
$sm = Bytes 'content/abilities/solar_flare.yml'
$rf = Bytes 'content/builds/ranger_fire.yml'
$ix = Bytes 'selftest/index.yml'
$pl = Bytes 'io/github/butterflysmp/rpg/core/combat/PulseLine.class'
$cw = Bytes 'io/github/butterflysmp/rpg/core/combat/CombatWorld.class'
$as = Bytes 'io/github/butterflysmp/rpg/paper/content/AbilitySchema.class'
$sb = Bytes 'io/github/butterflysmp/rpg/paper/selftest/SelfTestBuilds.class'
$ss = Bytes 'io/github/butterflysmp/rpg/paper/selftest/SelfTestSnapshot.class'
Entry 'content/abilities/solar_flare.yml'
Entry 'content/visuals/solar_flare_trail.yml'
Entry 'content/visuals/solar_flare_cast.yml'
Entry 'selftest/GATE-solar-flare.yml'
ValueLine $sm '  max_lifetime_ticks: 16' 'solar_flare.yml (F1: 16 ticks)'
ValueLine $sm '  body_scale: 2.5' 'solar_flare.yml (F9, SEAT PLACEHOLDER)'
ValueLine $rf '  ultimate: solar_flare' 'ranger_fire.yml (F11: the default)'
ValueLine $ix '  - solar-flare' 'selftest/index.yml'
Has $pl ' shot n=' 'PulseLine.class (F13: the shot line)'
Has $pl ' burst n=' 'PulseLine.class (F13: the burst line)'
Has $cw 'spawnDisplayMarker' 'CombatWorld.class (F9: the scaled body)'
Has $as '''s burst is ONE pulse front' 'AbilitySchema.class (F3: the burst block)'
Has $sm 'Fire three great fireballs in a fan.' 'solar_flare.yml (F12, SEAT PLACEHOLDER)'
Has $sb 'builds(store-refused)' 'SelfTestBuilds.class (D2: the restore)'
Has $ss ' builds=' 'SelfTestSnapshot.class (D2: the CAPTURED and RESTORED field; the settle line lives in an anonymous class)'
'--- absent'
Entry 'content/abilities/ultimate_placeholder_ranger.yml'
ValueLine $rf '  ultimate: ultimate_placeholder_ranger' 'ranger_fire.yml (the old default)'
'--- controls'
Has $sm 'smolder_ring' 'solar_flare.yml (the instrument reads the file; F10)'
ValueLine $rf 'class: ranger' 'ranger_fire.yml (an anchored needle finds a key that is there)'
$zip.Dispose()
