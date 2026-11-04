# University Counselling Appointment & Support System

**AMCS2194 Diploma Year 2 mini project**  
A native Android counselling-service app with a Supabase (PostgreSQL + Auth + PostgREST) backend.

The project includes all six required modules, three role-specific dashboards, and three unique features:

1. **Smart Counsellor Match** — ranks counsellors by concern specialisation and earliest free slot.
2. **Urgency Triage & Follow-up Board** — groups high-risk/follow-up session records into Overdue, Due soon and Later, and schedules follow-up appointments.
3. **Wellbeing Self-Check Recommender** — private 10-question screening with bracket-matched resources and a booking hand-off.

---

## Important: demo account status

The **Android code and backend schema are complete and the app builds successfully**. The current Supabase project's five demo Auth users were previously inserted directly into `auth.users`; their malformed rows cause a `500 Database error querying schema` when signing in. Unknown emails correctly return a normal `400 invalid_credentials`.

Recreate those users through **Supabase Authentication → Users** and follow [SETUP_GUIDE §9.16](docs/SETUP_GUIDE.md). Never insert directly into `auth.users` with raw SQL. Until this is repaired, the app's login screen is reachable but role-specific end-to-end testing cannot use the demo accounts.

---

## Team and module ownership

| Owner | Modules | Unique feature |
|---|---|---|
| **Beh** | Module 1 — Student Account & Profile<br>Module 2 — Appointment Booking | Smart Counsellor Match |
| **Jeremy** | Module 3 — Counsellor & Availability Management<br>Module 4 — Session Records & Follow-Up | Urgency Triage & Follow-up Board |
| **Michael** | Module 5 — Self-Help Resource Library<br>Module 6 — Enquiry & Session Feedback | Wellbeing Self-Check Recommender |

Shared work (theme, reusable UI, navigation, Supabase setup, documentation) is marked in file headers as shared.

---

## Tech stack

- Kotlin 2.2.x, Jetpack Compose, Material 3, Navigation Compose
- MVVM + repository pattern; manual dependency injection through `AppContainer` (no Hilt/Koin)
- Supabase Auth + PostgREST (`supabase-kt` 3.2.2 with Kotlin 2.2 compatibility)
- Ktor 3.2.2 Android engine, kotlinx.serialization
- PostgreSQL: RLS, security-definer RPCs, views, triggers, exclusion constraints
- minSdk 26, targetSdk 37, Java 17

`supabase-kt` is pinned to the latest stable 3.x line that compiles against this project's built-in Kotlin 2.2.10 compiler. Newer 3.9.x releases require Kotlin 2.4 metadata.

---

## Supabase configuration for hand-off

The repository intentionally tracks:

```text
supabase.properties
```

It contains the shared **publishable** Supabase URL and client key. This is a public client key protected by Row Level Security — it is safe to send with the source code. **Never replace it with a `service_role`/secret key.**

`local.properties` is still ignored because it contains the machine-specific Android SDK path. The build resolves configuration in this order:

1. `local.properties` (machine-local override, if present)
2. `supabase.properties` (shared, tracked)
3. environment variables

To point at a different project, edit `supabase.properties` or add an override to `local.properties`, then rebuild. No URL or key is hard-coded in Kotlin.

See [SETUP_GUIDE §6.1](docs/SETUP_GUIDE.md) for details.

---

## Quick start

### 1. Backend

Follow [docs/SETUP_GUIDE.md](docs/SETUP_GUIDE.md) in order:

1. Create the Supabase project and configure email auth.
2. Run `supabase/schema.sql`.
3. Run `supabase/seed_reference.sql`.
4. Create the five demo users **through Authentication → Users** (auto-confirm), not raw SQL.
5. Run `supabase/seed_demo.sql`.

### 2. Android

1. Open this folder in Android Studio and let Gradle sync.
2. Android Studio will create/update `local.properties` with your SDK path. The shared Supabase values come from `supabase.properties`.
3. Run the `app` configuration on an Android 8.0+ emulator or device.

Command line:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

### 3. Demo sign-in (after the Auth users are repaired)

| Role | Email | Password |
|---|---|---|
| Student | `student.aina@student.university.edu.my` | `Demo@12345` |
| Student | `student.faris@student.university.edu.my` | `Demo@12345` |
| Counsellor | `counsellor.aisyah@university.edu.my` | `Demo@12345` |
| Counsellor | `counsellor.daniel@university.edu.my` | `Demo@12345` |
| Admin | `admin@university.edu.my` | `Demo@12345` |

Reference enquiries: `ENQ-DEMO01`–`ENQ-DEMO04`.

---

## Role navigation

| Role | Bottom tabs |
|---|---|
| Student | Home · Appointments · Resources · Enquiries · Profile |
| Counsellor | Home · Schedule · Records · Enquiries · Profile |
| Admin | Home · Counsellors · Students · Resources · Profile |

Secondary screens (booking, availability, record forms, resource form, self-check, summaries, feedback, urgent support) are reached from these tabs and hide the bottom bar while open.

---

## Project map

```text
app/src/main/java/com/example/counselling/
  core/                 configuration, Supabase client, session, validation, dates, errors
  navigation/           routes, role destination sets, app graph
  ui/theme/             calm light/dark Material 3 theme
  ui/components/        buttons, fields, dropdowns, date/time pickers, cards, badges, dialogs, snackbar
  feature/auth/         login, registration, splash, configuration error
  feature/home/         role dashboards, next appointment, admin aggregate stats
  feature/student/      Module 1 profile, staff student directory
  feature/appointment/  Module 2 booking, match, slots, reschedule, cancellation, admin view
  feature/counsellor/   Module 3 directory and availability
  feature/records/      Module 4 private records, triage, follow-up, student summaries
  feature/resource/     Module 5 resource library and self-check
  feature/enquiry/      Module 6 enquiries and feedback
supabase/
  schema.sql            complete backend definition
  seed_reference.sql    reference data and slots
  seed_demo.sql         demo records, appointments, enquiries, feedback
docs/
  SETUP_GUIDE.md        full setup, privacy proof and test plan
  DATA_DICTIONARY.md    schema reference
  ERD.mmd               Mermaid ERD
  MODULE_NOTES.md       module implementation summary
  FINAL_CHECKLIST.md    final hand-off/submission checklist
```

---

## Verification

- `:app:assembleDebug` — **BUILD SUCCESSFUL**
- `:app:testDebugUnitTest` — **15/15 tests passed**
  - validation rules
  - Smart Counsellor Match ranking
  - weekly availability generation/validation
  - triage grouping
  - self-check brackets and recommendations

The setup guide also contains the SQL RLS proof queries, full CRUD test matrix and demo walkthrough.

---

## Security and privacy summary

- Supabase Auth identifies the user; `profiles.role` drives role-based navigation.
- RLS restricts students, counsellors and admins to their permitted rows.
- Appointments are mutated only through security-definer RPCs.
- Session records are visible only to the owning counsellor; students read the safe summary view; admins see aggregate stats only.
- Anonymous enquiries store `student_id = null`; lookup returns no identity.
- Resource drafts are hidden from students by RLS.
- The publishable client key is safe to ship; a `service_role`/secret key must never be used in this app.
