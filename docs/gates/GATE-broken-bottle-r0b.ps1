# GATE-broken-bottle R0b (2026-11-25, feat/broken-bottle-gate on feat/sharpened-glass on feat/broken-bottle on #308).
# Prediction: before '--- absent', 30 PRESENT and 0 ABSENT; between '--- absent' and '--- controls', 4 ABSENT and 0
# PRESENT; after '--- controls', 3 ABSENT and 0 PRESENT.
# Run: powershell -File <this> [-JarPath <jar>]
param([string]$JarPath = 'run\plugins\rpg-0.1.0-SNAPSHOT.jar')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $JarPath))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function ValueLine($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + '([ \t]*\r?$|[ \t]+#)')) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Has($text, $s, $label) { if ($text -ne $null -and $text.Contains($s)) { "PRESENT $label '$s'" } else { "ABSENT  $label '$s'" } }
function Entry($name) { if ($zip.GetEntry($name) -ne $null) { "PRESENT entry '$name'" } else { "ABSENT  entry '$name'" } }
$bb = Bytes 'content/weapons/broken_bottle.yml'
$sg = Bytes 'content/enchants/sharpened_glass.yml'
$lt = Bytes 'content/loot/cascade.yml'
$g = Bytes 'selftest/GATE-broken-bottle.yml'
$ix = Bytes 'selftest/index.yml'
$wl = Bytes 'io/github/butterflysmp/rpg/paper/content/WeaponLoader.class'
$ee = Bytes 'io/github/butterflysmp/rpg/core/enchant/EnchantEffect.class'
$rl = Bytes 'io/github/butterflysmp/rpg/paper/listener/RpgListeners.class'
$ph = Bytes 'io/github/butterflysmp/rpg/paper/health/PlayerHealthSystem.class'
$gl = Bytes 'io/github/butterflysmp/rpg/paper/selftest/SelfTestGateList.class'
$sr = Bytes 'io/github/butterflysmp/rpg/paper/selftest/SelfTestRun.class'
$dr = Bytes 'io/github/butterflysmp/rpg/paper/selftest/SelfTestDrivers.class'
ValueLine $bb 'id: broken_bottle' 'broken_bottle.yml'
ValueLine $bb 'rarity: common' 'broken_bottle.yml'
ValueLine $bb 'element: kinetic' 'broken_bottle.yml'
ValueLine $bb 'material: glass_bottle' 'broken_bottle.yml'
ValueLine $bb 'attack_damage: 18' 'broken_bottle.yml'
ValueLine $bb 'attack_speed: 2.0' 'broken_bottle.yml'
ValueLine $bb 'crit_chance: 0.10' 'broken_bottle.yml'
ValueLine $sg 'effect: crit_chance' 'sharpened_glass.yml'
ValueLine $sg 'weapon: broken_bottle' 'sharpened_glass.yml'
ValueLine $sg 'guaranteed: true' 'sharpened_glass.yml'
ValueLine $sg 'value_by_level: [5, 10, 15]' 'sharpened_glass.yml'
ValueLine $g 'BB0:' 'GATE-broken-bottle.yml'
ValueLine $g 'BB4:' 'GATE-broken-bottle.yml'
ValueLine $g 'BB8:' 'GATE-broken-bottle.yml'
ValueLine $ix '  - broken-bottle' 'index.yml'
Entry 'io/github/butterflysmp/rpg/core/weapon/HeldWeaponCrit.class'
Entry 'io/github/butterflysmp/rpg/core/enchant/CritChanceEnchant.class'
Entry 'io/github/butterflysmp/rpg/paper/health/CritChanceEnchantItems.class'
Entry 'io/github/butterflysmp/rpg/paper/weapon/MintedBottleUse.class'
Has $wl 'crit_chance' 'WeaponLoader'
Has $ee 'CRIT_CHANCE' 'EnchantEffect'
Has $rl 'onMintedBottleUse' 'RpgListeners'
Has $rl 'onMintedBottleDispense' 'RpgListeners'
Has $rl 'onMintedGearBrew' 'RpgListeners'
Has $ph 'HeldWeaponCrit' 'PlayerHealthSystem'
Has $ph 'CritChanceEnchantItems' 'PlayerHealthSystem'
Has $gl 'runs only after its chain' 'SelfTestGateList'
Has $gl 'Refusing: an empty row name after ' 'SelfTestGateList'
Has $sr 'rowDoneDrained' 'SelfTestRun'
Has $dr 'USE result useItemInHand=' 'SelfTestDrivers'
'--- absent'
ValueLine $bb 'sweep: 0.5' 'broken_bottle.yml'
Has $bb 'craft_result:' 'broken_bottle.yml'
Has $lt 'broken_bottle' 'cascade.yml'
Entry 'content/recipes/broken_bottle.yml'
'--- controls'
ValueLine $bb 'crit_chance: 0.30' 'broken_bottle.yml'
ValueLine $bb 'attack_damage: 20' 'broken_bottle.yml'
Has $gl 'No gate:row syntax' 'SelfTestGateList'
$zip.Dispose()
