# GATE-blast-fungus R0b (2026-11-26, feat/blast-fungus-gate): the deployed jar carries the stack and this gate.
# Prediction: before '--- absent', 18 PRESENT and 0 ABSENT; between '--- absent' and '--- controls', 4 ABSENT and 0 PRESENT;
# after '--- controls', 2 PRESENT and 0 ABSENT.
# Run: powershell -File <this> [-JarPath <jar>]
param([string]$JarPath = 'run\plugins\rpg-0.1.0-SNAPSHOT.jar')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $JarPath))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function Entry($name) { if ($zip.GetEntry($name) -ne $null) { "PRESENT Entry '$name'" } else { "ABSENT  Entry '$name'" } }
function ValueLine($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + '([ \t]*\r?$|[ \t]+#)')) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Has($text, $s, $label) { if ($text -ne $null -and $text.Contains($s)) { "PRESENT $label '$s'" } else { "ABSENT  $label '$s'" } }
$bf = Bytes 'content/weapons/blast_fungus.yml'
$ff = Bytes 'content/enchants/fast_and_fungal.yml'
$cb = Bytes 'content/enchants/cluster_bombs.yml'
$g = Bytes 'selftest/GATE-blast-fungus.yml'
$ix = Bytes 'selftest/index.yml'
$lc = Bytes 'content/loot/cascade.yml'
$ph = Bytes 'io/github/butterflysmp/rpg/core/mob/MobHostility$PvpPath.class'
$qt = Bytes 'io/github/butterflysmp/rpg/core/enchant/EnchantBehaviour$CastTune$Quantity.class'
$rl = Bytes 'io/github/butterflysmp/rpg/paper/listener/RpgListeners.class'
$wl = Bytes 'io/github/butterflysmp/rpg/paper/weapon/WeaponLore.class'
Entry 'io/github/butterflysmp/rpg/core/combat/FuseRules.class'
Entry 'io/github/butterflysmp/rpg/core/combat/FusedChunks.class'
Entry 'io/github/butterflysmp/rpg/core/combat/ClusterSplit.class'
Entry 'io/github/butterflysmp/rpg/core/ability/FusedPayload.class'
Entry 'io/github/butterflysmp/rpg/paper/weapon/FusedRemovals.class'
Entry 'io/github/butterflysmp/rpg/paper/weapon/MintedMushroomUse.class'
Has $ph 'FUSED_BURST' 'MobHostility$PvpPath.class'
Has $qt 'COOLDOWN' 'EnchantBehaviour$CastTune$Quantity.class'
Has $rl 'onMintedMushroomUse' 'RpgListeners.class'
ValueLine $bf 'id: blast_fungus' 'blast_fungus.yml'
ValueLine $bf '      fuse_ticks: 40' 'blast_fungus.yml (BEN: Keep)'
ValueLine $bf '    cooldown_ticks: 140' 'blast_fungus.yml (BEN: 7s)'
ValueLine $ff '  value_by_level: [20, 40, 60]' 'fast_and_fungal.yml (BEN: 1/2/3s)'
ValueLine $ff 'guaranteed: true' 'fast_and_fungal.yml'
ValueLine $cb '  damage_by_level: [55, 80, 105]' 'cluster_bombs.yml (BEN)'
ValueLine $cb '  radius: 5' 'cluster_bombs.yml (BEN: 5 blocks)'
ValueLine $g 'BF9:' 'GATE-blast-fungus.yml'
ValueLine $ix '  - blast-fungus' 'selftest/index.yml'
'--- absent'
ValueLine $bf 'craft_result:' 'blast_fungus.yml (outside the tiers: no craft mints it)'
Has $lc 'blast_fungus' 'loot/cascade.yml (in no source until the world-loot pass)'
ValueLine $cb 'guaranteed: true' 'cluster_bombs.yml (BEN: not guaranteed)'
ValueLine $bf '            amount: 35' 'blast_fungus.yml (the old game''s 35)'
'--- controls'
Has $wl 'Attack Speed: ' 'WeaponLore.class (the instrument reads a class string)'
ValueLine $bf '  right_click:' 'blast_fungus.yml (the anchored needle finds a key that is there)'
$zip.Dispose()
