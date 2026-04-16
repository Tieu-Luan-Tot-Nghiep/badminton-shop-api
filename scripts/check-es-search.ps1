param(
    [string]$ElasticsearchUris = $env:ELASTICSEARCH_URIS,
    [string]$ApiKey = $env:ELASTICSEARCH_API_KEY,
    [string]$IndexName = "products",
    [string]$Keyword = "yonex",
    [switch]$UseSemantic
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

Write-Output "[Phase 1] Cluster health"
Invoke-RestMethod -Method Get -Uri "$baseUri/_cluster/health" -Headers $headers | ConvertTo-Json -Depth 8

Write-Output "[Phase 1] Mapping of index $IndexName"
Invoke-RestMethod -Method Get -Uri "$baseUri/$IndexName/_mapping" -Headers $headers | ConvertTo-Json -Depth 16

Write-Output "[Phase 1] Field capabilities"
Invoke-RestMethod -Method Get -Uri "$baseUri/$IndexName/_field_caps?fields=name,brandName,categoryName,createdAt,basePrice,isActive,isDeleted,my_vector,clip_image_vector" -Headers $headers | ConvertTo-Json -Depth 16

Write-Output "[Phase 1] Lexical search with aggs + sort (same shape as backend)"
$lexicalBody = @"
{
  "query": {
    "bool": {
      "filter": [
        { "term": { "isDeleted": false } },
        { "term": { "isActive": true } }
      ],
      "must": [
        {
          "multi_match": {
            "query": "$Keyword",
            "fields": ["name^3", "shortDescription^2", "description", "brandName", "categoryName"],
            "fuzziness": "AUTO"
          }
        }
      ]
    }
  },
  "sort": [
    { "createdAt": { "order": "desc" } },
    { "id": { "order": "desc" } }
  ],
  "aggs": {
    "agg_brands": { "terms": { "field": "brandName", "size": 20 } },
    "agg_categories": { "terms": { "field": "categoryName", "size": 20 } },
    "agg_price_ranges": {
      "range": {
        "field": "basePrice",
        "ranges": [
          { "key": "under_500k", "to": 500000 },
          { "key": "500k_1m", "from": 500000, "to": 1000000 },
          { "key": "1m_2m", "from": 1000000, "to": 2000000 },
          { "key": "2m_5m", "from": 2000000, "to": 5000000 },
          { "key": "over_5m", "from": 5000000 }
        ]
      }
    }
  },
  "size": 5
}
"@
Invoke-RestMethod -Method Post -Uri "$baseUri/$IndexName/_search" -Headers $headers -Body $lexicalBody | ConvertTo-Json -Depth 12

if ($UseSemantic.IsPresent) {
    Write-Output "[Phase 1] KNN probe on my_vector (mapping check only)"
    $vector = (1..384 | ForEach-Object { "0.001" }) -join ","
    $knnBody = @"
{
  "query": {
    "bool": {
      "filter": [
        { "term": { "isDeleted": false } },
        { "term": { "isActive": true } }
      ]
    }
  },
  "knn": {
    "field": "my_vector",
    "query_vector": [$vector],
    "k": 5,
    "num_candidates": 50
  }
}
"@
    Invoke-RestMethod -Method Post -Uri "$baseUri/$IndexName/_search" -Headers $headers -Body $knnBody | ConvertTo-Json -Depth 12
}

Write-Output "Phase 1 diagnostic completed. If any command fails, use returned root_cause to map field mismatch."
