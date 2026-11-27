# GATE-held-target R0b (2026-11-26, feat/held-target-gate on feat/summon-holds-target on feat/owed-fixes): the deployed jar
# carries the owed fixes (R2 to R6), the summon that holds its target (G1 to G8) and this gate.
# Prediction: before '--- absent', 20 PRESENT and 0 ABSENT; between '--- absent' and '--- controls', 3 ABSENT and 0 PRESENT;
# after '--- controls', 2 PRESENT and 0 ABSENT.
# Run: powershell -File <this> [-JarPath <jar>]
param([string]$JarPath = 'run\plugins\rpg-0.1.0-SNAPSHOT.jar')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $JarPath))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function Entry($name) { if ($zip.GetEntry($name) -ne $null) { "PRESENT Entry '$name'" } else { "ABSENT  Entry '$name'" } }
function ValueLine($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + '([ \t]*\r?$|[ \t]+#)')) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Has($text, $s, $label) { if ($text -ne $null -and $text.Contains($s)) { "PRESENT $label '$s'" } else { "ABSENT  $label '$s'" } }
$wolf = Bytes 'content/summons/bone_whistle_wolf.yml'
$g = Bytes 'selftest/GATE-held-target.yml'
$ix = Bytes 'selftest/index.yml'
$bt = Bytes 'selftest/GATE-blink-tome.yml'
$bf = Bytes 'selftest/GATE-blast-fungus.yml'
$dec = Bytes 'io/github/butterflysmp/rpg/core/summon/SummonGuard$Decision.class'
$drop = Bytes 'io/github/butterflysmp/rpg/core/summon/SummonGuard$Dropped.class'
$trace = Bytes 'io/github/butterflysmp/rpg/core/summon/SummonTrace.class'
$loader = Bytes 'io/github/butterflysmp/rpg/paper/content/SummonLoader.class'
$drv = Bytes 'io/github/butterflysmp/rpg/paper/selftest/SelfTestDrivers.class'
$stg = Bytes 'io/github/butterflysmp/rpg/paper/selftest/SelfTestStaging.class'
$rule = Bytes 'io/github/butterflysmp/rpg/paper/selftest/ScenarioLint$Rule.class'
Entry 'selftest/GATE-held-target.yml'
Entry 'io/github/butterflysmp/rpg/core/summon/SummonGuard$Held.class'
Entry 'io/github/butterflysmp/rpg/core/summon/SummonGuard$Change.class'
Entry 'content/summons/bone_whistle_wolf.yml'
Has $dec 'acquired:' 'SummonGuard$Decision.class (G7)'
Has $dec 'switched:' 'SummonGuard$Decision.class (G7)'
Has $dec 'dropped:' 'SummonGuard$Decision.class (G7)'
Has $drop 'RANGE' 'SummonGuard$Dropped.class (G3: dropped for range)'
Has $trace ' reason=' 'SummonTrace.class (G7)'
Has $loader 'give_up_range' 'SummonLoader.class (G3: the key)'
Has $drv ' tier=' 'SelfTestDrivers.class (G7: probe_summon)'
Has $drv ' clear' 'SelfTestDrivers.class (S2: mob_target clear)'
Has $drv ' types=' 'SelfTestDrivers.class (R4: probe_entities)'
Has $stg 'ITEMS cleared=' 'SelfTestStaging.class (R4: the setup clear)'
Has $rule 'ACCESSORY_ARMED_AT_CLEAR' 'ScenarioLint$Rule.class (R2)'
Has $bt '# R2 (2026-11-26)' 'GATE-blink-tome.yml (R2: the tome off before the last clear)'
Has $bf '# R6 (2026-11-26)' 'GATE-blast-fungus.yml (R6: BF6 aligned before its press)'
ValueLine $wolf 'give_up_range: 72' 'bone_whistle_wolf.yml (BEN: 72)'
ValueLine $g 'S5:' 'GATE-held-target.yml'
ValueLine $ix '  - held-target' 'selftest/index.yml'
'--- absent'
ValueLine $wolf 'chase_limit: 32' 'bone_whistle_wolf.yml (the 32 is gone)'
Has $drop 'LIMIT' 'SummonGuard$Dropped.class (renamed RANGE)'
Has $loader 'chase_limit' 'SummonLoader.class (the old key)'
'--- controls'
Has $trace 'SUMMON target' 'SummonTrace.class (the instrument reads a class string)'
ValueLine $wolf 'trigger_radius: 30' 'bone_whistle_wolf.yml (the anchored needle finds a key that is there)'
$zip.Dispose()
