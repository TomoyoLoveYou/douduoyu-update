$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$androidDir = Join-Path $projectRoot "android"

if ($env:JAVA_HOME -and $env:JAVA_HOME.EndsWith("\bin")) {
    $env:JAVA_HOME = Split-Path -Parent $env:JAVA_HOME
}

if (-not $env:JAVA_HOME -or -not (Test-Path $env:JAVA_HOME)) {
    $candidates = @(
        "C:\Program Files\Java\jdk-24",
        "C:\Program Files\Android\Android Studio\jbr"
    )

    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) {
            $env:JAVA_HOME = $candidate
            break
        }
    }
}

if (-not $env:JAVA_HOME -or -not (Test-Path $env:JAVA_HOME)) {
    throw "JAVA_HOME is not set to a valid JDK directory."
}

Push-Location $androidDir
try {
    & .\gradlew.bat assembleDebug
} finally {
    Pop-Location
}
