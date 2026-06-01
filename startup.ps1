$ErrorActionPreference = 'Stop'

$pwsh = 'C:\Program Files\PowerShell\7\pwsh.exe'
$toolsDir = 'E:\tools'
$canalDir = Join-Path $toolsDir 'canal'

$targets = @(
    @{
        Name = 'Elasticsearch 8.11.4'
        WorkDir = Join-Path $toolsDir 'elasticsearch-8.11.4'
        Script = '.\bin\elasticsearch.bat'
    },
    @{
        Name = 'Kibana 8.11.4'
        WorkDir = Join-Path $toolsDir 'kibana-8.11.4'
        Script = '.\bin\kibana.bat'
    },
    @{
        Name = 'Canal Deployer 1.1.8'
        WorkDir = Join-Path $canalDir 'canal.deployer-1.1.8'
        Script = '.\startup.bat'
    },
    @{
        Name = 'Canal Adapter 1.1.8'
        WorkDir = Join-Path $canalDir 'canal.adapter-1.1.8'
        Script = '.\startup.cmd'
    }
)

if (-not (Test-Path -LiteralPath $pwsh)) {
    throw "pwsh not found: $pwsh"
}

foreach ($target in $targets) {
    $resolvedScript = Join-Path $target.WorkDir ($target.Script -replace '^[.][\\/]', '')
    if (-not (Test-Path -LiteralPath $resolvedScript)) {
        throw "launcher not found: $resolvedScript"
    }
}

foreach ($target in $targets) {
    Start-Process `
        -FilePath $pwsh `
        -WorkingDirectory $target.WorkDir `
        -ArgumentList @(
            '-NoExit',
            '-Command',
            "& { Set-Location -LiteralPath '$($target.WorkDir)'; & '$($target.Script)' }"
        ) `
        -WindowStyle Normal
}

Write-Host 'Startup commands have been launched in separate PowerShell windows.'
