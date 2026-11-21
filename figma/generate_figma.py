#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Wajabati -> Figma generator
Reads the exact palette / strings from the Android source and emits
one importable SVG per screen (390pt phone frame, RTL Arabic).
Import: drag any SVG from figma/screens/ into Figma, or import
figma/Wajabati-all-screens.svg for the whole board at once.
"""
import os, json, html

OUT = os.path.join(os.path.dirname(__file__), "screens")
os.makedirs(OUT, exist_ok=True)

# ---- Palette: Appearance.Palette (light) + colors.xml / styles.xml ----
P = dict(
    background="#F2F2F7", surface="#FFFFFF", ink="#1C1C1E", muted="#6C6C73",
    accent="#0064D8", primary="#0066DD", primary_deep="#0048B8",
    segment="#E3E3E8", warning_bg="#FFF6E2", warning_ink="#875600",
    danger="#AD362B", danger_bg="#FBE9E7", line="#C7C7CC",
    success_bg="#E4F5E9", success_ink="#17662B", chevron="#8E8E93",
    brand="#0066DD", brand_deep="#002D73",
)
D = dict(  # dark variant (Appearance.Palette dark=true)
    background="#000000", surface="#1C1C1E", ink="#F5F5F7", muted="#A1A1AA",
    accent="#64A8FF", primary="#0A63D0", primary_deep="#06408A",
    segment="#2C2C2E", warning_bg="#342812", warning_ink="#FFD27D",
    danger="#FF8279", danger_bg="#3A1A17", line="#48484A",
    success_bg="#12301C", success_ink="#7EE29A", chevron="#8E8E93",
    brand="#0066DD", brand_deep="#002D73",
)
FONT = "Inter, -apple-system, 'Segoe UI', Roboto, 'Noto Sans Arabic', sans-serif"
W = 390

def esc(s): return html.escape(str(s), quote=True)

class Frame:
    def __init__(self, name, height, pal=P, rtl=True):
        self.name, self.h, self.p, self.rtl = name, height, pal, rtl
        self.el = []
        self.y = 0
    # primitives ------------------------------------------------------
    def rect(self, x, y, w, h, fill, rx=0, stroke=None, sw=1, opacity=1):
        s = f' stroke="{stroke}" stroke-width="{sw}"' if stroke else ""
        self.el.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}" fill="{fill}" opacity="{opacity}"{s}/>')
    def grad(self, gid, c1, c2):
        self.el.append(f'<defs><linearGradient id="{gid}" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="{c1}"/><stop offset="1" stop-color="{c2}"/></linearGradient></defs>')
    def text(self, x, y, s, size=14, color=None, bold=False, anchor="start", opacity=1):
        color = color or self.p["ink"]
        w = ' font-weight="700"' if bold else ""
        self.el.append(f'<text x="{x}" y="{y}" font-family="{FONT}" font-size="{size}" fill="{color}" text-anchor="{anchor}" opacity="{opacity}"{w}>{esc(s)}</text>')
    def wrap(self, x, y, s, size=13, color=None, width=330, lh=19, anchor="start", bold=False):
        """Naive Arabic word-wrap (Figma will re-flow; this is for the SVG preview)."""
        color = color or self.p["ink"]
        words, lines, cur = str(s).split(), [], ""
        for wd in words:
            t = (cur + " " + wd).strip()
            if len(t) * size * 0.52 > width and cur:
                lines.append(cur); cur = wd
            else:
                cur = t
        if cur: lines.append(cur)
        for i, ln in enumerate(lines):
            self.text(x, y + i * lh, ln, size, color, bold, anchor)
        return len(lines) * lh
    # components (mirroring MainActivity helpers) --------------------
    def statusbar(self):
        self.rect(0, 0, W, 28, self.p["background"])
        self.text(W - 16, 19, "9:41", 12, self.p["ink"], True, "end")
        self.text(16, 19, "Wajabati", 11, self.p["muted"], False, "start")
    def brand(self):
        self.text(W - 20, 66, "Wajabati", 34, self.p["ink"], True, "end")
        self.text(W - 20, 88, "حجز الوجبات الجامعية · 0.13.1", 14, self.p["muted"], anchor="end")
        return 100
    def card(self, y, h, rx=20):
        self.rect(20, y, W - 40, h, self.p["surface"], rx)
        return y
    def heading(self, y, s):
        self.text(W - 36, y + 14, s, 13, self.p["muted"], anchor="end")
        return y + 22
    def button(self, y, label, kind="primary", h=50):
        x, w = 20, W - 40
        if kind == "primary":
            self.rect(x, y, w, h, self.p["primary"], 12)
            self.text(W / 2, y + h / 2 + 6, label, 16, "#FFFFFF", True, "middle")
        elif kind == "danger":
            self.rect(x, y, w, h, self.p["surface"], 12, self.p["danger"], 1)
            self.text(W / 2, y + h / 2 + 6, label, 16, self.p["danger"], True, "middle")
        elif kind == "nav":
            self.rect(x, y, w, h, self.p["surface"], 12)
            self.text(W - 32, y + h / 2 + 6, label, 16, self.p["accent"], anchor="end")
            self.text(32, y + h / 2 + 6, "‹", 20, self.p["chevron"], anchor="start")
        else:  # plain outlined
            self.rect(x, y, w, h, self.p["surface"], 12, self.p["line"], 1)
            self.text(W / 2, y + h / 2 + 6, label, 16, self.p["accent"], anchor="middle")
        return y + h + 8
    def row(self, y, icon, title, value=None, h=64):
        self.rect(20, y, W - 40, h, self.p["surface"], 12)
        self.rect(W - 72, y + 12, 40, 40, self.p["segment"], 11)
        self.text(W - 52, y + 38, icon, 18, self.p["accent"], True, "middle")
        self.text(W - 84, y + 28, title, 15, self.p["ink"], anchor="end")
        if value:
            self.text(W - 84, y + 50, value, 13, self.p["muted"], anchor="end")
        self.text(32, y + h / 2 + 6, "‹", 20, self.p["chevron"], anchor="start")
        return y + h + 2
    def chip(self, x, y, label, bg, ink):
        w = max(58, len(label) * 8 + 20)
        self.rect(x - w, y - 16, w, 24, bg, 10)
        self.text(x - w / 2, y + 1, label, 12, ink, True, "middle")
        return w
    def meal_row(self, y, meal, name, state=None):
        """state: None | 'booked' | 'missing' | 'replace'"""
        self.rect(W - 72, y + 4, 40, 40, self.p["segment"], 11)
        self.text(W - 52, y + 30, {"فطور الصباح": "☕", "الغداء": "🍲", "العشاء": "🌙"}[meal], 16, self.p["accent"], True, "middle")
        self.text(W - 84, y + 22, meal, 12, self.p["muted"], anchor="end")
        self.text(W - 84, y + 42, name, 15, self.p["ink"], True, "end")
        if state:
            bg, ink, label = {
                "booked": (self.p["success_bg"], self.p["success_ink"], "محجوز"),
                "missing": (self.p["warning_bg"], self.p["warning_ink"], "غير محجوز"),
                "replace": (self.p["danger_bg"], self.p["danger"], "يحتاج استبدالًا"),
            }[state]
            self.chip(W - 84, y + 24, label, bg, ink)
        return y + 52
    def tabs(self, active):
        y = self.h - 64
        self.rect(0, y, W, 64, self.p["surface"], 0, self.p["line"], 0.5)
        for i, (label, icon) in enumerate([("الرئيسية", "⌂"), ("الحجوزات", "▦"), ("الإعدادات", "⚙")]):
            cx = W - 65 - i * 130
            col = self.p["accent"] if i == active else self.p["muted"]
            if i == active:
                self.rect(cx - 38, y + 6, 76, 52, self.p["segment"], 14)
            self.text(cx, y + 28, icon, 18, col, True, "middle")
            self.text(cx, y + 50, label, 12, col, i == active, "middle")
    def toggle(self, x, y, on=True):
        self.rect(x, y, 52, 32, "#34C759" if on else "#E5E5EA", 16)
        self.rect(x + (22 if on else 3), y + 3, 26, 26, "#FFFFFF", 13)
    def field(self, y, label, value, password=False):
        self.text(W - 36, y + 14, label, 13, self.p["muted"], anchor="end")
        self.rect(20, y + 20, W - 40, 52, self.p["surface"], 10, self.p["chevron"], 1)
        self.text(W - 36, y + 54, value, 16, self.p["ink"], anchor="end")
        if password:
            self.text(36, y + 54, "👁", 16, self.p["muted"], anchor="start")
        return y + 80
    def choice(self, y, label, value):
        self.text(W - 36, y + 14, label, 14, self.p["ink"], anchor="end")
        self.rect(20, y + 20, W - 40, 52, self.p["surface"], 10, self.p["chevron"], 1)
        self.text(W - 36, y + 54, value, 16, self.p["ink"], anchor="end")
        self.text(36, y + 54, "⌄", 16, self.p["muted"], anchor="start")
        return y + 80
    def progress(self, y, pct, color=None):
        self.rect(20, y, W - 40, 5, self.p["line"], 3)
        self.rect(W - 20 - (W - 40) * pct, y, (W - 40) * pct, 5, color or self.p["accent"], 3)
        return y + 12
    def note(self, y, s, kind="danger"):
        bg = self.p["danger_bg"] if kind == "danger" else self.p["warning_bg"]
        ink = self.p["danger"] if kind == "danger" else self.p["warning_ink"]
        h = 44
        self.rect(20, y, W - 40, h, bg, 12)
        self.wrap(W - 34, y + 20, s, 13, ink, 320, 18, "end")
        return y + h + 8
    def dialog(self, title, body_lines, buttons, h=320):
        """Centered AlertDialog over a dimmed screen."""
        self.rect(0, 0, W, self.h, "#000000", 0, opacity=0.45)
        x, w = 32, W - 64
        y = (self.h - h) / 2
        self.rect(x, y, w, h, self.p["surface"], 16)
        self.text(W / 2, y + 34, title, 17, self.p["ink"], True, "middle")
        cy = y + 56
        for ln in body_lines:
            cy += self.wrap(W / 2, cy + 16, ln, 14, self.p["ink"], w - 40, 20, "middle") + 4
        by = y + h - 58
        bw = (w - 48) / len(buttons)
        for i, (label, kind) in enumerate(buttons):
            bx = x + 16 + i * (bw + 8)
            if kind == "danger":
                self.rect(bx, by, bw, 44, self.p["surface"], 10, self.p["danger"], 1)
                self.text(bx + bw / 2, by + 29, label, 15, self.p["danger"], True, "middle")
            elif kind == "primary":
                self.rect(bx, by, bw, 44, self.p["primary"], 10)
                self.text(bx + bw / 2, by + 29, label, 15, "#FFFFFF", True, "middle")
            else:
                self.rect(bx, by, bw, 44, self.p["surface"], 10, self.p["line"], 1)
                self.text(bx + bw / 2, by + 29, label, 15, self.p["accent"], anchor="middle")
        return y
    def sheet(self, title, items, selected=0):
        self.rect(0, 0, W, self.h, "#000000", 0, opacity=0.45)
        h = 120 + len(items) * 52 + 58
        y = self.h - h
        self.rect(0, y, W, h, self.p["surface"], 22)
        self.rect(W / 2 - 20, y + 8, 40, 5, self.p["line"], 3)
        self.text(W - 36, y + 44, title, 18, self.p["ink"], True, "end")
        cy = y + 58
        for i, it in enumerate(items):
            if i == selected:
                self.rect(16, cy, W - 32, 48, self.p["background"], 12)
            self.text(W - 32, cy + 31, it, 16, self.p["ink"], i == selected, "end")
            if i == selected:
                self.text(32, cy + 31, "✓", 18, self.p["accent"], True, "start")
            cy += 52
        self.rect(16, cy + 6, W - 32, 50, self.p["surface"], 12, self.p["line"], 1)
        self.text(W / 2, cy + 38, "إلغاء", 16, self.p["accent"], anchor="middle")
    def wizard_head(self, title, step):
        self.text(W - 20, 66, title, 22, self.p["ink"], True, "end")
        self.text(W - 20, 88, f"الخطوة {step} من 4", 14, self.p["muted"], anchor="end")
        self.text(28, 66, "✕", 18, self.p["muted"], anchor="start")
        self.progress(98, step / 4)
        return 118
    def svg(self):
        body = "\n".join(self.el)
        return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{self.h}" '
                f'viewBox="0 0 {W} {self.h}" dir="rtl"><title>{esc(self.name)}</title>'
                f'<rect width="{W}" height="{self.h}" fill="{self.p["background"]}"/>'
                f'<g font-family="{FONT}">{body}</g></svg>\n')

# =====================================================================
frames = []

# 1 — Home -----------------------------------------------------------------
f = Frame("01 · الرئيسية — Home", 1180)
f.statusbar(); y = f.brand()
f.grad("hero", f.p["primary"], f.p["primary_deep"])
f.rect(20, y, W - 40, 196, "url(#hero)", 22)
f.rect(W - 150, y + 14, 110, 24, "#FFFFFF", 10, opacity=0.18)
f.text(W - 95, y + 31, "خطوة واحدة تفصلك", 12, "#FFFFFF", True, "middle")
f.text(W - 36, y + 72, "تبقّى اختيار المطاعم", 24, "#FFFFFF", True, "end")
f.wrap(W - 36, y + 96, "بعد الإعداد، يتحقق التطبيق من الحجوزات كل ساعة عند توفر الإنترنت.", 13, "#FFFFFF", 300, 18, "end")
f.rect(36, y + 128, W - 72, 6, "#FFFFFF", 3, opacity=0.35)
f.rect(W - 36 - (W - 72) * 0.5, y + 128, (W - 72) * 0.5, 6, "#FFFFFF", 3)
f.text(W - 36, y + 152, "50% · اكتمال الإعداد 2/4", 12, "#FFFFFF", anchor="end")
f.rect(36, y + 160, W - 72, 28, "#FFFFFF", 14)
f.text(W / 2, y + 179, "التالي: اختيار المطاعم", 15, "#0052CC", True, "middle")
y += 210
f.card(y, 252)
f.text(W - 36, y + 30, "خطوات الإعداد", 17, f.p["ink"], True, "end")
steps = [("إدخال رقم التسجيل وكلمة المرور", "يُحفظ الحساب مشفّرًا على هاتفك فقط.", True),
         ("اختيار مطعم لكل وجبة", "فطور، غداء، عشاء — كل وجبة تختار مطعمها.", False),
         ("عرض الحجوزات وتأكيدها", "معاينة للقراءة فقط قبل أي حجز.", False),
         ("السماح بالعمل في الخلفية (البطارية)", "استثنِ «Wajabati» من توفير البطارية.", False)]
sy = y + 44
for i, (t, d, done) in enumerate(steps):
    f.rect(W - 66, sy, 30, 30, f.p["success_bg"] if done else f.p["surface"], 15, None if done else f.p["chevron"], 1)
    f.text(W - 51, sy + 21, "✓" if done else str(i + 1), 14, f.p["success_ink"] if done else f.p["muted"], True, "middle")
    f.text(W - 80, sy + 14, t, 15, f.p["ink"], i == 1, "end")
    f.text(W - 80, sy + 32, d, 13, f.p["muted"], anchor="end")
    sy += 50
y += 264
f.note(y, "لا يعمل الحجز التلقائي إذا كان الهاتف مغلقًا أو غير متصل بالإنترنت.", "warn"); y += 56
f.card(y, 208)
f.text(W - 36, y + 30, "وجبات اليوم", 17, f.p["ink"], True, "end")
f.text(36, y + 30, "الكل", 14, f.p["accent"], True, "start")
ty = y + 44
for meal, name, st in [("فطور الصباح", "مطعم الجامعة المركزي", "booked"), ("الغداء", "مطعم الإقامة", "missing"), ("العشاء", "مطعم الجامعة المركزي", "replace")]:
    ty = f.meal_row(ty, meal, name, st)
y += 220
f.card(y, 150)
f.text(W - 36, y + 32, "الحجز التلقائي", 17, f.p["ink"], True, "end")
f.toggle(36, y + 14, True)
f.text(W - 36, y + 58, "مفعّل", 14, f.p["muted"], anchor="end")
f.text(W - 36, y + 80, "التحقق كل ساعة", 13, f.p["accent"], anchor="end")
f.text(W - 36, y + 102, "آخر تأكيد: 21/11 · 09:41", 13, f.p["muted"], anchor="end")
f.text(W - 36, y + 124, "الاتصال متاح", 13, f.p["muted"], anchor="end")
y += 162
y = f.heading(y, "آخر عملية")
f.card(y, 118)
f.text(W - 36, y + 26, "لم يتم الفحص بعد.", 14, f.p["ink"], anchor="end")
y2 = f.button(y + 36, "التفاصيل", "nav", 40)
y2 = f.button(y2, "المحاولات المعلّقة", "nav", 40)
f.tabs(0)
frames.append(f)

# 2 — Bookings -------------------------------------------------------------
f = Frame("02 · الحجوزات — Bookings", 980)
f.statusbar()
f.text(W - 20, 66, "حجوزاتك", 22, f.p["ink"], True, "end")
y = 84
f.rect(20, y, W - 40, 52, f.p["segment"], 13)
f.wrap(W - 34, y + 20, "جارٍ قراءة حجوزاتك… قراءة فقط — لا يُرسل أي حجز في هذه الخطوة.", 13, f.p["accent"], 320, 18, "end")
y += 62
f.text(W - 20, y + 14, "آخر تأكيد كامل لحجوزات الأيام الثلاثة: 21/11 · 09:41.", 13, f.p["muted"], anchor="end")
y += 26
for day, rows in [("الثلاثاء 21/11 · اليوم", [("فطور الصباح", "مطعم الجامعة المركزي", "booked"), ("الغداء", "مطعم الإقامة", "missing"), ("العشاء", "مطعم الجامعة المركزي", "booked")]),
                  ("الأربعاء 22/11 · غدًا", [("فطور الصباح", "مطعم الجامعة المركزي", "missing"), ("الغداء", "مطعم الإقامة", "missing"), ("العشاء", "مطعم الجامعة المركزي", "replace")])]:
    f.card(y, 196)
    f.text(W - 36, y + 28, day, 14, f.p["ink"], True, "end")
    ry = y + 40
    for meal, name, st in rows:
        ry = f.meal_row(ry, meal, name, st)
    y += 208
y = f.button(y, "مراجعة وتأكيد الحجز", "primary")
y = f.button(y, "تحديث من Webetu", "plain")
f.tabs(1)
frames.append(f)

# 2b — Bookings empty state -------------------------------------------------
f = Frame("02b · الحجوزات — حالة فارغة", 844)
f.statusbar()
f.text(W - 20, 66, "حجوزاتك", 22, f.p["ink"], True, "end")
y = 96
f.card(y, 170)
f.text(W - 36, y + 34, "لم تُحدَّد المطاعم بعد", 19, f.p["ink"], True, "end")
f.text(W - 36, y + 60, "أكمل الإعداد لعرض حجوزاتك هنا.", 14, f.p["muted"], anchor="end")
f.rect(36, y + 76, W - 72, 50, f.p["primary"], 12)
f.text(W / 2, y + 108, "اذهب إلى الإعداد", 16, "#FFFFFF", True, "middle")
f.tabs(1)
frames.append(f)

# 3 — Settings -------------------------------------------------------------
f = Frame("03 · الإعدادات — Settings", 1420)
f.statusbar()
f.text(W - 20, 66, "الإعدادات", 22, f.p["ink"], True, "end")
y = 84
y = f.heading(y, "الحساب")
y = f.row(y, "👤", "رقم التسجيل", "2024/12345")
y = f.row(y, "🔒", "كلمة المرور", "•••••••• · محفوظة مشفّرة على هذا الهاتف")
y = f.heading(y, "مطاعم الوجبات الثلاث")
y = f.row(y, "☕", "فطور الصباح", "مطعم الجامعة المركزي")
y = f.row(y, "🍲", "الغداء", "مطعم الإقامة")
y = f.row(y, "🌙", "العشاء", "مطعم الجامعة المركزي")
y = f.row(y, "▦", "الاستثناءات الأسبوعية", "الثلاثاء · الخميس")
y = f.heading(y, "الحجز التلقائي")
y = f.row(y, "🕐", "مدة التحقق", "كل ساعة")
y = f.heading(y, "التنبيهات")
y = f.row(y, "🔔", "إشعارات نتيجة الحجز", "مفعّلة — عند نجاح الحجز أو تعذّره.")
y = f.heading(y, "العرض")
y = f.row(y, "◐", "المظهر", "حسب النظام")
y = f.row(y, "🌐", "اللغة", "العربية")
y = f.heading(y, "المتابعة")
y = f.row(y, "🕐", "المحاولات المعلّقة", None)
y = f.row(y, "⚙", "إعدادات التطبيق والبطارية", None)
y = f.button(y, "فحص الاتصال", "plain", 46)
y = f.button(y, "إعادة جدولة الفحص", "plain", 46)
y = f.heading(y, "حول")
y = f.row(y, "ⓘ", "حول التطبيق", None)
y = f.heading(y, "البيانات")
f.card(y, 108)
f.wrap(W - 36, y + 20, "يحذف الحساب والإعدادات من هذا الهاتف ويوقف الأتمتة. لا يُلغي الحجوزات الموجودة في Webetu.", 13, f.p["muted"], 320, 18, "end")
f.rect(36, y + 62, W - 72, 40, f.p["surface"], 12, f.p["danger"], 1)
f.text(W / 2, y + 89, "حذف البيانات", 15, f.p["danger"], True, "middle")
f.tabs(2)
frames.append(f)

# 4 — Wizard step 1: account ----------------------------------------------
f = Frame("04 · الإعداد — الخطوة 1: الحساب", 844)
f.statusbar(); y = f.wizard_head("الحساب", 1)
f.card(y, 330)
f.text(W - 36, y + 32, "حساب Webetu", 20, f.p["ink"], True, "end")
y2 = f.field(y + 44, "رقم التسجيل", "2024/12345")
y2 = f.field(y2, "كلمة المرور", "••••••••", password=True)
f.rect(36, y2 + 4, W - 72, 46, f.p["surface"], 12, f.p["line"], 1)
f.text(W / 2, y2 + 33, "الخصوصية والاستخدام", 15, f.p["accent"], anchor="middle")
y = f.button(f.h - 150, "تسجيل الدخول والمتابعة", "primary")
f.button(f.h - 92, "رجوع", "plain")
frames.append(f)

# 5 — Wizard step 2: meals + interval --------------------------------------
f = Frame("05 · الإعداد — الخطوة 2: الوجبات والمدة", 1180)
f.statusbar(); y = f.wizard_head("الوجبات والمدة", 2)
f.card(y, 330)
f.text(W - 36, y + 30, "مطعم مستقل لكل وجبة", 19, f.p["ink"], True, "end")
f.text(W - 36, y + 54, "تُعرض المطاعم التي تتيح الوجبة لحسابك فقط.", 13, f.p["muted"], anchor="end")
cy = f.choice(y + 64, "فطور الصباح", "مطعم الجامعة المركزي")
cy = f.choice(cy, "الغداء", "مطعم الإقامة")
cy = f.choice(cy, "العشاء", "اختر المطعم")
f.rect(36, cy + 2, W - 72, 44, f.p["surface"], 12, f.p["line"], 1)
f.text(W / 2, cy + 30, "تحديث قائمة المطاعم", 15, f.p["accent"], anchor="middle")
y += 344
f.card(y, 150)
f.text(W - 36, y + 28, "الاستثناءات الأسبوعية", 16, f.p["ink"], True, "end")
f.wrap(W - 36, y + 48, "استثناء يتكرر كل أسبوع في اليوم المحدد. الأيام بلا استثناء تستعمل المطاعم المعتادة.", 13, f.p["muted"], 320, 18, "end")
f.text(W - 36, y + 110, "الاثنين — المطاعم المعتادة", 14, f.p["accent"], anchor="end")
f.text(W - 36, y + 132, "الثلاثاء — مطعم الإقامة (غداء)", 14, f.p["accent"], anchor="end")
y += 162
f.card(y, 210)
f.text(W - 36, y + 28, "مدة التحقق", 16, f.p["ink"], True, "end")
cy = f.choice(y + 38, "مدة التحقق", "كل ساعة")
f.text(W - 36, cy + 14, "الوقت تقريبي ويعتمد على الإنترنت وقيود أندرويد.", 12, f.p["muted"], anchor="end")
f.button(f.h - 92, "مراجعة الخطة", "primary")
frames.append(f)

# 6 — Wizard step 3: review ------------------------------------------------
f = Frame("06 · الإعداد — الخطوة 3: المراجعة", 1080)
f.statusbar(); y = f.wizard_head("المراجعة", 3)
f.card(y, 560)
f.text(W - 36, y + 30, "خطة الأيام الثلاثة", 19, f.p["ink"], True, "end")
f.text(W - 36, y + 54, "حالة كل وجبة كما تظهر الآن في Webetu · التحقق كل ساعة", 13, f.p["muted"], anchor="end")
ry = y + 70
for day, rows in [("الثلاثاء 21/11 · اليوم", [("فطور الصباح", "مطعم الجامعة المركزي", "booked"), ("الغداء", "مطعم الإقامة", "missing"), ("العشاء", "مطعم الجامعة المركزي", "booked")]),
                  ("الأربعاء 22/11 · غدًا", [("فطور الصباح", "مطعم الجامعة المركزي", "missing"), ("الغداء", "مطعم الإقامة", "missing"), ("العشاء", "مطعم الجامعة المركزي", "missing")])]:
    f.text(W - 36, ry + 16, day, 14, f.p["ink"], True, "end"); ry += 24
    for meal, name, st in rows:
        ry = f.meal_row(ry, meal, name, st)
y = f.button(y + 574, "تأكيد وحجز الناقص", "primary")
y = f.button(y, "استبدال الحجوزات المتعارضة", "danger")
y = f.button(y, "تفعيل وإنهاء الإعداد", "primary")
y = f.button(y, "المتابعة دون أتمتة", "plain")
y = f.button(y, "تحديث المعاينة", "plain")
frames.append(f)

# 7 — Dialog: replacement ---------------------------------------------------
f = Frame("07 · حوار — تأكيد الاستبدال", 844)
f.statusbar(); f.brand()
f.dialog("إلغاء القديم وحجز البديل؟",
         ["الأربعاء 22/11 · العشاء: مطعم الجامعة المركزي ← مطعم الإقامة",
          "سيُلغى الحجز القديم ثم يُحجز البديل بعد التحقق من زواله. قد ينجح الإلغاء ويفشل الحجز الجديد، فتفقد حجز الوجبة. لا يمكن ضمان الاسترجاع.",
          "ستتوقف الأتمتة، ولن تُستبدل حجوزات أخرى تلقائيًا. هل توافق على هذه الاستبدالات فقط؟"],
         [("تراجع", "plain"), ("أوافق — استبدال", "danger")], h=340)
frames.append(f)

# 8 — Dialog: delete data ---------------------------------------------------
f = Frame("08 · حوار — حذف البيانات", 844)
f.statusbar(); f.brand()
f.dialog("حذف البيانات؟",
         ["سيُحذف الحساب والإعدادات وتتوقف الأتمتة. لن تُلغى الحجوزات الموجودة في Webetu."],
         [("إلغاء", "plain"), ("حذف", "danger")], h=260)
frames.append(f)

# 9 — Dialog: weekly day editor --------------------------------------------
f = Frame("09 · حوار — استثناء يوم (الثلاثاء)", 844)
f.statusbar(); f.brand()
f.dialog("الثلاثاء — المطاعم البديلة",
         ["يتكرر كل أسبوع حسب تاريخ الوجبة. اختر المعتاد لإزالة الاستثناء. الحفظ يوقف الأتمتة للمراجعة ولا يلغي الحجوزات الحالية."],
         [("إلغاء", "plain"), ("حفظ", "primary")], h=300)
# pickers drawn inside dialog area (preview)
f.choice(430, "فطور الصباح", "المعتاد — مطعم الجامعة المركزي")
f.choice(510, "الغداء", "مطعم الإقامة")
f.choice(590, "العشاء", "المعتاد — مطعم الجامعة المركزي")
frames.append(f)

# 10 — Dialog: interval -----------------------------------------------------
f = Frame("10 · حوار — مدة التحقق", 844)
f.statusbar(); f.brand()
f.dialog("مدة التحقق", ["من 15 دقيقة إلى 48 ساعة. التوقيت تقريبي."],
         [("إلغاء", "plain"), ("حفظ", "primary")], h=280)
f.choice(430, "مدة التحقق", "كل ساعة")
f.note(510, "المدد الطويلة قد تفوّت مواعيد بعض الوجبات. الحجز المتاح يبقى 3 أيام فقط.", "warn")
frames.append(f)

# 11 — Dialog: about --------------------------------------------------------
f = Frame("11 · حوار — حول التطبيق", 844)
f.statusbar(); f.brand()
f.dialog("Wajabati 0.13.1",
         ["عميل أندرويد لقراءة وحجز الوجبات الجامعية عبر Webetu: مطعم لكل وجبة، ومعاينة قبل الإرسال، وتحقق بعد الحجز.",
          "TFYCHE AHMED REDOUANE — طالب في ESGEN",
          "مشروع مستقل، غير تابع للوزارة ولا للديوان الوطني للخدمات الجامعية.",
          "لا يعمل الحجز التلقائي إذا كان الهاتف مغلقًا أو غير متصل بالإنترنت."],
         [("تم", "primary")], h=360)
f.rect(36, 470, W - 72, 44, f.p["surface"], 12, f.p["line"], 1)
f.text(W / 2, 498, "Instagram", 15, f.p["accent"], anchor="middle")
f.rect(36, 522, W - 72, 44, f.p["surface"], 12, f.p["line"], 1)
f.text(W / 2, 550, "LinkedIn", 15, f.p["accent"], anchor="middle")
frames.append(f)

# 12 — Dialogs: appearance / language / pending / details / privacy --------
for name, title, body, btns, h in [
    ("12 · حوار — المظهر", "المظهر", ["حسب النظام", "فاتح", "داكن"], [("إلغاء", "plain")], 300),
    ("13 · حوار — اللغة", "اللغة", ["العربية", "Français (bêta)"], [("إلغاء", "plain")], 300),
    ("14 · حوار — المحاولات المعلّقة", "المحاولات المعلّقة",
     ["الثلاثاء 21/11 · العشاء (21/11 · 09:41): بانتظار التحقق في Webetu",
      "تحقّق في Webetu قبل إزالة القفل. الإزالة لا تسترجع الحجز ولا تضمن البديل."],
     [("إلغاء", "plain"), ("تحقّقت — إزالة القفل", "danger")], 340),
    ("15 · حوار — تفاصيل العملية", "تفاصيل العملية",
     ["تعذّر تسجيل الدخول. تحقّق من رقم التسجيل وكلمة المرور ثم أعد المحاولة. (401)",
      "آخر محاولة تلقائية: 21/11 · 09:41 — آخر تأكيد كامل: 21/11 · 09:41"],
     [("تم", "primary")], 300),
    ("16 · حوار — الخصوصية والاستخدام", "الخصوصية والاستخدام",
     ["بيانات الحساب مشفّرة ومحفوظة على هاتفك. الاتصال بخدمتي MESRS وONOU فقط، دون خادم وسيط.",
      "استخدم الأتمتة إذا سمحت شروط الخدمة، وأوقفها عند الغياب. راجع الحجوزات في Webetu."],
     [("تم", "primary")], 340),
]:
    f = Frame(name, 844); f.statusbar(); f.brand()
    f.dialog(title, body, btns, h=h)
    frames.append(f)

# 17 — Bottom sheet: restaurant picker -------------------------------------
f = Frame("17 · ورقة سفلية — اختر المطعم", 844)
f.statusbar(); f.brand()
f.sheet("اختر المطعم", ["مطعم الجامعة المركزي", "مطعم الإقامة", "مطعم كلية العلوم"], selected=0)
frames.append(f)

# 18 — Dark mode: Home ------------------------------------------------------
f = Frame("18 · الرئيسية — الوضع الداكن", 1180, pal=D)
f.statusbar(); y = f.brand()
f.grad("herod", f.p["primary"], f.p["primary_deep"])
f.rect(20, y, W - 40, 196, "url(#herod)", 22)
f.text(W - 36, y + 72, "إعدادك مكتمل", 24, "#FFFFFF", True, "end")
f.wrap(W - 36, y + 96, "بعد الإعداد، يتحقق التطبيق من الحجوزات كل ساعة عند توفر الإنترنت.", 13, "#FFFFFF", 300, 18, "end")
f.rect(36, y + 128, W - 72, 6, "#FFFFFF", 3, opacity=0.35)
f.rect(36, y + 128, W - 72, 6, "#FFFFFF", 3)
f.text(W - 36, y + 152, "100% · اكتمال الإعداد 4/4", 12, "#FFFFFF", anchor="end")
f.rect(36, y + 160, W - 72, 28, "#FFFFFF", 14)
f.text(W / 2, y + 179, "عرض حجوزات الأيام الثلاثة", 15, "#0052CC", True, "middle")
y += 210
f.card(y, 150)
f.text(W - 36, y + 32, "الحجز التلقائي", 17, f.p["ink"], True, "end")
f.toggle(36, y + 14, True)
f.text(W - 36, y + 58, "مفعّل", 14, f.p["muted"], anchor="end")
f.text(W - 36, y + 80, "التحقق كل ساعة", 13, f.p["accent"], anchor="end")
f.text(W - 36, y + 102, "آخر تأكيد: 21/11 · 09:41", 13, f.p["muted"], anchor="end")
f.text(W - 36, y + 124, "الاتصال متاح", 13, f.p["muted"], anchor="end")
y += 162
y = f.heading(y, "آخر عملية")
f.card(y, 118)
f.text(W - 36, y + 26, "تم تأكيد الحجوزات بنجاح.", 14, f.p["ink"], anchor="end")
y2 = f.button(y + 36, "التفاصيل", "nav", 40)
y2 = f.button(y2, "المحاولات المعلّقة", "nav", 40)
f.tabs(0)
frames.append(f)

# ---- write individual files + combined board + tokens -------------------
for fr in frames:
    safe = fr.name.split("·")[0].strip().replace(" ", "-") + ".svg"
    with open(os.path.join(OUT, safe), "w", encoding="utf-8") as fh:
        fh.write(fr.svg())

# Combined board: all frames side-by-side in one SVG (import as one board)
gap, maxh = 40, max(fr.h for fr in frames)
total_w = sum(W + gap for _ in frames) + gap
parts = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{total_w}" height="{maxh + 80}" viewBox="0 0 {total_w} {maxh + 80}" dir="rtl">',
         f'<rect width="{total_w}" height="{maxh + 80}" fill="#E9E9EE"/>']
x = gap
for fr in frames:
    inner = fr.svg().split(">", 1)[1].rsplit("</svg>", 1)[0]
    parts.append(f'<g transform="translate({x},40)">{inner}</g>')
    parts.append(f'<text x="{x + W/2}" y="{maxh + 62}" font-family="{FONT}" font-size="13" fill="#333" text-anchor="middle">{esc(fr.name)}</text>')
    x += W + gap
parts.append("</svg>")
with open(os.path.join(os.path.dirname(__file__), "Wajabati-all-screens.svg"), "w", encoding="utf-8") as fh:
    fh.write("\n".join(parts) + "\n")

tokens = {
    "meta": {"app": "Wajabati", "version": "0.13.1", "source": "app/src/main/java/dz/local/mealguard/Appearance.java + res/values/{colors,styles}.xml",
             "frame": {"width": 390, "radius_card": 20, "radius_button": 12, "radius_field": 10, "padding_page": 20}},
    "color": {
        "brand/blue": P["brand"], "brand/deep": P["brand_deep"],
        "light/background": P["background"], "light/surface": P["surface"], "light/ink": P["ink"],
        "light/muted": P["muted"], "light/accent": P["accent"], "light/primary": P["primary"],
        "light/segment": P["segment"], "light/line": P["line"], "light/chevron": P["chevron"],
        "light/success/bg": P["success_bg"], "light/success/ink": P["success_ink"],
        "light/warning/bg": P["warning_bg"], "light/warning/ink": P["warning_ink"],
        "light/danger": P["danger"], "light/danger/bg": P["danger_bg"],
        "dark/background": D["background"], "dark/surface": D["surface"], "dark/ink": D["ink"],
        "dark/muted": D["muted"], "dark/accent": D["accent"], "dark/primary": D["primary"],
        "dark/segment": D["segment"], "dark/line": D["line"],
        "dark/success/bg": D["success_bg"], "dark/success/ink": D["success_ink"],
        "dark/warning/bg": D["warning_bg"], "dark/warning/ink": D["warning_ink"],
        "dark/danger": D["danger"], "dark/danger/bg": D["danger_bg"],
        "toggle/on": "#34C759",
    },
    "typography": {"font_family": "system sans (Android sans / Inter in Figma)", "brand_title": 34, "screen_title": 22,
                   "card_title": 17, "body": 14, "caption": 13, "button": 16, "tab": 12},
    "screens": [fr.name for fr in frames],
    "components": ["Button/Primary", "Button/Plain", "Button/Nav (chevron row)", "Button/Danger",
                   "Card", "SettingsRow (icon tile + title + value + chevron)", "MealRow + StatusChip",
                   "ChoiceField + BottomSheet picker", "PillToggle (64×50)", "ProgressBar", "AlertDialog", "BottomTabBar (3)"],
}
with open(os.path.join(os.path.dirname(__file__), "design-tokens.json"), "w", encoding="utf-8") as fh:
    json.dump(tokens, fh, ensure_ascii=False, indent=2)

print(f"OK: {len(frames)} screens -> {OUT}")
for fr in frames:
    print(" -", fr.name)
