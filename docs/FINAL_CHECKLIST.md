# Final Checklist — Hand-off & Submission

## Status at hand-off

| Item | Status |
|---|---|
| All 6 modules implemented | ✅ |
| All 3 unique features implemented | ✅ |
| Android debug build | ✅ `BUILD SUCCESSFUL` |
| Local unit tests | ✅ 15/15 passed |
| Supabase schema/RLS in repository | ✅ |
| Shared Supabase config included | ✅ `supabase.properties` |
| Live demo Auth accounts usable | ❌ Needs recreation — see below |
| End-to-end role walkthrough | ⏳ Blocked by the demo Auth rows |

The only known blocker is the malformed demo authentication rows in the current Supabase project. The Android code builds; fix the accounts before the final presentation.

---

## 1. What to send to your friend

Send the complete project folder or Git repository **including**:

- `app/`
- `gradle/`, `gradlew`, `gradlew.bat`
- `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`
- `supabase.properties` ← contains the shared publishable URL and key
- `supabase/` SQL files
- `docs/`
- `README.md`

Do **not** send:

- `local.properties` (machine-specific SDK path; Android Studio will create it)
- `.gradle/`, `build/`, `.idea/` caches
- any `service_role` / secret key
- real student data

The friend can open the folder in Android Studio; Gradle will create `local.properties` for their SDK and use the tracked `supabase.properties`.

Verified: after temporarily removing the Supabase values from `local.properties`, Gradle successfully generated `BuildConfig` from `supabase.properties`.

### Why `local.properties` stays ignored

`local.properties` contains a developer-specific `sdk.dir` (and optionally local overrides). The shareable credentials live in the tracked `supabase.properties` instead. This avoids sending a hard-coded Windows SDK path to your friend while still shipping a working Supabase configuration.

**Never put a service_role/secret key in either file.** The publishable key is intended for client apps; RLS protects data.

---

## 2. Build verification

From the project root:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

Expected:

- APK at `app/build/outputs/apk/debug/app-debug.apk`
- 15 tests, 0 failures

Test suites:

| Suite | Covers |
|---|---|
| `ValidatorsTest` | registration/profile validation |
| `SmartMatchTest` | concern filtering, earliest-slot ranking |
| `AvailabilityRulesTest` | weekly repeat, invalid ranges |
| `TriageTest` | Overdue/Due soon/Later and exclusions |
| `SelfCheckTest` | score brackets, dominant concern, recommendations, Hotline rule |

---

## 3. Supabase setup checklist

1. Create/confirm the Supabase project.
2. Run SQL in this exact order:
   1. `supabase/schema.sql`
   2. `supabase/seed_reference.sql`
   3. Create **admin/counsellors** through Authentication → Users (auto-confirmed). Create **Aina/Faris** through the app Register screen, or use the Dashboard metadata JSON from `SETUP_GUIDE.md` Step 5.3.
   4. `supabase/seed_demo.sql`
3. Confirm `supabase.properties` URL and publishable key belong to this project.
4. Turn **Confirm email** off for the demo.
5. Confirm the counsellor rows are linked and profiles have the correct roles.
6. Run the RLS proof queries in `SETUP_GUIDE.md` §11.

### Demo Auth recovery (required now)

The current demo rows were inserted with raw SQL. Symptoms:

- unknown email → `400 invalid_credentials`
- known demo email → `500 Database error querying schema`
- Dashboard delete → “Database loading error”
- Dashboard create → “Database error creating user”

There are two causes:

1. Malformed raw-SQL Auth rows.
2. Dashboard deletes are blocked by restrictive FKs from appointments, session records and feedback. Creating a student from the Dashboard also fails because the Auth trigger requires student metadata.

Fix:

1. Run [`supabase/reset_demo_auth.sql`](../supabase/reset_demo_auth.sql) in the SQL Editor. This is demo-only and destructive; it clears dependent demo app data and the five Auth users. It rolls back on any error.
2. Recreate admin and counsellors through **Authentication → Users**, auto-confirmed.
3. Recreate Aina and Faris through the app's **Register** screen with the exact metadata in `SETUP_GUIDE.md` Step 5.3. Do not create students in the Dashboard unless it offers a metadata JSON box.
4. Run `seed_demo.sql`.
5. Sign in again.

If creating even staff users still fails, the Auth trigger/schema is damaged. Create a fresh Supabase project and follow `SETUP_GUIDE.md` from Step 3 instead of modifying `auth.users`.

Full details: `SETUP_GUIDE.md` §9.16.

---

## 4. Demo walkthrough checklist

### Student (Aina)

- [ ] Sign in with a repaired student account.
- [ ] Home shows welcome + next appointment.
- [ ] **Appointments → Book** → choose a concern → verify ranked Smart Match and Best match.
- [ ] Pick a slot; choose mode; add a note; submit → pending appointment.
- [ ] Reschedule within 12 hours is blocked; reschedule a future slot.
- [ ] Cancel with confirm dialog; slot returns to availability.
- [ ] **Appointments → Past** → session summaries and session feedback.
- [ ] Rate a completed session, remove feedback and rate again.
- [ ] **Resources** search/filter/open; verify drafts are invisible.
- [ ] Home → **Wellbeing check-in** → complete 10 questions; confirm score/bracket, resource recommendations, Hotline only for High, disclaimer.
- [ ] Book from a Moderate/High result; verify Smart Match opens with the dominant concern.
- [ ] **Enquiries** → submit anonymous question; copy code; look up reply by code; withdraw open anonymous.
- [ ] Submit linked question; verify it appears in the list; withdraw.
- [ ] Home urgent card opens Hotline-only resources.
- [ ] Profile edit, password change, deactivation block with upcoming booking and success after cancellation.

### Counsellor (Aisyah)

- [ ] Sign in.
- [ ] Schedule: confirm a pending appointment; decline another pending request; cancel a confirmed session for an emergency; reschedule a confirmed session; complete/no-show; Upcoming/This week/All filters.
- [ ] Manage my availability: add a single slot, repeat 3 weeks, edit/delete free slot; booked slot is locked.
- [ ] Records: create a record for a completed appointment; validation for short notes/risk/follow-up.
- [ ] Triage board: Overdue / Due soon / Later and high-risk inclusion.
- [ ] Schedule follow-up using own free slot; student later sees Follow-up tag.
- [ ] Archive/restore record; 7-day read-only rule; permanent delete only after archive and confirmation.
- [ ] Enquiries: filter status, reply and attach a published resource; answered without reply is blocked.
- [ ] View session feedback.
- [ ] Student directory search from Home.

### Admin

- [ ] Sign in.
- [ ] Home shows aggregate stats, including feedback count/average.
- [ ] Counsellors: add a new directory entry; duplicate email error; edit; manage availability; removal rules.
- [ ] Students search/detail.
- [ ] Resources: add draft, publish, unpublish, edit, delete with confirmation.
- [ ] All appointments: read-only, no actions.
- [ ] Confirm no access to private notes or individual feedback.

---

## 5. Security / privacy checklist

- [ ] Supabase file contains only a publishable/anon key — no `service_role`.
- [ ] Student cannot see another student's appointments or linked enquiries.
- [ ] Anonymous enquiry row exposes no identity via list or code lookup.
- [ ] Admin cannot read private session notes or individual feedback.
- [ ] Counsellor sees only their own appointments, records and feedback.
- [ ] Resource drafts are absent for students.
- [ ] Profile edit cannot change role/student ID/is_active.
- [ ] Deactivation blocked while upcoming appointments exist.
- [ ] Appointments mutated only through RPCs.
- [ ] RLS proof queries produce expected counts.

---

## 6. Known limitations / notes

1. **Demo Auth users must be recreated.** This is a Supabase data issue, not an app defect.
2. **Manual role testing was not completed** because of that Auth failure. All modules compile and the pure business rules are tested.
3. **supabase-kt 3.2.2** is pinned for the project's Kotlin 2.2.10 built-in compiler. 3.9.x needs a Kotlin 2.4 toolchain migration.
4. The headless emulator in this environment produced system-wide ANRs under software rendering; it did install the APK and reach Login. Use Android Studio's hardware-accelerated emulator for the final walkthrough.
5. App release signing is not configured; the debug APK is intended for demonstration.

---

## 7. Submission package

- [ ] Source code / Git repository
- [ ] `supabase.properties` included
- [ ] `supabase/` SQL and `docs/`
- [ ] Debug APK
- [ ] Screenshots: login, student Home, Smart Match, appointment, triage board, self-check result, enquiry reply, resource library
- [ ] Report with module allocation (Beh 1–2, Jeremy 3–4, Michael 5–6)
- [ ] RLS proof output
- [ ] Demo accounts table and recovery note
