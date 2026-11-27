# GATE-blink-tome R0b (2026-11-26, feat/blast-fungus-gate moved onto feat/blink-selftest): the deployed jar carries the
# Blink Tome, its Blink, the targeting seam, the attack sink, the selftest's blink clears and this gate.
# Prediction: before '--- absent', 27 PRESENT and 0 ABSENT; between '--- absent' and '--- controls', 4 ABSENT and 0 PRESENT;
# after '--- controls', 2 PRESENT and 0 ABSENT.
# Run: powershell -File <this> [-JarPath <jar>]
param([string]$JarPath = 'run\plugins\rpg-0.1.0-SNAPSHOT.jar')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $JarPath))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function Entry($name) { if ($zip.GetEntry($name) -ne $null) { "PRESENT Entry '$name'" } else { "ABSENT  Entry '$name'" } }
function ValueLine($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + '([ \t]*\r?$|[ \t]+#)')) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Has($text, $s, $label) { if ($text -ne $null -and $text.Contains($s)) { "PRESENT $label '$s'" } else { "ABSENT  $label '$s'" } }
$bt = Bytes 'content/accessories/blink_tome.yml'
$g = Bytes 'selftest/GATE-blink-tome.yml'
$ix = Bytes 'selftest/index.yml'
$lc = Bytes 'content/loot/cascade.yml'
$rl = Bytes 'io/github/butterflysmp/rpg/paper/listener/RpgListeners.class'
$vb = Bytes 'io/github/butterflysmp/rpg/paper/selftest/Scenario$Verb.class'
$wf = Bytes 'io/github/butterflysmp/rpg/paper/weapon/WeaponFire.class'
$bc = Bytes 'io/github/butterflysmp/rpg/paper/adapter/BukkitCombatant$Handle.class'
$bk = Bytes 'io/github/butterflysmp/rpg/paper/accessory/BlinkTomes.class'
Entry 'io/github/butterflysmp/rpg/core/accessory/AccessoryBehaviour$Blink.class'
Entry 'io/github/butterflysmp/rpg/core/accessory/Blinks.class'
Entry 'io/github/butterflysmp/rpg/core/accessory/BlinkedPlayers.class'
Entry 'io/github/butterflysmp/rpg/paper/accessory/BlinkTomes.class'
Entry 'io/github/butterflysmp/rpg/paper/adapter/MobTargets.class'
Entry 'io/github/butterflysmp/rpg/paper/adapter/TargetGuard.class'
Entry 'io/github/butterflysmp/rpg/paper/adapter/AttackSink.class'
Entry 'content/accessories/blink_tome.yml'
Entry 'selftest/GATE-blink-tome.yml'
Has $rl 'onHiddenTarget' 'RpgListeners.class (K7: vanilla target event refused while blinked)'
Has $bk 'BLINK REFUSED-TARGET' 'BlinkTomes.class (K12)'
Has $bk 'accessory:blink' 'BlinkTomes.class (K9: the cooldown key)'
Has $vb 'PROBE_INVISIBILITY' 'Scenario$Verb.class'
Has $wf 'attacks' 'WeaponFire.class (K5: a cast tells the attack sink)'
Has $bc 'MobTargets' 'BukkitCombatant$Handle.class (K7: the aggro goes through the seam)'
ValueLine $bt 'material: book' 'blink_tome.yml (BEN: Rare Book)'
ValueLine $bt 'slot: class' 'blink_tome.yml (BEN: a class accessory)'
ValueLine $bt 'class: mage' 'blink_tome.yml (BEN: mage)'
ValueLine $bt 'type: tome' 'blink_tome.yml'
ValueLine $bt '  type: blink' 'blink_tome.yml (K1)'
ValueLine $bt '  trigger_radius: 5' 'blink_tome.yml (BEN: any mob within 5 blocks)'
ValueLine $bt '  range: 16' 'blink_tome.yml (BEN: within 16 blocks)'
ValueLine $bt '  clear_radius: 5' 'blink_tome.yml (BEN: no mobs within 5 blocks)'
ValueLine $bt '  invisibility_ticks: 160' 'blink_tome.yml (BEN: 8 seconds)'
ValueLine $bt '  cooldown_ticks: 1200' 'blink_tome.yml (BEN: 60s cooldown)'
ValueLine $g 'BT11:' 'GATE-blink-tome.yml'
ValueLine $ix '  - blink-tome' 'selftest/index.yml'
'--- absent'
ValueLine $bt 'modifiers:' 'blink_tome.yml (BEN: no other stats)'
ValueLine $bt '  mana_cost:' 'blink_tome.yml (SEAT PLACEHOLDER: no mana)'
Has $lc 'blink_tome' 'loot/cascade.yml (BEN: loot only; no source until the loot pass)'
Has $bk 'ResourcePool' 'BlinkTomes.class (no mana is read or spent)'
'--- controls'
Has $bk 'BLINK END' 'BlinkTomes.class (the instrument reads a class string)'
ValueLine $bt 'behaviour:' 'blink_tome.yml (the anchored needle finds a key that is there)'
$zip.Dispose()
