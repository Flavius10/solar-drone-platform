from roboflow import Roboflow

rf = Roboflow(api_key="ezPUvW1d1bNUQyUGGhft")
project = rf.workspace("flavius-a").project("solar-panel-detection-a5bec-q16hh")
version = project.version(1)
dataset = version.download("yolov11")
