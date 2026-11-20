# أصول المستودع · Repository artwork

> ملفات PNG/JPG هنا **مخرجات مولّدة**، والسكربت المولّد لها محفوظ في هذا المجلد حتى يمكن إعادة إنتاجها بتغيير نصّي فقط.

| الملف | الاستخدام | المقاس |
|---|---|---|
| `logo-mark.png` · `logo-mark-512.png` | الشعار كمربّع (أفاتار، أيقونة عرض) | 1024 · 512 |
| `logo-lockup.png` · `logo-lockup-dark.png` | الشعار مع الاسم، لخلفية فاتحة وداكنة | 648×258 |
| `banner.png` · `banner.jpg` | بانر أعلى README | 1600×560 |
| `social-preview.png` | Social preview في GitHub (Settings ← Social preview) | 1280×640 |
| `setup-journey.png` | رسم خطوات الإعداد الأربع | 1600×430 |
| `architecture.png` | رسم الطبقات (MainActivity / Runner / Engine / ServiceApi / Vault) | 1500×790 |

## إعادة التوليد

```sh
python -m venv .venv && .venv/bin/pip install Pillow arabic-reshaper python-bidi fonttools
# ضع الخطوط في /tmp/fonts أو مرّر WAJABATI_FONTS=/path/to/fonts
python docs/assets/generate.py
```

الخطوط المطلوبة (كلها برخصة OFL):

| الخط | الملف | المصدر |
|---|---|---|
| Noto Sans Arabic | `NSA-Regular.ttf` · `NSA-SemiBold.ttf` · `NSA-Bold.ttf` | `github.com/notofonts/notofonts.github.io` (fonts/NotoSansArabic/hinted/ttf) |
| Inter | `Inter-500.ttf` · `Inter-600.ttf` · `Inter-700.ttf` · `Inter-800.ttf` | حزمة npm `@fontsource/inter` (ملفات `inter-latin-*-normal.woff` تُحوَّل إلى TTF بـfontTools) |

## ملاحظات تقنية

- Pillow هنا بلا `libraqm`، لذا يقوم `rt.py` بتشكيل الحروف العربية بمكتبة `arabic-reshaper` وعكسها بـ`python-bidi` كلمة بكلمة، مع الحفاظ على ترتيب الكلمات اللاتينية والأرقام.
- الشعار في `mark.py` مرسوم بهندسة بسيطة (دائرة طبق + شوكة وملعقة + هلال)، وليس صورة مولّدة، لذا يبقى حادًّا في كل المقاسات.
- لا يحتوي هذا المجلد على أي صورة شخصية أو بيانات حساب.
