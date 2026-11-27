# GATE-healing-tome R0b (2026-11-26, feat/blast-fungus-gate moved onto feat/tome-selftest): the deployed jar carries the
# Healing Tome, its Radiant Pulse, the book's guards, the selftest's accessory staging and this gate.
# Prediction: before '--- absent', 25 PRESENT and 0 ABSENT; between '--- absent' and '--- controls', 4 ABSENT and 0 PRESENT;
# after '--- controls', 2 PRESENT and 0 ABSENT.
# Run: powershell -File <this> [-JarPath <jar>]
param([string]$JarPath = 'run\plugins\rpg-0.1.0-SNAPSHOT.jar')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $JarPath))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function Entry($name) { if ($zip.GetEntry($name) -ne $null) { "PRESENT Entry '$name'" } else { "ABSENT  Entry '$name'" } }
function ValueLine($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + '([ \t]*\r?$|[ \t]+#)')) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Has($text, $s, $label) { if ($text -ne $null -and $text.Contains($s)) { "PRESENT $label '$s'" } else { "ABSENT  $label '$s'" } }
$ht = Bytes 'content/accessories/healing_tome.yml'
$g = Bytes 'selftest/GATE-healing-tome.yml'
$ix = Bytes 'selftest/index.yml'
$lc = Bytes 'content/loot/cascade.yml'
$rl = Bytes 'io/github/butterflysmp/rpg/paper/listener/RpgListeners.class'
$vb = Bytes 'io/github/butterflysmp/rpg/paper/selftest/Scenario$Verb.class'
$lr = Bytes 'io/github/butterflysmp/rpg/paper/selftest/ScenarioLint$Rule.class'
$ph = Bytes 'io/github/butterflysmp/rpg/paper/health/PlayerHealthSystem.class'
$tp = Bytes 'io/github/butterflysmp/rpg/paper/accessory/TomePulses.class'
Entry 'io/github/butterflysmp/rpg/core/accessory/AccessoryBehaviour.class'
Entry 'io/github/butterflysmp/rpg/core/accessory/AccessoryBehaviour$HealingPulse.class'
Entry 'io/github/butterflysmp/rpg/core/accessory/HealingPulses.class'
Entry 'io/github/butterflysmp/rpg/paper/accessory/TomePulses.class'
Entry 'io/github/butterflysmp/rpg/paper/accessory/AccessoryVanillaUses.class'
Entry 'io/github/butterflysmp/rpg/paper/selftest/SelfTestAccessorySlots.class'
Has $rl 'onAccessoryTrade' 'RpgListeners.class (H12: a villager never buys one)'
Has $rl 'onAccessoryShelved' 'RpgListeners.class (H12: nor a chiseled bookshelf, by hand)'
Has $rl 'onAccessoryHoppered' 'RpgListeners.class (H12: nor by hopper)'
Has $vb 'CLASS' 'Scenario$Verb.class'
Has $vb 'ACCESSORY' 'Scenario$Verb.class'
Has $vb 'SATURATION' 'Scenario$Verb.class'
Has $vb 'PROBE_REGEN' 'Scenario$Verb.class'
Has $lr 'ACCESSORY_UNSTAGED' 'ScenarioLint$Rule.class'
Has $ph 'accessoryContributionsFor' 'PlayerHealthSystem.class (H3: the pulse re-checks through the reconcile reading)'
ValueLine $ht 'material: book' 'healing_tome.yml (BEN: Rare Book)'
ValueLine $ht 'slot: class' 'healing_tome.yml (BEN: a class accessory)'
ValueLine $ht 'class: mage' 'healing_tome.yml (BEN: mage specific)'
ValueLine $ht 'type: tome' 'healing_tome.yml (H2)'
ValueLine $ht '  heal_per_level: 1' 'healing_tome.yml (BEN: 1 health per level)'
ValueLine $ht '  interval_ticks: 80' 'healing_tome.yml (BEN: every 4 seconds)'
ValueLine $ht '  radius: 8' 'healing_tome.yml (BEN: 8 block radius)'
ValueLine $ht '  targets: players' 'healing_tome.yml (BEN: you and any players near you)'
ValueLine $g 'HT8:' 'GATE-healing-tome.yml'
ValueLine $ix '  - healing-tome' 'selftest/index.yml'
'--- absent'
ValueLine $ht 'modifiers:' 'healing_tome.yml (BEN: only the healing pulse)'
ValueLine $ht '  mana_cost:' 'healing_tome.yml (BEN: no mana cost)'
Has $lc 'healing_tome' 'loot/cascade.yml (BEN: loot only; no source until the loot pass)'
Has $tp 'ResourcePool' 'TomePulses.class (no mana is read or spent)'
'--- controls'
Has $tp 'TOMEPULSE ' 'TomePulses.class (the instrument reads a class string)'
ValueLine $ht 'behaviour:' 'healing_tome.yml (the anchored needle finds a key that is there)'
$zip.Dispose()
