<div align="center">

<img src="docs/assets/banner.png" alt="Wajabati — حجز الوجبات الجامعية على Webetu" width="100%">

# Wajabati

**تطبيق أندرويد شخصي لحجز الوجبات الجامعية عبر Webetu · الإصدار `0.13.1`**

[![version](https://img.shields.io/badge/version-0.13.1-0066DD?labelColor=374151)](https://github.com/Ahmed47-t/Wajabati/releases)
[![android](https://img.shields.io/badge/Android-8.0%2B-0066DD?labelColor=374151)](https://developer.android.com/about/versions/oreo)
[![java](https://img.shields.io/badge/Java-17-0066DD?labelColor=374151)](app/build.gradle)
[![status](https://img.shields.io/badge/status-experimental-6B7280?labelColor=374151)](#حدود-التطبيق)

</div>

> [!WARNING]
> **مشروع شخصي تجريبي وغير رسمي**، غير تابع لوزارة التعليم العالي (MESRS) أو الديوان الوطني للخدمات الجامعية (ONOU). لم يُختبر على حساب حقيقي أو هاتف فعلي، وملف التثبيت موقّع بمفتاح تطوير (Debug). لا تتجاوز أي قيود وصول أو التحقق بخطوتين أو CAPTCHA.

---

## الفكرة باختصار

يقرأ **Wajabati** حجوزاتك الحالية من منصة Webetu، ويعرض خطة الأيام الثلاثة القادمة لتراجعها، ولا يرسل أي حجز إلا بعد موافقتك.

| البند | التفصيل |
|---|---|
| المنصّة والتقنية | Android 8.0+ (targetSdk 35) · Java 17 بدون مكتبات واجهة خارجية |
| اللغات والمظهر | عربي افتراضيًا (RTL) + فرنسي · فاتح / داكن / حسب النظام |
| التخزين والأمان | تشفير كلمة المرور محليًا (`Android Keystore / AES-GCM`) · بدون نسخ سحابي |
| الاتصال | مباشر مع `MESRS` و`ONOU` عبر HTTPS · بدون سيرفر وسيط أو تتبع |
| الاختبارات المحلية | `209` تشغيلات اختبار (JUnit + Robolectric على API 28/35) · `0` أخطاء Lint |

---

## كيف يعمل

1. **قراءة الحساب:** تسجيل الدخول إلى Webetu لجلب الإقامة والمطاعم المتاحة والحجوزات الحالية (بدون إرسال أي حجز).
2. **اختيار المطاعم والجدول:** تحديد مطعم لكل وجبة (فطور، غداء، عشاء)، مع إمكانية وضع استثناءات لأيام الأسبوع، وتحديد مدة الفحص التلقائي (من 15 دقيقة إلى 48 ساعة).
3. **المعاينة والموافقة:** عرض خطة الأيام الثلاثة القادمة لتأكيد حجز الوجبات الناقصة يدويًا. عند وجود حجز في مطعم مختلف، يطلب التطبيق موافقة صريحة قبل إلغائه واستبداله.
4. **التنفيذ والتأكد:** يرسل الطلبات الناقصة فقط ثم يعيد قراءة الحجوزات للتحقق. إذا انقطع الاتصال أثناء الحجز، يعلّق تلك الوجبة ولا يعيد إرسالها تلقائيًا لمنع التكرار.

---

## حدود التطبيق

- **لا يعمل في الخلفية والهاتف مغلق أو بدون إنترنت.** يعتمد على `WorkManager` وقد يتأخر حسب قيود البطارية في النظام.
- **استبدال الحجز يتم يدويًا على خطوتين** (إلغاء القديم ثم حجز الجديد)؛ إذا فشل الحجز الجديد بعد الإلغاء، تبقى الوجبة بدون حجز.
- إيقاف الحجز التلقائي يمنع الفحوصات القادمة فقط ولا يلغي الحجوزات المسجلة مسبقًا.
- واجهات Webetu غير رسمية وقد تتوقف إذا غيّرت المنصة طريقة عملها.

---

## التثبيت والبناء من المصدر

### التثبيت
1. نزّل ملف الـ APK من صفحة [الإصدارات](https://github.com/Ahmed47-t/Wajabati/releases).
2. ثبّته على أندرويد 8.0 فما فوق. الحجز التلقائي يكون **متوقفًا افتراضيًا** حتى تكمل الإعداد وتراجع الخطة.


### البناء من المصدر
يتطلب **JDK 17** و**Android SDK 35**:

```sh
./gradlew --no-daemon :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease
```

الملف الناتج: `app/build/outputs/apk/release/app-release.apk`

---

## بنية الكود

| الملف | الدور |
|---|---|
| `MainActivity` | الواجهة، خطوات الإعداد، المعاينة، والموافقة اليدوية |
| `Engine` · `WeeklySchedule` | بناء خطة 3 أيام، استثناءات الأسبوع، ومنع تكرار الطلبات |
| `Runner` · `Replacement` | تنسيق التنفيذ ومنع التداخل، وتدفق إلغاء/استبدال الحجز |
| `ServiceApi` | الاتصال المباشر بواجهات Webetu وONOU عبر HTTPS |
| `Vault` | تشفير وحفظ كلمة المرور والإعدادات على الهاتف |
| `Automation` · `SyncPolicy` | الجدولة الدورية عبر `WorkManager` وإشعارات الحالة |

---

## الوثائق والترخيص

- [دليل الاستخدام](docs/USER_GUIDE_AR.md) · [نشر الـ APK في GitHub Packages](docs/PACKAGES.md) · [دليل اختبار الاستخدام](docs/USABILITY_TEST_AR.md) · [نتائج الفحص المحلي](docs/validation/README.md)
- مشروع شخصي مفتوح للاطلاع والمراجعة فقط؛ لا يمنح ترخيصًا لإعادة التوزيع أو الاستخدام التجاري.

---

<div dir="auto">

## English Summary

**Wajabati** is a personal, experimental Android app (Java 17, no third-party UI libraries) for managing university meal reservations on Algeria's **Webetu** platform.

- **Consent first:** Reads your current reservations, previews the next 3 days, and never books or replaces a meal without explicit user approval.
- **No duplicate retries:** Stores pending attempts locally before sending and verifies the final reservation list afterward.
- **On-device privacy:** Encrypts credentials with `Android Keystore (AES-GCM)` and talks directly to `MESRS` / `ONOU` over HTTPS — no intermediary server, no analytics, and `allowBackup=false`.
- **Not official:** Not affiliated with MESRS or ONOU, not tested on a live account or physical device, and signed with a debug key.

Build & verify (JDK 17 + Android SDK 35):

```sh
./gradlew --no-daemon :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease
```

</div>

<div align="center">
<sub>© 2026 TFYCHE AHMED REDOUANE</sub>
</div>
