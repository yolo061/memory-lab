# -*- coding: utf-8 -*-
"""把拾光纪 logo 图片处理成安卓自适应图标前景层 + 预览图"""
from PIL import Image, ImageDraw, ImageFilter
import numpy as np
import os

SRC = r"C:\Users\hilary\.zcode\cli\image-cache\sess_2d3c865b-9ed8-49c5-bcce-0f0ec0ee0696\image-b29fb5c3c32e8017e44ac605191467f1.png"
RES = r"F:\memory blank\app\src\main\res"

img = Image.open(SRC).convert("RGB")
W, H = img.size
arr = np.asarray(img).astype(int)

# ---- 1. 采样奶油底色（四角平均）----
corners = np.concatenate([
    arr[:20, :20].reshape(-1, 3), arr[:20, -20:].reshape(-1, 3),
    arr[-20:, :20].reshape(-1, 3), arr[-20:, -20:].reshape(-1, 3)])
bg = corners.mean(axis=0)
print("bg color =", "#%02X%02X%02X" % tuple(int(v) for v in bg))

# ---- 2. 非背景像素的行剖面：区分圆形标志与底部文字 ----
diff = np.abs(arr - bg).sum(axis=2)          # H×W
mask = diff > 55                              # 与底色差异明显的像素
row = mask.sum(axis=1)
th = W * 0.03
rows_on = row > th
# 找连续 band
bands, start = [], None
for y, on in enumerate(rows_on):
    if on and start is None: start = y
    if not on and start is not None: bands.append((start, y - 1)); start = None
if start is not None: bands.append((start, len(rows_on) - 1))
print("bands:", bands)
main = max(bands, key=lambda b: (b[1] - b[0]))
y0, y1 = main

# ---- 2.5 抹掉底部文字区（用奶油底色填充），保证裁剪框不带文字 ----
clean = img.copy()
ImageDraw.Draw(clean).rectangle([0, y1 + 4, W, H], fill=tuple(int(v) for v in bg))
arr2 = np.asarray(clean).astype(int)
diff2 = np.abs(arr2 - bg).sum(axis=2)
mask2 = diff2 > 55

band = mask2[y0:y1 + 1]
col = band.sum(axis=0)
xs = np.where(col > 2)[0]
x0, x1 = int(xs.min()), int(xs.max())
cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
d = max(x1 - x0, y1 - y0)
print(f"circle bbox=({x0},{y0})-({x1},{y1}) center=({cx:.0f},{cy:.0f}) d={d}")

# ---- 3. 生成前景层：1024 画布，圆形标志直径 = 画布 62%（安全区内）----
CANVAS = 1024
target_d = int(CANVAS * 0.62)
crop_side = int(d * 1.16)                     # 带一点奶油边
ccx, ccy = int(cx), int(cy)
half = crop_side // 2
# 裁剪框可能出界：先把图垫在奶油色画布上再裁
pad = Image.new("RGB", (W + 2 * crop_side, H + 2 * crop_side), tuple(int(v) for v in bg))
pad.paste(clean, (crop_side, crop_side))
crop = pad.crop((ccx - half + crop_side, ccy - half + crop_side,
                 ccx - half + crop_side + crop_side, ccy - half + crop_side + crop_side))
fg = crop.resize((target_d, target_d), Image.LANCZOS)

canvas = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
canvas.paste(fg, ((CANVAS - target_d) // 2, (CANVAS - target_d) // 2))

# 圆形羽化裁切：半径限制在贴图范围内，避免露出透明底的黑色
amask = Image.new("L", (CANVAS, CANVAS), 0)
ad = ImageDraw.Draw(amask)
r = target_d // 2 - 2
ad.ellipse([CANVAS // 2 - r, CANVAS // 2 - r, CANVAS // 2 + r, CANVAS // 2 + r], fill=255)
amask = amask.filter(ImageFilter.GaussianBlur(2))
canvas.putalpha(amask)

# ---- 4. 输出各密度前景层 ----
sizes = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
for dpi, s in sizes.items():
    out = os.path.join(RES, f"mipmap-{dpi}")
    os.makedirs(out, exist_ok=True)
    canvas.resize((s, s), Image.LANCZOS).save(os.path.join(out, "ic_launcher_fg.png"))
    print("saved", dpi, s)

# ---- 5. 预览：模拟 OPPO 圆形 / 小米方圆裁切 ----
bgc = tuple(int(v) for v in bg)
def preview(size, mask_circle):
    p = Image.new("RGB", (size, size), bgc)
    layer = canvas.resize((size, size), Image.LANCZOS)
    p.paste(layer, (0, 0), layer)
    if mask_circle:
        m = Image.new("L", (size, size), 0)
        ImageDraw.Draw(m).ellipse([0, 0, size, size], fill=255)
        out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        out.paste(p, (0, 0), m)
        bg2 = Image.new("RGB", (size, size), (238, 228, 214))
        bg2.paste(out, (0, 0), out)
        return bg2
    return p

pv = Image.new("RGB", (190 * 2 + 30, 190), (238, 228, 214))
pv.paste(preview(190, True), (10, 0))
pv.paste(preview(190, False), (200, 0))
pv = pv.resize((pv.width * 2, pv.height * 2), Image.LANCZOS)
pv.save(r"F:\memory blank\icon-preview-v2.png")
print("preview saved")
