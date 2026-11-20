<div align="center">

<img src="assets/logo-lockup.png" width="260" alt="Wajabati">

# نشر الـAPK كـPackage

</div>

هذا الملف يشرح كيف يظهر ملف `Wajabati-0.13.1.apk` داخل صندوق **Packages** في صفحة المستودع (الرابط الأيمن في GitHub).

## لماذا لا يوجد Package الآن

| السبب | التفصيل |
|---|---|
| لا يوجد ملف مطابق للشيفرة | بناء أندرويد يحتاج JDK 17 وAndroid SDK 35، وهما غير متوفّرين في البيئة التي عُدّلت فيها الشيفرة، لذا لم يُولَّد APK جديد بعد تغيير الاسم والشعار |
| الرفع محجوب في تلك البيئة | `maven.pkg.github.com` و`uploads.github.com` غير متاحين من البيئة (فشل SSL/EoF)، فحتى الملف القديم لا يمكن رفعه من هناك |
| الحل | بناء الملف مرة واحدة على جهازك أو على GitHub Actions، ثم النشر بالأمر أو بسير العمل أدناه |

## الطريقة ١ — GitHub Actions (موصى بها، لا تحتاج بيئة أندرويد على جهازك)

1. **Settings ← Actions ← General ← Workflow permissions:** اختر **Read and write permissions** ثم Save (صلاحية `packages: write` لازمة).
2. انسخ سير العمل الجاهز:
   ```sh
   mkdir -p .github/workflows && cp docs/ci/publish-apk-workflow.yml .github/workflows/publish-apk.yml
   git add .github/workflows/publish-apk.yml && git commit -m "ci: publish APK package" && git push
   ```
3. من تبويب **Actions ← publish-apk ← Run workflow**، أو ادفع وسمًا:
   ```sh
   git tag v0.13.1 && git push origin v0.13.1
   ```
4. بعد انتهاء التشغيل: **Packages ← wajabati-apk** يظهر في صفحة المستودع، وملف `Wajabati-0.13.1.apk` يُرفَق أيضًا بالإصدار.

السير العمل يبني الـAPK، يرتّب تخطيط Maven (`docs/ci/stage-apk-package.sh`)، ثم يرفع الملفات إلى:
`https://maven.pkg.github.com/Ahmed47-t/Wajabati/dz/local/mealguard/wajabati-apk/0.13.1/`

## الطريقة ٢ — من جهازك

```sh
./tools/release.sh                 # يبني + يفحص + يرتّب حزمة Maven في build/gh-packages/
GITHUB_ACTOR=Ahmed47-t GITHUB_TOKEN=$(gh auth token) ./tools/release.sh --publish
```

`--publish` ينشئ إصدار GitHub ويرفع الـAPK، ثم يحاول رفع الحزمة إن كان الرمز يملك صلاحية `packages:write`. رمز `gh auth token` الافتراضي قد لا يملكها؛ لإنشائها:

- **Settings ← Developer settings ← Personal access tokens:** أنشئ رمزًا بصلاحية `write:packages` (و`repo` للمستودع الخاص)، ثم:
  ```sh
  echo "$TOKEN" | gh auth login --with-token --scopes write:packages
  ```

## كيف تُنزّل الحزمة لاحقًا

الحزمة صيغة Maven، لذا تُنزَّل بالمسار المباشر (مع رمز وصول) أو بأداة Maven:

```sh
curl -L -u "USER:TOKEN" -o Wajabati-0.13.1.apk \
  https://maven.pkg.github.com/Ahmed47-t/Wajabati/dz/local/mealguard/wajabati-apk/0.13.1/wajabati-apk-0.13.1.apk
```

```xml
<repository>
  <id>github</id>
  <url>https://maven.pkg.github.com/Ahmed47-t/Wajabati</url>
</repository>
<dependency>
  <groupId>dz.local.mealguard</groupId>
  <artifactId>wajabati-apk</artifactId>
  <version>0.13.1</version>
  <type>apk</type>
</dependency>
```

> [!WARNING]
> الـAPK موقّع بمفتاح debug محلي، والحزمة على GitHub Packages **خاصة** افتراضيًا (تحتاج رمز وصول حتى للمستودع العام). راجع سياسة إعادة التوزيع في `README` قبل مشاركة الحزمة مع أي طرف آخر.
