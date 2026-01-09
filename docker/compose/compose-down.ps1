$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

$docs = [Environment]::GetFolderPath("MyDocuments")
if ([string]::IsNullOrWhiteSpace($docs)) {
    Write-Error "Cannot resolve Documents folder. Set HOST_DOCUMENTS_DIR manually."
    exit 1
}

$env:HOST_DOCUMENTS_DIR = $docs

docker compose --env-file .env down
