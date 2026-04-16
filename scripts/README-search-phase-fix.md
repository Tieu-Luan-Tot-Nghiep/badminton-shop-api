# Search Error 500 ([search_phase_execution_exception] all shards failed) - 4 Phase Runbook

This runbook is for dev environment where you can recreate index safely.

## Phase 1 - Diagnose

Run:

```powershell
./scripts/check-es-search.ps1 -ElasticsearchUris "http://your-es:9200" -ApiKey "<api-key>" -IndexName products -Keyword yonex -UseSemantic
```

What to inspect:
- `_mapping` mismatch for fields used by backend query (`brandName`, `categoryName`, `createdAt`, `basePrice`, `my_vector`, `clip_image_vector`)
- query failures in lexical/aggs/sort/knn probes and exact `root_cause`

## Phase 2 - Fix mapping/index safely

Run:

```powershell
./scripts/migrate-products-index.ps1 -ElasticsearchUris "http://your-es:9200" -ApiKey "<api-key>" -AliasName products -TargetIndex products_v2
```

Then rebuild from DB side:
- `POST /api/search/products/reindex`

## Phase 3 - Backend hardening (already in code)

`ProductSearchServiceImpl` has staged fallback behavior:
1. primary query
2. fallback lexical (if semantic enabled and failed)
3. degraded mode (without aggs/sort) to avoid hard 500 on mapping mismatch

Also logs root-cause chain for easier diagnosis.

## Phase 4 - End-to-end verify

Run:

```powershell
./scripts/verify-search-phase4.ps1 -BaseUrl "http://localhost:8081" -Keyword yonex
```

Expected:
- lexical endpoint returns 200
- semantic endpoint returns 200 (or lexical fallback data if semantic mapping still inconsistent)
- no unexpected 500 from `/api/search/products`
