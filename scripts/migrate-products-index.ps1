param(
    [string]$ElasticsearchUris = $env:ELASTICSEARCH_URIS,
    [string]$ApiKey = $env:ELASTICSEARCH_API_KEY,
    [string]$AliasName = "products",
    [string]$TargetIndex = "products_v2",
    [switch]$DeleteOldIndices
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($ElasticsearchUris)) {
    throw "ELASTICSEARCH_URIS is empty. Pass -ElasticsearchUris or set env variable."
}
if ([string]::IsNullOrWhiteSpace($ApiKey)) {
    throw "ELASTICSEARCH_API_KEY is empty. Pass -ApiKey or set env variable."
}

$baseUri = ($ElasticsearchUris -split ",")[0].Trim().TrimEnd("/")
$headers = @{ "Authorization" = "ApiKey $ApiKey"; "Content-Type" = "application/json" }

Write-Output "[Phase 2] Create target index with expected mapping"
$createBody = @"
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0
  },
  "mappings": {
    "properties": {
      "id": { "type": "long" },
      "name": { "type": "text", "analyzer": "standard" },
      "slug": { "type": "keyword" },
      "shortDescription": { "type": "text" },
      "description": { "type": "text" },
      "thumbnailUrl": { "type": "keyword" },
      "basePrice": { "type": "double" },
      "brandName": { "type": "keyword" },
      "brandSlug": { "type": "keyword" },
      "categoryName": { "type": "keyword" },
      "categorySlug": { "type": "keyword" },
      "isActive": { "type": "boolean" },
      "isDeleted": { "type": "boolean" },
      "my_vector": { "type": "dense_vector", "dims": 384, "index": true, "similarity": "cosine" },
      "clip_image_vector": { "type": "dense_vector", "dims": 512, "index": true, "similarity": "cosine" },
      "createdAt": { "type": "date" },
      "updatedAt": { "type": "date" }
    }
  }
}
"@

try {
    Invoke-RestMethod -Method Put -Uri "$baseUri/$TargetIndex" -Headers $headers -Body $createBody | Out-Null
    Write-Output "Created $TargetIndex"
} catch {
    if ($_.Exception.Message -notmatch "resource_already_exists_exception") {
        throw
    }
    Write-Output "$TargetIndex already exists, continue"
}

Write-Output "[Phase 2] Reindex old data from alias/index $AliasName to $TargetIndex"
$reindexBody = @"
{
  "source": { "index": "$AliasName" },
  "dest": { "index": "$TargetIndex" }
}
"@
Invoke-RestMethod -Method Post -Uri "$baseUri/_reindex?wait_for_completion=true" -Headers $headers -Body $reindexBody | ConvertTo-Json -Depth 10

Write-Output "[Phase 2] Switch alias $AliasName -> $TargetIndex"
$existingIndices = @()
try {
    $aliasInfo = Invoke-RestMethod -Method Get -Uri "$baseUri/_alias/$AliasName" -Headers $headers
    $existingIndices = $aliasInfo.PSObject.Properties.Name
} catch {
    Write-Output "Alias $AliasName not found yet, will create new alias mapping"
}

$actions = @()
foreach ($idx in $existingIndices) {
    $actions += @{ remove = @{ index = $idx; alias = $AliasName } }
}
$actions += @{ add = @{ index = $TargetIndex; alias = $AliasName } }

$aliasPayload = @{ actions = $actions } | ConvertTo-Json -Depth 8
Invoke-RestMethod -Method Post -Uri "$baseUri/_aliases" -Headers $headers -Body $aliasPayload | Out-Null

Write-Output "[Phase 2] Verify alias mapping"
Invoke-RestMethod -Method Get -Uri "$baseUri/_alias/$AliasName" -Headers $headers | ConvertTo-Json -Depth 10

if ($DeleteOldIndices.IsPresent -and $existingIndices.Count -gt 0) {
    Write-Output "Deleting old indices: $($existingIndices -join ', ')"
    foreach ($idx in $existingIndices) {
        if ($idx -ne $TargetIndex) {
            Invoke-RestMethod -Method Delete -Uri "$baseUri/$idx" -Headers $headers | Out-Null
        }
    }
}

Write-Output "Phase 2 completed. Next call POST /api/search/products/reindex to rebuild vectors from DB side."
