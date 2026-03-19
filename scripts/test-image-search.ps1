param(
    [Parameter(Mandatory = $true)]
    [string]$ImagePath,

    [string]$BaseUrl = "http://127.0.0.1:8080",

    [int]$Page = 0,

    [int]$Size = 12,

    [bool]$ActiveOnly = $true,

    [switch]$Pretty
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not (Test-Path -Path $ImagePath)) {
    throw "Image file not found: $ImagePath"
}

$endpoint = "$BaseUrl/api/search/products/by-image?page=$Page&size=$Size&activeOnly=$ActiveOnly"

Write-Host "Sending image search request..."
Write-Host "Endpoint: $endpoint"
Write-Host "Image: $ImagePath"

$bytes = [System.IO.File]::ReadAllBytes((Resolve-Path $ImagePath))

$httpClient = [System.Net.Http.HttpClient]::new()
try {
    $content = [System.Net.Http.MultipartFormDataContent]::new()

    $imageContent = [System.Net.Http.ByteArrayContent]::new($bytes)
    $imageContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse("application/octet-stream")

    $fileName = [System.IO.Path]::GetFileName($ImagePath)
    $content.Add($imageContent, "image", $fileName)

    $response = $httpClient.PostAsync($endpoint, $content).GetAwaiter().GetResult()
    $rawBody = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()

    if (-not $response.IsSuccessStatusCode) {
        Write-Host "Request failed with status $([int]$response.StatusCode) $($response.ReasonPhrase)" -ForegroundColor Red
        Write-Host $rawBody
        exit 1
    }

    $json = $rawBody | ConvertFrom-Json

    Write-Host ""
    Write-Host "Search succeeded." -ForegroundColor Green
    Write-Host "Page: $($json.page) / Size: $($json.size)"
    Write-Host "Total elements: $($json.totalElements)"
    Write-Host "Total pages: $($json.totalPages)"
    Write-Host "Last page: $($json.last)"

    if ($json.content -and $json.content.Count -gt 0) {
        Write-Host ""
        Write-Host "Top results:"
        $idx = 1
        foreach ($item in $json.content) {
            Write-Host ("[{0}] id={1}, name={2}, price={3}, brand={4}, category={5}" -f $idx, $item.id, $item.name, $item.basePrice, $item.brandName, $item.categoryName)
            $idx++
            if ($idx -gt 5) { break }
        }
    } else {
        Write-Host "No results returned."
    }

    if ($Pretty) {
        Write-Host ""
        Write-Host "Full JSON response:"
        $json | ConvertTo-Json -Depth 10
    }
}
finally {
    $httpClient.Dispose()
}
