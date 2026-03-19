# CLIP Service Runbook (Local or VPS)

This service exposes an HTTP endpoint for CLIP image embeddings used by Spring Boot image search.

## API contract

- `POST /embed/image`
- Request body: raw image bytes (`application/octet-stream`)
- Response:

```json
{
  "vector": [0.0123, -0.0456, ...]
}
```

- `GET /health`

## Scenario A: run on another VPS (recommended for production)

### 1. Prepare VPS (Ubuntu example)

```bash
sudo apt update
sudo apt install -y python3 python3-venv python3-pip git
```

### 2. Deploy service code

```bash
git clone <your-repo-url>
cd BadmintonShop/tools/clip-local-service
python3 -m venv .venv
source .venv/bin/activate
pip install --upgrade pip
pip install -r requirements.txt
```

### 3. Run service manually (first boot)

```bash
source .venv/bin/activate
uvicorn app:app --host 0.0.0.0 --port 8001
```

First run downloads CLIP model, so startup can take time.

### 4. Open firewall/security group

- Open inbound TCP `8001` for trusted source only (IP of your Spring Boot server).
- Do not expose port `8001` publicly to all IPs.

### 5. Health check from your machine

```bash
curl http://<VPS_IP>:8001/health
```

Expected:

```json
{"status":"ok","model":"openai/clip-vit-base-patch32","device":"cpu","ready":true}
```

### 6. Configure Spring Boot to call CLIP service on VPS

Set env var before starting Spring Boot:

Linux/macOS:

```bash
export CLIP_SERVICE_URL="http://<VPS_IP>:8001/embed/image"
```

Windows PowerShell:

```powershell
$env:CLIP_SERVICE_URL = "http://<VPS_IP>:8001/embed/image"
```

## Scenario B: run on same machine as Spring Boot

Use localhost URL:

```powershell
$env:CLIP_SERVICE_URL = "http://127.0.0.1:8001/embed/image"
```

## Run as systemd service on VPS (optional but recommended)

Create `/etc/systemd/system/clip-embedding.service`:

```ini
[Unit]
Description=CLIP Embedding FastAPI Service
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu/BadmintonShop/tools/clip-local-service
ExecStart=/home/ubuntu/BadmintonShop/tools/clip-local-service/.venv/bin/python -m uvicorn app:app --host 0.0.0.0 --port 8001
Restart=always
RestartSec=3

[Install]
WantedBy=multi-user.target
```

Enable and start:

```bash
sudo systemctl daemon-reload
sudo systemctl enable clip-embedding
sudo systemctl start clip-embedding
sudo systemctl status clip-embedding
```

## Troubleshooting

- If `/health` is not ready, wait for first-time model download to complete.
- If Spring Boot returns `503 CLIP image embedding provider is not configured`, verify `CLIP_SERVICE_URL` is set in the process environment.
- If connection times out, re-check VPS firewall and cloud security group rules for port `8001`.
