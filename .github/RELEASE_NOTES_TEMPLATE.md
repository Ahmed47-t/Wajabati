# قالب ملاحظات الإصدار · Release Notes Template

## عنوان الإصدار

```text
Wajabati <الإصدار> — <ملخص قصير للتغيير>
```

## نص الإصدار

```markdown
## Wajabati 0.x.y — <العنوان>

**البناء:** versionCode NN · Android 8.0+ (targetSdk 35) · موقّع بمفتاح Debug — **نسخة شخصية تجريبية وليست للنشر في المتجر**.

### ما الجديد
- <تغيير واضح للمستخدم في سطر واحد>

### الأمان والحجز
- <أي تغيير في مسار الحجز أو الاستبدال، أو «لا تغيير»>

### الفحص
- اختبارات `:app:testReleaseUnitTest`: ناجحة (0 فشل).
- فحص `:app:lintRelease`: 0 أخطاء.
- لم يُختبر على حساب حقيقي أو هاتف فعلي.

### الملف المرفق
- `<اسم الملف>.apk`
- SHA-256: `<البصمة>`
```

## قائمة التحقق قبل النشر

- [ ] تحديث `versionCode` و`versionName` في `app/build.gradle` و`User-Agent` في `ServiceApi.java`.
- [ ] نجاح `:app:testReleaseUnitTest` و`:app:lintRelease`.
- [ ] تحديث رقم الإصدار في `README.md` و`strings.xml` وقيمة `SHA-256` في `docs/USER_GUIDE_AR.md`.
- [ ] التأكد من خلو المستودع من أي كلمات مرور أو مفاتيح (`git status`).
