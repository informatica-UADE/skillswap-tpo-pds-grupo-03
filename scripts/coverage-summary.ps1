param(
    [string]$Report = "$PSScriptRoot/../target/site/jacoco/jacoco.xml",
    [string]$Output = "$PSScriptRoot/../target/coverage-summary.json"
)

$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $Report)) {
    throw 'Falta el reporte JaCoCo. Ejecutar mvn -Pcoverage clean verify primero.'
}
[xml]$coverage = Get-Content -LiteralPath $Report -Raw
$results = foreach ($area in @('domain', 'application')) {
    $packages = @($coverage.report.package | Where-Object {
        $_.name -like "ar/edu/uade/skillswap/$area/*"
    })
    if ($packages.Count -eq 0) { throw "El reporte no contiene $area." }
    $metrics = @{}
    foreach ($metric in @('LINE', 'BRANCH')) {
        $counters = $packages | ForEach-Object { $_.counter | Where-Object type -EQ $metric }
        $covered = [int](($counters | Measure-Object -Property covered -Sum).Sum)
        $missed = [int](($counters | Measure-Object -Property missed -Sum).Sum)
        $total = $covered + $missed
        $metrics[$metric] = [ordered]@{
            covered = $covered
            missed = $missed
            total = $total
            percent = if ($total -eq 0) { 100 } else { [Math]::Round(100 * $covered / $total, 2) }
        }
    }
    if ($metrics.LINE.total -eq 0 -or $metrics.LINE.covered / $metrics.LINE.total -lt 0.80) {
        throw "$area no alcanza el 80% de lineas."
    }
    [ordered]@{ area = $area; line = $metrics.LINE; branch = $metrics.BRANCH }
}
$results | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $Output -Encoding UTF8
$results | ForEach-Object {
    Write-Output ("{0}: lineas {1}/{2} ({3}%), ramas {4}/{5} ({6}%)" -f
        $_.area, $_.line.covered, $_.line.total, $_.line.percent,
        $_.branch.covered, $_.branch.total, $_.branch.percent)
}
