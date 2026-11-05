# Data Dictionary — University Counselling Appointment & Support System

**Project code:** AMCS2194 Diploma Year 2 mini project  
**Supabase schema:** `public` (plus Supabase-managed `auth.users`)  
**Database:** PostgreSQL 15+ on Supabase  
**Last schema update:** Phase 1 (`supabase/schema.sql`)

All application timestamps are stored as `timestamptz` in UTC. The Android app formats them in **Asia/Kuala_Lumpur** for display. `now()` in the database is evaluated at UTC; seed scripts convert generated Malaysian local times with `at time zone 'Asia/Kuala_Lumpur'`.

---

## 1. Shared constants

These values are mirrored in `core/Constants.kt` in the Android app.

| Constant | Values | Used by |
|---|---|---|
| Roles | `student`, `counsellor`, `admin` | `profiles.role` |
| Concern categories | `Stress`, `Anxiety`, `Academic Pressure`, `Relationships`, `Sleep`, `Financial`, `Loneliness`, `Other` | counsellor specialisations, appointments, resources, enquiries |
| Session modes | `in_person`, `online` | profiles, appointments |
| Appointment statuses | `pending`, `confirmed`, `completed`, `no_show`, `cancelled` | appointments |
| Risk levels | `low`, `medium`, `high` | session records |
| Resource media types | `Article`, `Video`, `Audio`, `Hotline`, `PDF` | resources |
| Self-check stress brackets | `low`, `moderate`, `high` | resources' target brackets |
| Maximum active bookings | `2` | enforced in booking/follow-up functions |
| Minimum student reschedule notice | `12 hours` | enforced in `reschedule_appointment` |
| Counsellor emergency notice | no 12-hour minimum | `counsellor_reschedule_appointment` for confirmed sessions |
| Session record edit window | `7 days` | enforced in `trg_session_record_update` |

The self-check score brackets are application constants (0–13 Low, 14–26 Moderate, 27–40 High). Questionnaire answers and scores are **not stored** anywhere in this schema.

---

## 2. Entity overview

| # | Table | Purpose |
|---|---|---|
| 1 | `profiles` | One app profile per Supabase Auth user; holds role and student details |
| 2 | `admin_whitelist` | Emails that receive the admin role on sign-up |
| 3 | `counsellors` | Counsellor directory and Auth-account link |
| 4 | `availability_slots` | Bookable counsellor time blocks |
| 5 | `appointments` | Student bookings and their lifecycle |
| 6 | `session_records` | Private/public post-session records and follow-ups |
| 7 | `resources` | Self-help resource library |
| 8 | `enquiries` | Anonymous or linked student enquiries |
| 9 | `feedback` | One 1–5 star rating per completed appointment |

Supabase's `auth.users` is managed by Supabase Auth. The application never writes passwords to these tables; password hashes and sessions stay in the Auth schema.

### Entity relationships

```text
auth.users 1 ─── 1 profiles
auth.users 0..1 ─── 0..1 counsellors (via counsellors.user_id)
counsellors 1 ─── * availability_slots
counsellors 1 ─── * appointments
profiles 1 ─── * appointments (as student)
availability_slots 1 ─── * appointments
appointments 1 ─── 0..1 session_records
appointments 1 ─── 0..1 feedback
appointments 0..1 ─── * appointments (follow_up_of self-reference)
profiles 0..1 ─── * enquiries (nullable when anonymous)
profiles 1 ─── * feedback
counsellors 1 ─── * feedback
resources 0..1 ─── * enquiries (recommended resource)
```

A visual version is in [`ERD.mmd`](ERD.mmd).

---

## 3. Table definitions

### 3.1 `profiles`

| Column | Type | Null | Default | Constraints / notes |
|---|---|---|---|---|
| `id` | `uuid` | No | — | **PK**. **FK** → `auth.users(id)` `ON DELETE CASCADE`. |
| `email` | `text` | No | — | Unique case-insensitive index `profiles_email_lower_uidx`. Copied from Auth at sign-up. |
| `role` | `text` | No | `'student'` | CHECK: `student`, `counsellor`, `admin`. Set only by the auth trigger. |
| `full_name` | `text` | No | — | CHECK: trimmed length 2–100. |
| `student_id` | `text` | Yes | — | CHECK when present: trimmed length 3–30. Unique case-insensitive partial index `profiles_student_id_lower_uidx`. Null for counsellors/admins. |
| `programme` | `text` | Yes | — | CHECK when present: trimmed length 2–120. |
| `phone` | `text` | Yes | — | CHECK when present: `01X-XXXXXXX` or `+601X...` Malaysian format. |
| `preferred_session_mode` | `text` | Yes | — | CHECK: `in_person` or `online`. Required for students by `profiles_student_fields_required`. |
| `is_active` | `boolean` | No | `true` | `false` means deactivated. Deactivated users can read their own profile so the app can sign them out. |
| `created_at` | `timestamptz` | No | `now()` | UTC. |
| `updated_at` | `timestamptz` | No | `now()` | Maintained by `trg_profiles_updated_at`. |

**Named check constraints**

| Name | Rule |
|---|---|
| `profiles_full_name_length` | `length(trim(full_name)) BETWEEN 2 AND 100` |
| `profiles_student_id_length` | If not null: `length(trim(student_id)) BETWEEN 3 AND 30` |
| `profiles_programme_length` | If not null: `length(trim(programme)) BETWEEN 2 AND 120` |
| `profiles_phone_format` | If not null: matches `^(01[0-9]-[0-9]{7}|\+601[0-9][0-9]{7,8})$` |
| `profiles_student_fields_required` | Students must have `student_id`, `programme`, `phone`, `preferred_session_mode` |

**Triggers**

| Trigger | Timing | Action |
|---|---|---|
| `on_auth_user_created` (on `auth.users`) | AFTER INSERT | Calls `handle_new_user()`; inserts this profile, selects role and links counsellor accounts. |
| `trg_profiles_updated_at` | BEFORE UPDATE | Sets `updated_at = now()`. |

### 3.2 `admin_whitelist`

| Column | Type | Null | Default | Constraints / notes |
|---|---|---|---|---|
| `id` | `bigint` | No | identity | **PK**, generated identity. |
| `email` | `text` | No | — | CHECK: non-empty and contains `@`. Unique case-insensitive index `admin_whitelist_email_lower_uidx`. |
| `created_at` | `timestamptz` | No | `now()` | UTC. |

- No foreign keys.
- `handle_new_user()` checks this table before creating a profile. If an email is in both `admin_whitelist` and `counsellors`, **admin wins**.
- No app UI writes this table; it is managed from SQL/setup or service-side operations. Demo `admin@university.edu.my` is inserted by `seed_reference.sql`.

### 3.3 `counsellors`

| Column | Type | Null | Default | Constraints / notes |
|---|---|---|---|---|
| `id` | `uuid` | No | `gen_random_uuid()` | **PK**. |
| `user_id` | `uuid` | Yes | — | **FK** → `auth.users(id)` `ON DELETE SET NULL`. UNIQUE. Null until the matching email creates an Auth account. Set by the auth trigger. |
| `full_name` | `text` | No | — | CHECK: trimmed length 2–100. |
| `email` | `text` | No | — | CHECK: basic email pattern. Unique case-insensitive index `counsellors_email_lower_uidx`. Must match the counsellor's Auth email for auto-linking. |
| `specialisations` | `text[]` | No | — | CHECK: 1–8 entries, no nulls, all entries from the eight concern categories. |
| `languages` | `text[]` | No | — | CHECK: 1–10 entries, no nulls and no empty strings. |
| `bio` | `text` | No | `''` | CHECK: maximum 1,000 characters. |
| `is_active` | `boolean` | No | `true` | `false` hides the counsellor from new booking via views/functions. |
| `created_at` | `timestamptz` | No | `now()` | UTC. |
| `updated_at` | `timestamptz` | No | `now()` | Maintained by `trg_counsellors_updated_at`. |

**Indexes:** `counsellors_email_lower_uidx` (unique), `counsellors_is_active_idx`, plus the inline unique constraint on `user_id`.

**Triggers:** `trg_counsellors_updated_at` (BEFORE UPDATE).

### 3.4 `availability_slots`

| Column | Type | Null | Default | Constraints / notes |
|---|---|---|---|---|
| `id` | `uuid` | No | `gen_random_uuid()` | **PK**. |
| `counsellor_id` | `uuid` | No | — | **FK** → `counsellors(id)` `ON DELETE CASCADE`. |
| `start_at` | `timestamptz` | No | — | UTC; must be in the future on insert/move. |
| `end_at` | `timestamptz` | No | — | CHECK `availability_slots_time_order`: `end_at > start_at`. |
| `created_at` | `timestamptz` | No | `now()` | UTC. |
| `updated_at` | `timestamptz` | No | `now()` | Maintained by `trg_availability_slots_updated_at`. |

**Constraints**

| Name | Type | Rule |
|---|---|---|
| `availability_slots_time_order` | CHECK | `end_at > start_at` |
| `availability_slots_exact_window_key` | UNIQUE | `(counsellor_id, start_at, end_at)` |
| `availability_slots_no_overlap` | EXCLUDE USING GIST | `(counsellor_id WITH =, tstzrange(start_at, end_at, '[)') WITH &&)`. Requires `btree_gist`. |

**Indexes:** `availability_slots_counsellor_start_idx`.

**Triggers**

| Trigger | Timing | Action |
|---|---|---|
| `trg_availability_slot_guard` | BEFORE INSERT or UPDATE of counsellor/times | Rejects past starts, invalid ranges, inactive counsellors, overlaps, and edits to slots already linked to any appointment. |
| `trg_availability_slot_delete` | BEFORE DELETE | Rejects deletion of a slot linked to any appointment. |
| `trg_availability_slots_updated_at` | BEFORE UPDATE | Sets `updated_at`. |

The `seed_demo.sql` file temporarily disables only `trg_availability_slot_guard` while inserting historical demo slots, then re-enables it.

### 3.5 `appointments`

| Column | Type | Null | Default | Constraints / notes |
|---|---|---|---|---|
| `id` | `uuid` | No | `gen_random_uuid()` | **PK**. |
| `student_id` | `uuid` | No | — | **FK** → `profiles(id)` `ON DELETE RESTRICT`. |
| `counsellor_id` | `uuid` | No | — | **FK** → `counsellors(id)` `ON DELETE RESTRICT`. Must match the slot's counsellor (`trg_appointment_counsellor_guard`). |
| `slot_id` | `uuid` | No | — | **FK** → `availability_slots(id)` `ON DELETE RESTRICT`. |
| `mode` | `text` | No | — | CHECK: `in_person`, `online`. |
| `concern_category` | `text` | No | — | CHECK: one of the eight constants; required. |
| `note` | `text` | Yes | — | Optional student note; CHECK maximum 1,000 trimmed characters. |
| `status` | `text` | No | `'pending'` | CHECK: `pending`, `confirmed`, `completed`, `no_show`, `cancelled`. |
| `follow_up_of` | `uuid` | Yes | — | **FK** → `appointments(id)` `ON DELETE SET NULL`. Set by `create_followup_appointment`; gives the student a "Follow-up" tag. |
| `created_at` | `timestamptz` | No | `now()` | UTC. |
| `updated_at` | `timestamptz` | No | `now()` | Maintained by `trg_appointments_updated_at`. |

**Constraints and indexes**

| Name | Type | Rule |
|---|---|---|
| `appointments_note_length` | CHECK | Note null or trimmed length ≤ 1,000 |
| `appointments_follow_up_not_self` | CHECK | `follow_up_of` is not the row's own id |
| `appointments_active_slot_uidx` | Partial UNIQUE | `slot_id` unique where status is `pending` or `confirmed`; cancelled/completed/no-show appointments do not hold a slot |
| `appointments_student_idx` | Index | `student_id` |
| `appointments_counsellor_idx` | Index | `counsellor_id` |
| `appointments_slot_idx` | Index | `slot_id` |
| `appointments_follow_up_of_idx` | Index | `follow_up_of` |

**Triggers**

| Trigger | Timing | Action |
|---|---|---|
| `trg_appointment_counsellor_guard` | BEFORE INSERT or UPDATE of slot/counsellor | Rejects a slot/counsellor mismatch |
| `trg_appointment_status_transition` | BEFORE UPDATE of status | Allows pending→confirmed/cancelled and confirmed→completed/no_show/cancelled only; terminal states cannot change |
| `trg_appointments_updated_at` | BEFORE UPDATE | Sets `updated_at` |

Rows are **never written directly through the API**. All writes go through the security-definer booking functions so student ownership, caps, overlaps and locks are checked atomically.

### 3.6 `session_records`

| Column | Type | Null | Default | Constraints / notes |
|---|---|---|---|---|
| `id` | `uuid` | No | `gen_random_uuid()` | **PK**. |
| `appointment_id` | `uuid` | No | — | **FK** → `appointments(id)` `ON DELETE RESTRICT`. UNIQUE: one record per appointment. |
| `counsellor_id` | `uuid` | No | — | **FK** → `counsellors(id)` `ON DELETE RESTRICT`. Filled by trigger from the completed appointment. |
| `student_id` | `uuid` | No | — | **FK** → `profiles(id)` `ON DELETE RESTRICT`. Filled by trigger. |
| `private_notes` | `text` | No | — | CHECK: trimmed length 20–5,000. Counsellor-only. Never returned by the student view and never exposed to admins. |
| `public_summary` | `text` | No | — | CHECK: trimmed length 1–2,000. Exposed to the student through the safe view. |
| `risk_level` | `text` | No | — | CHECK: `low`, `medium`, `high`. Required. |
| `follow_up_needed` | `boolean` | No | `false` | |
| `follow_up_due` | `timestamptz` | Yes | — | Must be future when a record is created or its follow-up fields are set. Required when `follow_up_needed = true`. |
| `is_archived` | `boolean` | No | `false` | Archive replaces hard delete. |
| `created_at` | `timestamptz` | No | `now()` | Forced to `now()` by the insert trigger; drives the 7-day edit window. |
| `updated_at` | `timestamptz` | No | `now()` | Maintained by `trg_session_records_updated_at`. |

**Named constraints**

| Name | Rule |
|---|---|
| `session_records_appointment_id_key` | Unique appointment (one record per appointment) |
| `session_records_private_notes_length` | `length(trim(private_notes)) BETWEEN 20 AND 5000` |
| `session_records_public_summary_length` | `length(trim(public_summary)) BETWEEN 1 AND 2000` |
| `session_records_follow_up_consistency` | Follow-up needed ⇒ due date present; not needed ⇒ due date null |
| `session_records_risk_level_check` | Risk in `low`, `medium`, `high` |

**Indexes:** unique `appointment_id`, `session_records_counsellor_idx`, `session_records_student_idx`, partial due-date index for the Triage board.

**Triggers**

| Trigger | Timing | Action |
|---|---|---|
| `trg_session_record_prepare` | BEFORE INSERT | Requires the appointment to be `completed`; fills counsellor/student IDs; rejects duplicates; forces `created_at`, `is_archived=false`; requires a future due date when follow-up is needed |
| `trg_session_record_update` | BEFORE UPDATE | Blocks identity changes; blocks content edits after 7 days; allows archive/unarchive at any time; validates an updated follow-up date |
| `trg_session_records_updated_at` | BEFORE UPDATE | Sets `updated_at` |

### 3.7 `resources`

| Column | Type | Null | Default | Constraints / notes |
|---|---|---|---|---|
| `id` | `uuid` | No | `gen_random_uuid()` | **PK**. |
| `title` | `text` | No | — | CHECK: trimmed length 3–120. Unique case-insensitive index `resources_title_lower_uidx`. |
| `description` | `text` | No | — | CHECK: trimmed length 10–2,000. |
| `category` | `text` | No | — | CHECK: one of the eight concern categories. |
| `media_type` | `text` | No | — | CHECK: `Article`, `Video`, `Audio`, `Hotline`, `PDF`. |
| `content_url` | `text` | No | — | CHECK: HTTP/HTTPS URL, maximum 2,048 characters. |
| `target_stress_brackets` | `text[]` | No | — | CHECK: 1–3 entries, subset of `low`, `moderate`, `high`. |
| `is_published` | `boolean` | No | `false` | Students only see `true` rows. |
| `created_at` | `timestamptz` | No | `now()` | UTC. |
| `updated_at` | `timestamptz` | No | `now()` | Maintained by `trg_resources_updated_at`. |

**Indexes:** unique lower(title), `resources_category_media_idx`, `resources_published_idx`.

**Triggers:** `trg_resources_updated_at` (BEFORE UPDATE).

The self-check recommender filters published resources whose `target_stress_brackets` contain the score bracket, and keeps `Hotline` items for the High bracket only. Recommendations are computed in the app and are not stored.

### 3.8 `enquiries`

| Column | Type | Null | Default | Constraints / notes |
|---|---|---|---|---|
| `id` | `uuid` | No | `gen_random_uuid()` | **PK**. |
| `reference_code` | `text` | No | — | UNIQUE. CHECK format `^ENQ-[A-Z0-9]{6}$` (e.g. `ENQ-7K3P9Q`). Generated by `submit_enquiry()`. |
| `student_id` | `uuid` | Yes | — | **FK** → `profiles(id)` `ON DELETE SET NULL`. `NULL` for anonymous enquiries. |
| `is_anonymous` | `boolean` | No | `false` | Must match the identity consistency check. |
| `category` | `text` | No | — | CHECK: one of the eight concern categories; required. |
| `message` | `text` | No | — | CHECK: trimmed length 10–1,000. |
| `status` | `text` | No | `'open'` | CHECK: `open`, `answered`, `closed`. |
| `reply` | `text` | Yes | — | Counsellor reply; CHECK trimmed length 1–2,000 when present. `answered` requires a non-empty reply (trigger). |
| `recommended_resource_id` | `uuid` | Yes | — | **FK** → `resources(id)` `ON DELETE SET NULL`. Optional attachment from Module 5. |
| `replied_at` | `timestamptz` | Yes | — | Set/cleared automatically by `protect_enquiry_reply`. |
| `created_at` | `timestamptz` | No | `now()` | UTC. |
| `updated_at` | `timestamptz` | No | `now()` | Maintained by `trg_enquiries_updated_at`. |

**Named constraints**

| Name | Rule |
|---|---|
| `enquiries_reference_code_unique` | Unique reference code |
| `enquiries_reference_code_format` | `^ENQ-[A-Z0-9]{6}$` |
| `enquiries_message_length` | Trimmed message 10–1,000 |
| `enquiries_reply_length` | Reply null or trimmed 1–2,000 |
| `enquiries_identity_consistency` | Anonymous ⇒ `student_id IS NULL`; non-anonymous ⇒ `student_id IS NOT NULL` |
| `enquiries_category_check` | One of the eight categories |
| `enquiries_status_check` | `open`, `answered`, `closed` |

**Indexes:** unique reference code, `enquiries_student_idx`, `enquiries_status_idx`.

**Triggers**

| Trigger | Timing | Action |
|---|---|---|
| `trg_enquiry_reply_guard` | BEFORE INSERT/UPDATE | Rejects changes to the original enquiry; trims reply; requires a reply before `answered`; manages `replied_at` |
| `trg_enquiries_updated_at` | BEFORE UPDATE | Sets `updated_at` |

Insert is not exposed as a direct table operation: `submit_enquiry()` creates the row and returns the code, because anonymous rows have no SELECT policy through which PostgREST could return the code.

### 3.9 `feedback`

| Column | Type | Null | Default | Constraints / notes |
|---|---|---|---|---|
| `id` | `uuid` | No | `gen_random_uuid()` | **PK**. |
| `appointment_id` | `uuid` | No | — | **FK** → `appointments(id)` `ON DELETE RESTRICT`. UNIQUE: one rating per completed appointment. |
| `student_id` | `uuid` | No | — | **FK** → `profiles(id)` `ON DELETE RESTRICT`. Filled by trigger from the appointment. |
| `counsellor_id` | `uuid` | No | — | **FK** → `counsellors(id)` `ON DELETE RESTRICT`. Filled by trigger from the appointment. |
| `rating` | `integer` | No | — | CHECK: 1–5. |
| `comment` | `text` | Yes | — | CHECK: null or trimmed length 1–500. |
| `created_at` | `timestamptz` | No | `now()` | UTC. |

**Indexes:** unique appointment, `feedback_counsellor_idx`, `feedback_student_idx`.

**Trigger**

| Trigger | Timing | Action |
|---|---|---|
| `trg_feedback_prepare` | BEFORE INSERT | Requires the appointment to be `completed`; fills student/counsellor; rejects duplicate feedback; trims/blank comment → null; sets `created_at` |

No UPDATE policy or trigger exists. The student removes an unwanted rating and may create a new one.

---

## 4. Views

Views run with **owner rights** (PostgreSQL's default), deliberately, because they must read rows the caller cannot read directly:

- The student summary view needs to hide the `session_records` base table entirely.
- The available-slots view needs to see **all** students' bookings to exclude booked slots.

Both views expose only safe columns and are granted only to `authenticated`; `PUBLIC` and `anon` have no access.

### 4.1 `available_slots_view`

Filters to active counsellors, future slots, and slots with no `pending`/`confirmed` appointment.

| Column | Type | Notes |
|---|---|---|
| `slot_id` | uuid | `availability_slots.id` |
| `counsellor_id` | uuid | `counsellors.id` |
| `counsellor_name` | text | Counsellor's directory name |
| `specialisations` | text[] | Used by Smart Counsellor Match |
| `languages` | text[] | Used by the counsellor filter UI |
| `bio` | text | Used on counsellor detail |
| `start_at` | timestamptz | UTC; app formats to KL |
| `end_at` | timestamptz | UTC; app formats to KL |

### 4.2 `student_session_summaries`

Exposes only **public** session information for the calling student. `auth.uid()` limits rows.

| Column | Type | Notes |
|---|---|---|
| `record_id` | uuid | Session record id |
| `appointment_id` | uuid | Link to the appointment |
| `session_start` | timestamptz | Slot start |
| `concern_category` | text | Reason for session |
| `counsellor_name` | text | Counsellor display name |
| `public_summary` | text | Public, student-readable summary |
| `summary_created_at` | timestamptz | When the record was written |

**Never included:** `private_notes`, `risk_level`, `follow_up_needed` or any other counsellor-only field. Archived records are excluded. Students have no SELECT policy on `session_records` at all.

---

## 5. Database functions (RPCs)

All RPC functions are `SECURITY DEFINER` with an explicit `search_path`. They revoke PUBLIC/anon execution and grant only `authenticated`. Friendly `RAISE EXCEPTION` messages are surfaced to the app.

| Function | Signature | Who may use it | Purpose |
|---|---|---|---|
| `app_role()` | `() → text` | RLS + authenticated | Returns the active caller's role without RLS recursion |
| `app_counsellor_id()` | `() → uuid` | RLS + authenticated | Returns the counsellor row linked to an active counsellor Auth user |
| `handle_new_user()` | trigger on `auth.users` | Auth trigger only | Creates profile; chooses role; links counsellor |
| `book_appointment` | `(p_slot_id uuid, p_mode text, p_concern_category text, p_note text default null) → uuid` | Active students | Atomic booking with active cap, overlap and slot checks |
| `reschedule_appointment` | `(p_appointment_id uuid, p_new_slot_id uuid) → void` | Owning active student | Moves booking to a free slot with the same counsellor, ≥12 hours before the old start |
| `cancel_appointment` | `(p_appointment_id uuid) → void` | Owning active student | Sets `cancelled`, releasing the slot |
| `counsellor_reschedule_appointment` | `(p_appointment_id uuid, p_new_slot_id uuid) → void` | Assigned active counsellor | Moves a confirmed session to another free slot of the same counsellor; no 12-hour student notice; checks future slot, student overlap and slot race |
| `update_appointment_status` | `(p_appointment_id uuid, p_new_status text) → void` | Assigned active counsellor | pending→confirmed/cancelled (decline); confirmed→completed/no_show/cancelled (emergency); blocks actions after the slot has started |
| `create_followup_appointment` | `(p_record_id uuid, p_slot_id uuid) → uuid` | Owning active counsellor | Creates a pending follow-up appointment with `follow_up_of` set |
| `deactivate_account` | `() → void` | Active students | Blocks if upcoming active appointments exist, then sets `profiles.is_active = false` |
| `delete_counsellor` | `(p_counsellor_id uuid) → text` | Admins | Blocks upcoming active appointments; deactivates if history exists, otherwise hard deletes; a linked Auth profile is disabled on hard delete, and the function returns `deleted` or `deactivated` |
| `submit_enquiry` | `(p_category text, p_message text, p_is_anonymous boolean default false) → text` | Active students | Validates and creates an anonymous or linked enquiry; returns the generated code |
| `get_enquiry_by_code` | `(p_code text) → table(...)` | Authenticated (code holder) | Returns safe enquiry fields for a reference code; no identity is exposed |
| `withdraw_enquiry_by_code` | `(p_code text) → void` | Authenticated (code holder) | Deletes an OPEN enquiry by code |
| `admin_dashboard_stats` | `() → jsonb` | Admin only | Returns aggregate counts and average rating; no notes/session content |

**Trigger functions** (not callable as normal RPCs): `validate_availability_slot`, `prevent_booked_slot_delete`, `validate_appointment_counsellor`, `enforce_appointment_status_transition`, `prepare_session_record`, `guard_session_record_update`, `prepare_feedback`, `protect_enquiry_reply`, `set_updated_at`.

**Additional support functions:** `delete_counsellor()` and `submit_enquiry()` are implementation helpers required to satisfy the Module 3 delete rules and to return an anonymous reference code securely. They are not extra user-facing features.

### Function errors shown by the app (examples)

- `Only students can book counselling appointments.`
- `You can have at most 2 active upcoming appointments. Cancel one first.`
- `Appointments can only be rescheduled at least 12 hours in advance.`
- `This slot is linked to an appointment and cannot be deleted.`
- `Session records can only be added for completed appointments.`
- `Follow-up due date must be in the future.`
- `This counsellor has upcoming appointments. Resolve them before removing the counsellor.`
- `Please enter a valid reference code (for example ENQ-7K3P9Q).`

---

## 6. Row Level Security (RLS) matrix

RLS is enabled on every table. `anon` has no access to any application table or view. The matrix below describes the effective authenticated-user behaviour after both RLS and column-level grants.

| Table | Student | Counsellor | Admin |
|---|---|---|---|
| `profiles` | Read own; update own safe columns | Read own + all student profiles; update own safe columns | Read own + all student profiles; update own safe columns |
| `admin_whitelist` | — | — | Read |
| `counsellors` | Read all directory rows | Read all | Read all; insert; update |
| `availability_slots` | Read all | Read all; insert/update/delete own | Read all; insert/update/delete any |
| `appointments` | Read own | Read own assigned | Read all (read-only) |
| `session_records` | **No base-table access**; safe summary view only | Read/insert/update own | **No access**; aggregate counts only via RPC |
| `resources` | Read published only | Read all; insert/update/delete | Read all; insert/update/delete |
| `enquiries` | Read own non-anonymous; delete own open; create via RPC | Read all; update reply/status/resource | **No access** |
| `feedback` | Read/insert/delete own | Read own sessions' ratings | **No base access**; aggregate via stats RPC |
| `student_session_summaries` | Read own rows (public columns) | None (no matching student id) | None |
| `available_slots_view` | Read | Read | Read |

**Important:** A `session_records` SELECT by a student or admin returns zero rows. Admins never see `private_notes`; students never see them or `risk_level`. Direct writes to `appointments` are impossible for every role, including admin.

### Column-level protections

- `profiles`: authenticated users can update only `full_name`, `programme`, `phone`, `preferred_session_mode`. Role, student ID, email and active flag are not writable.
- `enquiries`: counsellors can update only `status`, `reply`, `recommended_resource_id`.
- `session_records`: update privilege excludes appointment/student/counsellor identity and `created_at`.
- `counsellors`: `user_id` is not writable through the API; only the Auth trigger can link it.
- No anonymous (`anon`) grants exist.

---

## 7. Security-definer and RLS proof notes

- `app_role()` exists instead of `current_role` because `current_role` is a reserved PostgreSQL keyword.
- Every policy in `schema.sql` has a plain-English comment above it.
- All public functions use `SET search_path`; the RPCs and auth trigger are `SECURITY DEFINER` with authorization checks.
- Students and admins have no base-table policy on `session_records`.
- Anonymous enquiries have no student identity stored and no table SELECT policy. Access is only through the two code-based functions (plus create via `submit_enquiry`).
- RLS proof queries are included in [`SETUP_GUIDE.md`](SETUP_GUIDE.md#11-prove-rls-and-privacy).

---

## 8. Key rules enforced in the database

| Rule | Where enforced |
|---|---|
| Max 2 active upcoming bookings per student | `book_appointment`, `create_followup_appointment` |
| No overlapping appointments for one student | `book_appointment`, `reschedule_appointment`, `counsellor_reschedule_appointment`, `create_followup_appointment` |
| No double booking | Partial unique index + slot row lock in booking functions |
| No past slots | `trg_availability_slot_guard` + functions |
| No overlapping counsellor slots | `availability_slots_no_overlap` + guard trigger |
| 12-hour student reschedule notice | `reschedule_appointment` |
| Counsellor emergency changes before start | `counsellor_reschedule_appointment`, `update_appointment_status` |
| Valid status transitions | `trg_appointment_status_transition` + status RPC |
| Completed-only session records | `prepare_session_record()` trigger |
| One record per appointment | Unique constraint + trigger |
| 7-day record editing | `guard_session_record_update()` |
| Archive instead of delete | RLS has no DELETE policy; app toggles `is_archived` |
| One rating per completed appointment | Unique constraint + RLS + `prepare_feedback()` |
| Anonymous enquiry identity is null | `enquiries_identity_consistency` + `submit_enquiry()` |
| Unique reference codes | Unique constraint + generation loop |
| Unique case-insensitive resource titles | `resources_title_lower_uidx` |
| Malaysian phone format | `profiles_phone_format` |
| Password strength | Supabase Auth password policy (configured in dashboard) + app validator |
| Role assignment | `handle_new_user()` checks whitelist/counsellor table |
| Admin cannot read notes | No RLS policy + aggregate-only RPC |

---

## 9. Storage and retention notes

- No media files are stored in Supabase Storage; resources use external `http`/`https` content URLs.
- Deactivating an account keeps appointments, records and feedback for continuity; it does not hard-delete the Auth user.
- Archiving a session record keeps the row and hides it from the student view and Triage board filters (the board queries non-archived).
- Cancelling keeps the appointment row for history and releases the slot.
- Reference-code lookup returns no identity or private notes.
