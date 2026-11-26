# GATE-bone-club R0b (2026-11-26, feat/blast-fungus-gate moved onto feat/drumming-beat): the deployed jar carries the Bone
# Club, Drumming Beat, the one minted-bone guard, the two new drivers and this gate.
# Prediction: before '--- absent', 21 PRESENT and 0 ABSENT; between '--- absent' and '--- controls', 5 ABSENT and 0 PRESENT;
# after '--- controls', 2 PRESENT and 0 ABSENT.
# Run: powershell -File <this> [-JarPath <jar>]
param([string]$JarPath = 'run\plugins\rpg-0.1.0-SNAPSHOT.jar')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $JarPath))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function Entry($name) { if ($zip.GetEntry($name) -ne $null) { "PRESENT Entry '$name'" } else { "ABSENT  Entry '$name'" } }
function ValueLine($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + '([ \t]*\r?$|[ \t]+#)')) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Has($text, $s, $label) { if ($text -ne $null -and $text.Contains($s)) { "PRESENT $label '$s'" } else { "ABSENT  $label '$s'" } }
$bc = Bytes 'content/weapons/bone_club.yml'
$db = Bytes 'content/enchants/drumming_beat.yml'
$g = Bytes 'selftest/GATE-bone-club.yml'
$ix = Bytes 'selftest/index.yml'
$lc = Bytes 'content/loot/cascade.yml'
$rl = Bytes 'io/github/butterflysmp/rpg/paper/listener/RpgListeners.class'
$ee = Bytes 'io/github/butterflysmp/rpg/core/enchant/EnchantEffect.class'
$vb = Bytes 'io/github/butterflysmp/rpg/paper/selftest/Scenario$Verb.class'
$wl = Bytes 'io/github/butterflysmp/rpg/paper/weapon/WeaponLore.class'
Entry 'io/github/butterflysmp/rpg/core/enchant/DrumBeats.class'
Entry 'io/github/butterflysmp/rpg/core/enchant/DrumStreaks.class'
Entry 'io/github/butterflysmp/rpg/core/enchant/EnchantBehaviour$DrummingBeat.class'
Entry 'io/github/butterflysmp/rpg/paper/weapon/MintedBoneUse.class'
Entry 'io/github/butterflysmp/rpg/paper/weapon/DrummingBeatHits.class'
Has $rl 'onMintedItemEntityInteract' 'RpgListeners.class'
Has $rl 'onDrumSlotChange' 'RpgListeners.class'
Has $ee 'DRUMMING_BEAT' 'EnchantEffect.class'
Has $vb 'RESET_CHARGE' 'Scenario$Verb.class'
Has $vb 'INTERACT_ENTITY' 'Scenario$Verb.class'
ValueLine $bc 'id: bone_club' 'bone_club.yml'
ValueLine $bc 'material: bone' 'bone_club.yml'
ValueLine $bc 'attack_damage: 44' 'bone_club.yml (BEN)'
ValueLine $bc 'attack_speed: 0.8' 'bone_club.yml (BEN: 0.8s)'
ValueLine $db 'weapon: bone_club' 'drumming_beat.yml'
ValueLine $db 'guaranteed: true' 'drumming_beat.yml (BEN)'
ValueLine $db '  gap_ticks: 100' 'drumming_beat.yml (BEN: same conditions)'
ValueLine $db '  step_percent_by_level: [3, 4, 5]' 'drumming_beat.yml (BEN: 3/4/5)'
ValueLine $g 'DB8:' 'GATE-bone-club.yml'
ValueLine $ix '  - bone-club' 'selftest/index.yml'
ValueLine $ix '  - blast-fungus' 'selftest/index.yml (the gate below, still listed)'
'--- absent'
ValueLine $bc 'craft_result:' 'bone_club.yml (outside the tiers: no craft mints it)'
ValueLine $bc 'sweep:' 'bone_club.yml (BEN: no sweep)'
ValueLine $bc 'crit_chance:' 'bone_club.yml (the bare club is plain)'
Has $lc 'bone_club' 'loot/cascade.yml (in no source until the loot pass)'
Has $rl 'onSummonerWeaponEntityInteract' 'RpgListeners.class (the old guard is gone: ONE guard)'
'--- controls'
Has $wl 'Attack Speed: ' 'WeaponLore.class (the instrument reads a class string)'
ValueLine $bc '  left_click:' 'bone_club.yml (the anchored needle finds a key that is there)'
$zip.Dispose()
