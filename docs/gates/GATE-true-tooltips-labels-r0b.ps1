# GATE-true-tooltips, the class labels (TT20-TT27) R0b (2026-11-26, feat/class-damage-labels on master).
# Prediction: before '--- absent', 6 PRESENT and 0 ABSENT; between '--- absent' and '--- controls', 2 ABSENT and 0
# PRESENT; after '--- controls', 1 PRESENT and 0 ABSENT.
# Run: powershell -File <this> [-JarPath <jar>]
param([string]$JarPath = 'run\plugins\rpg-0.1.0-SNAPSHOT.jar')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $JarPath))
function Bytes($name) { $e = $zip.GetEntry($name); if ($e -eq $null) { return $null }; $s = $e.Open(); $m = New-Object IO.MemoryStream; $s.CopyTo($m); $s.Close(); [Text.Encoding]::GetEncoding(28591).GetString($m.ToArray()) }
function ValueLine($text, $l, $label) { if ($text -ne $null -and $text -match ("(?m)^" + [regex]::Escape($l) + '([ \t]*\r?$|[ \t]+#)')) { "PRESENT $label '$l'" } else { "ABSENT  $label '$l'" } }
function Has($text, $s, $label) { if ($text -ne $null -and $text.Contains($s)) { "PRESENT $label '$s'" } else { "ABSENT  $label '$s'" } }
$g = Bytes 'selftest/GATE-true-tooltips.yml'
$wc = Bytes 'io/github/butterflysmp/rpg/paper/weapon/WeaponClassLabel.class'
$wl = Bytes 'io/github/butterflysmp/rpg/paper/weapon/WeaponLore.class'
$sl = Bytes 'io/github/butterflysmp/rpg/paper/selftest/ScenarioLint.class'
$sa = Bytes 'io/github/butterflysmp/rpg/paper/selftest/SelfTestAsk.class'
Has $wc 'damageLabel' 'WeaponClassLabel.class'
Has $wl 'damageLabel' 'WeaponLore.class'
Has $sl 'ASK_WINDOW_SHORT' 'ScenarioLint.class'
Has $sa ' seconds.' 'SelfTestAsk.class'
ValueLine $g 'TT20:' 'GATE-true-tooltips.yml'
ValueLine $g 'TT27:' 'GATE-true-tooltips.yml'
'--- absent'
Has $wl 'elementName' 'WeaponLore.class (the element label''s helper, removed)'
Has $g '"1 1200 words Hover the Flint Staff' 'GATE-true-tooltips.yml (TT27 at the short window)'
'--- controls'
Has $wl 'Attack Speed: ' 'WeaponLore.class (the instrument reads a class string)'
$zip.Dispose()
