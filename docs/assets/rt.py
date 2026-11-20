# -*- coding: utf-8 -*-
"""Bidi-aware text helper for Pillow without libraqm.

Arabic is reshaped word by word (contextual forms) and reversed; the words themselves are
laid out right-to-left, while a Latin/digit word keeps its own left-to-right order.
"""
import re
from PIL import ImageFont
import arabic_reshaper
from bidi.algorithm import get_display

AR = re.compile(r'[\u0600-\u06FF\u0750-\u077F\uFB50-\uFDFF\uFE70-\uFEFF]')


class FontSet:
    def __init__(self, ar_path, latin_path, size):
        self.ar = ImageFont.truetype(ar_path, size)
        self.latin = ImageFont.truetype(latin_path, size)
        self.size = size

    def token_font(self, tok):
        return self.ar if AR.search(tok) else self.latin


def _shape(tok):
    return get_display(arabic_reshaper.reshape(tok))


LAT = re.compile(r'[A-Za-z0-9]')


def _tokens(text):
    raw = text.split(' ')
    out, ltr = [], []
    for i, tok in enumerate(raw):
        if AR.search(tok):
            if ltr:
                out.extend(reversed(ltr))
                ltr.clear()
            out.append(tok)
        elif LAT.search(tok):
            ltr.append(tok)
        else:
            # Keep neutral separators (e.g. '·', '+', '/') inside an active LTR run only if another LTR word follows.
            next_is_ltr = any(LAT.search(t) for t in raw[i + 1:i + 2]) and not AR.search(raw[i + 1]) if i + 1 < len(raw) else False
            if ltr and next_is_ltr:
                ltr.append(tok)
            else:
                if ltr:
                    out.extend(reversed(ltr))
                    ltr.clear()
                out.append(tok)
    if ltr:
        out.extend(reversed(ltr))
    return out


def measure(text, fs):
    w = h = 0
    toks = _tokens(text)
    for i, tok in enumerate(toks):
        f = fs.token_font(tok)
        s = _shape(tok) if AR.search(tok) else tok
        bb = f.getbbox(s)
        w += bb[2] - bb[0]
        h = max(h, bb[3] - bb[1])
        if i < len(toks) - 1:
            w += fs.ar.getlength(' ')
    return w, h


def draw(d, xy, text, fs, fill, anchor='rm'):
    """anchor[0]: r/l/c edge · anchor[1]: m (middle), t, b, s (baseline)."""
    total, _ = measure(text, fs)
    x_right, y = xy
    if anchor[0] == 'c':
        x_right = x_right + total / 2
    elif anchor[0] == 'l':
        x_right = x_right + total
    toks = _tokens(text)
    for i, tok in enumerate(toks):
        f = fs.token_font(tok)
        s = _shape(tok) if AR.search(tok) else tok
        bb = f.getbbox(s)
        tw = bb[2] - bb[0]
        asc, desc = f.getmetrics()
        if anchor[1] == 'm':
            ty = y - (asc + desc) / 2
        elif anchor[1] == 't':
            ty = y
        elif anchor[1] == 'b':
            ty = y - (asc + desc)
        else:
            ty = y - asc
        d.text((x_right - tw - bb[0], ty), s, font=f, fill=fill)
        x_right -= tw
        if i < len(toks) - 1:
            x_right -= fs.ar.getlength(' ')
    return total
