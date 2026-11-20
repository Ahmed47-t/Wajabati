# صور وشعارات المستودع · Repository Assets

الصور داخل هذا المجلد مولّدة برمجيًا عبر `generate.py` حتى يمكن تعديل نصوصها بسهولة:

| الملف | الاستخدام | المقاس |
|---|---|---|
| `logo-mark.png` · `logo-mark-512.png` | الشعار المربع | `1024×1024` · `512×512` |
| `logo-lockup.png` · `logo-lockup-dark.png` | الشعار مع الاسم (فاتح وداكن) | `648×258` |
| `banner.png` · `banner.jpg` | بانر الصفحة الرئيسية | `1600×560` |
| `social-preview.png` | صورة المشاركة في GitHub | `1280×640` |
| `setup-journey.png` | مخطط خطوات الإعداد الأربع | `1600×430` |
| `architecture.png` | مخطط بنية التطبيق | `1500×790` |

## إعادة توليد الصور

```sh
python -m venv .venv && .venv/bin/pip install Pillow arabic-reshaper python-bidi fonttools
WAJABATI_FONTS=/tmp/fonts python docs/assets/generate.py
```

الخطوط المستخدمة في `/tmp/fonts` (رخصة OFL):
- **Noto Sans Arabic** (`NSA-Regular.ttf`, `NSA-SemiBold.ttf`, `NSA-Bold.ttf`)
- **Inter** (`Inter-500.ttf` .. `Inter-800.ttf`)

يستخدم `rt.py` مكتبتي `arabic-reshaper` و`python-bidi` لضبط تشكيل واتجاه النصوص العربية واللاتينية داخل Pillow.
