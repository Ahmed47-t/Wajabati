<div align="center">

<img src="assets/logo-lockup.png" width="260" alt="Wajabati">

# بناء ونشر الـ APK في GitHub Packages

</div>

يشرح هذا الملف طريقة بناء `Wajabati-0.13.1.apk` وإرفاقه في **Releases** ونشره في **GitHub Packages** (`wajabati-apk`).

## الطريقة 1 — عبر GitHub Actions (بدون Android SDK على جهازك)

1. من إعدادات المستودع: **Settings ← Actions ← General ← Workflow permissions**، اختر **Read and write permissions** ثم احفظ.
2. انسخ ملف السير الجاهز إلى `.github/workflows/`:
   ```sh
   mkdir -p .github/workflows && cp docs/ci/publish-apk-workflow.yml .github/workflows/publish-apk.yml
   git add .github/workflows/publish-apk.yml && git commit -m "ci: add publish-apk workflow" && git push
   ```
3. شغّل السير يدويًا من تبويب **Actions ← publish-apk ← Run workflow**، أو ادفع وسمًا مثل `v0.13.1`:
   ```sh
   git tag v0.13.1 && git push origin v0.13.1
   ```

يقوم السير باختبار الكود، بناء الـ APK، تجهيز بنية Maven (`docs/ci/stage-apk-package.sh`)، ثم رفع الحزمة إلى GitHub Packages وإرفاق الـ APK بالإصدار.

## الطريقة 2 — من جهازك (JDK 17 + Android SDK 35)

```sh
./tools/release.sh
```

للنشر المباشر (يتطلب `gh` وصلاحية `write:packages` إذا أردت رفع الحزمة):

```sh
GITHUB_ACTOR=Ahmed47-t GITHUB_TOKEN=$(gh auth token) ./tools/release.sh --publish
```

> [!NOTE]
> ملف الـ APK الناتج موقّع بمفتاح Debug محلي، وحزمة GitHub Packages تتطلب رمز وصول للتنزيل حتى في المستودعات العامة.
