# -*- coding: utf-8 -*-
"""生成应用图标（卡片 + NFC 波纹）"""
import os
from PIL import Image, ImageDraw

BASE = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'res')
SIZES = [('mipmap-mdpi', 48), ('mipmap-hdpi', 72), ('mipmap-xhdpi', 96),
         ('mipmap-xxhdpi', 144), ('mipmap-xxxhdpi', 192)]


def draw(size, path):
    s = size
    img = Image.new('RGBA', (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, s - 1, s - 1], radius=int(s * 0.22), fill=(11, 114, 231, 255))
    # 卡片
    cx, cy = int(s * 0.10), int(s * 0.32)
    cw, ch = int(s * 0.46), int(s * 0.36)
    d.rounded_rectangle([cx, cy, cx + cw, cy + ch], radius=int(s * 0.06), fill=(255, 255, 255, 255))
    d.rounded_rectangle([cx, cy + int(ch * 0.6), cx + cw, cy + ch], radius=int(s * 0.03),
                        fill=(255, 214, 102, 255))
    # NFC 波纹
    center = (int(s * 0.58), cy + ch // 2)
    for i in range(3):
        r = int(s * (0.10 + i * 0.085))
        d.arc([center[0] - r, center[1] - r, center[0] + r, center[1] + r],
              start=-52, end=52, fill=(255, 255, 255, 255), width=max(2, int(s * 0.037)))
    img.save(path)


for folder, px in SIZES:
    out_dir = os.path.join(BASE, folder)
    os.makedirs(out_dir, exist_ok=True)
    draw(px, os.path.join(out_dir, 'ic_launcher.png'))
print('icons done')
