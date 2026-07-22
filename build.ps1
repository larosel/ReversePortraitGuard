param(
    [ValidateSet("Debug", "Release")]
    [string]$Configuration = "Debug"
)

$ErrorActionPreference = "Stop"

$project = $PSScriptRoot
$jdkHome = (Get-ChildItem -LiteralPath "$project\tools\jdk-extract" -Directory | Select-Object -First 1).FullName

if (-not $jdkHome) {
    throw "Portable JDK not found under tools/jdk-extract."
}

$env:JAVA_HOME = $jdkHome
$env:ANDROID_HOME = "$project\tools\android-sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:ANDROID_USER_HOME = "$project\tools\android-home"
$env:GRADLE_USER_HOME = "$project\tools\gradle-home"
$env:GRADLE_OPTS = "-Duser.home=$project\tools\user-home"

New-Item -ItemType Directory -Force -Path $env:ANDROID_USER_HOME, "$project\tools\user-home" | Out-Null

& "$project\tools\gradle-8.9\bin\gradle.bat" --no-daemon "assemble$Configuration" "lint$Configuration"
if ($LASTEXITCODE -ne 0) {
    throw "Android build failed."
}

Write-Host "Build completed: $Configuration"
