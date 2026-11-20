<#
.SYNOPSIS
    Carga datos de demostración en FisioVital: fisioterapeutas, pacientes, horarios, planes y citas.

.DESCRIPTION
    Todo pasa por la API real (no escribe en la base de datos), así que respeta las reglas del negocio:
    las citas quedan en los próximos días y van cambiando de estado a medida que pasan.
    Se puede ejecutar varias veces: reutiliza las cuentas, los horarios y los planes que ya existen, y en los
    planes existentes completa las citas futuras que falten (útil cuando las anteriores ya pasaron).

    Los correos son @example.com (dominio reservado): los recordatorios de US13 no le llegan a nadie real.

.EXAMPLE
    .\scripts\seed-demo.ps1
    .\scripts\seed-demo.ps1 -BaseUrl http://localhost:8080/api/v1 -Days 21
#>
param(
    [string]$BaseUrl = 'http://localhost:8080/api/v1',
    [string]$Password = 'Demo2026*',
    [int]$Days = 14
)

$ErrorActionPreference = 'Stop'

# --- Datos de demostración --------------------------------------------------------------------------------------

$physiotherapists = @(
    @{ key = 'mariana'; fullName = 'Mariana Rojas'; email = 'mariana.rojas@example.com'; phone = '987654321'
       licenseNumber = 'DEMO-101'; specialty = 'DEPORTIVA'; yearsOfExperience = 8
       description = 'Fisioterapeuta deportiva con enfoque en readaptación de lesiones de rodilla y tobillo.'
       shifts = @(@{ start = '08:00'; end = '12:00'; duration = 60 }, @{ start = '16:00'; end = '19:00'; duration = 60 }) },
    @{ key = 'jorge'; fullName = 'Jorge Paredes'; email = 'jorge.paredes@example.com'; phone = '976543210'
       licenseNumber = 'DEMO-102'; specialty = 'COLUMNA_POSTURA'; yearsOfExperience = 12
       description = 'Especialista en dolor de espalda, cuello y corrección postural.'
       shifts = @(@{ start = '09:00'; end = '13:00'; duration = 60 }, @{ start = '15:00'; end = '18:00'; duration = 60 }) },
    @{ key = 'lucia'; fullName = 'Lucía Vargas'; email = 'lucia.vargas@example.com'; phone = '965432109'
       licenseNumber = 'DEMO-103'; specialty = 'NEUROLOGICA'; yearsOfExperience = 6
       description = 'Rehabilitación neurológica para pacientes tras un ACV o con enfermedad de Parkinson.'
       shifts = @(@{ start = '08:00'; end = '12:00'; duration = 60 }) },
    @{ key = 'diego'; fullName = 'Diego Salazar'; email = 'diego.salazar@example.com'; phone = '910987654'
       licenseNumber = 'DEMO-104'; specialty = 'POSTQUIRURGICA'; yearsOfExperience = 10
       description = 'Rehabilitación postquirúrgica de hombro y rodilla (manguito rotador, LCA, prótesis).'
       shifts = @(@{ start = '09:00'; end = '13:00'; duration = 60 }, @{ start = '15:00'; end = '19:00'; duration = 60 }) }
)

$patients = @(
    @{ key = 'carlos'; fullName = 'Carlos Mendoza'; email = 'carlos.mendoza@example.com'; phone = '954321098' },
    @{ key = 'ana'; fullName = 'Ana Torres'; email = 'ana.torres@example.com'; phone = '943210987' },
    @{ key = 'luis'; fullName = 'Luis Quispe'; email = 'luis.quispe@example.com'; phone = '932109876' },
    @{ key = 'sofia'; fullName = 'Sofía Huamán'; email = 'sofia.huaman@example.com'; phone = '921098765' }
)

# book = citas a reservar; hour = hora preferida; cancel/reschedule = una cita de ejemplo con ese cambio.
$plans = @(
    @{ patient = 'carlos'; physio = 'mariana'; diagnosis = 'Esguince de tobillo grado II'
       totalSessions = 10; frequencyPerWeek = 2; book = 4; hour = 8 },
    @{ patient = 'carlos'; physio = 'jorge'; diagnosis = 'Cervicalgia postural'
       totalSessions = 6; frequencyPerWeek = 1; book = 2; hour = 15 },
    @{ patient = 'ana'; physio = 'mariana'; diagnosis = 'Tendinopatía rotuliana'
       totalSessions = 8; frequencyPerWeek = 2; book = 3; hour = 17; cancel = $true },
    @{ patient = 'luis'; physio = 'jorge'; diagnosis = 'Lumbalgia mecánica crónica'
       totalSessions = 12; frequencyPerWeek = 3; book = 5; hour = 9; reschedule = $true },
    @{ patient = 'sofia'; physio = 'lucia'; diagnosis = 'Rehabilitación tras ACV isquémico'
       totalSessions = 20; frequencyPerWeek = 3; book = 6; hour = 10 },
    @{ patient = 'ana'; physio = 'diego'; diagnosis = 'Rehabilitación tras cirugía de manguito rotador'
       totalSessions = 10; frequencyPerWeek = 2; book = 4; hour = 11 },
    @{ patient = 'luis'; physio = 'diego'; diagnosis = 'Recuperación tras reconstrucción de LCA'
       totalSessions = 12; frequencyPerWeek = 1; book = 2; hour = 16 },
    @{ patient = 'sofia'; physio = 'jorge'; diagnosis = 'Dorsalgia por mala postura'
       totalSessions = 6; frequencyPerWeek = 1; book = 2; hour = 16 }
)

# --- Utilidades -------------------------------------------------------------------------------------------------

function Invoke-Api {
    param([string]$Method, [string]$Path, $Body = $null, [string]$Token = $null)
    $request = @{
        Method = $Method; Uri = "$BaseUrl$Path"; UseBasicParsing = $true
        Headers = @{ 'Accept-Language' = 'es-419' }; ContentType = 'application/json; charset=utf-8'
    }
    if ($Token) { $request.Headers.Authorization = "Bearer $Token" }
    if ($null -ne $Body) { $request.Body = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 5)) }
    try {
        $response = Invoke-WebRequest @request
        # Spring no declara charset: se decodifica como UTF-8 para no romper las tildes.
        $text = [Text.Encoding]::UTF8.GetString($response.RawContentStream.ToArray())
        $data = $null
        if ($text) { $data = $text | ConvertFrom-Json }
        return @{ Ok = $true; Status = [int]$response.StatusCode; Data = $data }
    } catch {
        if (-not $_.Exception.Response) { throw "No se pudo conectar con $BaseUrl. ¿Está corriendo el backend?" }
        # En Windows PowerShell 5.1 el cuerpo del error no llega en ErrorDetails: se lee del stream.
        $message = $_.ErrorDetails.Message
        if (-not $message) {
            $stream = $_.Exception.Response.GetResponseStream()
            $message = (New-Object IO.StreamReader($stream, [Text.Encoding]::UTF8)).ReadToEnd()
        }
        try { $message = ($message | ConvertFrom-Json).message } catch { }
        return @{ Ok = $false; Status = [int]$_.Exception.Response.StatusCode; Message = $message }
    }
}

function Connect-Account {
    param([string]$Kind, [hashtable]$Account)
    $body = @{}
    foreach ($field in $Account.Keys) {
        if ($field -notin 'key', 'shifts') { $body[$field] = $Account[$field] }
    }
    $body.password = $Password
    $signUp = Invoke-Api POST "/auth/sign-up/$Kind" $body
    $session = Invoke-Api POST '/auth/sign-in' @{ email = $Account.email; password = $Password }
    if (-not $session.Ok) {
        throw "No se pudo entrar como $($Account.email): $($session.Message) (registro: $($signUp.Message))"
    }
    $state = if ($signUp.Ok) { 'creada' } else { 'ya existía' }
    Write-Host ("  {0,-16} {1,-30} {2}" -f $Account.fullName, $Account.email, $state)
    return $session.Data
}

function Get-FreeSlots {
    param([long]$PhysioId, [string]$Token)
    $from = (Get-Date).ToString('yyyy-MM-dd')
    $to = (Get-Date).AddDays($Days).ToString('yyyy-MM-dd')
    $result = Invoke-Api GET "/physiotherapists/$PhysioId/slots?from=$from&to=$to" -Token $Token
    if (-not $result.Ok) { throw "No se pudieron leer los horarios: $($result.Message)" }
    return @($result.Data.slots | Where-Object { $_.status -eq 'FREE' -and [datetime]$_.startTime -gt (Get-Date).AddHours(2) })
}

# Elige un horario libre desde $After, en la primera fecha disponible y lo más cerca posible de la hora preferida.
function Select-Slot {
    param($Slots, [datetime]$After, [int]$Hour)
    $candidates = @($Slots | Where-Object { ([datetime]$_.startTime).Date -ge $After.Date })
    if ($candidates.Count -eq 0) { return $null }
    $firstDay = ($candidates | ForEach-Object { ([datetime]$_.startTime).Date } | Sort-Object | Select-Object -First 1)
    return $candidates |
        Where-Object { ([datetime]$_.startTime).Date -eq $firstDay } |
        Sort-Object { [math]::Abs(([datetime]$_.startTime).Hour - $Hour) } |
        Select-Object -First 1
}

# --- 1. Cuentas -------------------------------------------------------------------------------------------------

Write-Host "`nCuentas (contraseña de todas: $Password)" -ForegroundColor Cyan
$physioSessions = @{}
foreach ($physio in $physiotherapists) { $physioSessions[$physio.key] = Connect-Account 'physiotherapist' $physio }
$patientSessions = @{}
foreach ($patient in $patients) { $patientSessions[$patient.key] = Connect-Account 'patient' $patient }

# --- 2. Horarios publicados (US06) ------------------------------------------------------------------------------

Write-Host "`nHorarios de los próximos $Days días (lunes a sábado)" -ForegroundColor Cyan
foreach ($physio in $physiotherapists) {
    $published = 0
    for ($day = 1; $day -le $Days; $day++) {
        $date = (Get-Date).Date.AddDays($day)
        if ($date.DayOfWeek -eq 'Sunday') { continue }
        foreach ($shift in $physio.shifts) {
            # Los sábados solo se atiende en la mañana.
            if ($date.DayOfWeek -eq 'Saturday' -and [int]$shift.start.Substring(0, 2) -ge 13) { continue }
            $result = Invoke-Api POST '/physiotherapists/me/slots' @{
                date = $date.ToString('yyyy-MM-dd'); startTime = $shift.start; endTime = $shift.end
                durationMinutes = $shift.duration
            } -Token $physioSessions[$physio.key].token
            if ($result.Ok) { $published += @($result.Data).Count }
            elseif ($result.Status -ne 409) { Write-Warning "$($physio.fullName) $($date.ToString('dd/MM')): $($result.Message)" }
        }
    }
    Write-Host ("  {0,-16} {1} horarios nuevos" -f $physio.fullName, $published)
}

# --- 3. Planes y citas (US08, US09, US11, US12) -----------------------------------------------------------------

Write-Host "`nPlanes de tratamiento y citas" -ForegroundColor Cyan
foreach ($plan in $plans) {
    $token = $patientSessions[$plan.patient].token
    $physioId = $physioSessions[$plan.physio].profileId
    $patientName = ($patients | Where-Object { $_.key -eq $plan.patient }).fullName
    $physioName = ($physiotherapists | Where-Object { $_.key -eq $plan.physio }).fullName

    $created = Invoke-Api POST '/treatment-plans' @{
        physiotherapistId = $physioId; diagnosis = $plan.diagnosis
        totalSessions = $plan.totalSessions; frequencyPerWeek = $plan.frequencyPerWeek
    } -Token $token
    $existing = $false
    if ($created.Status -eq 409) {
        # El plan ya se cargó en una ejecución anterior: se reutiliza y solo se completan las citas futuras.
        $mine = Invoke-Api GET '/treatment-plans/me' -Token $token
        $current = @($mine.Data) | Where-Object { $_.physiotherapistId -eq $physioId -and $_.status -eq 'ACTIVE' } |
            Select-Object -First 1
        if (-not $current) { Write-Warning "$patientName con ${physioName}: $($created.Message)"; continue }
        $existing = $true
    } elseif (-not $created.Ok) {
        Write-Warning "$patientName con ${physioName}: $($created.Message)"; continue
    } else {
        $current = $created.Data
    }

    # Cuántas citas futuras reservadas tiene ya este plan.
    $upcoming = 0
    if ($existing) {
        $history = Invoke-Api GET '/appointments/me?page=0&size=200' -Token $token
        $upcoming = @($history.Data.appointments | Where-Object {
            $_.physiotherapistId -eq $physioId -and $_.diagnosis -eq $current.diagnosis -and
            $_.status -eq 'RESERVED' -and [datetime]$_.startTime -gt (Get-Date) }).Count
    }
    $remaining = $current.totalSessions - $current.bookedSessions
    $toBook = [math]::Min($plan.book - $upcoming, $remaining)

    $booked = @()
    if ($toBook -gt 0) {
        $slots = Get-FreeSlots $physioId $token
        $next = (Get-Date).AddDays(1)
        $gap = [math]::Max(1, [math]::Floor(7 / $plan.frequencyPerWeek))
        $attempts = 0
        while ($booked.Count -lt $toBook -and $attempts -lt 40) {
            $attempts++
            $slot = Select-Slot $slots $next $plan.hour
            if (-not $slot) { Write-Warning "Ya no hay horarios libres para $patientName con $physioName"; break }
            $slots = @($slots | Where-Object { $_.id -ne $slot.id })
            $result = Invoke-Api POST "/treatment-plans/$($current.id)/sessions" @{ slotId = $slot.id } -Token $token
            if (-not $result.Ok) {
                # Choque de horario del paciente o frecuencia semanal llena: se prueba el día siguiente.
                if ($result.Status -in 409, 422) { $next = ([datetime]$slot.startTime).Date.AddDays(1); continue }
                Write-Warning "${patientName}: $($result.Message)"; break
            }
            $booked += $result.Data
            $next = ([datetime]$slot.startTime).Date.AddDays($gap)
        }
    }

    $changes = ''
    if ($booked.Count -gt 0 -and $plan.cancel) {
        $last = $booked[-1]
        $result = Invoke-Api PATCH "/sessions/$($last.id)/cancel" -Token $token
        if ($result.Ok) { $changes = ", canceló la del $(([datetime]$last.startTime).ToString('dd/MM HH:mm'))" }
    }
    if ($booked.Count -gt 0 -and $plan.reschedule) {
        $last = $booked[-1]
        $newSlot = Get-FreeSlots $physioId $token |
            Where-Object { ([datetime]$_.startTime).Date -eq ([datetime]$last.startTime).Date.AddDays(1) } |
            Select-Object -First 1
        if ($newSlot) {
            $result = Invoke-Api PATCH "/sessions/$($last.id)/reschedule" @{ newSlotId = $newSlot.id } -Token $token
            if ($result.Ok) { $changes = ", reprogramó una al $(([datetime]$newSlot.startTime).ToString('dd/MM HH:mm'))" }
        }
    }
    $origin = if ($existing) { "plan existente, $upcoming futuras previas" } else { 'plan nuevo' }
    Write-Host ("  {0,-16} -> {1,-14} {2}: {3} citas nuevas ({4}){5}" -f $patientName, $physioName,
        $plan.diagnosis, $booked.Count, $origin, $changes)
}

Write-Host "`nListo. Entra a la Web App con cualquiera de los correos de arriba." -ForegroundColor Green
