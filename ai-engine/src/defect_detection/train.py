from ultralytics import YOLO

model = YOLO("yolo11s.pt")

model.train(
    data="Solar-Panel-Detection-1/data.yaml",
    epochs=150,
    patience=25,
    imgsz=640,
    batch=16,
    device="cpu",
    workers=11,
    cos_lr=True,
    project="../../models/defect_detection",
    name="solar_defect_v1",
    plots=True,
)