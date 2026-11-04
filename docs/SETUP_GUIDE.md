# Setup Guide — University Counselling Appointment & Support System

**Module code:** AMCS2194 · Diploma Year 2 mini project  
**App package:** `com.example.counselling`  
**Stack:** Kotlin · Jetpack Compose · Material 3 · MVVM · Supabase (Auth + Postgres/PostgREST)

This guide takes a fresh Supabase project to a working demo database, then explains how to run the Android app, prove the privacy rules and run the test plan. Follow the order exactly, especially the SQL/user-creation order: the Auth trigger decides the role from the `admin_whitelist` and `counsellors` tables **at sign-up time**.

---

## 0. Files used in this guide

```text
supabase/
  schema.sql           # tables, constraints, RLS, views, triggers, RPCs
  seed_reference.sql    # admin whitelist, counsellors, resources, future slots
  seed_demo.sql         # demo appointments, records, enquiries, feedback
docs/
  SETUP_GUIDE.md        # this file
  DATA_DICTIONARY.md
  ERD.mmd
  MODULE_NOTES.md       # module ownership and implementation notes
  FINAL_CHECKLIST.md    # submission / hand-off checklist
supabase.properties     # shared publishable Supabase URL + key (tracked)
```

Backend setup can be completed before the Android screens are finished. The SQL and RLS proof tests run entirely in the Supabase SQL Editor. Only the student-account fallback in Step 7 needs the app.

---

## 1. Three-tier architecture (presentation → business → data)

| Tier | Where it lives | Responsibility |
|---|---|---|
| **Presentation** | `ui/` Compose screens, Material 3 theme, reusable components, `navigation/`, `feature/*/ui` | Renders state, collects input, shows Snackbar/errors, role-based bottom navigation |
| **Business logic** | `core/Validators.kt`, ViewModel rules, and PostgreSQL functions/triggers | Field rules, ownership checks, 12-hour reschedule notice, max 2 active bookings, 7-day edit window, status transitions, role assignment |
| **Data** | `feature/*/data` repositories + Supabase tables/views/RPCs | `Result<T>` queries/mutations, friendly error mapping, RLS-scoped reads and writes |

Additional design choices:

- One `AppContainer` singleton is created by `CounsellingApp`; ViewModel factories receive repositories from it. Hilt/Koin are **not** used.
- Each ViewModel exposes a `StateFlow<UiState>` and a one-case message channel for Snackbars.
- All timestamps in Postgres are `timestamptz` (UTC); `DateTimeUtils` renders Asia/Kuala_Lumpur.
- Security is enforced in the database as well as in the app. The Android app uses only the publishable/anon key, never a secret/service key.

---

## 2. Prerequisites

| Item | Requirement |
|---|---|
| Android Studio | Any current stable version with Android SDK for API 26+ |
| JDK | The JDK bundled with current Android Studio / JDK 17+ |
| Emulator or phone | Android 8.0 (API 26) or newer, internet access |
| Supabase account | Free tier is sufficient |
| SQL editor | Supabase Dashboard → SQL Editor |
| Network | The emulator/phone must be able to reach `https://<project>.supabase.co` |

The app targets `minSdk 26`, as required by the brief.

---

## 3. Create the Supabase project

1. Go to <https://supabase.com/dashboard> and sign in.
2. Click **New project**.
3. Choose an organisation, a project name (for example `counselling-support`), a strong database password and the region closest to Malaysia (for example Singapore).
4. Wait for the database to finish provisioning.
5. In **Project Settings → API**, copy:
   - **Project URL** (looks like `https://abcdefghijkl.supabase.co`)
   - **Publishable key** (starts with `sb_publishable_...`) or the legacy **anon/public** key.
6. Never copy the **secret**/`service_role` key into the Android project. The app must use only the publishable/anon key.

> If you use new Supabase API keys, "publishable" is the safe client key. The secret key bypasses RLS and is for trusted server/administration only. It must never appear in `local.properties`, BuildConfig, source, screenshots or Git.

---

## 4. Authentication settings

Do this **before creating the demo users**, so the demo can sign in without checking email.

1. Open **Authentication → Sign In / Providers → Email**.
2. Keep the Email provider enabled.
3. Turn **Confirm email** OFF (the brief explicitly permits this for the demo).
4. Under password security settings, set:
   - Minimum password length: **8**
   - Require lowercase letter: **yes**
   - Require uppercase letter: **yes**
   - Require digit: **yes**
   - Symbols: optional but recommended
5. Save.

Why this matters:

- Supabase Auth is the server-side password authority. The app also validates 8+ characters with upper, lower and digit before calling `signUp` / `updateUser`.
- With "Confirm email" off, the app can sign in immediately after registration and the demo accounts work without an SMTP server.

If you create users in the Dashboard, tick **Auto Confirm User** (or the equivalent) so they are immediately usable.

---

## 5. Run the SQL in the correct order

Open **SQL Editor → New query** for each file. Paste the entire file and click **Run**. Wait for success before moving on.

### 5.1 `supabase/schema.sql` — first

This creates:

- extensions (`btree_gist`, needed for the slot-overlap exclusion constraint)
- all nine application tables
- indexes, constraints, triggers and functions
- row-level security policies and grants
- `available_slots_view` and `student_session_summaries`
- the `auth.users` trigger that assigns roles

Verify:

```sql
select table_name
from information_schema.tables
where table_schema = 'public'
order by table_name;
```

Expected: `admin_whitelist`, `appointments`, `availability_slots`, `counsellors`, `enquiries`, `feedback`, `profiles`, `resources`, `session_records` (plus the two views if your tool lists views separately).

Check the availability trigger and view exist:

```sql
select tgname, tgenabled
from pg_trigger
where tgrelid = 'public.availability_slots'::regclass
order by tgname;

select table_name
from information_schema.views
where table_schema = 'public'
order by table_name;
```

When `seed_demo.sql` has not run, `trg_availability_slot_guard` must be **enabled** (`O` or `true`, depending on the display). If it is disabled later, see Troubleshooting.

### 5.2 `supabase/seed_reference.sql` — second, before users

This inserts:

- the admin whitelist email
- the two counsellor directory entries
- the demo resource library
- future availability slots generated from `now()` (weekdays, next four weeks, Kuala Lumpur time)

Verify:

```sql
select email from public.admin_whitelist;
select full_name, email, is_active from public.counsellors order by email;
select count(*) as resources from public.resources;
select count(*) as future_slots
from public.availability_slots
where start_at > now();
```

Expected:

- `admin@university.edu.my` is whitelisted.
- Dr. Aisyah Rahman and Mr. Daniel Lim exist with `is_active = true`.
- 11 resources (one is an unpublished draft).
- Dozens of future slots.

**Do not create the Auth users before this step.** If a counsellor email signs up before the counsellor row exists, the trigger gives it the student role and does not link the counsellor directory row.

### 5.3 Create the five demo Auth users — third

The trigger immediately creates the matching `profiles` row and chooses the role.

#### Staff accounts (Dashboard)

Go to **Authentication → Users → Add user**.

| Account | Email | Password | Auto Confirm | Notes |
|---|---|---|---|---|
| Admin | `admin@university.edu.my` | `Demo@12345` | Yes | Pre-whitelisted by `seed_reference.sql` |
| Counsellor 1 | `counsellor.aisyah@university.edu.my` | `Demo@12345` | Yes | Matches seeded counsellor row; links automatically |
| Counsellor 2 | `counsellor.daniel@university.edu.my` | `Demo@12345` | Yes | Matches seeded counsellor row; links automatically |

User metadata is optional for staff. The admin profile falls back to `System Administrator`; counsellor profiles take their official name from the `counsellors` row. If your Dashboard offers a metadata box and you want a custom admin name, use:

```json
{ "full_name": "System Administrator" }
```

#### Student accounts

Student sign-up **requires metadata** (`full_name`, `student_id`, `programme`, `phone`, `preferred_session_mode`). There are two supported ways:

**Option A (recommended if available): Dashboard metadata box**

Use **Authentication → Users → Add user**, auto-confirm, and paste:

Aina:

```json
{
  "full_name": "Aina Zulaikha",
  "student_id": "DAMCS2401",
  "programme": "Diploma in Computer Science",
  "phone": "012-3456789",
  "preferred_session_mode": "in_person"
}
```

Faris:

```json
{
  "full_name": "Muhammad Faris",
  "student_id": "DAMCS2402",
  "programme": "Diploma in Information Technology",
  "phone": "+60123456789",
  "preferred_session_mode": "online"
}
```

| Student | Email | Password |
|---|---|---|
| Aina Zulaikha | `student.aina@student.university.edu.my` | `Demo@12345` |
| Muhammad Faris | `student.faris@student.university.edu.my` | `Demo@12345` |

**Option B: Register in the app (always works)**

If your Dashboard version does not show a user-metadata field:

1. Complete Step 6 (confirm the shared Supabase config and run the app).
2. Use the app's **Register** screen for Aina and then Faris, entering exactly the same emails, student IDs, programme, Malaysian phone and session mode shown above.
3. Sign out between registrations.
4. Return to the SQL Editor and run `seed_demo.sql`.

Do not skip or guess the student metadata: the database trigger validates it and a malformed or missing field blocks sign-up with a friendly error (the Auth service may wrap trigger errors in a generic "Database error saving new user", so follow the field rules exactly).

#### Verify role assignment before continuing

```sql
select p.email, p.role, p.is_active, c.id is not null as linked_counsellor
from public.profiles p
left join public.counsellors c on c.user_id = p.id
order by p.role, p.email;
```

Expected:

| email | role |
|---|---|
| `admin@university.edu.my` | `admin` |
| `counsellor.aisyah@university.edu.my` | `counsellor` (linked) |
| `counsellor.daniel@university.edu.my` | `counsellor` (linked) |
| `student.aina@student.university.edu.my` | `student` |
| `student.faris@student.university.edu.my` | `student` |

If the admin is a student, see Troubleshooting (the whitelist was added too late).

### 5.4 `supabase/seed_demo.sql` — fourth, after all users exist

This script looks users up by email, verifies that the counsellor accounts are linked, then inserts historical and upcoming samples.

It temporarily disables **only** `trg_availability_slot_guard` to insert past availability slots, then enables it again in the same transaction. Everything is idempotent where possible, so you can re-run it after fixing a setup problem.

Expected SQL Editor notice:

```text
Demo data ready. Reference codes: ENQ-DEMO01 (open), ENQ-DEMO02 (answered), ENQ-DEMO03 (Aina, open), ENQ-DEMO04 (Faris, answered).
```

Verify:

```sql
select reference_code, is_anonymous, student_id, status
from public.enquiries
where reference_code like 'ENQ-DEMO%'
order by reference_code;

select a.status, a.concern_category, s.start_at,
       p.full_name as student, c.full_name as counsellor,
       (a.follow_up_of is not null) as is_follow_up
from public.appointments a
join public.availability_slots s on s.id = a.slot_id
join public.profiles p on p.id = a.student_id
join public.counsellors c on c.id = a.counsellor_id
order by s.start_at;

select risk_level, follow_up_needed, follow_up_due, is_archived
from public.session_records
order by created_at;
```

Demo data summary:

| Data | What the demo contains |
|---|---|
| Upcoming appointments | Aina: confirmed with Aisyah + pending follow-up (tagged) with Aisyah. Faris: pending online with Daniel. |
| Completed sessions | Aina with Aisyah (high risk, follow-up due in ~2 days); Faris with Daniel (medium risk, due in ~6 days) |
| Cancelled session | Faris with Aisyah (historical) |
| Session records | 2, both follow-up needed; Triage board shows Due soon / Later. Overdue starts empty and fills naturally when a due date passes. |
| Feedback | Aina 5 stars; Faris 4 stars |
| Enquiries | `ENQ-DEMO01` anonymous open, `ENQ-DEMO02` anonymous answered, `ENQ-DEMO03` Aina open, `ENQ-DEMO04` Faris answered |
| Resources | 10 published + 1 draft |
| Slots | Future weekday slots for four weeks + three historical slots used by completed/cancelled demo sessions |

If the script reports missing users or roles, **do not work around it by editing random tables**. Follow the order in this guide; Troubleshooting below has the exact repair query.

---

## 6. Configure the Android app

### 6.1 Supabase configuration (shared file + local override)

The repository **intentionally tracks** `supabase.properties` at the project root so the publishable project URL and client key travel with the source code for hand-off:

```properties
SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co
SUPABASE_ANON_KEY=YOUR_PUBLISHABLE_OR_ANON_KEY
```

This is the **publishable client key**, not a server secret. It is designed to be shipped inside a client app; row-level security protects the data. The file is checked in and is not ignored.

`local.properties` remains machine-specific because it also contains `sdk.dir`. It is still ignored by Git and can override the Supabase values for your own machine. The build order is:

1. `local.properties` (if present and non-blank)
2. `supabase.properties`
3. `SUPABASE_URL` / `SUPABASE_ANON_KEY` environment variables

Rules:

- No quotes around values.
- `SUPABASE_URL` has **no** trailing slash.
- `SUPABASE_ANON_KEY` must be the publishable/anon key. **Never use a service_role/secret key** in either file, BuildConfig, source, screenshots or Git.
- If you point at a different Supabase project, update the shared file (or your local override) and rebuild.

Gradle exposes the values as `BuildConfig.SUPABASE_URL` and `BuildConfig.SUPABASE_ANON_KEY`. Missing values still allow the project to compile; the app shows a configuration-error screen at launch. No URL or key is hard-coded in Kotlin source.

For the exact Gradle code, see [app/build.gradle.kts](../app/build.gradle.kts).

### 6.2 Run the app

1. Open the project folder in Android Studio.
2. Wait for Gradle sync / install dependencies.
3. Choose an emulator or connected Android 8+ device.
4. Run the `app` configuration.
5. Sign in with a demo account or register a new student (a new registration always becomes a student unless the email is pre-approved).

Sign in as each role to verify navigation:

| Role | Bottom navigation (5 tabs) |
|---|---|
| Student | Home · Appointments · Resources · Enquiries · Profile |
| Counsellor | Home · Schedule · Records · Enquiries · Profile |
| Admin | Home · Counsellors · Students · Resources · Profile |

The admin Home includes the read-only all-appointments list.

---

## 7. Demo accounts

All demo accounts use password **`Demo@12345`** (change it in a real deployment).

| Role | Display name | Email | Student ID | Programme / specialisations | Preferred mode |
|---|---|---|---|---|---|
| Admin | System Administrator | `admin@university.edu.my` | — | — | — |
| Counsellor | Dr. Aisyah Rahman | `counsellor.aisyah@university.edu.my` | — | Stress, Anxiety, Academic Pressure, Sleep | — |
| Counsellor | Mr. Daniel Lim | `counsellor.daniel@university.edu.my` | — | Relationships, Loneliness, Financial, Academic Pressure, Other | — |
| Student | Aina Zulaikha | `student.aina@student.university.edu.my` | `DAMCS2401` | Diploma in Computer Science | in_person |
| Student | Muhammad Faris | `student.faris@student.university.edu.my` | `DAMCS2402` | Diploma in Information Technology | online |

**Known anonymous reference codes (from the seed):**

- `ENQ-DEMO01` — open, safe to demonstrate "Check reply by reference code" and withdrawal
- `ENQ-DEMO02` — answered, safe to demonstrate a counsellor reply
- `ENQ-DEMO03` — Aina's linked, open enquiry
- `ENQ-DEMO04` — Faris's linked, answered enquiry

Because anonymous enquiries store no identity, the **student must copy and keep** the code. There is no way to recover a lost code from their account.

---

## 8. Role and privacy behaviour built into the backend

| Behaviour | Backend mechanism |
|---|---|
| Sign-up role selection | `handle_new_user()`: admin whitelist → admin; counsellor email → counsellor and link; otherwise student |
| Deactivated login | The app signs in, reads its own profile (`is_active = false`), signs out and shows a message. All app data policies also return nothing when `app_role()` is null. |
| Student privacy | Students can only read their own appointments, own non-anonymous enquiries, own feedback and own public session summaries |
| Private notes | No student or admin SELECT policy on `session_records`; students use a safe owner-rights view; admins use aggregate stats only |
| Anonymous enquiries | `student_id` NULL, no identity columns returned by the lookup function, no direct SELECT policy for students |
| Admin read-only appointments | Admin has an appointments SELECT policy but no write privilege or write policy |
| Counsellor inbox | Counsellors can read all enquiries; anonymous rows have no student ID to display |
| Role capture | Role is read from `profiles` through `app_role()`, never trusted from client metadata or JWT user metadata |

---

## 9. Troubleshooting

### 9.1 "permission denied", "new row violates row-level security", or lists are always empty

1. Confirm you are signed in and the session has not expired.
2. Check the account's profile and role:
   ```sql
   select p.id, p.email, p.role, p.is_active
   from public.profiles p
   where lower(p.email) = lower('student.aina@student.university.edu.my');
   ```
3. Check the JWT and helper inside a transaction (see the RLS proof section).
4. Remember that RLS returns **zero rows** rather than an error for reads. An empty list can mean "not allowed", not "database broken".
5. The admin is not allowed to read session notes or enquiries; that is intentional.
6. For an insert, check that the app sends only the writable columns. For example, do not try to update `profiles.role` or set `session_records.counsellor_id`; those columns are protected.
7. Sign out and back in after changing a role/profile in SQL, so the app refreshes its `SessionManager`.

### 9.2 HTTP 401 / "Invalid JWT" / "Invalid API key"

- Check `supabase.properties` (shared) and `local.properties` (machine override): both properties must resolve, with no quotes and no trailing slash on the URL.
- The key must be the **publishable/anon** key. A mismatch between Project URL and key produces 401.
- Rebuild after editing either configuration file; `BuildConfig` values are generated at build time.
- Clear the app data or sign out/in after changing Supabase projects or keys.
- Confirm the device clock/time zone is automatic and correct.
- If email confirmation is on, sign-in fails with "Email not confirmed"; disable confirmation for the demo and/or auto-confirm the existing user.

### 9.3 "relation ... does not exist" / "Could not find the table"

- Run `schema.sql` again in the correct project. New API calls may be cached by PostgREST.
- Reload the API schema:
  ```sql
  notify pgrst, 'reload schema';
  ```
  If that is not picked up, open **Project Settings → API → Reload schema cache** (or restart the local development session).
- Check the Table Editor to confirm you are in the project whose URL/key match the resolved configuration.

### 9.4 "function ... does not exist" or wrong RPC arguments

- Run the current `schema.sql`; the functions and grants are all created there.
- Use the parameter names exactly as documented (e.g. `p_slot_id`, `p_mode`, `p_concern_category`, `p_note`). Supabase RPC parameter names are case-sensitive in the JSON request.
- Check the function list:
  ```sql
  select proname, pg_get_function_identity_arguments(oid)
  from pg_proc
  where pronamespace = 'public'::regnamespace
  order by proname;
  ```

### 9.5 `btree_gist` / exclusion constraint error

If Supabase reports that the access method or operator class is unavailable:

1. Enable the **btree_gist** extension from **Database → Extensions**.
2. Re-run the table section in `schema.sql` (or the whole file on a fresh project).
3. Do not replace the exclusion constraint with an app-only check; overlapping counsellor slots must be blocked by Postgres.

### 9.6 Sign-up fails with "Database error saving new user"

The Auth service hides some trigger errors behind a generic response. Common causes:

- Missing student metadata fields or an invalid Malaysian phone number.
- A duplicate student ID (case-insensitive).
- The email is a counsellor email but the counsellor row has not been seeded, or an admin was created before the whitelist entry.
- The preferred session mode is not exactly `in_person` or `online`.

Use the app's Register screen (it sends all metadata) or your Dashboard metadata box. Correct fields and try a different demo email/ID if needed.

If the error persists, test the trigger manually in SQL (as the SQL Editor's `postgres` role) by checking the auth attempt's metadata in **Authentication → Users**. In the demo, the simplest reliable path is to verify the order in Step 5 and recreate the affected user.

### 9.7 Admin signed up as a student / counsellor not linked

The trigger only runs at user creation. If the order was wrong:

1. For an admin, insert/confirm the whitelist first:
   ```sql
   insert into public.admin_whitelist (email)
   values ('admin@university.edu.my')
   on conflict do nothing;
   ```
   Then fix the existing profile:
   ```sql
   update public.profiles
   set role = 'admin'
   where lower(email) = lower('admin@university.edu.my');
   ```
2. For a counsellor, confirm the seed row exists, then link it:
   ```sql
   update public.counsellors c
   set user_id = u.id
   from auth.users u
   where lower(c.email) = lower('counsellor.aisyah@university.edu.my')
     and lower(u.email) = lower(c.email);

   update public.profiles p
   set role = 'counsellor'
   from auth.users u
   where p.id = u.id
     and lower(u.email) = lower('counsellor.aisyah@university.edu.my');
   ```
3. Sign out and in again.

For a clean demo, deleting the affected Auth user and recreating it after fixing the seed order is usually simpler.

### 9.8 `seed_demo.sql` fails

- It deliberately requires the five demo users to exist with the right roles. Re-read its error message and Step 5.3.
- It requires `seed_reference.sql` to have been run **before** the counsellor users were created.
- If it fails, the whole DO block/transaction rolls back. Check the availability trigger state:
  ```sql
  select tgname, tgenabled
  from pg_trigger
  where tgrelid = 'public.availability_slots'::regclass
    and tgname = 'trg_availability_slot_guard';
  ```
  If it is disabled (not `O`/`true`), enable it, then fix the cause and re-run:
  ```sql
  alter table public.availability_slots enable trigger trg_availability_slot_guard;
  ```
- If there are no future slots, run `seed_reference.sql` again; slots are generated relative to today.
- If a demo student already has the maximum active appointments, remove/cancel one before re-running.

### 9.9 Emulator / phone cannot connect

- The app needs internet. An emulator is not automatically offline, but check Wi-Fi and mobile data.
- A cloud Supabase URL is public internet; do **not** replace it with `localhost` or `10.0.2.2`.
- `10.0.2.2` is only for reaching a server on the **host machine** from the Android emulator. It is not the Supabase cloud host.
- Check that the device date/time is correct and the network has no captive portal.
- Try opening `https://<project>.supabase.co` in the device browser.
- If you use a VPN or corporate proxy, disable it temporarily.
- After changing keys, uninstall/reinstall or clear app data.

### 9.10 Wrong date/time or slots appear in the past

- The database stores UTC. The app displays Asia/Kuala_Lumpur.
- If you see a time shift, check `DateTimeUtils` and the emulator time zone; do not write local-time strings into `timestamptz` columns.
- Seed slots are generated relative to `now()` in KL. Re-run `seed_reference.sql` if the demo database was created more than four weeks ago.

### 9.11 A slot cannot be deleted

This is expected when an appointment (including a cancelled/historical one) still references it. Deleting would break session history. Choose another free slot. A completely unused free slot can be deleted by its counsellor or an admin.

### 9.12 A session record cannot be edited

- Only records created within the last 7 days can have their content edited. Archive/unarchive is still allowed.
- Only completed appointments can have records.
- Follow-up due dates must be in the future when set/updated.
- Duplicate records for one appointment are blocked.

To test the 7-day rule without waiting, use a **disposable test appointment**, temporarily bypass the update guard to backdate the fixture, then immediately re-enable enforcement:

```sql
-- Replace <RECORD_ID> with a disposable record. Never leave the trigger disabled.
alter table public.session_records disable trigger trg_session_record_update;
update public.session_records
set created_at = now() - interval '8 days'
where id = '<RECORD_ID>';
alter table public.session_records enable trigger trg_session_record_update;
```

Then try editing the record's content in the app as the owning counsellor. It must be rejected with the 7-day message; archiving must still work.

### 9.13 Supabase Advisor lists "Security Definer View"

This is expected for `available_slots_view` and `student_session_summaries`. The brief requires a safe view that can read data the caller cannot read directly:

- `available_slots_view` must see all bookings so it can hide booked slots.
- `student_session_summaries` must filter the base table that students cannot access.

Both views expose only safe columns, and only `authenticated` is granted SELECT. Do not switch them to `security_invoker = true`; that would break the booking catalogue and could hide records from students. If you make any change to a view, re-check the RLS proof queries in Section 11.

### 9.14 Anonymous enquiry code lost

There is no identity linkage to recover it by design. The counsellor can see the content in the inbox, but the student cannot retrieve their own anonymous row without the code. For testing, use the seeded codes.

### 9.15 Admin cannot read notes or individual feedback

Intentional. Admin only has aggregate counts (`admin_dashboard_stats()`); private notes are counsellor-only. Do not "fix" this by adding an admin policy.

### 9.16 Demo sign-in returns HTTP 500 "Database error querying schema"

If every known demo email fails this way while an unknown email returns the normal `400 invalid_credentials`, GoTrue is working but the stored demo Auth rows are malformed. This happens when accounts are inserted directly into `auth.users` with raw SQL instead of being created through Supabase Auth.

**Do not insert into `auth.users` directly.** Recreate the affected demo accounts:

1. In **Authentication → Users**, delete the five malformed demo users. This cascades their `profiles` rows and linked demo appointments.
2. Recreate each user with **Add user**, **Auto Confirm User** enabled and password `Demo@12345`, following Step 5.3. Include the student metadata for Aina and Faris.
3. Re-run `seed_reference.sql`, then `seed_demo.sql`, so the counsellor links, student fields and demo appointments are restored.
4. Sign in again from the app.

If an unknown email *also* returns HTTP 500, the problem is at the project/Auth level rather than one user row: check **Project Settings → General** for a paused project, review **Logs → Auth**, and contact Supabase support if the database is unavailable.

---

## 10. Demo walkthrough (suggested order)

1. **Student Aina** signs in. Home shows welcome and next appointment. Open **Self-Help Resources → Wellbeing Self-Check**, complete 10 questions, see score/bracket and recommendations; tap **Book a counsellor** on Moderate/High to open Smart Match.
2. Aina books a new session with Daniel via **Appointments → Book**. Choose concern Anxiety to see Daniel ranked; pick a free slot; submit. Snackbar success.
3. Try booking the same slot with a second student/Faris: friendly "just booked" message. Try a third active booking for Aina: max 2 message.
4. Aina reschedules within 12 hours of a current appointment: blocked. Use a slot more than 12 hours away for success.
5. Aina cancels an appointment; the slot returns to the catalogue.
6. **Counsellor Aisyah** signs in. Schedule shows her appointments. Confirm a pending one, then mark a confirmed one completed.
7. Aisyah creates a session record for a completed appointment; review private/public fields, risk and follow-up due. Open **Records**; the Triage board shows due dates. Schedule a follow-up from a card; sign in to the student account to see the Follow-up tag.
8. Aisyah opens **Enquiries**, filters Open, replies to `ENQ-DEMO01`/Aina's enquiry, attaches a resource, sets Answered.
9. **Michael's Module 5**: counsellor/admin creates/edits a resource and toggles publish. Student sees only published items.
10. Students rate a completed session (1–5 + comment), see their feedback, delete it, rate again.
11. **Admin** signs in. Home lists all appointments read-only and aggregate stats. Add a counsellor, add weekly slots, edit a slot, try deleting a booked slot, and test the counsellor delete block. Open Students and search by name/ID.
12. Anonymous flow: submit an anonymous enquiry as a student, copy the code, sign out, sign back in, check reply by code. Withdraw the open anonymous enquiry with its code.
13. Deactivation: as a student with an upcoming appointment, attempt deactivate → blocked. Cancel active bookings → deactivate → attempt sign in → app signs out with an inactive message.
14. Run the RLS proof queries in Section 11.

---

## 11. Prove RLS and privacy

These tests run as the SQL Editor's `postgres` role but switch to the `authenticated` role and set a simulated user JWT. They do not modify data. Always finish with `rollback`.

### 11.1 Collect user IDs

```sql
select id, email
from auth.users
where email in (
  'student.aina@student.university.edu.my',
  'student.faris@student.university.edu.my',
  'counsellor.aisyah@university.edu.my',
  'counsellor.daniel@university.edu.my',
  'admin@university.edu.my'
)
order by email;
```

Copy the five UUIDs into the placeholders below.

### 11.2 Student Aina: own data only, no base notes

```sql
begin;
select set_config(
  'request.jwt.claims',
  '{"sub":"<AINA_UUID>","role":"authenticated","aud":"authenticated"}',
  true
);
select set_config('request.jwt.claim.sub', '<AINA_UUID>', true);
set local role authenticated;

-- 1. She sees only her own profile.
select count(*) as profiles_visible from public.profiles;
-- Expected: 1

-- 2. No appointment outside her own student id.
select count(*) as foreign_appointments
from public.appointments
where student_id <> auth.uid();
-- Expected: 0

-- 3. Base session_records table is inaccessible (no policy).
select count(*) as base_records from public.session_records;
-- Expected: 0

-- 4. Safe summary view exposes her own completed summary only.
select count(*) as summaries from public.student_session_summaries;
-- Expected: 1

-- 5. Student sees her linked non-anonymous enquiry, not the anonymous ones.
select count(*) as own_enquiries
from public.enquiries
where student_id = auth.uid();
-- Expected: 1 (ENQ-DEMO03)

select count(*) as anonymous_rows_visible
from public.enquiries
where is_anonymous = true;
-- Expected: 0

-- 6. The reference-code RPC can still read an anonymous enquiry safely.
select reference_code, status, student_id
from public.get_enquiry_by_code('ENQ-DEMO01');
-- Expected: one row, student_id is NULL (function does not even expose identity)

rollback;
```

Also inspect the view's public columns:

```sql
select column_name
from information_schema.columns
where table_schema = 'public'
  and table_name = 'student_session_summaries'
order by ordinal_position;
```

Expected: no `private_notes` and no `risk_level`.

### 11.3 Counsellor Aisyah: own sessions only, anonymous inbox without identity

```sql
begin;
select set_config(
  'request.jwt.claims',
  '{"sub":"<AISYAH_UUID>","role":"authenticated","aud":"authenticated"}',
  true
);
select set_config('request.jwt.claim.sub', '<AISYAH_UUID>', true);
set local role authenticated;

select count(*) as own_records from public.session_records;
-- Expected: 1 (Aina's record; Faris's record belongs to Daniel)

select count(*) as own_ratings from public.feedback;
-- Expected: 1 (Aina's rating for Aisyah)

select count(*) as inbox from public.enquiries;
-- Expected: 4 (all demo enquiries)

select student_id as anonymous_student_id
from public.enquiries
where reference_code = 'ENQ-DEMO01';
-- Expected: NULL; no identity is stored for anonymous enquiries

rollback;
```

### 11.4 Admin: oversight without private notes

```sql
begin;
select set_config(
  'request.jwt.claims',
  '{"sub":"<ADMIN_UUID>","role":"authenticated","aud":"authenticated"}',
  true
);
select set_config('request.jwt.claim.sub', '<ADMIN_UUID>', true);
set local role authenticated;

select count(*) as all_appointments from public.appointments;
-- Expected: >= 5 (seed data plus anything added during testing)

select count(*) as base_records_from_admin from public.session_records;
-- Expected: 0. There is no admin SELECT policy, so private notes are blocked.

select public.admin_dashboard_stats();
-- Expected: a JSON object containing total_students, total_counsellors,
-- upcoming_appointments, average_rating, etc. No notes or session text.

select count(*) as enquiries_from_admin from public.enquiries;
-- Expected: 0 (admin oversight does not include the enquiry inbox)

rollback;
```

### 11.5 Student cross-user write test (expected failure)

Run in a separate transaction. The insert attempts to add a resource while authenticated as Aina; the role has no INSERT grant/policy. A privilege/RLS error is the expected pass condition. (Because the statement errors, your SQL session may need a rollback/new query afterwards.)

```sql
begin;
select set_config('request.jwt.claims', '{"sub":"<AINA_UUID>","role":"authenticated"}', true);
select set_config('request.jwt.claim.sub', '<AINA_UUID>', true);
set local role authenticated;

insert into public.resources (
  title, description, category, media_type, content_url,
  target_stress_brackets, is_published
)
values (
  'Student should not be able to create this',
  'This insert must be rejected by privileges and RLS.',
  'Stress', 'Article', 'https://example.com/', array['low'], true
);
-- Expected: ERROR: permission denied for table resources / row-level security

rollback;
```

### 11.6 What this proves

- All nine tables have RLS with policies, not just UI hiding.
- A student cannot select another student's profile, appointments, enquiries, feedback or private notes.
- An admin cannot read `private_notes`, and has no enquiry inbox policy.
- The anonymous enquiry row contains no student identity and cannot be read by a student through the table.
- The safe views expose only the intended columns.

---

## 12. CRUD and validation test checklist (Chapter 4 test plan)

Use one row per test in your report. The "Expected result" column is the pass criterion. Test as each role with the demo accounts; reset by re-running `seed_demo.sql` or by creating fresh records.

### Module 1 — Student Account & Profile (Beh)

| ID | Test | Steps | Expected result |
|---|---|---|---|
| M1-01 | Register a valid student | Register with all fields, valid Malaysian phone `012-3456789`, strong password | Account created; profile role = student; signed in or prompted; no errors |
| M1-02 | Required-field validation | Leave name/student ID/programme/phone/mode empty in turn | Inline error under each field; no request sent |
| M1-03 | Phone validation | Enter `12345`, `01234567`, `+6512345678` | Rejected: friendly Malaysian phone error; DB regex would also reject |
| M1-04 | Password validation | Use `abcdefgh`, `ABCDEFGH`, `Abcdefgh`, `Abcdefg1` with missing types/short | Rejected with specific password-strength message |
| M1-05 | Duplicate email | Register with Aina's email | Friendly duplicate-email message from Auth |
| M1-06 | Duplicate student ID | Register new email using `DAMCS2401` | Friendly duplicate student-ID/database error; no profile created |
| M1-07 | Retrieve own profile | Student → Profile | Correct name, ID, programme, phone, mode |
| M1-08 | Staff search students | Counsellor/Admin → Students; search by name and by ID | Only matching student rows; detail opens with permitted information |
| M1-09 | Update profile | Change full name, programme, phone, mode; save | Success Snackbar; values persist after restart; invalid values rejected |
| M1-10 | Change password | Use current/new password | Success; old password fails; new password signs in |
| M1-11 | Blocked deactivation | Student with an upcoming active booking → Deactivate | Clear message that upcoming appointments must be cancelled first; account remains active |
| M1-12 | Deactivate account | Cancel all upcoming appointments, confirm deactivation | Success; account `is_active = false`; login signs out with inactive message; app data inaccessible |
| M1-13 | Privacy | Try to read another student's profile via the app/SQL | No rows or permission error; RLS test in Section 11 |

### Module 2 — Appointment Booking (Beh)

| ID | Test | Steps | Expected result |
|---|---|---|---|
| M2-01 | Smart Counsellor Match | Student → Book → choose concern `Anxiety` | Counsellors whose specialisations include Anxiety appear, ranked by earliest free slot; Best match badge and next free slot shown |
| M2-02 | Book via Smart Match | Tap recommended counsellor → choose future slot → choose mode/concern/add note → submit | Success Snackbar; appointment status `pending`; appears in Upcoming; slot disappears from available list |
| M2-03 | Client validation | Submit without concern, slot or mode | Inline errors; no RPC call; submit disabled while loading |
| M2-04 | Double booking | Two students select the same slot; submit close together | One succeeds; the other receives a friendly "just booked" message; DB has one active appointment for the slot |
| M2-05 | Student overlap | Student books a slot overlapping their existing active appointment | Friendly overlap message; not created |
| M2-06 | Maximum active bookings | Book 2 upcoming, then attempt a third | Friendly max-2 message; third not created |
| M2-07 | Past slot | Force/attempt a past slot | Friendly "already passed" message; guard trigger also blocks past availability creation |
| M2-08 | Upcoming/Past/Cancelled tabs | View all three tabs and filter chips | Correct records per status/date; empty and loading states shown |
| M2-09 | Reschedule inside 12 hours | Reschedule an appointment starting in less than 12 hours | Rejected with 12-hour notice message |
| M2-10 | Reschedule outside 12 hours | Choose another free slot with the same counsellor | Success; old slot free again; new slot held; no overlap |
| M2-11 | Counsellor confirms | Counsellor → Schedule → pending appointment → Confirm | Status becomes confirmed; student sees updated status |
| M2-12 | Invalid status transition | Try completed from pending, or completing without confirming | Friendly transition message; status unchanged |
| M2-13 | Complete / no-show | Confirm then mark completed/no-show | Allowed transition; record creation becomes available only after completed |
| M2-14 | Cancel and release | Student cancels a confirmed/upcoming appointment | Status cancelled; removed from Upcoming; slot appears in available slots again; confirmation dialog used |
| M2-15 | Admin read-only | Admin → Home appointment list | All appointments visible; no edit/cancel controls; direct write denied |
| M2-16 | Follow-up tag | Student views an appointment created via the triage board | Follow-up badge/tag shown (Demo Aina) |

### Module 3 — Counsellor & Availability Management (Jeremy)

| ID | Test | Steps | Expected result |
|---|---|---|---|
| M3-01 | Add counsellor | Admin → Counsellors → Add; valid name/email/specialisation/languages/bio | Success; counsellor appears unlinked until that email signs up; duplicate email rejected |
| M3-02 | Validation | Leave specialisations empty; invalid email; short name | Inline/database errors; no record |
| M3-03 | Add single slot | Admin/counsellor picks future date, start 09:00, end 10:00 | Slot saved; appears in available slots for active counsellor |
| M3-04 | Repeat weekly slots | Add slot with repeat 3 weeks | Exactly 3 rows, seven days apart; no duplicates |
| M3-05 | Invalid times | End before start, or start in the past | Friendly validation error; DB check/trigger rejects |
| M3-06 | Overlapping slot | Add 09:30–10:30 when 09:00–10:00 exists | Friendly overlap error from app and DB exclusion constraint |
| M3-07 | Search counsellors | Search by specialisation and "available this week" | Correct filtered results; detail shows upcoming free slots |
| M3-08 | Counsellor edits own slot | Counsellor → Schedule; edit free future slot | Success; updated time shown |
| M3-09 | Counsellor edits another slot | Attempt on another counsellor's slot | Not permitted; backend RLS blocks |
| M3-10 | Admin edits any slot | Admin edits a slot | Allowed |
| M3-11 | Delete free slot | Confirm delete of an unused free slot | Removed; available list updated |
| M3-12 | Delete booked slot | Try deleting a slot linked to an appointment | Blocked with friendly message; appointment intact |
| M3-13 | Deactivate / delete counsellor with upcoming | Admin attempts removal with a future active appointment | Blocked with clear message |
| M3-14 | Delete counsellor with history, no upcoming | Attempt removal on a counsellor with past appointments | Counsellor becomes inactive (`is_active = false`), disappears from booking, historical data retained |
| M3-15 | Hard delete counsellor with no history | Remove a newly added test counsellor with no appointments | Hard delete; `deleted` success message; row gone |
| M3-16 | Inactive hiding | Try booking an inactive counsellor | Not in available slots; direct booking function rejects |

### Module 4 — Session Records & Follow-Up (Jeremy)

| ID | Test | Steps | Expected result |
|---|---|---|---|
| M4-01 | Completed-only create | Counsellor tries to create a record for pending/confirmed appointment | Blocked with completed-only message |
| M4-02 | Create valid record | For a completed appointment enter 20+ char private notes, public summary, risk level, follow-up needed + future due | Success Snackbar; record saved; visible to owning counsellor |
| M4-03 | Notes validation | Private notes under 20 characters | Inline/database rejection; no record |
| M4-04 | Risk required | Omit risk | Inline validation; DB `NOT NULL` rejects |
| M4-05 | Follow-up due validation | follow-up needed with blank/past due | Rejected with friendly future-date message |
| M4-06 | One record per appointment | Try creating a second record for the same completed appointment | Friendly duplicate message; unique constraint remains |
| M4-07 | Edit within 7 days | Change summary/risk/follow-up before 7 days | Success; student public summary updates; private notes remain private |
| M4-08 | Edit after 7 days | On a disposable record, temporarily disable `trg_session_record_update` in SQL, set `created_at = now() - interval '8 days'`, re-enable the trigger, then edit content as its counsellor | Rejected with 7-day message; archive still possible |
| M4-09 | Archive | Archive a record, toggle "show archived" | Record hidden by default, visible with toggle; student view no longer shows it; no hard delete |
| M4-10 | Student privacy | Student views session history | Only public summaries via the safe view; no private notes or risk level |
| M4-11 | Admin privacy | Admin views dashboard | No notes or individual records; aggregate counts only |
| M4-12 | Counsellor scope | Counsellor views records | Only their own records; no other counsellor's records |
| M4-13 | Triage board | Counsellor → Records → Triage board | High-risk or follow-up records in Overdue / Due soon / Later, sorted by due date |
| M4-14 | Schedule follow-up | Tap "Schedule follow-up", choose one of the counsellor's free slots | Pending appointment created, linked via `follow_up_of`; student sees Follow-up tag; max/overlap rules still apply |
| M4-15 | Triage without available slots | No free slot for the counsellor | Empty state explains no slots, with retry; no appointment created |

### Module 5 — Self-Help Resource Library (Michael)

| ID | Test | Steps | Expected result |
|---|---|---|---|
| M5-01 | Create resource | Counsellor/admin adds valid Article/PDF/etc. | Success; visible to staff; student visibility depends on publish flag |
| M5-02 | URL validation | Use `ftp://...`, `example.com`, or blank URL | Client and server reject with friendly URL error |
| M5-03 | Category/media validation | Omit/craft invalid category or media type | Rejected; only the fixed constants accepted |
| M5-04 | Duplicate title | Use an existing title differing only by case | Friendly duplicate-title error; unique lower-title index blocks it |
| M5-05 | Length limits | Title too long/short, description limits | Inline errors; DB checks |
| M5-06 | Retrieve/search/filter | Student/Staff browse, search, filter category and media type | Correct results, loading/empty/error states, pull-to-refresh |
| M5-07 | Student visibility | Student browse while a draft exists | Draft is not shown; published only |
| M5-08 | Publish/unpublish | Staff toggle `is_published` | Students see/hide item on refresh; success Snackbar |
| M5-09 | Edit resource | Change title/description/media/target brackets | Success; data persists; title uniqueness still enforced |
| M5-10 | Delete resource | Confirm delete dialog then delete | Row removed; success Snackbar; confirm dialog prevents accidental deletion |
| M5-11 | Self-check brackets | Complete questionnaire with low, moderate and high totals (0–13, 14–26, 27–40) | Correct score/bracket; Low/Moderate/High results match the constants |
| M5-12 | Recommendations | View result resources for each bracket | Matching published resources; Hotline only for High; no answers/scores stored |
| M5-13 | Book from self-check | Moderate/High result → Book a counsellor | Opens Module 2 Smart Match with the relevant concern; Low result does not require it (spec allows the result screen) |
| M5-14 | Disclaimer | Open result screen | Clear "screening aid only, not a diagnosis" disclaimer visible |

### Module 6 — Enquiry & Session Feedback (Michael)

| ID | Test | Steps | Expected result |
|---|---|---|---|
| M6-01 | Anonymous enquiry | Student submits anonymous enquiry with valid category/message | Success; reference code shown with copy button; `student_id` NULL in DB |
| M6-02 | Message validation | 9 chars / 1001 chars / no category | Client inline and server rejection; no row |
| M6-03 | Non-anonymous enquiry | Submit without anonymous toggle | Shows in student's own list; identity is stored |
| M6-04 | Check reply by code | Enter `ENQ-DEMO02` in the lookup screen | Message, reply, status, attached resource title; no identity fields |
| M6-05 | Invalid code | Enter `abc`, wrong code | Friendly "valid reference code" or not found error |
| M6-06 | Withdraw anonymous | Open anonymous enquiry + reference code → confirm withdraw | Deleted if open; success; code no longer finds it |
| M6-07 | Withdraw linked | Withdraw Aina's own open enquiry | Success; row gone; other students' enquiries untouched |
| M6-08 | Counsellor inbox/filter | Counsellor → Enquiries filter Open/Answered/Closed | All visible; anonymous displays "Anonymous student"; no identity shown |
| M6-09 | Reply and status | Reply, set Answered, optionally attach a Module 5 resource | Success; student sees reply; resource title shown; replied_at set |
| M6-10 | Answered without reply | Set status Answered with blank reply | Rejected with friendly message |
| M6-11 | Protected enquiry fields | Try editing message/student/anonymity via SQL as counsellor | Denied by column privileges + trigger |
| M6-12 | Rate completed session | Student rates own completed appointment 1–5 + optional comment | Success; counsellor sees rating; admin stats update |
| M6-13 | Invalid rating | Rating 0/6 (crafted) | Client and server rejection; no row |
| M6-14 | Only own/completed | Try rating someone else's or a pending appointment | Denied by RLS/trigger; no row |
| M6-15 | One rating | Submit a second rating for same completed appointment | Friendly duplicate message; unique constraint |
| M6-16 | Remove own feedback | Delete own rating, then optionally rate again | First delete succeeds; second rating permitted after removal |
| M6-17 | Admin aggregates | Admin Home stats | Feedback count and average shown; no individual comments or notes |
| M6-18 | Anonymity proof | Counsellor opens anonymous enquiry; inspect API/SQL | No student UUID/name can be derived from row; only message/reply/ref code |

### Integration / security checks

| ID | Test | Expected result |
|---|---|---|
| I-01 | Role-based bottom nav | Student/counsellor/admin see exactly the 5 tabs in the brief |
| I-02 | Shared data integration | Booking Smart Match uses counsellor/availability data; follow-up creates appointment; resource attaches to reply; feedback shows in counsellor view and admin aggregates |
| I-03 | RLS proof | Section 11 expected counts hold |
| I-04 | Snackbar on every action | Success green; error red; destructive confirmation dialogs present |
| I-05 | Empty/loading/error states | Every list has shimmer/loading, empty message, retry on error |
| I-06 | Session restore | Sign in / app restart restores correct role and dashboard |
| I-07 | Deactivated account | Login blocked with message; direct data reads empty |
| I-08 | Admin appointment read-only | No write affordances; enforced backend too |

---

## 13. Reset the demo

If you need a clean run:

1. Create a new Supabase project (safest) and repeat Steps 3–6.
2. Or, in the SQL Editor, drop the app views/tables **and** the Auth users, then re-run the scripts. Dropping application tables while Auth users still exist will leave users without profiles; delete/recreate the users or use a fresh project.

`supabase.properties` intentionally tracks the publishable project URL and client key. Do **not** commit a service_role/secret key, screenshots containing a secret key, or database dumps containing real student data. Keep machine-specific `local.properties` out of Git.

---

## 14. Quick reference

| Action | Command / location |
|---|---|
| Run full backend | SQL files: `schema.sql` → `seed_reference.sql` → users → `seed_demo.sql` |
| Reload PostgREST | `notify pgrst, 'reload schema';` |
| Check roles | `select email, role, is_active from public.profiles order by email;` |
| Find anonymous code | Seeded codes only: `ENQ-DEMO01`, `ENQ-DEMO02` |
| Student mode format | `in_person` or `online` (lowercase, underscore) |
| Phone format | `012-3456789` or `+60123456789` |
| Seed slot trigger | `trg_availability_slot_guard` on `public.availability_slots` |
| RLS helpers | `public.app_role()`, `public.app_counsellor_id()` |
| Safe student summaries | `public.student_session_summaries` |
| Available booking catalogue | `public.available_slots_view` |
| Admin aggregate stats | `public.admin_dashboard_stats()` |
