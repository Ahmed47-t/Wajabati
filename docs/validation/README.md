# نتائج الفحص المحلي · Local Validation

<div align="center">

<img src="../assets/logo-lockup.png" width="220" alt="Wajabati">

</div>

يحفظ هذا المجلد تقارير الفحص المحلي، مثل تقرير Lint: [`lint-0.13.0.txt`](lint-0.13.0.txt) (`0` أخطاء · `22` تحذيرًا).

## ما الذي تم فحصه محليًا؟

| تم فحصه (اختبارات محاكاة) | لم يُفحص بعد (يحتاج حسابًا وجهازًا فعليًا) |
|---|---|
| `209` تشغيل اختبار بـ JUnit وRobolectric على API 28 و35 | تسجيل الدخول بحساب حقيقي |
| منطق الخطة، الاستبدال، الجدولة، الواجهة، والترجمة | الحجز الفعلي على خوادم Webetu |
| فحص Android Lint | سلوك الجدولة والبطارية على هاتف فعلي |

## إعادة تشغيل الفحوصات

```sh
./gradlew --no-daemon :app:testReleaseUnitTest :app:lintRelease
```

تقرير Lint الناتج يُحفظ في: `app/build/reports/lint-results-release.txt`.
