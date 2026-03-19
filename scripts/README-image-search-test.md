# Image Search E2E Test Script

This script calls the Spring Boot endpoint:

- `POST /api/search/products/by-image`

## Prerequisites

1. CLIP service is running (local or VPS):
	- Local: `http://127.0.0.1:8001/embed/image`
	- VPS: `http://<VPS_IP>:8001/embed/image`
2. Spring Boot app is running at `http://127.0.0.1:8080`
3. Environment variable is set before starting Spring Boot:

```powershell
$env:CLIP_SERVICE_URL = "http://<VPS_IP>:8001/embed/image"
```

4. Health check CLIP service:

```powershell
Invoke-RestMethod -Uri "http://<VPS_IP>:8001/health" -Method GET
```

## Run

```powershell
.\scripts\test-image-search.ps1 -ImagePath "D:\path\to\query-image.jpg"
```

Optional flags:

```powershell
.\scripts\test-image-search.ps1 -ImagePath "D:\path\to\query-image.jpg" -Page 0 -Size 12 -ActiveOnly $true -Pretty
```

## Expected output

- Pagination info (`totalElements`, `totalPages`)
- Top 5 matched products
- Full JSON response if `-Pretty` is provided
