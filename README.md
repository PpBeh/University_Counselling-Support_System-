# University Counselling Appointment & Support System

**AMCS2194 Diploma Year 2 mini project**  
A native Android counselling-service app with a Supabase (PostgreSQL + Auth + PostgREST) backend.

> **OOP refactor: all six modules and all three unique features converted.**
> The Supabase schema is the class-table-inheritance design described in
> [`docs/ERD.mmd`](docs/ERD.mmd) and [`docs/DATA_DICTIONARY.md`](docs/DATA_DICTIONARY.md).
> Beh, Jeremy and Michael features now use domain entities, repository
> interfaces, DTO mappers and shared base infrastructure. Phase 7 covers the
> final class diagram, OOP notes and cleanup. See
> [`docs/SETUP_GUIDE.md`](docs/SETUP_GUIDE.md).

The project includes all six required modules, three role-specific dashboards, and three unique features:

1. **Smart Counsellor Match** — ranks counsellors by concern specialisation and earliest free slot.
2. **Urgency Triage & Follow-up Board** — groups high-risk/follow-up session records into Overdue, Due soon and Later, and schedules follow-up appointments.
3. **Wellbeing Self-Check Recommender** — private 10-question screening with bracket-matched resources and a booking hand-off.

---

## Important: demo account status

The **OOP database schema and Android conversion are complete**. The remaining
blocker is recreating the demo Auth accounts before the final role walkthrough.

For a fresh project, use the new setup order:

1. `supabase/schema.sql`
2. `supabase/seed_reference.sql` (pre-provisions admin/counsellors)
3. Create the five demo Auth users (staff link by email; students need metadata)
4. `supabase/seed_demo.sql`

For an existing project, test
[`supabase/migrations/20261120_oop_refactor.sql`](supabase/migrations/20261120_oop_refactor.sql)
on a **copy** first. The old tables are preserved in schema `legacy`; nothing is
dropped.

Full steps: [SETUP_GUIDE](docs/SETUP_GUIDE.md). Never insert directly into
`auth.users` with raw SQL. The old `reset_demo_auth.sql` cleanup is only safe on
the pre-OOP schema and must not be used after the OOP migration.

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
4. Create the **admin and two counsellors** through **Authentication → Users** (auto-confirm), not raw SQL.
5. Build/run the app, then register Aina and Faris through the **Register** screen so their required metadata reaches the Auth trigger.
6. Run `supabase/seed_demo.sql`.

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

## Permission-driven navigation

| Signed-in User | Bottom tabs |
|---|---|
| Student | Home · Appointments · Resources · Enquiries · Profile |
| Counsellor | Home · Schedule · Records · Enquiries · Profile |
| Admin | Home · Counsellors · Students · Resources · Profile |

Secondary screens (booking, availability, record forms, resource form, self-check, summaries, feedback, urgent support) are reached from these tabs and hide the bottom bar while open.

---

## Project map

```text
app/src/main/java/com/example/counselling/
  core/domain/          pure value objects, User hierarchy, AppointmentStatus,
                        AvailabilitySlot, UserFactory, UserRepository
  core/data/            BaseSupabaseRepository, ErrorTranslator, UserViewRow,
                        SupabaseUserRepository
  core/ui/              BaseViewModel with shared snackbar channel
  core/                 AppContainer composition root, SessionManager, dates,
                        validation, constants
  navigation/           permission-driven destinations, routes, app graph
  ui/theme/             calm light/dark Material 3 theme
  ui/components/        buttons, fields, dropdowns, date/time pickers, cards,
                        badges, dialogs, snackbar
  feature/auth/         login/registration + AuthRepository domain/data
  feature/home/         permission-driven dashboards, next appointment, admin stats
  feature/student/      Module 1 profile/directory + StudentRepository domain/data
  feature/appointment/  Module 2 Appointment entity, Smart Match strategy,
                        booking/reschedule/status UI
  feature/counsellor/   Module 3 CounsellorRepository + availability
  feature/records/      Module 4 SessionRecord/TriageBoard/FollowUp + UI
  feature/resource/     Module 5 Resource hierarchy, self-check, ResourceRepository
  feature/enquiry/      Module 6 Enquiry hierarchy, feedback, repositories
supabase/
  schema.sql            OOP class-table schema (fresh install)
  seed_reference.sql    reference data, pre-provisioned staff, slots
  seed_demo.sql         demo records, appointments, enquiries, feedback
  optional_drop_old_tables.sql
  test_queries_oop_rls.sql
  migrations/
    20261120_oop_refactor.sql  non-destructive existing-project migration
docs/
  SETUP_GUIDE.md        full setup, privacy proof and test plan
  DATA_DICTIONARY.md    schema reference
  ERD.mmd               Mermaid ERD
  CLASS_DIAGRAM.md      final domain/persistence class diagrams
  OOP_NOTES.md          assessor evidence map for every OOP construct
  UPDATE_LOG.md         complete hand-off changelog for a teammate
  MODULE_NOTES.md       module implementation summary
  FINAL_CHECKLIST.md    final hand-off/submission checklist
```

---

## Verification

- `:app:assembleDebug` — **BUILD SUCCESSFUL**
- `:app:testDebugUnitTest` — **54/54 tests passed**
  - value objects, User hierarchy, UserFactory, AppointmentStatus
  - Appointment entity reschedule/cancel rules
  - Smart Counsellor Match strategy
  - counsellor availability and weekly repeat rules
  - triage grouping
  - resource subtypes/draft validation and self-check recommendations
  - identified/anonymous enquiry and feedback validation

The setup guide also contains the SQL RLS proof queries, full CRUD test matrix
and demo walkthrough. The assessor-facing OOP evidence is in
[`docs/OOP_NOTES.md`](docs/OOP_NOTES.md) with the diagrams in
[`docs/CLASS_DIAGRAM.md`](docs/CLASS_DIAGRAM.md).

---

## Security and privacy summary

- Supabase Auth identifies the user; the session loads a polymorphic
  `Student`/`Counsellor`/`Administrator`, and navigation/permissions are methods
  on that object. UI code does not branch on a role string.
- RLS restricts students, counsellors and admins to their permitted rows.
- Appointments are mutated only through security-definer RPCs.
- Session records are visible only to the owning counsellor; students read the
  safe `student_session_summaries` view; admins see aggregate stats only.
- Anonymous enquiries are identified by an `anonymous_enquiries` subtype and
  store no student identity; lookup returns no identity.
- Resource drafts are hidden from students by RLS.
- The publishable client key is safe to ship; a `service_role`/secret key must never be used in this app.
