from roboflow import Roboflow
rf = Roboflow(api_key="ezPUvW1d1bNUQyUGGhft")
project = rf.workspace("insigteye-workspace").project("solar-6z0ye")
version = project.version(6)
dataset = version.download("yolov11")