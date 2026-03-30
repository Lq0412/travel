param(
    [string]$MilvusBase = "http://127.0.0.1:19530",
    [string]$Collection = "travel_knowledge",
    [int]$Limit = 200
)

$Limit = [Math]::Max(1, [Math]::Min($Limit, 5000))

$queryBody = @{
    collectionName = $Collection
    filter = "id >= 0"
    limit = $Limit
    outputFields = @("id", "title", "source")
} | ConvertTo-Json -Depth 6

Write-Output "[1/2] Query entities by filter: id >= 0"
$queryResp = Invoke-WebRequest -Method Post -Uri "$MilvusBase/v2/vectordb/entities/query" -ContentType "application/json" -Body $queryBody -SkipHttpErrorCheck
Write-Output "STATUS=$($queryResp.StatusCode)"

if ($queryResp.StatusCode -ne 200) {
    Write-Output $queryResp.Content
    exit 1
}

$queryJson = $queryResp.Content | ConvertFrom-Json
if ($queryJson.code -ne 0) {
    Write-Output "Milvus code=$($queryJson.code), message=$($queryJson.message)"
    exit 1
}

$rows = @($queryJson.data)
$count = $rows.Count
$sample = $rows | Select-Object -First 5

Write-Output "queryCount=$count"
Write-Output "truncated=$($count -ge $Limit)"
Write-Output "sampleRows="
$sample | ConvertTo-Json -Depth 6

Write-Output "[2/2] Read get_stats for reference"
$statsBody = @{ collectionName = $Collection } | ConvertTo-Json
$statsResp = Invoke-WebRequest -Method Post -Uri "$MilvusBase/v2/vectordb/collections/get_stats" -ContentType "application/json" -Body $statsBody -SkipHttpErrorCheck
Write-Output "STATUS=$($statsResp.StatusCode)"
Write-Output $statsResp.Content
