# Project Update Log — OOP Refactor & New Supabase Project

**For:** my project partner  
**Project:** University Counselling Appointment & Support System (AMCS2194)  
**Date:** 20 November 2026  
**Status:** Android code complete · database setup still required

---

## 0. Read this first

The project has been **fully refactored to OOP** and the Android app now uses a
**new Supabase project**.

But the new Supabase project is currently **empty**. The app will build, open and
show errors until someone runs the SQL scripts from the Supabase website.

**One-line version:**

> The code is finished. The database is designed and ready as SQL files, but the
> new Supabase project still needs `schema.sql`, the seeds and the demo Auth
> users created in the dashboard.

If you only do one thing after reading this:

1. Open the new Supabase project.
2. Run `supabase/schema.sql` in SQL Editor.
3. Run `supabase/seed_reference.sql`.
4. Create the three staff Auth users.
5. Register the two students through the app.
6. Run `supabase/seed_demo.sql`.

Full details are in Section 5 and in [`SETUP_GUIDE.md`](SETUP_GUIDE.md).

---

## 1. What changed since the original project

| Area | Before | Now |
|---|---|---|
| Database | `profiles` table with a role string | `users` supertype + `students` / `counsellors` / `administrators` |
| Resources | One flat `resources` table | `resources` supertype + article/video/audio/PDF/hotline subtype tables |
| Enquiries | One flat enquiries table | `enquiries` supertype + identified/anonymous subtype tables |
| Android data | Repositories returned row models | Repository interfaces return domain objects |
| Roles | `UserRole` enum and string checks | Polymorphic `User` objects with `permissions` and `homeTabs()` |
| Appointment rules | String status comparisons in UI | Sealed `AppointmentStatus` State classes |
| Error handling | Mixed mapper calls | `ErrorTranslator` + `BaseSupabaseRepository` |
| UI state | Separate message channels | `BaseViewModel` shared message channel |
| Demo project | Old broken Auth rows | New Supabase project, empty and ready for setup |

---

## 2. New Supabase project

**Project URL:** `https://trssqffqfhdrcoxozqym.supabase.co`  
**Project ref:** `trssqffqfhdrcoxozqym`

The URL and publishable key have been updated in:

- [`supabase.properties`](../supabase.properties) — tracked/shared config
- [`local.properties`](../local.properties) — machine-local override (git-ignored)

The APK was rebuilt after this change. The generated `BuildConfig` contains the
new project URL and publishable key.

> `local.properties` is **not** in Git. If you clone/pull the project, either
> update your own `local.properties` or remove its Supabase lines so the tracked
> `supabase.properties` values are used.

**Current database state:** empty. No schema/seeds have been applied by me. The
publishable key cannot create tables; the SQL must be run in the Supabase
Dashboard SQL Editor.

---

## 3. Database changes

### 3.1 New schema

- **27 base tables**
- **10 views**
- Class-table inheritance:
  - `users` → `students` / `counsellors` / `administrators`
  - `resources` → article / video / audio / PDF / hotline
  - `enquiries` → identified / anonymous
- Association tables:
  - counsellor specialisations
  - counsellor languages
  - resource target stress brackets
- Reference tables:
  - concern categories
  - session modes
  - risk levels
  - stress brackets
  - appointment statuses and allowed transitions
- Exactly-one-subtype deferred triggers
- RLS policies on every table/view
- Security-definer RPCs for all writes that need rules

### 3.2 Actions no longer done from Android table writes

These now go through RPCs:

- booking an appointment
- rescheduling/cancelling
- updating appointment status
- creating/updating/deleting a counsellor
- creating/updating/deleting a resource
- submitting/looking up/withdrawing an enquiry
- creating a follow-up appointment
- deactivating an account
- updating a student profile
- admin dashboard statistics
- counsellor creation/removal

### 3.3 Important SQL files

| File | Purpose |
|---|---|
| [`supabase/schema.sql`](../supabase/schema.sql) | Fresh project: complete OOP schema |
| [`supabase/seed_reference.sql`](../supabase/seed_reference.sql) | Reference data, staff directory rows, resources, availability |
| [`supabase/seed_demo.sql`](../supabase/seed_demo.sql) | Demo appointments, records, feedback and enquiries |
| [`supabase/test_queries_oop_rls.sql`](../supabase/test_queries_oop_rls.sql) | RLS/privacy proof queries |
| [`supabase/migrations/20261120_oop_refactor.sql`](../supabase/migrations/20261120_oop_refactor.sql) | Existing old project → new OOP schema, non-destructive |
| [`supabase/optional_drop_old_tables.sql`](../supabase/optional_drop_old_tables.sql) | Optional cleanup after migration is verified |

### 3.4 Fresh project vs existing project

**Fresh project:** use `schema.sql` → `seed_reference.sql` → Auth users →
`seed_demo.sql`.

**Existing project with old tables:** do **not** run `schema.sql`. Use
`migrations/20261120_oop_refactor.sql` on a copy, verify, then test. The old
tables are preserved in the `legacy` schema. Do not run the optional drop yet.

Because the old project had broken demo Auth rows, the fresh-project route is
recommended.

---

## 4. Android changes

### 4.1 Architecture

```text
UI / Compose
   ↓
ViewModel : BaseViewModel
   ↓
Repository INTERFACE (domain)
   ↓
Supabase*Repository : BaseSupabaseRepository
   ↓
PostgREST view / RPC
   ↓
private DTO → mapper → domain object
```

- Domain packages are pure Kotlin.
- No Android/AndroidX/Supabase imports inside domain packages.
- ViewModels depend on repository interfaces, not concrete Supabase classes.
- `AppContainer` is the only place that constructs concrete repositories.
- `SessionManager` holds the signed-in polymorphic `User`.
- `AppNavGraph` asks `user.homeTabs()` for tabs.
- UI actions are gated through `UserPermissions`.

### 4.2 OOP constructs

| Concept | Implementation |
|---|---|
| Encapsulation | Value objects, entity validation, immutable `val`s |
| Inheritance | `User`, `Resource`, `Enquiry`, `AppointmentStatus`, base classes |
| Polymorphism | `homeTabs()`, `permissions`, resource subtypes, status State |
| Abstraction | Nine repository interfaces |
| Composition | `Appointment`, `SessionRecord`, `Counsellor`/`AvailabilitySlot` |
| Domain model | Pure domain packages + DTO mappers |
| Patterns | Repository, Template Method, Factory, Strategy, State |

The full evidence map is in [`OOP_NOTES.md`](OOP_NOTES.md) and the diagrams are
in [`CLASS_DIAGRAM.md`](CLASS_DIAGRAM.md).

### 4.3 Module status

| Module | Owner | Status | Main change |
|---|---|---|---|
| 1. Student Account & Profile | Beh | ✅ converted | Auth/User/Student repositories, domain Profile, session User |
| 2. Appointment Booking + Smart Match | Beh | ✅ converted | `Appointment` entity, `AppointmentStatus` State, Smart Match strategy |
| 3. Counsellor & Availability | Jeremy | ✅ converted | `CounsellorRepository`, `Counsellor` HAS-MANY `AvailabilitySlot`, weekly pattern |
| 4. Session Records + Triage | Jeremy | ✅ converted | `SessionRecord`, `FollowUp`, `TriageBoard`, safe student summaries |
| 5. Resource Library + Self-Check | Michael | ✅ converted | `Resource` hierarchy, `ResourceDraft`, self-check domain |
| 6. Enquiries + Feedback | Michael | ✅ converted | Sealed `Enquiry`, `Feedback`, repository interfaces |

### 4.4 Unique features

1. **Smart Counsellor Match** — `SpecialisationThenEarliestSlotStrategy`.
2. **Urgency Triage & Follow-up Board** — `TriageBoard` + `FollowUp`.
3. **Wellbeing Self-Check Recommender** — pure `SelfCheckContent` + domain resources.

All three are preserved.

### 4.5 Removed legacy code

- `UserRole`
- `Profile`
- old `AppointmentRepository` / appointment row models
- old counsellor models/repository
- old session-record models/repository
- old resource models/repository
- old enquiry/feedback models/repositories
- old `SmartMatch` model/test
- empty legacy source folders
- temporary SQL harness

---

## 5. Exact setup steps from the Supabase website

Do these in order.

### Step 1 — Turn off email confirmation

**Authentication → Providers → Email → Confirm email = OFF**  
(or **Authentication → Settings** depending on dashboard version)

Use **Auto Confirm** when adding users.

### Step 2 — Run the schema

1. **SQL Editor → New query**
2. Open [`supabase/schema.sql`](../supabase/schema.sql)
3. Copy the **whole file**
4. Paste and **Run**

If you get a `btree_gist` error:

- **Database → Extensions**
- enable `btree_gist`
- re-run `schema.sql`

Verify:

```sql
select count(*) from information_schema.tables
where table_schema = 'public' and table_type = 'BASE TABLE';
-- 27

select count(*) from information_schema.views
where table_schema = 'public';
-- 10
```

### Step 3 — Run the reference seed

New SQL Editor query → paste all of
[`supabase/seed_reference.sql`](../supabase/seed_reference.sql) → **Run**.

Verify:

```sql
select count(*) from public.counsellors;      -- 2
select count(*) from public.administrators;   -- 1
select count(*) from public.resources;        -- 11
```

### Step 4 — Create the three staff Auth users

**Authentication → Users → Add user**:

| Email | Password |
|---|---|
| `admin@university.edu.my` | `Demo@12345` |
| `counsellor.aisyah@university.edu.my` | `Demo@12345` |
| `counsellor.daniel@university.edu.my` | `Demo@12345` |

Enable **Auto Confirm User**.

They must be created **after** `seed_reference.sql`, because that file creates
their staff directory rows. The Auth trigger links them by email.

Verify:

```sql
select email, user_type, auth_user_id
from public.users
order by email;
```

The staff rows should now have a non-null `auth_user_id`.

### Step 5 — Create the two students

Reliable route: register through the app.

| Email | Password |
|---|---|
| `student.aina@student.university.edu.my` | `Demo@12345` |
| `student.faris@student.university.edu.my` | `Demo@12345` |

Student data:

| Field | Aina | Faris |
|---|---|---|
| Full name | Aina Zulaikha | Muhammad Faris |
| Student ID | DAMCS2401 | DAMCS2402 |
| Programme | Diploma in Computer Science | Diploma in Information Technology |
| Phone | 012-3456789 | +60123456789 |
| Preferred mode | In person | Online |

If your Add User dialog has a **User Metadata** field, you can use the JSON in
[`SETUP_GUIDE.md`](SETUP_GUIDE.md) instead.

**Do not insert directly into `auth.users` with SQL.** That creates the malformed
Auth rows that caused the old project problems.

### Step 6 — Run the demo seed

Only after all five Auth users exist:

New query → paste all of [`supabase/seed_demo.sql`](../supabase/seed_demo.sql) →
**Run**.

Verify:

```sql
select count(*) from public.students;          -- 2
select count(*) from public.counsellors;       -- 2
select count(*) from public.administrators;    -- 1
select count(*) from public.appointments;      -- 6
select count(*) from public.session_records;   -- 2
select count(*) from public.feedback;          -- 2
select count(*) from public.enquiries;         -- 4
select count(*) from public.resources;         -- 11
```

### Step 7 — Optional RLS proof

SQL Editor → paste and run
[`supabase/test_queries_oop_rls.sql`](../supabase/test_queries_oop_rls.sql).

It simulates student/counsellor/admin identities and prints expected privacy
counts.

---

## 6. App configuration

The new project is already configured in:

- `supabase.properties`
- `local.properties`

If you clone the project elsewhere, do this:

1. Copy the Project URL from **Project Settings → API**.
2. Copy the **Publishable key** (starts with `sb_publishable_...`).
3. Update `supabase.properties`.
4. Update your local override or remove its two Supabase lines.
5. Rebuild.

```powershell
.\gradlew.bat :app:assembleDebug
```

The current APK is at:

`app/build/outputs/apk/debug/app-debug.apk`

Do not use a `service_role` / secret key in the app.

---

## 7. Demo accounts

All demo passwords: `Demo@12345`

- `student.aina@student.university.edu.my`
- `student.faris@student.university.edu.my`
- `counsellor.aisyah@university.edu.my`
- `counsellor.daniel@university.edu.my`
- `admin@university.edu.my`

Reference enquiry codes: `ENQ-DEMO01` – `ENQ-DEMO04`.

---

## 8. Verification already done

- **58/58 JVM tests pass**
- Android debug build: **BUILD SUCCESSFUL**
- All domain packages free of Android/Supabase imports
- No remaining `UserRole`, `Profile`, or legacy repository references
- SQL smoke-tested:
  - resource create/update/delete RPCs
  - enquiry submit/lookup/inbox
  - feedback view
  - counsellor create/update/delete RPCs
  - admin dashboard statistics
  - student profile update RPC
  - appointment views
- Migration dry-run tested against the old demo schema

---

## 9. What is still not done

1. **New Supabase project is empty** — run the SQL steps above.
2. **Manual role walkthrough** — needs the demo Auth users and app installed.
3. **Screenshots** for the report — not captured yet.
4. **Release signing** — debug APK only.
5. **Optional legacy drop** — do not run until migration is fully verified.

---

## 10. Phase history

| Phase | What changed |
|---|---|
| Phase 0 | Audit of the old code and OOP design plan |
| Phase 1 | OOP database schema, seeds, migration, RLS proof |
| Phase 2 | Core pure-Kotlin domain: ValueObjects, User hierarchy, AppointmentStatus |
| Phase 3 | Shared infrastructure: ErrorTranslator, BaseSupabaseRepository, BaseViewModel, UserFactory, navigation |
| Phase 4 | Beh modules: auth, student, appointment, booking, Smart Match |
| Phase 5 | Jeremy modules: counsellor, availability, records, triage |
| Phase 6 | Michael modules: resources, self-check, enquiries, feedback |
| Phase 7 | Final class diagram, OOP notes, cleanup, 54-test verification |

---

## 11. Key files

### Database

- [`supabase/schema.sql`](../supabase/schema.sql)
- [`supabase/seed_reference.sql`](../supabase/seed_reference.sql)
- [`supabase/seed_demo.sql`](../supabase/seed_demo.sql)
- [`supabase/test_queries_oop_rls.sql`](../supabase/test_queries_oop_rls.sql)
- [`supabase/migrations/20261120_oop_refactor.sql`](../supabase/migrations/20261120_oop_refactor.sql)

### Setup docs

- [`SETUP_GUIDE.md`](SETUP_GUIDE.md)
- [`FINAL_CHECKLIST.md`](FINAL_CHECKLIST.md)
- [`DATA_DICTIONARY.md`](DATA_DICTIONARY.md)
- [`ERD.mmd`](ERD.mmd)

### OOP docs

- [`OOP_NOTES.md`](OOP_NOTES.md)
- [`CLASS_DIAGRAM.md`](CLASS_DIAGRAM.md)
- [`MODULE_NOTES.md`](MODULE_NOTES.md)

### Android

- [`core/domain`](../app/src/main/java/com/example/counselling/core/domain)
- [`core/data`](../app/src/main/java/com/example/counselling/core/data)
- [`core/ui`](../app/src/main/java/com/example/counselling/core/ui)
- [`feature`](../app/src/main/java/com/example/counselling/feature)

---

## 12. One final warning

Do **not** run `schema.sql` on a project that already contains the old tables.
For an existing project, use the migration on a copy and verify it. For this new
empty project, `schema.sql` is correct.

---

## 13. Post-handoff UI fixes — 20 November 2026

These are Android-only fixes; no database change is needed.

### 13.1 Home tab navigation

**Problem:** after opening another top-level page from Home, tapping Home again
did not return to Home because the bottom-bar navigation restored a saved child
stack.

**Fix:** [`AppNavGraph.kt`](../app/src/main/java/com/example/counselling/navigation/AppNavGraph.kt)
now pops directly back to the Home destination for the Home tab. Other tabs keep
their saved-state behaviour.

### 13.2 Broken time separator

**Problem:** the booking slot label showed `â€“` instead of an en dash on the
phone, which looked like random symbols.

**Fix:** removed the mojibake from
[`BookAppointmentScreen.kt`](../app/src/main/java/com/example/counselling/feature/appointment/ui/BookAppointmentScreen.kt)
and the counsellor specialisation separator. Time labels now render as
`9:00 AM – 10:00 AM`.

### 13.3 Choose-time redesign

**Problem:** every slot was a full-width chip, so a counsellor with several days
of slots produced a long, inefficient list.

**Fix:** the booking slot step now has:

1. a horizontally scrollable **date selector**;
2. a two-column **time grid** for the selected day;
3. a selected-slot check mark and start/end time;
4. automatic clearing of the old slot when the student changes date.

Implemented in
[`BookAppointmentScreen.kt`](../app/src/main/java/com/example/counselling/feature/appointment/ui/BookAppointmentScreen.kt).

### 13.4 Inline appointment feedback

**Problem:** rating a completed session required opening the separate feedback
screen and then choosing the session again.

**Fix:** the completed appointment card now has **Rate session** directly on the
card. It opens a rating dialog, submits in place, shows the existing star rating,
and supports **Update rating** / **Remove** without leaving Appointments.

Changed files:

- [`StudentAppointmentsScreen.kt`](../app/src/main/java/com/example/counselling/feature/appointment/ui/StudentAppointmentsScreen.kt)
- [`AppointmentsViewModel.kt`](../app/src/main/java/com/example/counselling/feature/appointment/ui/AppointmentsViewModel.kt)
- [`AppointmentCard.kt`](../app/src/main/java/com/example/counselling/feature/appointment/ui/AppointmentCard.kt)
- [`AppNavGraph.kt`](../app/src/main/java/com/example/counselling/navigation/AppNavGraph.kt)

The separate Student Feedback screen remains reachable from Enquiries, but it is
no longer required for normal appointment feedback.

### Verification after fixes

- `:app:assembleDebug` — **BUILD SUCCESSFUL**
- `:app:testDebugUnitTest` — **58/58 tests pass**
- No database/migration changes required

---

## 14. Appointment workflow & notification update — 20 November 2026

### 14.1 Refresh actually shows a changed status

**Problem:** after a counsellor confirmed a booking, pulling to refresh did not
update the visible card until you switched pages.

**Root cause:** the list was wrapped in `remember(state.appointments)`.
`Appointment.equals` is identity-by-id, so a list with the same ids but a new
status compared equal and Compose kept the old derived list.

**Fix:** student, counsellor and admin appointment screens now derive the
filtered list on each recomposition instead of remembering it by list equality.
Cards also use workflow-aware LazyColumn keys (status + last event + slot +
feedback), so Compose cannot skip a status-only update. After a successful
mutation the ViewModel applies the server-accepted status optimistically and
then reloads. Reschedule returns automatically to the list. The student and
counsellor top bars also have an explicit refresh button.

### 14.2 In-app notification badge on cards

The database now stores `appointments.last_event_code` and exposes it through
`v_appointments`. The card shows a `!` notice:

| Recipient | Notice examples |
|---|---|
| Student | booking confirmed, time changed by counsellor, cancelled by counsellor, completed/no-show, confirm follow-up |
| Counsellor | new booking request, student rescheduled, student cancelled, student confirmed follow-up, waiting for follow-up confirmation |

The RPCs record the event:

- `book_appointment` → `booked`
- student reschedule → `rescheduled_by_student` (and the appointment returns to pending so the counsellor re-confirms)
- counsellor reschedule → `rescheduled_by_counsellor`
- confirm/complete/no-show/cancel → matching event
- student cancel → `cancelled_by_student`
- follow-up proposal → `followup_requested`
- student follow-up confirmation → `followup_confirmed_by_student`

### 14.3 Follow-up is confirmed by the student

`create_followup_appointment` still creates a pending follow-up, but the
counsellor no longer confirms it. The student card shows **Confirm your
follow-up** and calls the new `confirm_followup_appointment` RPC. The counsellor
then sees the confirmation notice.

If the **student reschedules a follow-up**, it returns to pending with
`rescheduled_by_student`; this time the **counsellor** reconfirms it. The
student confirmation RPC rejects this case so the workflow cannot go backwards.
The counsellor card shows “Student rescheduled the follow-up — reconfirm it”
and exposes the Confirm action.

SQL changes added to [`schema.sql`](../supabase/schema.sql) and the migration:

- `appointments.last_event_code` + constraint
- `v_appointments.last_event_code`
- updated reschedule/cancel/status/follow-up event writes
- `confirm_followup_appointment(uuid)` and grant
- `confirmed → pending` transition for a student reschedule

**If you already ran the previous new OOP schema, run
[`20261120_appointment_workflow_upgrade.sql`](../supabase/migrations/20261120_appointment_workflow_upgrade.sql)
once. It is idempotent. The upgrade drops and recreates `v_appointments` because
PostgreSQL's `CREATE OR REPLACE VIEW` cannot insert a column in the middle, and
restores its select grant afterwards. If you have not run any schema yet, use
`schema.sql`; it already contains everything.**

### 14.4 Reschedule and follow-up time UI

Both screens now use the same date + two-column time grid as booking via
[`SessionSlotPicker.kt`](../app/src/main/java/com/example/counselling/ui/components/SessionSlotPicker.kt).
Changing the date clears the previously selected time.

### 14.5 Counsellor feedback on cards

The counsellor schedule loads the safe `v_feedback` rows and shows the student's
star rating and comment directly on completed appointment cards. Admin still sees
aggregate stats only.

### 14.6 Admin availability planner redesign

The availability manager is now efficient for admins managing many slots:

- slots grouped by day;
- Upcoming / This week / All filters (default Upcoming);
- top-bar refresh;
- full-screen editor instead of a cramped AlertDialog;
- one-tap next-7-day date chips plus an "Other date" picker;
- start-time chips (30-minute intervals);
- duration presets (30 / 45 / 60 / 90 / 120 minutes);
- automatic end time;
- repeat weekly 1–8 weeks.

Changed file:
[`CounsellorAvailabilityScreen.kt`](../app/src/main/java/com/example/counselling/feature/counsellor/ui/CounsellorAvailabilityScreen.kt).

### 14.7 Tests and build

- `:app:testDebugUnitTest` — **58/58 tests pass**
- `:app:assembleDebug` — **BUILD SUCCESSFUL**
- Updated migration: [`20261120_oop_refactor.sql`](../supabase/migrations/20261120_oop_refactor.sql)
