# -*- coding: utf-8 -*-
"""The Wajabati mark: white plate + blue fork & spoon + crescent notch on the upper-right edge.

`build` draws it on a solid blue tile (launcher icon mockups, PNG previews);
`build_alpha` draws the same geometry on transparency (for banners and the adaptive icon).
"""
import math
from PIL import Image, ImageDraw

BLUE = (0, 102, 221, 255)
WHITE = (255, 255, 255, 255)


def _cutlery(d, cx, cy, r, color=BLUE):
    """Fork on the left, spoon on the right, centred on the plate."""
    h = r * 1.05
    top, bot = cy - h / 2, cy + h / 2
    stroke = r * 0.105
    gap = r * 0.225
    fx, sx = cx - gap, cx + gap

    headw, headh, tines = stroke * 2.6, h * 0.36, 4
    space = headw / (tines * 2 - 1)
    for i in range(tines):
        x = fx - headw / 2 + i * space * 2
        d.rounded_rectangle([x, top, x + space, top + headh], radius=space * 0.48, fill=color)
    d.rounded_rectangle([fx - headw / 2, top + headh * 0.66, fx + headw / 2,
                         top + headh + stroke * 0.55], radius=stroke * 0.8, fill=color)
    d.rounded_rectangle([fx - stroke / 2, top + headh * 0.80, fx + stroke / 2, bot],
                        radius=stroke / 2, fill=color)

    bw, bh = stroke * 3.0, h * 0.40
    d.ellipse([sx - bw / 2, top, sx + bw / 2, top + bh], fill=color)
    d.rounded_rectangle([sx - stroke / 2, top + bh * 0.72, sx + stroke / 2, bot],
                        radius=stroke / 2, fill=color)


def _geometry(W, plate_r, notch_deg):
    cx = cy = W / 2.0
    r = plate_r
    a = math.radians(notch_deg)
    nr = r * 0.40
    nx, ny = cx + math.cos(a) * (r + nr * 0.30), cy - math.sin(a) * (r + nr * 0.30)
    cr = nr * 0.60
    ccx, ccy = nx + math.cos(a) * nr * 0.30, ny - math.sin(a) * nr * 0.30
    return dict(cx=cx, cy=cy, r=r, a=a, nr=nr, nx=nx, ny=ny, cr=cr, ccx=ccx, ccy=ccy,
                ox=cr * 0.42, oy=-cr * 0.24)


def build(W, plate_r, notch_deg=42.0):
    g = _geometry(W, plate_r, notch_deg)
    img = Image.new("RGBA", (W, W), BLUE)
    d = ImageDraw.Draw(img)
    d.ellipse([g['cx'] - g['r'], g['cy'] - g['r'], g['cx'] + g['r'], g['cy'] + g['r']], fill=WHITE)
    _cutlery(d, g['cx'], g['cy'], g['r'])
    d.ellipse([g['nx'] - g['nr'], g['ny'] - g['nr'], g['nx'] + g['nr'], g['ny'] + g['nr']], fill=BLUE)
    d.ellipse([g['ccx'] - g['cr'], g['ccy'] - g['cr'], g['ccx'] + g['cr'], g['ccy'] + g['cr']], fill=WHITE)
    d.ellipse([g['ccx'] - g['cr'] + g['ox'], g['ccy'] - g['cr'] + g['oy'],
               g['ccx'] + g['cr'] + g['ox'], g['ccy'] + g['cr'] + g['oy']], fill=BLUE)
    return img


def build_alpha(W, plate_r, notch_deg=42.0):
    g = _geometry(W, plate_r, notch_deg)
    cx, cy, r = g['cx'], g['cy'], g['r']

    pmask = Image.new("L", (W, W), 0)
    pd = ImageDraw.Draw(pmask)
    pd.ellipse([cx - r, cy - r, cx + r, cy + r], fill=255)
    pd.ellipse([g['nx'] - g['nr'], g['ny'] - g['nr'], g['nx'] + g['nr'], g['ny'] + g['nr']], fill=0)

    plate = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    plate.paste(Image.new("RGBA", (W, W), WHITE), (0, 0), pmask)

    cut = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    _cutlery(ImageDraw.Draw(cut), cx, cy, r)
    plate.alpha_composite(Image.composite(cut, Image.new("RGBA", (W, W), (0, 0, 0, 0)), pmask))

    cmask = Image.new("L", (W, W), 0)
    cd = ImageDraw.Draw(cmask)
    cd.ellipse([g['ccx'] - g['cr'], g['ccy'] - g['cr'], g['ccx'] + g['cr'], g['ccy'] + g['cr']], fill=255)
    cd.ellipse([g['ccx'] - g['cr'] + g['ox'], g['ccy'] - g['cr'] + g['oy'],
                g['ccx'] + g['cr'] + g['ox'], g['ccy'] + g['cr'] + g['oy']], fill=0)
    cres = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    cres.paste(Image.new("RGBA", (W, W), WHITE), (0, 0), cmask)
    return Image.alpha_composite(plate, cres)
