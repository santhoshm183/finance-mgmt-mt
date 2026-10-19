$ErrorActionPreference = 'Stop'

$jarPath = [IO.Path]::GetFullPath((Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) 'finance-mgmt-0.0.1-SNAPSHOT.jar'))

try {
    $javaProcesses = Get-CimInstance Win32_Process -Filter "Name = 'java.exe' OR Name = 'javaw.exe'" |
        Where-Object {
            $_.CommandLine -and
            $_.CommandLine.IndexOf($jarPath, [StringComparison]::OrdinalIgnoreCase) -ge 0
        }

    if (-not $javaProcesses) {
        Write-Host 'Finance Management is not running.'
        exit 0
    }

    foreach ($javaProcess in $javaProcesses) {
        Write-Host "Stopping Finance Management (process $($javaProcess.ProcessId))..."
        Stop-Process -Id $javaProcess.ProcessId -Force
    }

    Write-Host 'Finance Management has been stopped.'
    Write-Host 'The database remains intact. Any database transaction in progress was rolled back.'
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
