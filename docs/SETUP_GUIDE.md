# Setup Guide — OOP Refactor (Phase 7)

**Project:** University Counselling Appointment & Support System (AMCS2194)
**Database:** PostgreSQL / Supabase, OOP class-table schema
**Fresh schema:** [`supabase/schema.sql`](../supabase/schema.sql)
**Existing-project migration:** [`supabase/migrations/20261120_oop_refactor.sql`](../supabase/migrations/20261120_oop_refactor.sql)

> **Phase 7 status:** the OOP database, all six Android modules and the final
> class/OOP documentation are complete. Test the SQL on a **copy** of the
> Supabase project first, then recreate the demo Auth accounts for the final
> walkthrough.

---

## 0. What changed

The old `profiles`-with-role design is replaced by:

- `users` supertype + `students`, `counsellors`, `administrators`
- `resources` supertype + 5 subtype tables
- `enquiries` supertype + identified/anonymous subtypes
- association tables for specialisations, languages and target brackets
- reference tables for categories, modes, risks, stress brackets and statuses
- `appointment_status_transitions` as the database State-pattern table

The old tables are preserved in schema `legacy` by the migration. They are not
read by the new app.

Files:

```text
supabase/
  schema.sql                                  # fresh install
  seed_reference.sql                          # fresh install reference data
  seed_demo.sql                               # fresh install demo data
  migrations/
    20261120_oop_refactor.sql                 # non-destructive existing-project migration
  optional_drop_old_tables.sql                # destructive, run only after verification
  test_queries_oop_rls.sql                    # RLS/privacy proof
docs/
  ERD.mmd
  DATA_DICTIONARY.md
```

---

## 1. Prerequisites and configuration

- Supabase project with SQL Editor access.
- PostgreSQL 15+ (Supabase-managed).
- Android Studio for the app phases.
- The publishable Supabase URL and key in the shared
  [`supabase.properties`](../supabase.properties).
- Do **not** use a `service_role`/secret key in the Android app.

If you use a local override, put only `sdk.dir` and local Supabase overrides in
`local.properties`; it is git-ignored. The tracked shared file is intentional so
a teammate can build the app.

---

# Part A — Fresh Supabase project

## 2. Run the SQL in order

### 2.1 `supabase/schema.sql` — first

Paste the whole file into a new SQL Editor query and run it. It creates:

- 27 tables and 10 views
- reference/association tables
- class-table inheritance subtype triggers
- RPCs and RLS policies
- the `auth.users` sign-up trigger

Verify:

```sql
select count(*) as base_tables
from information_schema.tables
where table_schema = 'public' and table_type = 'BASE TABLE';
-- Expected: 27

select count(*) as views
from information_schema.views
where table_schema = 'public';
-- Expected: 10

select count(*) as policies
from pg_policies
where schemaname = 'public';
-- Expected: at least 26
```

If `btree_gist` is missing, enable it under **Database → Extensions** and re-run
only the extension line, or run the schema again on a fresh project. The
`availability_slots_no_overlap` exclusion constraint depends on it.

### 2.2 `supabase/seed_reference.sql` — second, before Auth users

It seeds:

- concern categories, modes, risks, stress brackets, statuses, transitions and
  languages
- one pre-provisioned administrator (`users` + `administrators`, auth null)
- two pre-provisioned counsellors (`users` + `counsellors` + associations)
- 11 resources (supertype + subtype + target brackets)
- four weeks of free counsellor availability

Verify:

```sql
select count(*) from public.concern_categories;          -- 8
select count(*) from public.appointment_statuses;         -- 5
select count(*) from public.appointment_status_transitions; -- 5
select count(*) from public.counsellors;                 -- 2
select count(*) from public.administrators;              -- 1
select count(*) from public.resources;                   -- 11
select count(*) from public.availability_slots;          -- 80, before demo history
```

### 2.3 Create the Auth users — third

Staff rows already exist with `auth_user_id = NULL`; the sign-up trigger links
them by email. Create the admin and two counsellor Auth accounts with these
emails and their usual passwords:

- `admin@university.edu.my`
- `counsellor.aisyah@university.edu.my`
- `counsellor.daniel@university.edu.my`

Students must be created with metadata because they are a new student object.
Create these two accounts in the Dashboard **with metadata** (or register them
through the app once Phases 4–6 are complete):

| Email | Password | Metadata |
|---|---|---|
| `student.aina@student.university.edu.my` | `Demo@12345` | `full_name=Aina Zulaikha`, `student_id=DAMCS2401`, `programme=Diploma in Computer Science`, `phone=012-3456789`, `preferred_session_mode=in_person` |
| `student.faris@student.university.edu.my` | `Demo@12345` | `full_name=Muhammad Faris`, `student_id=DAMCS2402`, `programme=Diploma in Information Technology`, `phone=+60123456789`, `preferred_session_mode=online` |

The “Add user” metadata field, when available, must be a JSON object:

```json
{
  "full_name": "Aina Zulaikha",
  "student_id": "DAMCS2401",
  "programme": "Diploma in Computer Science",
  "phone": "012-3456789",
  "preferred_session_mode": "in_person"
}
```

Turn **Confirm email OFF** for the demo so the app receives a session
immediately.

### 2.4 `supabase/seed_demo.sql` — fourth

Run after all five Auth users exist. It creates two completed sessions with
records and ratings, upcoming appointments, a cancelled session, and four
enquiries with codes `ENQ-DEMO01`–`ENQ-DEMO04`.

The script temporarily disables only the availability guard for historical
slots and restores it; everything runs in one transaction.

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

---

## 3. Verify the subtype rules

```sql
-- Every resource has exactly one matching subtype row.
select count(*) as invalid_resources
from public.resources r
where (
    select count(*) from (
        select 1 from public.article_resources where resource_id = r.id
        union all select 1 from public.video_resources where resource_id = r.id
        union all select 1 from public.audio_resources where resource_id = r.id
        union all select 1 from public.pdf_resources where resource_id = r.id
        union all select 1 from public.hotline_resources where resource_id = r.id
    ) x
) <> 1;
-- Expected: 0

-- Every enquiry has exactly one identity subtype.
select count(*) as invalid_enquiries
from public.enquiries e
where (
    select count(*) from (
        select 1 from public.identified_enquiries where enquiry_id = e.id
        union all select 1 from public.anonymous_enquiries where enquiry_id = e.id
    ) x
) <> 1;
-- Expected: 0

-- Every users row has exactly one role subtype.
select count(*) as invalid_users
from public.users u
where (
    select count(*) from (
        select 1 from public.students where user_id = u.id
        union all select 1 from public.counsellors where user_id = u.id
        union all select 1 from public.administrators where user_id = u.id
    ) x
) <> 1;
-- Expected: 0
```

---

# Part B — Existing Supabase project (non-destructive)

## 4. Prepare a copy

**Do not run the migration on the live demo project first.**

1. In Supabase, create a copy/restore or a second project.
2. Apply the current old schema if the copy is empty.
3. Verify the copy has the old nine tables: `profiles`, `admin_whitelist`,
   `counsellors`, `availability_slots`, `appointments`, `session_records`,
   `resources`, `enquiries`, `feedback`.
4. Export/back up the copy.
5. Run
   [`supabase/migrations/20261120_oop_refactor.sql`](../supabase/migrations/20261120_oop_refactor.sql)
   once in the SQL Editor.

What it does:

1. Preflights old tables, duplicate emails, UUID collisions, incomplete
   counsellor profiles.
2. Drops the old `auth.users` trigger temporarily.
3. Moves the old tables/views to schema `legacy`; revokes API access to them.
4. Creates the new OOP schema and reference rows.
5. Copies users/subtypes, associations, slots, appointments, records, feedback,
   resources/subtypes, enquiries/subtypes.
6. Disables only the historical-slot and record-prepare guards for the copy
   window, then restores them.
7. Runs count/FK verification and **raises an error (rollback)** on mismatch.
8. Keeps `legacy` for rollback.

The old `profiles`/`counsellors`/etc. tables are **not dropped** by this
migration.

## 5. Verify the migrated copy

```sql
select count(*) from public.users;             -- students + counsellors + admins
select count(*) from public.students;          -- = old profiles role student
select count(*) from public.counsellors;       -- = old counsellors
-- The old admin_whitelist is preserved only for rollback:
select count(*) from legacy.admin_whitelist;
select count(*) from public.appointments;      -- = old appointments
select count(*) from public.session_records;   -- = old records
select count(*) from public.feedback;          -- = old feedback
select count(*) from public.resources;         -- = old resources
select count(*) from public.enquiries;         -- = old enquiries
```

Then run the subtype checks in Section 3. If any count is wrong, **do not**
proceed and do not run the optional drop; restore the copy.

## 6. Optional destructive cleanup

[`supabase/optional_drop_old_tables.sql`](../supabase/optional_drop_old_tables.sql)
drops schema `legacy` cascade. Run it only after:

- migration verification passes,
- a backup/export exists,
- the new app is tested against the copy.

Until then, `legacy` is the rollback copy. The app does not read it.

---

## 7. Prove RLS and privacy

Run [`supabase/test_queries_oop_rls.sql`](../supabase/test_queries_oop_rls.sql).
It switches to the `authenticated` role with simulated JWTs and ends every
block with `ROLLBACK`.

Key expected results on the demo data:

| Test | Expected |
|---|---|
| Aina `users` | 1 |
| Aina `appointments` | 3 |
| Aina base `session_records` | 0 |
| Aina safe summaries | 1 |
| Aina `resources` | 10 published (draft hidden) |
| Aina `enquiries` | 1 identified; anonymous 0 |
| Aisyah own appointments | 4 |
| Aisyah own records | 1 |
| Aisyah students | 2 |
| Aisyah inbox | 4 |
| Admin appointments | 6 |
| Admin records | 0 |
| Admin enquiries | 0 |
| Student direct resource insert | permission denied |
| Student direct appointment insert | permission denied |

The old views are now `v_students`, `v_counsellors`, `v_administrators`,
`v_availability_slots`, `v_appointments`, `v_session_records`, `v_feedback`,
`v_resources`, `v_enquiries`, plus `student_session_summaries`.

Supabase Advisor may label these as **Security Definer View**. That is intentional
for the same reason as the old safe views: each view is owner-rights and applies
an explicit `app_role()`/`auth.uid()` predicate, while the base tables keep RLS.
Do not flip a view to security-invoker without re-testing the privacy matrix.

---

## 8. Android configuration (for the later phases)

The app still uses the old repositories until Phases 4–6. When the app phases
begin:

1. Keep `supabase.properties` tracked with the publishable URL/key.
2. `build.gradle.kts` resolves from `local.properties` → `supabase.properties`
   → environment.
3. Build with `.\gradlew.bat :app:assembleDebug`.
4. Install on the emulator with the latest APK.
5. Do not test against the migrated database with the old APK; the old
   repositories query `profiles`, which is now in `legacy`.

---

## 9. Troubleshooting

### “relation public.profiles does not exist”
The database has been migrated to OOP, but the app is still the old build. This
is expected until Phases 4–6; use the legacy schema only for inspection.

### Migration raises “duplicate email addresses”
Resolve the duplicate in the old `profiles`, `counsellors` or `admin_whitelist`
rows on the **copy**, then restore the backup and retry. Do not edit live data
blindly.

### Migration raises “profile id collides with a counsellor id”
The old UUIDs overlap. Inspect the specific rows with:

```sql
select p.id, p.email, c.email
from public.profiles p
join public.counsellors c on c.id = p.id;
```

### “This administrator profile is incomplete” during Auth sign-up
The pre-provisioned `users` row exists but its subtype is missing. Re-run
`seed_reference.sql`; it completes missing administrator/counsellor subtypes.

### `btree_gist` / exclusion error
Enable the extension in **Database → Extensions**, or run on a fresh project.
The availability overlap rule is a real database constraint.

### Auth sign-up says “Database error saving new user”
Check **Logs → Auth** and the SQL error. Common causes:

- student metadata missing (`full_name`, `student_id`, `programme`, `phone`,
  `preferred_session_mode`);
- duplicate student ID;
- student row already exists with a different type;
- the pre-provisioned staff row is missing its subtype.

### Admin signs in as student / counsellor not linked
The migration links existing `counsellors.user_id` to
`users.auth_user_id`; fresh setup links by email. Verify:

```sql
select u.email, u.user_type, u.auth_user_id, c.user_id as counsellor_subtype
from public.users u
left join public.counsellors c on c.user_id = u.id
where lower(u.email) in (
  'admin@university.edu.my',
  'counsellor.aisyah@university.edu.my',
  'counsellor.daniel@university.edu.my'
);
```

### Student cannot rate a completed session
The app-side grey-button bug is fixed in Phase 1 code, but the database still
requires a completed appointment owned by the student and one rating per
appointment. Check:

```sql
select a.id, a.status_code, f.id as feedback_id
from public.appointments a
left join public.feedback f on f.appointment_id = a.id
where a.student_id = public.app_user_id();
```

### Resources are always empty for a student
Students only see `is_published = true` and the view uses their role. Check
`v_resources` as the student JWT; do not query `resources` as postgres and
assume the app result.

### Old demo Auth rows are broken
The OOP migration does not repair malformed `auth.users` rows. If Dashboard
login returns HTTP 500/DB schema errors, follow the clean Auth recovery in the
project notes or use a fresh Supabase project. The migration preserves valid
Auth UUIDs.

---

> The app UI still needs Phases 4–6 before these can be run end-to-end against
> the OOP database. The module behaviours themselves do not change.

## 10. CRUD and validation test checklist (Chapter 4 test plan)

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
| M2-17 | Counsellor declines pending | Counsellor → Schedule → pending request → Decline booking → confirm | Status cancelled; slot released; student sees the request in Cancelled; success Snackbar |
| M2-18 | Counsellor cancels confirmed | Counsellor → Schedule → confirmed session → Cancel session → confirm | Emergency cancellation succeeds before the start; slot released; student sees Cancelled |
| M2-19 | Counsellor reschedules confirmed | Counsellor → Schedule → confirmed session → Reschedule → choose a future free slot of their own | Status stays confirmed; original slot released; student sees the new time; no 12-hour student restriction applies |
| M2-20 | Counsellor action after start | Try declining/cancelling/rescheduling after the session start | Blocked with a friendly "already started" message; status unchanged |

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
| M4-09 | Archive | Archive a record, toggle "show archived" | Record hidden by default, visible with toggle; student view no longer shows it; hard-delete still requires confirmation |
| M4-10 | Student privacy | Student views session history | Only public summaries via the safe view; no private notes or risk level |
| M4-11 | Admin privacy | Admin views dashboard | No notes or individual records; aggregate counts only |
| M4-12 | Counsellor scope | Counsellor views records | Only their own records; no other counsellor's records |
| M4-13 | Triage board | Counsellor → Records → Triage board | High-risk or follow-up records in Overdue / Due soon / Later, sorted by due date |
| M4-14 | Schedule follow-up | Tap "Schedule follow-up", choose one of the counsellor's free slots | Pending appointment created, linked via `follow_up_of`; student sees Follow-up tag; max/overlap rules still apply |
| M4-15 | Triage without available slots | No free slot for the counsellor | Empty state explains no slots, with retry; no appointment created |
| M4-16 | Delete archived record | Archive a record → show archived → Delete forever → confirm | Permanently removed; student summary disappears; success Snackbar; appointment remains |
| M4-17 | Delete unarchived record | Attempt a direct/delete request on a non-archived record | Blocked by RLS + BEFORE DELETE trigger with "Archive this session record before deleting it permanently"; record intact |

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

---

## 11. Reset / rollback

### Fresh project

Re-running `seed_demo.sql` is idempotent for the demo rows; it will not duplicate
the records, ratings, enquiries or appointments. Re-run `seed_reference.sql` if
the four-week availability window has passed.

For a full reset, create a new Supabase project and repeat Part A. Do not delete
the shared `supabase.properties`.

### Migrated project

- The old tables are in `legacy`. To roll back before dropping it, restore the
  project backup or rename/restore from `legacy`; do not point the app at the
  new schema while still on the old APK.
- [`optional_drop_old_tables.sql`](../supabase/optional_drop_old_tables.sql)
  removes the rollback copy permanently.
- Running `seed_demo.sql` twice is safe; it checks by appointment slot, record,
  feedback and reference code.

---

## 12. Quick reference

| Item | Value |
|---|---|
| Fresh schema | `supabase/schema.sql` |
| Existing-project migration | `supabase/migrations/20261120_oop_refactor.sql` |
| Reference seed | `supabase/seed_reference.sql` |
| Demo seed | `supabase/seed_demo.sql` |
| Old-table cleanup | `supabase/optional_drop_old_tables.sql` |
| RLS proof | `supabase/test_queries_oop_rls.sql` |
| Supertype/subtype helpers | `public.app_user_id()`, `app_identity_id()`, `app_role()`, `app_counsellor_id()` |
| Profile method | `update_student_profile` |
| Appointment methods | `book_appointment`, `reschedule_appointment`, `cancel_appointment`, `update_appointment_status`, `counsellor_reschedule_appointment`, `create_followup_appointment` |
| Aggregate methods | `create_counsellor`, `update_counsellor`, `create_resource`, `update_resource`, `delete_resource` |
| Enquiry methods | `submit_enquiry`, `get_enquiry_by_code`, `withdraw_enquiry_by_code` |
| Admin aggregate | `admin_dashboard_stats` |
| Auth trigger | `public.handle_new_user()` on `auth.users` |
| Views | `v_students`, `v_counsellors`, `v_administrators`, `v_availability_slots`, `v_appointments`, `v_session_records`, `v_feedback`, `v_resources`, `v_enquiries`, `student_session_summaries` |

---

## 13. Phase status

| Area | Status |
|---|---|
| OOP database schema | Complete |
| Non-destructive migration | Complete |
| Reference/demo seeds | Complete |
| RLS proof | Complete |
| Android domain layer | ✅ Phase 2 (`core/domain`) |
| Shared repository/ViewModel infrastructure | ✅ Phase 3 |
| Beh feature repositories/DTOs/mappers | ✅ Phase 4 |
| Jeremy feature repositories/DTOs/mappers | ✅ Phase 5 |
| Michael feature repositories/DTOs/mappers | ✅ Phase 6 |
| Beh modules (student, appointment, Smart Match) | ✅ Phase 4 |
| Jeremy modules (counsellor, availability, records, triage) | ✅ Phase 5 |
| Michael modules (resources, enquiries, feedback) | ✅ Phase 6 |
| Class diagram / OOP notes / final cleanup | ✅ Phase 7 |

All screens now read the OOP views and RPCs. Before the final walkthrough,
recreate the demo Auth rows and test on a copy of the Supabase project. The
assessor-facing evidence is in
[`OOP_NOTES.md`](OOP_NOTES.md) and [`CLASS_DIAGRAM.md`](CLASS_DIAGRAM.md).
