from __future__ import annotations

import io
from typing import Any

import torch
from fastapi import FastAPI, HTTPException, Request
from PIL import Image
from transformers import CLIPModel, CLIPProcessor

MODEL_ID = "openai/clip-vit-base-patch32"
DEVICE = "cuda" if torch.cuda.is_available() else "cpu"

app = FastAPI(title="CLIP Local Embedding Service", version="1.0.0")

_model: CLIPModel | None = None
_processor: CLIPProcessor | None = None


@app.on_event("startup")
def startup() -> None:
    global _model, _processor
    _processor = CLIPProcessor.from_pretrained(MODEL_ID)
    _model = CLIPModel.from_pretrained(MODEL_ID).to(DEVICE)
    _model.eval()


@app.get("/health")
def health() -> dict[str, Any]:
    ready = _model is not None and _processor is not None
    return {
        "status": "ok" if ready else "not_ready",
        "model": MODEL_ID,
        "device": DEVICE,
        "ready": ready,
    }


@app.post("/embed/image")
async def embed_image(request: Request) -> dict[str, list[float]]:
    if _model is None or _processor is None:
        raise HTTPException(status_code=503, detail="Model is not loaded yet")

    image_bytes = await request.body()
    if not image_bytes:
        raise HTTPException(status_code=400, detail="Request body is empty")

    try:
        image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    except Exception as exc:
        raise HTTPException(status_code=400, detail=f"Invalid image payload: {exc}") from exc

    try:
        inputs = _processor(images=image, return_tensors="pt")
        pixel_values = inputs["pixel_values"].to(DEVICE)

        with torch.no_grad():
            image_features = _model.get_image_features(pixel_values=pixel_values)
            image_features = image_features / image_features.norm(dim=-1, keepdim=True)

        vector = image_features.squeeze(0).detach().cpu().tolist()
        return {"vector": [float(v) for v in vector]}
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Failed to compute CLIP embedding: {exc}") from exc
