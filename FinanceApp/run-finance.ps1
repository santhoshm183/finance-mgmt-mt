$ErrorActionPreference = 'Stop'

$bundleDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$jarPath = Join-Path $bundleDirectory 'finance-mgmt-0.0.1-SNAPSHOT.jar'
$schemaPath = Join-Path $bundleDirectory 'database\create_finance_mgmt_schema.sql'
$databaseName = 'finance_mgmt'
$databaseHost = 'localhost'
$databasePort = '5432'

try {
    $javaCommand = Get-Command 'java.exe' -ErrorAction SilentlyContinue
    if (-not $javaCommand) {
        throw 'Java was not found. Install Java 21 and ensure java.exe is on PATH.'
    }
    if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
        throw "JAR not found: $jarPath"
    }
    if (-not (Test-Path -LiteralPath $schemaPath -PathType Leaf)) {
        throw "Database setup script not found: $schemaPath"
    }

    $psqlCommand = Get-Command 'psql.exe' -ErrorAction SilentlyContinue
    if ($psqlCommand) {
        $psqlPath = $psqlCommand.Source
    } else {
        $postgresBin = Join-Path $env:ProgramFiles 'PostgreSQL'
        $psqlCandidate = Get-ChildItem -Path (Join-Path $postgresBin '*\bin\psql.exe') -File -ErrorAction SilentlyContinue |
            Sort-Object { [int]$_.Directory.Parent.Name } -Descending |
            Select-Object -First 1
        if (-not $psqlCandidate) {
            throw 'psql.exe was not found. Add PostgreSQL bin to PATH or install PostgreSQL command-line tools.'
        }
        $psqlPath = $psqlCandidate.FullName
    }

    $createdbPath = Join-Path (Split-Path -Parent $psqlPath) 'createdb.exe'
    if (-not (Test-Path -LiteralPath $createdbPath -PathType Leaf)) {
        throw "createdb.exe was not found next to psql.exe: $createdbPath"
    }

    $databaseUser = Read-Host 'PostgreSQL username [postgres]'
    if ([string]::IsNullOrWhiteSpace($databaseUser)) {
        $databaseUser = 'postgres'
    }
    $securePassword = Read-Host 'PostgreSQL password' -AsSecureString
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try {
        $databasePassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
        $securePassword.Dispose()
    }

    $env:PGPASSWORD = $databasePassword
    $databaseExists = & $psqlPath --host=$databaseHost --port=$databasePort --username=$databaseUser --dbname=postgres --tuples-only --no-align --command="SELECT 1 FROM pg_database WHERE datname = '$databaseName';"
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not connect to PostgreSQL. Check that the PostgreSQL service is running and the username/password are correct.'
    }
    if (($databaseExists | Out-String).Trim() -ne '1') {
        Write-Host "Creating PostgreSQL database '$databaseName'..."
        & $createdbPath --host=$databaseHost --port=$databasePort --username=$databaseUser $databaseName
        if ($LASTEXITCODE -ne 0) {
            throw "Could not create database '$databaseName'. The PostgreSQL user must have permission to create databases."
        }
    }

    $schemaReady = & $psqlPath --host=$databaseHost --port=$databasePort --username=$databaseUser --dbname=$databaseName --tuples-only --no-align --command="SELECT CASE WHEN to_regclass('finance_mgmt.customers') IS NOT NULL AND to_regclass('finance_mgmt.cash_flow') IS NOT NULL AND to_regclass('finance_mgmt.loans') IS NOT NULL AND to_regclass('finance_mgmt.financial_transactions') IS NOT NULL AND to_regclass('finance_mgmt.cash_transfers') IS NOT NULL THEN 1 ELSE 0 END;"
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not check the database schema.'
    }
    if (($schemaReady | Out-String).Trim() -ne '1') {
        Write-Host 'First-time database setup: applying the schema script...'
        & $psqlPath --host=$databaseHost --port=$databasePort --username=$databaseUser --dbname=$databaseName --set=ON_ERROR_STOP=1 --file=$schemaPath
        if ($LASTEXITCODE -ne 0) {
            throw 'The database setup script failed. The application was not started.'
        }
    } else {
        Write-Host 'Database schema already exists; skipping setup.'
    }

    $env:SUPABASE_DB_URL = "jdbc:postgresql://${databaseHost}:${databasePort}/${databaseName}"
    $env:SUPABASE_DB_USERNAME = $databaseUser
    $env:SUPABASE_DB_PASSWORD = $databasePassword

    Write-Host 'Starting Finance Management. Open http://localhost:8080 in your browser.'
    Write-Host 'Keep this window open while using the application. Press Ctrl+C to stop it.'
    & $javaCommand.Source -jar $jarPath
    if ($LASTEXITCODE -ne 0) {
        throw "The application stopped with exit code $LASTEXITCODE."
    }
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
} finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:SUPABASE_DB_URL -ErrorAction SilentlyContinue
    Remove-Item Env:SUPABASE_DB_USERNAME -ErrorAction SilentlyContinue
    Remove-Item Env:SUPABASE_DB_PASSWORD -ErrorAction SilentlyContinue
    $databasePassword = $null
}
