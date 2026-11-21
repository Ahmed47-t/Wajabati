# Wajabati 0.14.0 / وجباتي

Personal experimental Android meal-booking client. Not affiliated with MESRS or ONOU.

**Not live-account tested.** Do not rely on it until the read-only login, preview, manually approved booking and official Webetu verification all pass. Use only if permitted by the service's terms. No CAPTCHA, MFA, access-control or reservation-window bypass is implemented. Manual cancellation/rebooking is available only with separate explicit approval and may lose the old reservation if rebooking fails.

## 0.14.0 — battery-setup gate after booking

- The confirmation area shown after a successful booking was replaced by a **battery setup card** inside the review step: why the exclusion matters, a live status line (excluded / not yet) and a direct Arabic button to the system battery-optimisation screen (choose «غير مقيّد» / unrestricted). The previous «تحديث المعاينة» / «المتابعة دون أتمتة» post-booking flow is gone.
- **Activation is refused until the app is excluded from battery optimisation**: pressing «تفعيل وإنهاء الإعداد» first opens the battery prompt and shows a hint instead of enabling automation.
- USER_GUIDE_AR documents the battery step; the About dialog now reads 0.14.0.
- Validation: **228 passing local tests** (226 existing + the battery-gate/Swap test), lint 0 errors. Not verified on a physical device; this build is debug-signed with a **different** key than 0.13.1, so it cannot update an existing 0.13.1 install — uninstall first (app data is wiped).

## 0.13.1 — optional meals and weekly exceptions

- **Optional meals**: a meal can be left unconfigured instead of forcing all three.
- **Weekly exceptions can enable a meal for one weekday only** («هذا اليوم فقط»), even a meal that is otherwise unconfigured; the exception recurs weekly and pausing automation keeps existing reservations untouched.
- Validation: **226 passing local tests**, lint clean. Same debug certificate as 0.13.0 (versionCode 15), installs in place over 0.13.0.

## 0.13.0 — stuck-booking recovery and audit cleanups

Fixes the "pending attempt" deadlock seen on 0.12.0 (one ambiguous write froze the whole three-day plan) and applies the full audit of 0.12.0 (dead code, duplication, stale assumptions).

- **A pending lock now holds only its own meal.** `Engine.apply` books and verifies every other meal; the locked meal is never re-sent and is reported at the end with the exact fix path (الإعدادات ← الأدوات ← «المحاولات المعلّقة»). The old behaviour blocked the entire plan before any write (EngineTest now asserts the per-meal rule).
- **Journal locks whose day left the booking window are expired automatically**, so ambiguous leftovers cannot rot the journal forever.
- **Pending journal entries record `at` and `reason`**; the pending-attempts dialog lists each entry with its time and cause instead of raw keys.
- **"المحاولات المعلّقة" is one tap away** from the Home "آخر عملية" card.
- **Today's meals card reads real state**: the plan window is tomorrow..day 3, so the read-only cache now also stores today's rows; the "لم تُقرأ حالة الحجز بعد" hint is only shown before the first read, and reading the Bookings tab really does fill the card.
- **Restaurant names are display-sanitised** (`Engine.clean`): tatweel and combining stroke marks (U+0336) that the service may return no longer render as strike-through lines. Name comparison (`norm`) is unchanged.
- Dead code removed: `compactPlan`, `Engine.describe`, `WeeklySchedule.summary`, seven unused fields, an unreachable duplicate check in `Runner.book`, the unused `RECEIVE_BOOT_COMPLETED` permission, `ic_meal.xml`, and the obsolete `mipmap-anydpi-v26` qualifier. `tools/patch_*.py` (already-applied one-shot mutators) are dropped from the tree.
- Duplicated logic unified: `Automation.disengage()` for the automation-off triple, one shared failure-notification prefix.
- Stale metadata fixed: User-Agent now `MealGuard/0.13.0`; README/doc banners no longer link to missing release-note files.
- Behaviour deliberately unchanged: write-then-verify journaling, never-resend policy for uncertain writes, replacement flow, scheduling policy, and all security/storage choices.

## 0.12.0

- Password field: the field and the show/hide button are one outlined box (field takes the free width, 48dp button at the end, slashed eye while the password is visible), so text never runs under the icon.
- Battery step opens the system prompt for this app directly (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, new permission `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`); falls back to the app info page.
- About: the name is now TFYCHE AHMED REDOUANE.

## 0.11.0 — owner's notes after trying 0.10.0 on a phone

- **Weekly exceptions inside step 2**: the seven days are edited in "الوجبات والمدة" under the three restaurants; Settings → "الاستثناءات الأسبوعية" opens that step. Opening a day before the usual restaurants were saved saves the current picks first. The separate weekly screen is gone.
- **Shorter login step**: the "قبل إدخال كلمة المرور" box, the "leave the password empty" hint and the Keystore line were removed at the owner's request. The same facts remain under "الخصوصية والاستخدام" and on the Home checklist.
- **No confirmation dialogs for booking and activation**: "تأكيد وحجز الناقص" and "تفعيل" act on the first tap. The checks before them are unchanged (same-day preview matching the saved account and choices; notification permission). The dialog before **replacing existing reservations** is kept, because it lists what will be cancelled.
- **Battery is step 4**: the checklist and progress are now out of 4; the step opens the system battery-optimisation list and is ticked when the app is excluded. Booking does not depend on it.
- **"إعدادك مكتمل" card is shown once**: after it has been seen and the user leaves Home, it stays hidden (it returns if a step becomes undone).

## 0.10.0 — mockup-based screens (booking logic unchanged)

- **Home**: progress card (0–3 of 3, percentage, next-step button) and a three-step setup checklist with done / current / pending states; a later step cannot be opened before the previous one. "Today's meals" card: a status chip is shown only when it comes from an actual read-only reading that is newer than the last booking run — the app never shows "booked" by assumption.
- **Reservations tab** (replaces the Meals tab): next three days as one card per day, read-only loading banner with skeleton cards, "refresh from Webetu", and a button to the existing review/consent step when something is missing. Reading is cached for 10 minutes; nothing is booked from this tab.
- **Setup wizard**: title + "step n of 3" + progress bar, close (X) button, show/hide password, Android Keystore · AES-GCM note; "Back" is pinned at the bottom.
- **Settings**: rows with icons and current values (student number, masked password state, restaurant per meal, weekly exceptions, check interval, notification state → system notification settings). The weekly schedule is now reached from Settings.
- **Not added on purpose**: the notifications tab and the header bell (owner's request); university / residence name and bed counts from the mockup (the app does not have that data).
- `tools/strings_fr.py` and `tools/strings_ar.json` were removed: `values/strings.xml` and `values-fr/strings.xml` are now edited directly (220 keys each).

## 0.9.0 — phase 3 of the UX audit (booking logic unchanged)

- Bottom navigation with three pinned destinations: Home, Meals, Settings. The top segmented tabs are gone.
- New Meals screen: usual restaurant per meal and a seven-day weekly schedule; each day opens its editor (replaces the nested seven-item dialog).
- `ChoiceField` bottom sheet with large rows replaces every stock `Spinner` (meal restaurants, interval, weekly editor).
- The primary action of each setup step and its error message are pinned above the keyboard, outside the scroll area.
- Welcome and trust points on the first screen.
- All UI-layer strings moved to `res/values/strings.xml` (182 strings); French (beta) in `res/values-fr`, chosen in Settings > Language. Arabic stays the default whatever the phone language. Booking-engine messages, notifications and the About bio are still Arabic.
- Adaptive launcher icon with a monochrome layer.
- Usability test protocol: `docs/USABILITY_TEST_AR.md` (not run yet).
- **194 passing local tests** (168 original + 26 in `UxTest`); lint 0 errors / 22 warnings. Not verified on a physical device or with a live account.

## 0.8.0 — UX audit fixes (built on 0.7.0, booking logic unchanged)

- System Back steps back inside the setup and settings instead of closing the app; the wizard returns to the page it was opened from.
- A pinned progress bar is visible on every page while work is running; disabled controls are dimmed.
- Errors stay next to the action that caused them (no transient toasts); 401/403 are explained in plain Arabic.
- Home shows per-meal booking status for the coming days (last full confirmation, or last read-only preview with its time).
- Review screen shows one row per meal with a text status chip; the replacement action is styled as destructive.
- Login step explains where the password goes before asking for it; fields have visible labels and outlines.
- No fixed heights (checked at font scale 1.6); headings, live regions, tab state and labels for screen readers; 3:1 outline on the off switch and the selected tab.
- Chevron only on navigation rows; values always on the trailing edge; "الوضع الليلي" renamed "المظهر".
- **184 passing local tests** (168 existing + 16 new in `UxTest`); lint 0 errors / 18 warnings.
- Optional screenshots: `MEALGUARD_SHOTS=<folder> ./gradlew :app:testReleaseUnitTest --tests '*ScreenshotTest'`.
- Not verified on a physical device or with a live account. The APK is signed with a local debug key, so it cannot update an install signed with a different key.


## 0.7.0 — explicitly approved reservation replacement

Release notes for 0.7.0 are not part of this archive.

- Read-only preview now lists conflicts with current reservation ID/name and desired restaurant. Separate explicit consent is required for the displayed list; background automation never cancels.
- Cancellation is **not an atomic swap**: the old reservation can be cancelled while the new booking fails, leaving no meal reservation. No automatic rollback or guarantee of recovery.
- A new manual `Replacement` state machine revalidates the approved account/day/schedule/conflict list, checks offerings, saves a durable lock, sends one DELETE for the own-account reservation ID, then verifies absence before one replacement POST. Writes never retry/fallback/refresh-and-replay. Lost responses reconcile through read-only history.
- Ambiguous results retain account/date/meal locks. Changed/duplicate/unnamed/unknown-state reservations fail closed. Partial replacement batches are possible; the remaining plan must be refreshed and manually confirmed before automation is re-enabled.
- Signed DELETE `/reservemeal/{id}` with meal token and account context follows reviewed third-party client source, **not an official API specification or successful live test**. Server cancellation/booking rules may refuse it.
- **168 passing local tests**, 0 failures/errors/skips; lint 0 errors / 17 warnings. No live account, actual cancellation/booking, physical Samsung or encrypted-device upgrade/timing/security audit.
- VersionCode 8; APK signature verified and certificate matches 0.6.0.

## Previous release 0.6.0 — weekly restaurant exceptions

Release notes for 0.6.0 are not part of this archive.

- Default restaurants remain independent for the three meals. Optional recurring overrides cover any of seven weekdays and any meal, using account-derived offered restaurants only.
- Settings → weekly exceptions refreshes account restaurants, then lets the user edit one day at a time. Default removes an override; Cancel does not save.
- ISO weekday of the meal's Algeria-local date selects the restaurant, including cross-year plans. The booking window remains tomorrow through day 3.
- Actual schedule changes stop automation and invalidate approval. Preview lists the actual restaurant for every meal. Existing reservations are never cancelled/replaced; conflicts or unavailable required restaurants block the entire plan before writes. Pending locks remain account/date/meal-scoped.
- Upgrade preserves current default choices with no invented exceptions. System/light/dark, About and 15–2880-minute intervals remain.
- **136 passing local tests**, no failures/errors/skips; lint 0 errors / 16 warnings. API 28/35 UI tests use Robolectric, not a physical Samsung. No live account, actual booking, encrypted physical-device migration, timing or independent security validation.
- VersionCode 7, same package and verified development signing certificate as 0.5.0.

## Previous release 0.5.0 — appearance, 48-hour checks and About

Release notes for 0.5.0 (not in this archive).

- Verification interval: 15–2880 minutes, including 24-hour and 48-hour presets. This is not a longer booking window or an exact timing guarantee; new-day/failure checks still apply.
- Persistent system / light / dark appearance, themed controls and dialogs.
- About: AHMED TFYECHE — طالب في ESGEN, with the supplied Instagram and LinkedIn links, opened externally only on tap.
- Interval-aware stale-confirmation warning and lifecycle restoration of settings/meal drafts. Existing booking protections remain unchanged.
- Local validation: **113 passing tests**, no failures/errors/skips; lint 0 errors / 16 warnings. No live account, physical Samsung, real-time scheduler or encrypted-device upgrade validation.
- VersionCode 6, same package and development signing certificate as 0.4.0.

## Previous release 0.4.0 — generalized account-driven APK

Release notes for 0.4.0 are not part of this archive. This is an APK-first iteration, not a Play Store production release.

- No location-specific restaurant names, IDs or selection rules in the UI or planning logic. The authenticated service supplies the account's offered restaurants and real profile identifiers. This is structural generalization, **not verified coverage of every Algerian university**.
- Three independent choices: `breakfast_id`, `lunch_id`, `dinner_id`. Same or different restaurants are allowed when they explicitly offer the meal. This version requires all three; unavailable meals or missing/ambiguous current housing fail safely rather than using invented defaults.
- Guided setup: account verification -> three meal choices and interval -> read-only review -> explicitly confirmed booking -> optional automation activation.
- Presets 15 / 30 / 60 / 180 / 360 minutes, plus custom integer minutes 15–1440. Arabic digits are parsed. Changing the interval updates WorkManager and the actual due policy. OS/network constraints and best-effort timing still apply.
- Upgrade copies legacy residence choice to breakfast/dinner, preserves lunch, stops automation and invalidates the previous test until review. No server-side cancellation is performed. The existing AES key and signing certificate remain unchanged.
- Pending journals are account-scoped with a hashed local key. The previous unscoped encrypted journal is merged into the saved account's scope before removal of its legacy storage key.
- Profile parsing accepts additional genuine identifier field variants but never guesses university/residence/region IDs, uses last year's housing as a fallback, or invents residence 0 for missing records.
- Final local validation: **96 passing tests**, 0 failures/skips; lint 0 errors / 16 warnings. No live multi-university account, physical-device encrypted migration, real meal booking, independent security or physical Samsung scheduling validation was performed.

## Previous release 0.3.0

Release notes for 0.3.0 are not part of this archive. VersionCode 4 retains the existing development signing certificate and account storage format.

- `SyncPolicy.CHECK_INTERVAL_MS` is now one hour, not six. Network-constrained booking opportunities and the stale-confirmation watchdog are hourly; scheduler timing is still best effort. On-process network-return rechecks use a 5-second initial delay, not an execution guarantee.
- Short online windows can be missed or insufficient, especially if the OS has killed the process or delayed work. No closed-phone or offline booking is possible.
- Apple-inspired light UI: grouped white cards, blue actions, a white selected segment, and an original accessible animated pill switch. No Apple fonts, assets or SDKs are used.
- Already-confirmed plans avoid nine redundant history reads during apply; mixed plans are revalidated before their first write, writes retain pre/post verification, and a final full read remains mandatory.
- Final validation: **73 passing local tests**, 0 failures/skips; lint 0 errors / 11 warnings. API 28/35 UI coverage is Robolectric-based, not physical Samsung or live-account validation.

## Previous release 0.2.0

Release notes for 0.2.0 are not part of this archive. This release separates Home and Settings, removes promotional/test badges from the main screen, places technical detail in About/Operation details, and prominently warns that new automatic bookings cannot run with the phone powered off or without internet. The write action remains hidden until preview, and explicit confirmation remains required. Scheduling, authentication-recovery and pending-write safeguards are retained.

Validation: **64 passing local tests**, 0 failures/skips; Android lint 0 errors / 11 warnings. UI tests run with Robolectric on API 28 and 35, not a physical Samsung. No real account, live booking, physical-device upgrade, malware-scanner or independent-security validation was performed for this release. The previous certificate is retained for compatible updates, not replaced with a new production key. The APK remains development-signed. No Play Protect or other OS security control is disabled or bypassed, and scan prompts may recur.

## Previous update 0.1.1

Release notes for 0.1.1 are not part of this archive. Install over the previous APK: same package, versionCode 2 (now 3 in 0.2.0), verified identical development certificate. No user data or journal reset is performed.

This release adds sanitized S01–S09 failure stages, token-prefix normalization, bounded read-only session recovery and alternate own-account read tokens on HTTP 401. It never automatically resends reservation POSTs. HTTP 403 does not trigger fallback or re-login. Network failures do not trigger an auth loop.

A best-effort network callback exists only while the process lives; constrained WorkManager remains the scheduler outside that lifetime. The status notification is not a foreground service, a process-liveness guarantee, or proof of successful booking. Unchanged account/restaurant settings no longer disable automation.

Historical 0.1.1 validation: 56 passing tests (29 booking, 14 simulated protocol/auth, 7 scheduling policy, 4 local recovery, 2 Robolectric UI); lint has 0 errors and 10 warnings. No live-account, physical-Samsung, real airplane-mode, or upgrade-with-account-data validation was performed.

## User documentation

- [Arabic setup guide](docs/USER_GUIDE_AR.md)

## Build

Requires JDK 17, Android SDK platform 35 and build tools, plus internet access to fetch pinned Gradle/plugin/library dependencies. Set `ANDROID_HOME` to your SDK directory, or create your own untracked `local.properties` with `sdk.dir=...`.

Linux/macOS:

```sh
chmod +x gradlew
./gradlew --no-daemon :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease
```

Windows (JDK 17 and Android SDK configured):

```bat
gradlew.bat --no-daemon :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`.

The release build is deliberately **test-signed using Gradle's local debug signing key**, not production-signed. Building on another machine usually generates a different signing key; it cannot update an existing installation signed with a different certificate. Do not publish this as a production release. Configure your own protected release signing for distribution. No signing private keys or account credentials are included in this source archive.

## Structure

- `MainActivity`: Arabic native UI, read-only preview, explicit manual approval, and disabled-by-default automation.
- `Vault`: Android Keystore AES-GCM, authenticated by record name; no plaintext account fallback or backups.
- `ServiceApi`: fixed HTTPS endpoints, system certificate validation, bounded reads and sanitized failures. Compatibility adapter for observed requests, not an official SDK. Shared request-signing material is protocol data, not a student credential.
- `Engine`, `WeeklySchedule`: three-day plan, recurring per-weekday/per-meal overrides, conservative write/verify journal; default restaurants apply unless an explicit weekly exception replaces them.
- `Runner`: in-process write lock and fresh account/restaurant/reservation reads.
- `Automation`: network-constrained periodic WorkManager with failure-aware user-selected successful-sync suppression, new-day sync, bounded retry backoff, separate ongoing state notification and a best-effort stale-success watchdog.
- `MealGuardApp`, `NetworkState`, `SyncPolicy`: process-lifetime network callbacks, validated-internet checks and a pure scheduling policy.
- `validation`: unit/simulation results and lint output for this build.

WorkManager's user-selected periodic opportunities are not exact alarms. No permanent foreground service is started. Device restrictions can delay both jobs and notifications. The journal blocks ambiguous retries rather than claiming success. Stopping automation does not undo a request already sent or cancel any server-side reservation.

This version has **no absence calendar, automatic cancellation, remote service, telemetry or auto-update**. Manual replacement does perform explicitly approved cancellation followed by rebooking. Pause it when away and manage already-booked meals through the official app.

Compatibility research references and commit IDs are documented in the validation report. The independently built client may need changes when the service changes its protocol or authentication.
