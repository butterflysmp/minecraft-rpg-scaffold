# GATE-smolder R0b (2026-11-27, feat/smolder-gate on feat/water-douses-scorch on feat/smolder on
# feat/regen-tooltips-sheet-unit): the deployed jar carries the regen tooltips (#336), Smolder (#337), water puts out
# Scorch (#338) and this gate.
# Prediction: before '--- absent', 21 PRESENT and 0 ABSENT; between '--- absent' and '--- controls', 3 ABSENT and 0 PRESENT;
# after '--- controls', 3 PRESENT and 0 ABSENT.
# Run: powershell -File <this> [-JarPath <jar>]
param([string]$JarPath = 'run\plugins\rpg-0.1.0-SNAPSHOT.jar')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $JarPath))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function Entry($name) { if ($zip.GetEntry($name) -ne $null) { "PRESENT Entry '$name'" } else { "ABSENT  Entry '$name'" } }
function ValueLine($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + '([ \t]*\r?$|[ \t]+#)')) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Has($text, $s, $label) { if ($text -ne $null -and $text.Contains($s)) { "PRESENT $label '$s'" } else { "ABSENT  $label '$s'" } }
$sm = Bytes 'content/abilities/smolder.yml'
$mf = Bytes 'content/builds/mage_fire.yml'
$sc = Bytes 'content/statuses/scorch.yml'
$ix = Bytes 'selftest/index.yml'
$stones = Bytes 'io/github/butterflysmp/rpg/paper/build/Stones.class'
$pl = Bytes 'io/github/butterflysmp/rpg/core/combat/PulseLine.class'
$dl = Bytes 'io/github/butterflysmp/rpg/core/combat/DouseLine$Event.class'
$rl = Bytes 'io/github/butterflysmp/rpg/paper/listener/RpgListeners.class'
$drv = Bytes 'io/github/butterflysmp/rpg/paper/selftest/SelfTestDrivers.class'
$ab = Bytes 'io/github/butterflysmp/rpg/paper/content/AbilityLoader.class'
$ssl = Bytes 'io/github/butterflysmp/rpg/core/combat/StatsSheetLines.class'
$eqm = Bytes 'io/github/butterflysmp/rpg/paper/menu/EquipmentMenu.class'
Entry 'content/abilities/smolder.yml'
Entry 'content/visuals/smolder_ring.yml'
Entry 'content/visuals/smolder_pulse.yml'
Entry 'io/github/butterflysmp/rpg/core/combat/PulseFront.class'
Entry 'io/github/butterflysmp/rpg/core/combat/Douse.class'
Entry 'selftest/GATE-smolder.yml'
ValueLine $sm '  type: pulses' 'smolder.yml (Z1)'
ValueLine $sm 'cooldown_ticks: 6000' 'smolder.yml (BEN: 300 s)'
ValueLine $mf '  ultimate: smolder' 'mage_fire.yml (Z8: the default)'
Has $stones 'BUILD unoffered player=' 'Stones.class (A3: the logged line)'
Has $pl ' cancelled caster=' 'PulseLine.class (Z10)'
Has $ab 'description: must be a YAML list' 'AbilityLoader.class (Z9)'
Has $dl 'SCORCH doused' 'DouseLine$Event.class (Y7)'
Has $dl 'IGNITE refused' 'DouseLine$Event.class (E1)'
Has $rl 'getToExtinguish' 'RpgListeners.class (D: the splash set)'
Has $sc 'Douse and ScorchStatus are the account' 'scorch.yml (the water line)'
Has $drv 'STAGE stone slot=' 'SelfTestDrivers.class (the stone verb)'
ValueLine $ix '  - smolder' 'selftest/index.yml'
Has $ssl 'perFiveSecondsBonus' 'StatsSheetLines.class (#336, U1)'
Has $eqm 'ACCESSORY menu ' 'EquipmentMenu.class (#336, W4)'
Has $sm 'Four pulses, 3s apart, burst from your feet.' 'smolder.yml (Z9, SEAT PLACEHOLDER)'
'--- absent'
Entry 'content/abilities/ultimate_placeholder_mage.yml'
Has $sc 'the damage of whatever applied the stacks' 'scorch.yml (A2: the retired cap sentence)'
Has $sm '- type: status' 'smolder.yml (A1: no status step; its comment names the key in backticks, never after a dash)'
'--- controls'
Has $sm 'smolder_ring' 'smolder.yml (the instrument reads the file)'
Has $sm '- type: damage' 'smolder.yml (the same needle form finds the step that IS there)'
ValueLine $ix '  - mana-battery' 'selftest/index.yml (an anchored needle finds a key that is there)'
$zip.Dispose()
