# instalar_task.ps1
# Execute como Administrador: powershell -ExecutionPolicy Bypass -File instalar_task.ps1

$pythonExe  = (Get-Command python -ErrorAction SilentlyContinue)?.Source
if (-not $pythonExe) {
    $pythonExe = "$env:LOCALAPPDATA\Programs\Python\Python312\python.exe"
}
if (-not (Test-Path $pythonExe)) {
    Write-Error "Python não encontrado. Instale o Python e tente novamente."
    exit 1
}

$projetoDir = "D:\LOVE CLASS"
$script     = "$projetoDir\sprint_loop.py"

# Ação: roda o script Python
$action = New-ScheduledTaskAction `
    -Execute $pythonExe `
    -Argument "`"$script`"" `
    -WorkingDirectory $projetoDir

# Trigger: uma vez (agora) + repete a cada 1 hora para sempre
$trigger = New-ScheduledTaskTrigger `
    -Once `
    -At (Get-Date) `
    -RepetitionInterval  (New-TimeSpan -Hours 1) `
    -RepetitionDuration  ([TimeSpan]::MaxValue)

# Configurações: roda mesmo sem usuário logado, prioridade alta
$settings = New-ScheduledTaskSettingsSet `
    -ExecutionTimeLimit (New-TimeSpan -Minutes 10) `
    -RestartCount 3 `
    -RestartInterval (New-TimeSpan -Minutes 5) `
    -StartWhenAvailable `
    -WakeToRun

Register-ScheduledTask `
    -TaskName    "SPRINT Loop Horário" `
    -Action      $action `
    -Trigger     $trigger `
    -Settings    $settings `
    -RunLevel    Highest `
    -Force

Write-Host ""
Write-Host "✅ Task agendada com sucesso!"
Write-Host "   Nome:      SPRINT Loop Horário"
Write-Host "   Executa:   a cada 1 hora"
Write-Host "   Script:    $script"
Write-Host "   Python:    $pythonExe"
Write-Host ""
Write-Host "▶️  Para testar agora:"
Write-Host "   Start-ScheduledTask -TaskName 'SPRINT Loop Horário'"
Write-Host ""
Write-Host "▶️  Para ver o log:"
Write-Host "   Get-Content '$projetoDir\.sprint\loop.log' -Wait"
