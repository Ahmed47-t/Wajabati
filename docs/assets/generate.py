# -*- coding: utf-8 -*-
"""Render the README artwork: logo mark, lockup, banner, social preview, diagrams.

Pillow has no Arabic shaping in this environment (no libraqm), so `rt.py` reshapes and
reverses each word and justaposes LTR words in reading order. Run from anywhere:

    pip install Pillow arabic-reshaper python-bidi
    python docs/assets/generate.py

Fonts are expected in /tmp/fonts (Noto Sans Arabic + Inter); see docs/assets/README.md.
"""
import os
import sys
import math

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from PIL import Image, ImageDraw, ImageFilter, ImageFont
from rt import FontSet, draw, measure
from mark import build, build_alpha

OUT = os.path.dirname(os.path.abspath(__file__))
FONTS = os.environ.get("WAJABATI_FONTS", "/tmp/fonts")

BLUE = (0, 102, 221)
INK = (11, 18, 32)
MUTED = (91, 100, 114)
WHITE = (255, 255, 255)

AR_B = f"{FONTS}/NSA-Bold.ttf"
AR_SB = f"{FONTS}/NSA-SemiBold.ttf"
AR_R = f"{FONTS}/NSA-Regular.ttf"
LAT_B = f"{FONTS}/Inter-800.ttf"
LAT_R = f"{FONTS}/Inter-500.ttf"


def gradient(w, h, c1, c2, angle=32, n=512):
    small = Image.new("RGB", (n, n))
    px = small.load()
    a = math.radians(angle)
    dx, dy = math.cos(a), math.sin(a)
    for y in range(n):
        for x in range(n):
            t = (x * dx + y * dy) / (n * (dx + dy))
            t = 0.0 if t < 0 else (1.0 if t > 1 else t)
            px[x, y] = tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(3))
    return small.resize((w, h), Image.BICUBIC)


def dots(canvas, step=44, r=2, color=(255, 255, 255, 24)):
    ov = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(ov)
    for y in range(step // 2, canvas.size[1], step):
        for x in range(step // 2, canvas.size[0], step):
            d.ellipse([x - r, y - r, x + r, y + r], fill=color)
    return Image.alpha_composite(canvas, ov)


def glow(img, blobs, blur=80):
    lay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(lay)
    for box, col in blobs:
        d.ellipse(box, fill=col)
    return Image.alpha_composite(img, lay.filter(ImageFilter.GaussianBlur(blur)))


def pill(img, xy, text, fs, fg, bg, padx=22, pady=11):
    d = ImageDraw.Draw(img)
    tw, _ = measure(text, fs)
    asc, desc = fs.ar.getmetrics()
    h = asc + desc + pady * 2
    w = tw + padx * 2
    x, y = xy
    x0 = x - w
    lay = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ImageDraw.Draw(lay).rounded_rectangle([x0, y - h / 2, x0 + w, y + h / 2], radius=h / 2, fill=bg)
    img.alpha_composite(lay)
    draw(d, (x0 + padx + tw, y), text, fs, fg, anchor="rm")
    return w


def framed_mark(size, ratio=0.365, **kw):
    W = size * 4
    tile = gradient(W, W, (0, 122, 242), (0, 58, 150), 40).convert("RGBA")
    glow(tile, [((W * 0.1, -W * 0.2, W * 0.9, W * 0.55), (120, 190, 255, 70))], blur=W * 0.12)
    inner = int(W * 0.78)
    tile.alpha_composite(build_alpha(inner, int(inner) * ratio, **kw), ((W - inner) // 2, (W - inner) // 2))
    mask = Image.new("L", (W, W), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, W - 1, W - 1], radius=int(W * 0.235), fill=255)
    tile.putalpha(mask)
    return tile.resize((size, size), Image.LANCZOS)


def banner(w=1600, h=560):
    img = gradient(w, h, (0, 122, 240), (0, 44, 110), 30).convert("RGBA")
    img = glow(img, [((w * 0.55, -h * 0.7, w * 1.2, h * 0.85), (100, 180, 255, 64)),
                     ((-w * 0.25, h * 0.35, w * 0.4, h * 1.4), (0, 22, 70, 110))], 100)
    img = dots(img, 46, 2, (255, 255, 255, 20))

    m = int(h * 0.66)
    mark = build_alpha(int(m * 1.9), int(m * 1.9) * 0.365).resize((m, m), Image.LANCZOS)
    img.alpha_composite(mark, (int(w * 0.075), int((h - m) / 2)))
    d = ImageDraw.Draw(img)
    cx, cy = int(w * 0.075) + m // 2, h // 2
    d.ellipse([cx - m * 0.70, cy - m * 0.70, cx + m * 0.70, cy + m * 0.70], outline=(255, 255, 255, 46), width=2)
    d.ellipse([cx - m * 0.88, cy - m * 0.88, cx + m * 0.88, cy + m * 0.88], outline=(255, 255, 255, 26), width=2)

    right = w - 84
    draw(d, (right, 92), "تطبيق أندرويد لحجز الوجبات الجامعية · 0.13.1",
         FontSet(AR_SB, LAT_R, 27), (186, 216, 255), anchor="rm")
    f_word = ImageFont.truetype(f"{FONTS}/Inter-800.ttf", 124)
    d.text((right - d.textlength("Wajabati", font=f_word), 156), "Wajabati", font=f_word, fill=WHITE)
    draw(d, (right, 344), "تعاين خطتك وتوافق قبل إرسال أي حجز",
         FontSet(AR_SB, LAT_R, 42), WHITE, anchor="rm")
    draw(d, (right, 398), "تشفير محلي · اتصال مباشر بـ Webetu · إيقاف في أي وقت",
         FontSet(AR_R, LAT_R, 29), (198, 222, 255), anchor="rm")

    f_chip = FontSet(AR_SB, LAT_R, 24)
    x = right
    for text, fg, bg in [("الإصدار 0.13.1", (7, 52, 122), (255, 255, 255, 245)),
                         ("Java 17", WHITE, (255, 255, 255, 40)),
                         ("Android 8 فما فوق", WHITE, (255, 255, 255, 40)),
                         ("عربي · فرنسي", WHITE, (255, 255, 255, 40))]:
        x -= pill(img, (x, 462), text, f_chip, fg, bg) + 14
    img.convert("RGB").save(f"{OUT}/banner.png", optimize=True)
    img.convert("RGB").save(f"{OUT}/banner.jpg", quality=94, optimize=True)


def social(w=1280, h=640):
    img = gradient(w, h, (0, 120, 240), (0, 41, 104), 26).convert("RGBA")
    img = glow(img, [((w * 0.12, -h * 0.55, w * 1.0, h * 0.62), (110, 185, 255, 66))], 90)
    img = dots(img, 40, 2, (255, 255, 255, 18))
    m = 232
    img.alpha_composite(build_alpha(int(m * 3), int(m * 3) * 0.365).resize((m, m), Image.LANCZOS),
                        (int(w / 2 - m / 2), 62))
    d = ImageDraw.Draw(img)
    f_word = ImageFont.truetype(f"{FONTS}/Inter-800.ttf", 104)
    wtw = d.textlength("Wajabati", font=f_word)
    d.text((w / 2 - wtw / 2, 344), "Wajabati", font=f_word, fill=WHITE)
    draw(d, (w / 2, 486), "حجز الوجبات الجامعية · معاينة ثم موافقة قبل أي حجز",
         FontSet(AR_SB, LAT_R, 32), (212, 231, 255), anchor="cm")
    f = ImageFont.truetype(f"{FONTS}/Inter-600.ttf", 20)
    d.text((w / 2, 566), "A N D R O I D    ·    J A V A    ·    v 0 . 1 3 . 1    ·    W E B E T U",
           font=f, fill=(255, 255, 255, 165), anchor="mm")
    img.convert("RGB").save(f"{OUT}/social-preview.png", quality=95, optimize=True)


def lockup(hgt=300, dark=False):
    m = int(hgt * 0.86)
    gap = int(hgt * 0.26)
    fs_title = FontSet(AR_B, LAT_B, int(hgt * 0.335))
    f1 = ImageFont.truetype(f"{FONTS}/Inter-600.ttf", int(hgt * 0.085))
    f2 = ImageFont.truetype(f"{FONTS}/Inter-500.ttf", int(hgt * 0.056))
    lab1, lab2 = "W A J A B A T I   ·   V 0 . 1 3 . 1", "U N I V E R S I T Y   M E A L   B O O K I N G"
    ink = WHITE if dark else INK
    sub = (198, 222, 255) if dark else MUTED

    block = Image.new("RGBA", (int(hgt * 2.4), int(hgt * 1.2)), (0, 0, 0, 0))
    bd = ImageDraw.Draw(block)
    asc, desc = fs_title.ar.getmetrics()
    draw(bd, (block.width - 8, asc), "وجباتي", fs_title, ink, anchor="rm")
    y1 = asc + desc + int(hgt * 0.05)
    bd.text((block.width - 8 - bd.textlength(lab2, font=f2), y1 + int(hgt * 0.098)), lab2, font=f2, fill=sub)
    bd.text((block.width - 8 - bd.textlength(lab1, font=f1), y1), lab1, font=f1, fill=sub)
    block = block.crop(block.getbbox())

    W = m + gap + block.width + 18
    img = Image.new("RGBA", (W, hgt), (0, 0, 0, 0))
    img.alpha_composite(framed_mark(m, 0.365), (0, (hgt - m) // 2))
    img.alpha_composite(block, (W - block.width - 9, (hgt - block.height) // 2))
    return img.crop(img.getbbox())


def architecture(w=1500, h=790):
    BG, CARD, LINE = (247, 249, 253), WHITE, (223, 230, 240)
    img = Image.new("RGB", (w, h), BG)
    d = ImageDraw.Draw(img)
    for x in range(0, w, 40):
        d.line([(x, 0), (x, h)], fill=(240, 243, 249))
    for y in range(0, h, 40):
        d.line([(0, y), (w, y)], fill=(240, 243, 249))

    cw, ch = 250, 124

    def card(xr, y0, title, sub, accent=BLUE, mono=""):
        x0, x1 = xr - cw, xr
        d.rounded_rectangle([x0, y0, x1, y0 + ch], radius=16, fill=CARD, outline=LINE, width=2)
        d.rounded_rectangle([x1 - 7, y0 + 12, x1, y0 + ch - 12], radius=4, fill=accent)
        if mono:
            d.text((x0 + 22, y0 + 18), mono, font=ImageFont.truetype(f"{FONTS}/Inter-600.ttf", 15), fill=accent)
        draw(d, (x1 - 24, y0 + 52), title, FontSet(AR_SB, LAT_B, 24), INK, anchor="rm")
        draw(d, (x1 - 24, y0 + 86), sub, FontSet(AR_R, LAT_R, 17), MUTED, anchor="rm")

    def arrow(p0, p1, color=(150, 164, 182)):
        d.line([p0, p1], fill=color, width=3)
        ang = math.atan2(p1[1] - p0[1], p1[0] - p0[0])
        s = 9
        d.polygon([p1, (p1[0] - s * math.cos(ang - 0.5), p1[1] - s * math.sin(ang - 0.5)),
                   (p1[0] - s * math.cos(ang + 0.5), p1[1] - s * math.sin(ang + 0.5))], fill=color)

    def bi(p_right, p_left, color=(150, 164, 182)):
        arrow(p_right, p_left, color)
        ang = math.atan2(p_left[1] - p_right[1], p_left[0] - p_right[0])
        s = 9
        d.polygon([p_right, (p_right[0] - s * math.cos(ang - 0.5), p_right[1] - s * math.sin(ang - 0.5)),
                   (p_right[0] - s * math.cos(ang + 0.5), p_right[1] - s * math.sin(ang + 0.5))], fill=color)

    draw(d, (w - 62, 56), "كيف يعمل التطبيق", FontSet(AR_B, LAT_B, 38), INK, anchor="rm")
    draw(d, (w - 62, 110), "قراءة  ‹  معاينة  ‹  موافقة  ‹  حجز الناقص  ‹  تأكيد",
         FontSet(AR_R, LAT_R, 23), MUTED, anchor="rm")

    xs = [1226, 930, 634, 338]
    y1, y2 = 178, 396
    card(xs[0], y1, "الواجهة", "شاشات عربية وفرنسية", mono="MainActivity")
    card(xs[1], y1, "المنسّق", "منع تداخل العمليات", accent=(58, 132, 214), mono="Runner")
    card(xs[2], y1, "خطة الحجز", "3 أيام ومنع التكرار", accent=(40, 152, 96), mono="Engine")
    card(xs[3], y1, "الاستبدال", "تبديل يدوي بموافقة", accent=(192, 128, 20), mono="Replacement")
    for i in range(3):
        bi((xs[i] - cw - 6, y1 + ch / 2), (xs[i + 1] + 6, y1 + ch / 2))

    card(xs[2], y2, "الشبكة", "اتصال HTTPS مباشر", accent=(92, 100, 196), mono="ServiceApi")
    card(xs[1], y2, "منصة Webetu", "MESRS / ONOU", accent=(124, 136, 154), mono="Webetu")
    card(xs[0], y2, "الجدولة", "فحص دوري في الخلفية", accent=(40, 152, 96), mono="Automation")
    arrow((xs[2] - cw / 2, y1 + ch + 8), (xs[2] - cw / 2, y2 - 10))
    bi((xs[2] + 6, y2 + ch / 2), (xs[1] - cw - 6, y2 + ch / 2))
    bi((xs[1] + 6, y2 + ch / 2), (xs[0] - cw - 6, y2 + ch / 2))

    vy = 578
    d.rounded_rectangle([xs[3] - cw, vy, w - 62, vy + 100], radius=18, fill=(233, 241, 255), outline=(206, 223, 250), width=2)
    draw(d, (w - 90, vy + 34), "Vault · Android Keystore + AES-GCM",
         FontSet(AR_SB, LAT_B, 25), (26, 60, 128), anchor="rm")
    draw(d, (w - 90, vy + 70), "كلمة المرور مشفّرة على الهاتف فقط · بدون سيرفر وسيط أو تتبع أو نسخ سحابي",
         FontSet(AR_R, LAT_R, 19), (72, 100, 148), anchor="rm")
    for x in (xs[2] - cw / 2, xs[1] - cw / 2, xs[0] - cw / 2):
        arrow((x, vy - 8), (x, y2 + ch + 10), color=(150, 176, 226))
    d.text((w - 62, h - 22), "docs/assets/architecture.png",
           font=ImageFont.truetype(f"{FONTS}/Inter-500.ttf", 15), fill=(178, 186, 198), anchor="rm")
    img.save(f"{OUT}/architecture.png", optimize=True)


def journey(w=1600, h=430):
    img = Image.new("RGB", (w, h), (252, 253, 255))
    d = ImageDraw.Draw(img)
    draw(d, (w - 64, 56), "خطوات الإعداد الأربع", FontSet(AR_B, LAT_B, 40), INK, anchor="rm")
    draw(d, (w - 64, 112), "كل خطوة تفتح بعد إكمال الخطوة التي قبلها",
         FontSet(AR_R, LAT_R, 23), MUTED, anchor="rm")

    steps = [
        ("1", "الحساب", "تسجيل الدخول والقراءة فقط", "لا يتم أي حجز هنا", BLUE),
        ("2", "الوجبات", "اختيار مطعم لكل وجبة", "مع استثناءات أيام الأسبوع", BLUE),
        ("3", "المراجعة", "عرض خطة 3 أيام قادمة", "لا حجز قبل موافقتك", BLUE),
        ("4", "البطارية", "السماح بالعمل في الخلفية", "اختياري لكن مستحسن", (40, 152, 96)),
    ]
    margin, gap = 64, 26
    cardw = (w - 2 * margin - gap * 3) / 4
    y0, ch = 186, 190
    for i, (num, title, sub, note, accent) in enumerate(steps):
        x1 = w - margin - i * (cardw + gap)
        x0 = x1 - cardw
        d.rounded_rectangle([x0, y0, x1, y0 + ch], radius=18, fill=WHITE, outline=(226, 233, 244), width=2)
        d.rounded_rectangle([x0, y0, x1, y0 + 6], radius=3, fill=accent)
        br = 26
        bcx, bcy = x1 - 44, y0 + 62
        d.ellipse([bcx - br, bcy - br, bcx + br, bcy + br], fill=accent)
        d.text((bcx, bcy - 2), num, font=ImageFont.truetype(f"{FONTS}/Inter-700.ttf", 30), fill=WHITE, anchor="mm")
        draw(d, (bcx - br - 16, bcy + 14), title, FontSet(AR_SB, LAT_B, 27), INK, anchor="rm")
        d.line([(x0 + 20, y0 + 104), (x1 - 20, y0 + 104)], fill=(236, 241, 248), width=2)
        draw(d, (x1 - 24, y0 + 138), sub, FontSet(AR_R, LAT_R, 19), MUTED, anchor="rm")
        draw(d, (x1 - 24, y0 + 170), note, FontSet(AR_R, LAT_R, 18), accent, anchor="rm")
        if i < 3:
            ax, ay = x0 - gap / 2, y0 + ch / 2
            d.line([(ax + 8, ay), (ax - 8, ay)], fill=(196, 208, 226), width=3)
            d.polygon([(ax - 11, ay), (ax + 1, ay - 7), (ax + 1, ay + 7)], fill=(196, 208, 226))
    img.save(f"{OUT}/setup-journey.png", optimize=True)


if __name__ == "__main__":
    framed_mark(1024).convert("RGB").save(f"{OUT}/logo-mark.png", optimize=True)
    framed_mark(512).save(f"{OUT}/logo-mark-512.png", optimize=True)
    lockup().save(f"{OUT}/logo-lockup.png", optimize=True)
    lockup(dark=True).save(f"{OUT}/logo-lockup-dark.png", optimize=True)
    banner()
    social()
    architecture()
    journey()
    print("assets written to", OUT)
