param(
    [string]$BaseUrl = "http://localhost:8081",
    [string]$Keyword = "yonex"
)

$ErrorActionPreference = "Stop"

function Hit-Api([string]$url) {
    Write-Output "--- GET $url"
    try {
        Invoke-RestMethod -Method Get -Uri $url -TimeoutSec 30 | ConvertTo-Json -Depth 8
    } catch {
        Write-Output $_.Exception.Message
        if ($_.ErrorDetails.Message) {
            Write-Output $_.ErrorDetails.Message
        }
    }
}

$normalizedBase = $BaseUrl.TrimEnd('/')
if ($normalizedBase.ToLower().EndsWith('/api')) {
    $apiPrefix = $normalizedBase
} else {
    $apiPrefix = "$normalizedBase/api"
}

Write-Output "[Phase 4] Verify lexical search"
Hit-Api "$apiPrefix/search/products?keyword=$Keyword&page=0&size=5"

Write-Output "[Phase 4] Verify lexical + sort"
Hit-Api "$apiPrefix/search/products?keyword=$Keyword&sortBy=createdAt&sortDir=desc&page=0&size=5"

Write-Output "[Phase 4] Verify semantic hybrid"
Hit-Api "$apiPrefix/search/products?keyword=$Keyword&useSemantic=true&page=0&size=5"

Write-Output "[Phase 4] Verify suggestions"
Hit-Api "$apiPrefix/search/products/suggestions?query=$Keyword&size=5"

Write-Output "Phase 4 verification completed."
