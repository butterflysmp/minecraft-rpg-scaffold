# GATE-mana-battery R0b (2026-11-27, feat/mana-battery-gate on feat/owed-v7 on feat/mana-battery): the deployed jar
# carries Mana Battery (B1 to B6), the owed V7 (a) to (e) and this gate.
# Prediction: before '--- absent', 22 PRESENT and 0 ABSENT; between '--- absent' and '--- controls', 3 ABSENT and 0 PRESENT;
# after '--- controls', 2 PRESENT and 0 ABSENT.
# Run: powershell -File <this> [-JarPath <jar>]
param([string]$JarPath = 'run\plugins\rpg-0.1.0-SNAPSHOT.jar')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $JarPath))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function Entry($name) { if ($zip.GetEntry($name) -ne $null) { "PRESENT Entry '$name'" } else { "ABSENT  Entry '$name'" } }
function ValueLine($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + '([ \t]*\r?$|[ \t]+#)')) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Has($text, $s, $label) { if ($text -ne $null -and $text.Contains($s)) { "PRESENT $label '$s'" } else { "ABSENT  $label '$s'" } }
$mb = Bytes 'content/enchants/mana_battery.yml'
$g = Bytes 'selftest/GATE-mana-battery.yml'
$ht = Bytes 'selftest/GATE-held-target.yml'
$ix = Bytes 'selftest/index.yml'
$eff = Bytes 'io/github/butterflysmp/rpg/core/enchant/EnchantEffect.class'
$ai = Bytes 'io/github/butterflysmp/rpg/paper/weapon/ArmorItems.class'
$line = Bytes 'io/github/butterflysmp/rpg/paper/weapon/EnchantEffectLine.class'
$cmd = Bytes 'io/github/butterflysmp/rpg/paper/command/RpgCommand.class'
$scan = Bytes 'io/github/butterflysmp/rpg/paper/health/ManaBatteryModifierItems.class'
$stg = Bytes 'io/github/butterflysmp/rpg/paper/selftest/SelfTestStaging.class'
$gl = Bytes 'io/github/butterflysmp/rpg/paper/selftest/SelfTestGateList.class'
$trace = Bytes 'io/github/butterflysmp/rpg/core/summon/SummonTrace.class'
$lint = Bytes 'io/github/butterflysmp/rpg/paper/selftest/ScenarioLint.class'
Entry 'content/enchants/mana_battery.yml'
Entry 'selftest/GATE-mana-battery.yml'
Entry 'io/github/butterflysmp/rpg/paper/health/ManaBatteryModifierItems.class'
ValueLine $mb 'effect: mana_regen' 'mana_battery.yml (B2)'
ValueLine $mb 'class: armor' 'mana_battery.yml (B1)'
ValueLine $mb 'value_by_level: [1, 2, 3]' 'mana_battery.yml (BEN: 1/2/3 per five seconds, "B")'
ValueLine $mb 'icon: redstone_block' 'mana_battery.yml (SEAT PLACEHOLDER)'
Has $eff 'MANA_REGEN' 'EnchantEffect.class (B2)'
Has $ai 'Mana Regen' 'ArmorItems.class (B5: the stat-block label, inlined; ArmorLoreLines already says Mana Regen per second)'
Has $line ' Mana Regen' 'EnchantEffectLine.class (B5: the suffix)'
Has $scan 'manabattery:' 'ManaBatteryModifierItems.class (B3: its own source key)'
Has $scan 'perSecondFromPerFiveSeconds' 'ManaBatteryModifierItems.class (C2: the one conversion, called by the scan)'
Has $cmd 'Mana set to ' 'RpgCommand.class (rpg mana set, CC''S CALL)'
Has $stg 'ACCESSORIES cleared=' 'SelfTestStaging.class (V7 a)'
Has $gl 'predates the pad and would run on your ground. Refused.' 'SelfTestGateList.class (V7 e)'
Has $trace ('reason=' + [char]1 + ' tick=') 'SummonTrace.class (V7 d: the target line''s recipe, never the hit''s tick=%d)'
Has $lint 'before the row holds an item of its' 'ScenarioLint.class (V7 a: SELF_STAGED)'
Has $ht '# V7 (b), 2026-11-27' 'GATE-held-target.yml (V7 b: S3 aligned)'
Has $ht '# V7 (c), 2026-11-27' 'GATE-held-target.yml (V7 c: S5 waits)'
ValueLine $ht '  - wait: 120' 'GATE-held-target.yml (V7 c)'
ValueLine $g 'MB6:' 'GATE-mana-battery.yml'
ValueLine $ix '  - mana-battery' 'selftest/index.yml'
'--- absent'
ValueLine $mb 'class: universal' 'mana_battery.yml (the dangerous typo)'
ValueLine $mb 'armor_slot: head' 'mana_battery.yml (a normal slot, not piece-specific)'
Has $mb 'element:' 'mana_battery.yml (never unique)'
'--- controls'
ValueLine $mb 'max_level: 3' 'mana_battery.yml (the anchored needle finds a key that is there)'
Has $stg 'ITEMS cleared=' 'SelfTestStaging.class (the instrument reads a class string)'
$zip.Dispose()
