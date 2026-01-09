$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

$docs = [Environment]::GetFolderPath("MyDocuments")
if ([string]::IsNullOrWhiteSpace($docs)) {
    Write-Error "Cannot resolve Documents folder. Set HOST_DOCUMENTS_DIR manually."
    exit 1
}
if (-not (Test-Path -Path $docs)) {
    Write-Error "Resolved Documents folder does not exist: $docs"
    exit 1
}

$env:HOST_DOCUMENTS_DIR = $docs
Write-Host "HOST_DOCUMENTS_DIR resolved to: $docs"

docker compose --env-file .env up -d
