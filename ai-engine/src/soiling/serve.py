from fastapi import FastAPI, File, UploadFile
from ultralytics import YOLO
from PIL import Image
import io

app = FastAPI()
model = YOLO("../../../models/soiling/solar_soiling_v1/weights/best.pt")

CLASS_MAP = {0: "SOILING_MODERATE", 1: "SOILING_HEAVY", 2: "CLEAN"}

@app.post("/predict")
async def predict(file: UploadFile = File(...)):
    image_bytes = await file.read()
    image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    results = model.predict(image, conf=0.25, verbose=False)[0]

    if  len(results.boxes) == 0:
        return {"status": "CLEAN", "confidence": 0.9}

    best = max(results.boxes, key=lambda b: float(b.conf[0]))
    class_id = int(best.cls[0])
    confidence = float(best.conf[0])

    return {
        "status": CLASS_MAP.get(class_id, "CLEAN"),
        "confidence": round(confidence, 4)
    }

@app.get("/health")
async def health():
    return {"status": "ok"}
