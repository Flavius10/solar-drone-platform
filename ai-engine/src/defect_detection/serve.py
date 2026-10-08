from fastapi import FastAPI, File, UploadFile
from ultralytics import YOLO
from PIL import Image
import io

app = FastAPI()
model = YOLO("../../../models/defect_detection/solar_defect_v1/weights/best.pt")

CLASS_MAP = {0: "CRACK", 1: "HEALTHY"}


@app.post("/predict")
async def predict(file: UploadFile = File(...)):
    image_bytes = await file.read()
    image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    results = model.predict(image, conf=0.20, verbose=False)[0]

    if len(results.boxes) == 0:
        return {"status": "CLEAN", "confidence": 0.9, "boxes": []}

    boxes_out = []
    best_label = None
    best_conf = -1.0
    for box in results.boxes:
        class_id = int(box.cls[0])
        confidence = float(box.conf[0])
        x1, y1, x2, y2 = [float(v) for v in box.xyxy[0]]
        label = CLASS_MAP.get(class_id, "UNKNOWN")
        boxes_out.append({
            "label": label,
            "confidence": round(confidence, 4),
            "box": {"x1": x1, "y1": y1, "x2": x2, "y2": y2},
        })
        if confidence > best_conf:
            best_conf = confidence
            best_label = label

    status = "CRACK" if best_label == "CRACK" else "CLEAN"
    return {"status": status, "confidence": round(best_conf, 4), "boxes": boxes_out}


@app.get("/health")
async def health():
    return {"status": "ok"}
