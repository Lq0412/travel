Param(
  [string]$RootDir = "src"
)

function Get-HeaderForJava([string]$filePath, [string]$relativePath) {
  $date = (Get-Date).ToString('yyyy-MM-dd')
  return @(
    '/*',
    " * File: $relativePath",
    ' * Description: Auto-generated file header comment.',
    " * Created: $date",
    ' */'
  )
}

function Get-HeaderForYaml([string]$filePath, [string]$relativePath) {
  $date = (Get-Date).ToString('yyyy-MM-dd')
  return @(
    "# File: $relativePath",
    '# Description: Auto-generated file header comment.',
    "# Created: $date"
  )
}

function Get-HeaderForXml([string]$filePath, [string]$relativePath) {
  $date = (Get-Date).ToString('yyyy-MM-dd')
  return @(
    '<!--',
    "  File: $relativePath",
    '  Description: Auto-generated file header comment.',
    "  Created: $date",
    '-->'
  )
}

function Needs-Header([string[]]$lines, [string]$ext) {
  # Find first non-empty, non-whitespace line robustly
  $firstIdx = $null
  for ($i = 0; $i -lt $lines.Count; $i++) {
    if ($lines[$i] -and $lines[$i].Trim() -ne '') { $firstIdx = $i; break }
  }
  if ($null -eq $firstIdx) { return $true }
  $firstLine = $lines[$firstIdx].TrimStart()
  switch ($ext.ToLower()) {
    '.java' { return -not ($firstLine.StartsWith('/*') -or $firstLine.StartsWith('//')) }
    { $_ -in @('.yml', '.yaml') } { return -not ($firstLine.StartsWith('#')) }
    '.xml' {
      if ($firstLine.StartsWith('<?xml')) {
        # Look for next non-empty line after XML declaration
        for ($i = $firstIdx + 1; $i -lt $lines.Count; $i++) {
          if ($lines[$i] -and $lines[$i].Trim() -ne '') {
            $nextLine = $lines[$i].TrimStart()
            return -not ($nextLine.StartsWith('<!--'))
          }
        }
        return $true
      }
      return -not ($firstLine.StartsWith('<!--'))
    }
    default { return $false }
  }
}

function Insert-Header([string]$filePath, [string[]]$lines, [string[]]$headerLines, [string]$ext) {
  $eol = "`r`n"  # preserve CRLF on Windows
  switch ($ext.ToLower()) {
    '.xml' {
      # If XML declaration exists, insert after it (and after any immediate blank lines)
      $idx = 0
      if ($lines.Count -gt 0 -and $lines[0] -and $lines[0].TrimStart().StartsWith('<?xml')) {
        $idx = 1
        while ($idx -lt $lines.Count -and ($lines[$idx] -eq $null -or $lines[$idx].Trim() -eq '')) { $idx++ }
      }
      $before = @()
      if ($idx -gt 0) { $before = $lines[0..($idx-1)] }
      $after = @()
      if ($idx -lt $lines.Count) { $after = $lines[$idx..($lines.Count-1)] }
      $newContent = @()
      $newContent += $before
      $newContent += $headerLines
      $newContent += ''
      $newContent += $after
      [IO.File]::WriteAllText($filePath, ($newContent -join $eol), (New-Object System.Text.UTF8Encoding($false)))
    }
    default {
      $newContent = @()
      $newContent += $headerLines
      $newContent += ''
      $newContent += $lines
      [IO.File]::WriteAllText($filePath, ($newContent -join $eol), (New-Object System.Text.UTF8Encoding($false)))
    }
  }
}

function Add-Headers([string]$root) {
  $rootFull = Resolve-Path $root
  $files = Get-ChildItem -Path $rootFull -Recurse -File -Include *.java,*.yml,*.yaml,*.xml
  foreach ($file in $files) {
    $ext = [IO.Path]::GetExtension($file.FullName)
    $rel = Resolve-Path -Relative $file.FullName
    $raw = Get-Content -LiteralPath $file.FullName -Raw -Encoding UTF8
    if ($null -eq $raw) { $lines = @() } else { $lines = $raw -split "\r?\n" }

    if (-not (Needs-Header -lines $lines -ext $ext)) { continue }

    switch ($ext.ToLower()) {
      '.java' { $header = Get-HeaderForJava -filePath $file.FullName -relativePath $rel }
      { $_ -in @('.yml', '.yaml') } { $header = Get-HeaderForYaml -filePath $file.FullName -relativePath $rel }
      '.xml' { $header = Get-HeaderForXml -filePath $file.FullName -relativePath $rel }
      default { continue }
    }

    Write-Host "Adding header to: $rel"
    Insert-Header -filePath $file.FullName -lines $lines -headerLines $header -ext $ext
  }
}

Add-Headers -root $RootDir


