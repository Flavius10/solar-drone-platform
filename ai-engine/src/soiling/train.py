from ultralytics import YOLO

model = YOLO("yolo11s.pt")

results = model.train(
    data="../../datasets/soiling/Solar-6/data.yaml",
    epochs=200,
    patience=30,
    imgsz=640,
    batch=16,
    device="cpu",
    workers=11,
    cos_lr=True,
    project="../../models/soiling",
    name="solar_soiling_v1",
    plots=True
)

print("Model antrenat, greutati salvate in models/soiling/solar_soiling_v1/weights/best.pt")