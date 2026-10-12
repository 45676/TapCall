# TapCall - a simple NFC tap-to-dial tool for family use
# Copyright (C) 2026 TapCall contributors
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.

param(
  [string]$Sdk = 'D:\AndroidBuild\sdk',
  [string]$Jdk = 'D:\AndroidBuild\jdk',
  [string]$Python = 'python',
  [string]$Stage = ''
)
# NOTE: aapt2/d8 are native tools and fail on non-ASCII paths, so the project is
# copied into an ASCII staging directory first; the APK is copied back at the end.
$ErrorActionPreference = 'Stop'
$app = $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($Stage)) { $Stage = Join-Path $env:SystemDrive 'AndroidBuild\stage' }

if (Test-Path $Stage) {
  for ($i = 0; $i -lt 10; $i++) {
    try { Remove-Item -Recurse -Force $Stage -ErrorAction Stop; break } catch { Start-Sleep -Milliseconds 700 }
  }
}
New-Item -ItemType Directory -Force -Path $Stage | Out-Null
Copy-Item (Join-Path $app 'src') -Destination $Stage -Recurse -Force
Copy-Item (Join-Path $app 'res') -Destination $Stage -Recurse -Force
Copy-Item (Join-Path $app 'tools') -Destination $Stage -Recurse -Force
Copy-Item (Join-Path $app 'AndroidManifest.xml') -Destination $Stage -Force

$out = Join-Path $Stage 'build'
$gen = Join-Path $out 'gen'
$cls = Join-Path $out 'classes'
$dex = Join-Path $out 'dex'
$bt  = Join-Path $Sdk 'build-tools\34.0.0'
$aj  = Join-Path $Sdk 'platforms\android-34\android.jar'
$apk = Join-Path $app 'TapCall.apk'

foreach ($p in @("$bt\aapt2.exe", "$bt\d8.bat", "$bt\zipalign.exe", "$bt\apksigner.bat", $aj, "$Jdk\bin\javac.exe", "$Jdk\bin\keytool.exe")) {
  if (-not (Test-Path $p)) { throw "missing build component: $p" }
}
New-Item -ItemType Directory -Force -Path $gen, $cls, $dex | Out-Null
$env:JAVA_HOME = $Jdk
$env:Path = "$Jdk\bin;$env:Path"

Write-Host '[1/6] aapt2 compile resources'
& "$bt\aapt2.exe" compile --dir (Join-Path $Stage 'res') -o (Join-Path $out 'res.zip')
if ($LASTEXITCODE -ne 0) { throw 'aapt2 compile failed' }

Write-Host '[2/6] aapt2 link'
& "$bt\aapt2.exe" link -o (Join-Path $out 'base.apk') -I $aj --manifest (Join-Path $Stage 'AndroidManifest.xml') -R (Join-Path $out 'res.zip') --java $gen --min-sdk-version 23 --target-sdk-version 34 --version-code 2 --version-name 1.0.1 --auto-add-overlay
if ($LASTEXITCODE -ne 0) { throw 'aapt2 link failed' }

Write-Host '[3/6] javac'
$srcs = @(Get-ChildItem (Join-Path $Stage 'src') -Recurse -Filter *.java | ForEach-Object { $_.FullName })
$srcs += @(Get-ChildItem $gen -Recurse -Filter *.java | ForEach-Object { $_.FullName })
& "$Jdk\bin\javac.exe" -encoding UTF-8 -source 8 -target 8 -nowarn -bootclasspath $aj -classpath $aj -d $cls @srcs
if ($LASTEXITCODE -ne 0) { throw 'javac failed' }

Write-Host '[4/6] d8'
$classFiles = @(Get-ChildItem $cls -Recurse -Filter *.class | ForEach-Object { $_.FullName })
& "$bt\d8.bat" --lib $aj --min-api 23 --output $dex @classFiles
if ($LASTEXITCODE -ne 0) { throw 'd8 failed' }

Write-Host '[5/6] package dex + zipalign'
& $Python (Join-Path $Stage 'tools\add_dex.py') (Join-Path $out 'base.apk') (Join-Path $dex 'classes.dex') (Join-Path $out 'unsigned.apk')
if ($LASTEXITCODE -ne 0) { throw 'add dex failed' }
& "$bt\zipalign.exe" -f 4 (Join-Path $out 'unsigned.apk') (Join-Path $out 'aligned.apk')
if ($LASTEXITCODE -ne 0) { throw 'zipalign failed' }

Write-Host '[6/6] sign'
$ks = Join-Path $app 'tapcall.jks'
if (-not (Test-Path $ks)) {
  & "$Jdk\bin\keytool.exe" -genkeypair -keystore $ks -alias tapcall -keyalg RSA -keysize 2048 -validity 10000 -storepass tapcall123 -keypass tapcall123 -dname "CN=TapCall,O=Family,C=CN"
  if ($LASTEXITCODE -ne 0) { throw 'keytool failed' }
}
& "$bt\apksigner.bat" sign --ks $ks --ks-pass pass:tapcall123 --key-pass pass:tapcall123 --out $apk (Join-Path $out 'aligned.apk')
if ($LASTEXITCODE -ne 0) { throw 'apksigner failed' }
& "$bt\apksigner.bat" verify --print-certs $apk

Write-Host ''
Write-Host "BUILD OK => $apk"
