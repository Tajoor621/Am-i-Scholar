#!/usr/bin/env python3
"""Generate deterministic PWA and Android launcher assets from the selected logo.
Source: brand/icon-master.webp (cropped from the user-approved Am I Scholar logo).
Requires Pillow. Do not modify existing project data or research code.
"""
from pathlib import Path
from PIL import Image
import shutil

ROOT = Path(__file__).resolve().parents[1]
master = Image.open(ROOT / "brand/icon-master.webp").convert("RGB")

def output(path, size, inset=0):
    dest = ROOT / path
    dest.parent.mkdir(parents=True, exist_ok=True)
    image = Image.new("RGB", (size, size), "#ffffff")
    area = size - 2 * round(size * inset)
    inner = master.resize((area, area), Image.Resampling.LANCZOS)
    image.paste(inner, ((size - area) // 2, (size - area) // 2))
    image.save(dest, "PNG", optimize=True)

for size in (16, 32, 48, 64, 96, 128, 144, 152, 180, 192, 256, 384, 512):
    output(f"brand/icon-{size}.png", size)
for size in (192, 512):
    output(f"brand/icon-maskable-{size}.png", size, inset=0.18)
output("brand/apple-touch-icon.png", 180)
density = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
for dpi, size in density.items():
    output(f"app/src/main/res/mipmap-{dpi}/ic_launcher.png", size, inset=0.06)
    output(f"app/src/main/res/mipmap-{dpi}/ic_launcher_round.png", size, inset=0.18)
# The source WebP is the Android adaptive foreground, with XML inset for masking.
for file in (ROOT / "brand").glob("*.png"):
    target = ROOT / "docs/brand" / file.name
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(file, target)
print("Generated browser, PWA, iOS, and Android icons from the approved logo.")
