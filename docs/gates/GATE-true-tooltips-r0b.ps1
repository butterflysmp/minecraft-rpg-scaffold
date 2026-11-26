# GATE-true-tooltips R0b (2026-11-26, feat/true-tooltips-gate on feat/true-tooltips on feat/true-tooltips-resolvers on #312).
# Prediction: before '--- absent', 17 PRESENT and 0 ABSENT; between '--- absent' and '--- controls', 4 ABSENT and 0
# PRESENT; after '--- controls', 2 PRESENT and 0 ABSENT.
# Run: powershell -File <this> [-JarPath <jar>]
param([string]$JarPath = 'run\plugins\rpg-0.1.0-SNAPSHOT.jar')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $JarPath))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function ValueLine($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + '([ \t]*\r?$|[ \t]+#)')) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Has($text, $s, $label) { if ($text -ne $null -and $text.Contains($s)) { "PRESENT $label '$s'" } else { "ABSENT  $label '$s'" } }
function Entry($name) { if ($zip.GetEntry($name) -ne $null) { "PRESENT entry '$name'" } else { "ABSENT  entry '$name'" } }
$ce = Bytes 'content/weapons/cursed_emerald.yml'
$fb = Bytes 'content/enchants/focused_beam.yml'
$sc = Bytes 'content/enchants/shatter_caps.yml'
$af = Bytes 'content/enchants/airburst_flames.yml'
$g = Bytes 'selftest/GATE-true-tooltips.yml'
$ix = Bytes 'selftest/index.yml'
$wl = Bytes 'io/github/butterflysmp/rpg/paper/weapon/WeaponLore.class'
$ll = Bytes 'io/github/butterflysmp/rpg/core/weapon/WeaponLoreLines.class'
$cx = Bytes 'io/github/butterflysmp/rpg/core/ability/CastExecutor.class'
$wd = Bytes 'io/github/butterflysmp/rpg/paper/content/WeaponLoader.class'
$el = Bytes 'io/github/butterflysmp/rpg/paper/content/EnchantLoader.class'
Entry 'io/github/butterflysmp/rpg/core/ability/BehaviourCasts.class'
Entry 'io/github/butterflysmp/rpg/core/weapon/BehaviourBlocks.class'
Entry 'io/github/butterflysmp/rpg/core/weapon/DescriptionTokens.class'
Entry 'io/github/butterflysmp/rpg/core/weapon/ItemEnchantTerms.class'
Entry 'io/github/butterflysmp/rpg/paper/weapon/ItemEnchantTermsReader.class'
ValueLine $ce '      - "Channel, then loose {shots_word}"' 'cursed_emerald.yml'
Has $fb 'ability_description:' 'focused_beam.yml'
Has $sc '{shots_word} chunks after 5 blocks.' 'shatter_caps.yml'
Has $af 'into three after 6 blocks.' 'airburst_flames.yml'
ValueLine $g 'TT0:' 'GATE-true-tooltips.yml'
ValueLine $g 'TT13:' 'GATE-true-tooltips.yml'
ValueLine $g 'TT19:' 'GATE-true-tooltips.yml'
ValueLine $ix '  - true-tooltips' 'index.yml'
Has $wl 'ItemEnchantTerms' 'WeaponLore.class'
Has $cx 'BehaviourCasts' 'CastExecutor.class'
Has $wd 'DescriptionTokens' 'WeaponLoader.class'
Has $el 'ability_description' 'EnchantLoader.class'
'--- absent'
Has $ce 'loose three"' 'cursed_emerald.yml (the literal word, now the token)'
Has $wl 'Range: ' 'WeaponLore.class (C: no range line)'
Has $wl 'Wind-up' 'WeaponLore.class (C: no wind-up line)'
Has $ll 'Reload: ' 'WeaponLoreLines.class (C: no reload line)'
'--- controls'
Has $wl ' Damage: ' 'WeaponLore.class (the instrument reads a class string)'
Has $wl 'Crit Chance: ' 'WeaponLore.class (the instrument reads a class string)'
$zip.Dispose()
