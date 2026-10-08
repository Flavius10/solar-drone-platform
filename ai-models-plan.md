# AI Models — Plan & Datasets

Status as of 2026-09-05. Backlog items #84-88 from the project task list.

## Model 1 — Defect detection with bounding boxes (RGB) — #84

Datasets found:
- `gao-shou-zheng-b6xqc/solar-panel-0swal` — 2,280 images. Classes: crack, normal, cover, dust. Pretrained model available (mAP@50 74.9%).
  https://universe.roboflow.com/gao-shou-zheng-b6xqc/solar-panel-0swal
- `yolo-ruk3b/solar-panel-2dgpy` — 1,428 images. Classes: Broken Glass, Diode Failure, Hot Spots, Obscured, PID Effect. CC BY 4.0, updated recently.
  https://universe.roboflow.com/yolo-ruk3b/solar-panel-2dgpy

## Model 2 — Thermal heatmap + hotspot pinpointing — #85

Datasets found:
- `liam-btmjc/dji123-pholl` — 3,038 images from a DJI drone (some thermal). Classes: crack, diode, hotspot, PID, shelter.
  https://universe.roboflow.com/liam-btmjc/dji123-pholl
- `hotspots-0n0e6/solar-panel-defects-mqw9g` — 5,769 images, single class (hotspot-bypass), mix of RGB and FLIR thermal. Extra volume source.
  https://universe.roboflow.com/hotspots-0n0e6/solar-panel-defects-mqw9g

## Model 3 — Dirt / obstructions, multi-class — #86

Dataset found:
- `insigteye-workspace/solar-6z0ye` — 1,523 aerial RGB images. Classes: clean, Mid-Soiled, Soiled. Pretrained YOLOv11 model (mAP@50 69%).
  https://universe.roboflow.com/insigteye-workspace/solar-6z0ye

Gap: no dataset found with a separate class for bird droppings specifically — only soiling severity levels, not soiling type. Needs either another dataset or manual labeling for that subclass.

## Model 4 — Degradation prediction over history (time-series) — #88

No dataset — needs real accumulated inspection history from live farms, which doesn't exist yet. Deferred until the app has been in use long enough to collect it.

## #87 — Repair cost lookup table

Not AI. Plain backend feature (defect type -> estimated cost table). No dataset dependency.
