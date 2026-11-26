# Data Dictionary — OOP Schema

**Project:** University Counselling Appointment & Support System (AMCS2194)
**Schema:** `public` + Supabase-managed `auth.users`
**Design:** Class-Table Inheritance, reference vocabularies, association tables
**Fresh install:** [`supabase/schema.sql`](../supabase/schema.sql)
**Existing project:** [`supabase/migrations/20261120_oop_refactor.sql`](../supabase/migrations/20261120_oop_refactor.sql)

The old flat `profiles` design has been replaced by a real supertype/subtype
model. The old tables are preserved in schema `legacy` by the migration and are
not read by the new app.

---

## 1. Conventions

- `*_code` values are stable lowercase codes. Display labels live in the
  reference table, never duplicated on transactional rows.
- Timestamps are `timestamptz` in UTC; the Android app formats Asia/Kuala_Lumpur.
- Every subtype shares its primary key with its supertype. Composite foreign
  keys include `user_type`/`resource_type`/`enquiry_type` so a row cannot point
  at the wrong subtype.
- Exactly-one subtype is enforced by deferred constraint triggers for
  resources and enquiries.
- All application tables use RLS. Security-definer functions are the only write
  paths for appointments and resource/counsellor aggregates.
- **3NF note:** every table below lists why its non-key attributes depend only
  on its key, with no repeating groups or transitive dependency.

---

## 2. Reference / vocabulary tables

### 2.1 `concern_categories`

| Column | Type | Null | Notes |
|---|---|---|---|
| `code` | text | No | PK, lowercase code |
| `label` | text | No | Unique display label |
| `sort_order` | smallint | No | Unique UI order |

**3NF:** `code` is the only key; `label` and `sort_order` are independent
attributes of the category. No kind/type column is needed.

### 2.2 `session_modes`

| Column | Type | Null | Notes |
|---|---|---|---|
| `code` | text | No | PK (`in_person`, `online`) |
| `label` | text | No | Unique label |
| `sort_order` | smallint | No | UI order |

**3NF:** one key, two non-key attributes, no transitive dependency.

### 2.3 `risk_levels`

| Column | Type | Null | Notes |
|---|---|---|---|
| `code` | text | No | PK (`low`, `medium`, `high`) |
| `label` | text | No | Unique label |
| `rank` | smallint | No | Unique ordering/severity |

**3NF:** rank is an intrinsic attribute of the risk level, not derived from a
transaction.

### 2.4 `stress_brackets`

| Column | Type | Null | Notes |
|---|---|---|---|
| `code` | text | No | PK (`low`, `moderate`, `high`) |
| `label` | text | No | Unique label |
| `min_score` | smallint | No | Inclusive minimum |
| `max_score` | smallint | No | Inclusive maximum |
| `sort_order` | smallint | No | UI order |

**Constraints:** `max_score >= min_score`; unique score window; unique order.
**3NF:** each attribute describes the bracket itself; no score is stored on a
student or resource row.

### 2.5 `appointment_statuses`

| Column | Type | Null | Notes |
|---|---|---|---|
| `code` | text | No | PK (`pending`, `confirmed`, `completed`, `no_show`, `cancelled`) |
| `label` | text | No | Unique label |
| `is_terminal` | boolean | No | Final-state marker |
| `sort_order` | smallint | No | UI order |

**3NF:** status label and terminal flag belong to the status reference row, not
to each appointment.

### 2.6 `appointment_status_transitions`

| Column | Type | Null | Notes |
|---|---|---|---|
| `from_status_code` | text | No | PK part; FK status |
| `to_status_code` | text | No | PK part; FK status |

**Constraints:** PK (`from`, `to`); `from <> to`.
**3NF:** the table stores only the permitted pair; no duplicated status labels.
This is the database State-pattern transition table.

### 2.7 `languages`

| Column | Type | Null | Notes |
|---|---|---|---|
| `language_id` | smallint identity | No | PK |
| `name` | text | No | Unique case-insensitively via index |

**3NF:** language name depends on the language key only. The association table
stores the many-to-many link.

---

## 3. User generalisation / specialisation

### 3.1 `users` (supertype)

| Column | Type | Null | Notes |
|---|---|---|---|
| `id` | uuid | No | PK, default `gen_random_uuid()` |
| `auth_user_id` | uuid | Yes | Unique; FK `auth.users(id)` `ON DELETE SET NULL` |
| `email` | text | No | Unique lower-case index; format check |
| `full_name` | text | No | Trimmed length 2–100 |
| `phone` | text | Yes | Malaysian format check |
| `is_active` | boolean | No | Default true |
| `user_type` | text | No | `student`, `counsellor`, `administrator` |
| `created_at` | timestamptz | No | Default now() |
| `updated_at` | timestamptz | No | Trigger-maintained |

**Keys:** PK `id`; unique `auth_user_id`; unique lower(`email`);
unique(`id`,`user_type`) for subtype FKs.
**3NF:** common identity attributes are stored once in the supertype. Role-
specific attributes live in subtypes; there is no nullable "student fields on
every user" problem.

### 3.2 `students` (subtype)

| Column | Type | Null | Notes |
|---|---|---|---|
| `user_id` | uuid | No | PK, FK `users(id)` cascade |
| `user_type` | text | No | Default `student`, check, part of composite FK |
| `student_id` | text | No | Student number; unique lower-case index |
| `programme` | text | No | 2–120 chars |
| `preferred_session_mode` | text | No | FK `session_modes(code)` |
| `created_at` / `updated_at` | timestamptz | No | Auditing |

**3NF:** each academic attribute depends on the student key; session mode is a
reference, not a repeated text value.

### 3.3 `counsellors` (subtype)

| Column | Type | Null | Notes |
|---|---|---|---|
| `user_id` | uuid | No | PK, FK `users(id)` cascade |
| `user_type` | text | No | Default `counsellor`, check |
| `bio` | text | No | Default empty, max 2,000 |
| `created_at` / `updated_at` | timestamptz | No | Auditing |

**3NF:** only counsellor-specific data is here. Names/emails/active state stay
in `users`; specialisations/languages are associations.

### 3.4 `administrators` (subtype)

| Column | Type | Null | Notes |
|---|---|---|---|
| `user_id` | uuid | No | PK, FK `users(id)` cascade |
| `user_type` | text | No | Default `administrator`, check |
| `created_at` | timestamptz | No | Auditing |

**3NF:** no role string is duplicated; the existence of this row is the admin
specialisation.

---

## 4. Association tables

### 4.1 `counsellor_specialisations`

| Column | Type | Null | Notes |
|---|---|---|---|
| `counsellor_id` | uuid | No | PK/FK counsellors |
| `category_code` | text | No | PK/FK concern_categories |

**3NF:** all-key relation; no non-key attribute, so no transitive dependency.

### 4.2 `counsellor_languages`

| Column | Type | Null | Notes |
|---|---|---|---|
| `counsellor_id` | uuid | No | PK/FK counsellors |
| `language_id` | smallint | No | PK/FK languages |

**3NF:** all-key relation; replaces the old `languages text[]`.

### 4.3 `resource_target_brackets`

| Column | Type | Null | Notes |
|---|---|---|---|
| `resource_id` | uuid | No | PK/FK resources |
| `bracket_code` | text | No | PK/FK stress_brackets |

**3NF:** all-key relation; replaces `target_stress_brackets text[]`.

---

## 5. Availability and appointment tables

### 5.1 `availability_slots`

| Column | Type | Null | Notes |
|---|---|---|---|
| `id` | uuid | No | PK |
| `counsellor_id` | uuid | No | FK counsellors, cascade |
| `start_at` | timestamptz | No | |
| `end_at` | timestamptz | No | Must be after start |
| `created_at` / `updated_at` | timestamptz | No | Auditing |

**Constraints:** unique exact window per counsellor; GiST exclusion prevents
overlap; guard trigger rejects past/inactive/edited-booked slots.
**3NF:** start/end are independent facts about the slot; counsellor relationship
is a foreign key, not duplicated name/email.

### 5.2 `appointments`

| Column | Type | Null | Notes |
|---|---|---|---|
| `id` | uuid | No | PK |
| `student_id` | uuid | No | FK students, restrict |
| `counsellor_id` | uuid | No | FK counsellors, restrict |
| `slot_id` | uuid | No | FK availability_slots, restrict |
| `mode_code` | text | No | FK session_modes |
| `concern_code` | text | No | FK concern_categories |
| `note` | text | Yes | Max 1,000 |
| `status_code` | text | No | FK appointment_statuses; default pending |
| `follow_up_of` | uuid | Yes | Self FK, set null |
| `last_event_code` | text | No | Last workflow event used for the in-app card notice; default booked |
| `created_at` / `updated_at` | timestamptz | No | Auditing |

**Constraints:** partial unique `(slot_id)` where status is pending/confirmed;
self-follow-up check; status-transition trigger; allowed `last_event_code`
values (booked, rescheduled_by_student, rescheduled_by_counsellor, confirmed,
declined, cancelled_by_student, cancelled_by_counsellor, completed, no_show,
followup_requested, followup_confirmed_by_student).
**3NF:** status/mode/concern labels are not copied into this row; the references
resolve current display values. `follow_up_of` is a relationship, not a text
flag.

### 5.3 `session_records`

| Column | Type | Null | Notes |
|---|---|---|---|
| `id` | uuid | No | PK |
| `appointment_id` | uuid | No | Unique FK appointments, restrict |
| `counsellor_id` | uuid | No | FK counsellors, restrict |
| `student_id` | uuid | No | FK students, restrict |
| `private_notes` | text | No | 20–5,000 |
| `public_summary` | text | No | 1–2,000 |
| `risk_code` | text | No | FK risk_levels |
| `follow_up_needed` | boolean | No | Default false |
| `follow_up_due` | timestamptz | Yes | Required iff follow-up needed |
| `is_archived` | boolean | No | Archive first; then delete allowed |
| `created_at` / `updated_at` | timestamptz | No | Auditing |

**Constraints:** one record per appointment; follow-up consistency; 7-day edit
trigger; archive-only delete trigger.
**3NF:** risk is a reference, not a copied label; follow-up due is an attribute
of this record's follow-up, with a consistency check.

### 5.4 `feedback`

| Column | Type | Null | Notes |
|---|---|---|---|
| `id` | uuid | No | PK |
| `appointment_id` | uuid | No | Unique FK appointments, restrict |
| `student_id` | uuid | No | FK students, restrict |
| `counsellor_id` | uuid | No | FK counsellors, restrict |
| `rating` | integer | No | 1–5 |
| `comment` | text | Yes | 1–500 if present |
| `created_at` | timestamptz | No | Auditing |

**3NF:** each attribute depends on the feedback row; student/counsellor are
relationships, not duplicated names. Appointment uniqueness enforces one rating.

---

## 6. Resource generalisation / specialisation

### 6.1 `resources` (supertype)

| Column | Type | Null | Notes |
|---|---|---|---|
| `id` | uuid | No | PK |
| `title` | text | No | 3–120; unique lower-case index |
| `description` | text | No | 10–2,000 |
| `category_code` | text | No | FK concern_categories |
| `resource_type` | text | No | article/video/audio/pdf/hotline |
| `is_published` | boolean | No | Default false |
| `created_at` / `updated_at` | timestamptz | No | Auditing |

**Constraints:** `(id, resource_type)` unique for subtype FKs; deferred trigger
requires exactly one matching subtype.
**3NF:** common resource fields are stored once; subtype-only fields are not
nullable columns on the supertype.

### 6.2 `article_resources`, `video_resources`, `audio_resources`, `pdf_resources`

Each table has:

| Column | Type | Null | Notes |
|---|---|---|---|
| `resource_id` | uuid | No | PK/FK resources, cascade |
| `resource_type` | text | No | Fixed check for the subtype |
| `content_url` | text | No | HTTP(S) URL check |

**3NF:** each table has only the attributes of its subtype; `content_url` is not
repeated in the supertype.

### 6.3 `hotline_resources`

| Column | Type | Null | Notes |
|---|---|---|---|
| `resource_id` | uuid | No | PK/FK resources, cascade |
| `resource_type` | text | No | Fixed `hotline` |
| `phone_number` | text | Yes | Loose phone format |
| `operating_hours` | text | Yes | Max 120 |
| `website_url` | text | Yes | HTTP(S); preserves old hotline URL |

**3NF:** hotline-specific contact attributes exist only here; no meaningless
`content_url` requirement on a phone-only hotline.

---

## 7. Enquiry generalisation / specialisation

### 7.1 `enquiries` (supertype)

| Column | Type | Null | Notes |
|---|---|---|---|
| `id` | uuid | No | PK |
| `reference_code` | text | No | Unique, `ENQ-XXXXXX` |
| `category_code` | text | No | FK concern_categories |
| `message` | text | No | 10–1,000 |
| `status` | text | No | open/answered/closed |
| `reply` | text | Yes | 1–2,000; required when answered |
| `recommended_resource_id` | uuid | Yes | FK resources, set null |
| `replied_at` | timestamptz | Yes | Trigger-maintained |
| `enquiry_type` | text | No | identified/anonymous |
| `created_at` / `updated_at` | timestamptz | No | Auditing |

**Constraints:** unique code; `(id, enquiry_type)` unique for subtype FK; reply
guard trigger; exactly-one subtype trigger.
**3NF:** common enquiry fields are not duplicated in subtypes. The reference
code is common because both linked and anonymous enquiries expose a code in
this application; this avoids a nullable identity-flag design.
**Note:** `status` is a small vocabulary but is kept as a check because the
brief's reference-table list does not include enquiry statuses; Kotlin models it
as a type-safe `EnquiryStatus`.

### 7.2 `identified_enquiries` (subtype)

| Column | Type | Null | Notes |
|---|---|---|---|
| `enquiry_id` | uuid | No | PK/FK enquiries, cascade |
| `enquiry_type` | text | No | Fixed `identified` |
| `student_id` | uuid | No | FK students, restrict |

**3NF:** identity is in a relationship table, not a nullable `student_id` on
the supertype.

### 7.3 `anonymous_enquiries` (subtype)

| Column | Type | Null | Notes |
|---|---|---|---|
| `enquiry_id` | uuid | No | PK/FK enquiries, cascade |
| `enquiry_type` | text | No | Fixed `anonymous` |

**3NF:** no student identity columns; the absence of a row in
`identified_enquiries` is not used as meaning. The reference code is on the
supertype (see 7.1).

---

## 8. Database functions (methods)

All functions use `SECURITY DEFINER` and an explicit `search_path`. Execution is
revoked from `PUBLIC`/`anon` and granted to `authenticated` where appropriate.

| Function | Who may use it | Purpose |
|---|---|---|
| `app_user_id()` | RLS/authenticated | Active users.id for the Auth uid |
| `app_identity_id()` | RLS/authenticated | users.id even when inactive |
| `app_role()` | RLS/authenticated | Active user_type |
| `app_counsellor_id()` | RLS/authenticated | Active counsellor users.id |
| `resolve_*()` | internal | Label/code → reference code |
| `book_appointment` | Active students | Atomic **confirmed** booking with caps/overlap |
| `reschedule_appointment` | Owning active student | Same-counsellor move, ≥12h notice |
| `cancel_appointment` | Owning active student | Cancel before start |
| `update_appointment_status` | Assigned active counsellor | Confirm/decline/complete/no-show/cancel |
| `counsellor_reschedule_appointment` | Assigned active counsellor | Emergency confirmed move |
| `create_followup_appointment` | Owning active counsellor | Pending follow-up for a record |
| `confirm_followup_appointment` | Owning active student | Student confirms a proposed follow-up; sets confirmed + notice event |
| `deactivate_account` | Active student | Block if upcoming; set inactive |
| `update_student_profile` | Active student | Atomically update `users.full_name/phone` and `students.programme/preferred_session_mode` |
| `delete_counsellor` | Administrator | Block/deactivate/hard-delete rule |
| `create_counsellor` | Administrator | Insert users + subtype + associations atomically |
| `update_counsellor` | Administrator | Update aggregate + associations |
| `create_resource` | Counsellor/admin | Insert supertype + subtype + brackets atomically |
| `update_resource` | Counsellor/admin | Update and replace subtype/brackets atomically |
| `delete_resource` | Counsellor/admin | Delete resource and subtype cascade |
| `submit_enquiry` | Active student | Create supertype + identified/anonymous subtype |
| `get_enquiry_by_code` | Authenticated | Safe code lookup, no identity |
| `withdraw_enquiry_by_code` | Authenticated | Delete only an open code |
| `admin_dashboard_stats` | Administrator | Aggregate counts only |
| `handle_new_user` | Auth trigger | Link pre-provisioned staff or create student |

**Trigger functions:** `set_updated_at`, `validate_availability_slot`,
`prevent_booked_slot_delete`, `validate_appointment_counsellor`,
`enforce_appointment_status_transition`, `prepare_session_record`,
`guard_session_record_update`, `prevent_unarchived_session_record_delete`,
`prepare_feedback`, `protect_enquiry_reply`, `validate_resource_subtype`,
`validate_enquiry_subtype`.

---

## 9. Read views

| View | Purpose | Authorization |
|---|---|---|
| `v_students` | Student supertype+subtype | Staff or self |
| `v_counsellors` | Counsellor + aggregated associations | Any active signed-in user |
| `v_administrators` | Admin rows | Admin only |
| `v_availability_slots` | Future booking catalogue including booked slots, with `is_booked`; free-only consumers filter it | Any active signed-in user |
| `v_appointments` | Appointment + names/slot/status/category/last event | Student own, counsellor own, admin all |
| `v_session_records` | Records incl. private notes | Owning counsellor only |
| `v_feedback` | Rating + session context | Student own or counsellor own |
| `v_resources` | Resource + subtype fields + brackets | Students published; staff all |
| `v_enquiries` | Enquiry + identified identity if any | Student own identified; counsellor all |
| `student_session_summaries` | Public summary only | Student own completed, non-archived |

The views are owner-rights views with explicit authorization predicates, as in
the old safe views. The base tables still have RLS; the views never expose
`private_notes` or `risk_level` to students/admins.

---

## 10. RLS and privileges summary

| Table | Student | Counsellor | Administrator |
|---|---|---|---|
| `users` | own row; update name/phone | own + students (read) | own + students (read) |
| `students` | own read/update programme/mode | all read | all read |
| `counsellors` | read | read | read |
| `administrators` | own | own | own |
| `availability_slots` | read | read; own write | read/write all |
| `appointments` | own read | own assigned read | all read |
| `session_records` | no base access; safe view | own read/insert/update/delete archived | no access |
| `feedback` | own read/insert/delete | own sessions read | no base access; aggregate RPC |
| `resources` | published read | all read; writes via RPC | all read; writes via RPC |
| resource subtypes | published read | all read | all read |
| `enquiries` | own identified read/delete open | all read/reply | no access |
| `identified_enquiries` | own read | all read | no access |
| `anonymous_enquiries` | no select | no select | no select; code RPC only |
| reference tables | read | read | read |

No `anon` grants exist. Direct `INSERT/UPDATE/DELETE` is not granted for
appointments, resources, user/counsellor aggregates; those paths are database
functions.

---

## 11. 3NF acceptance summary

| Table group | 3NF check |
|---|---|
| Reference tables | Single-column code keys; labels/orders/ranks depend on code only |
| `users` | Common identity once; role-specific fields not nullable |
| `students`, `counsellors`, `administrators` | Only subtype-specific attributes; PK is supertype FK |
| Association tables | All-key relations; no repeated arrays |
| `availability_slots`, `appointments` | No names/labels copied; FKs resolve reference rows |
| `session_records`, `feedback` | Risk/rating facts depend on the record; ownership is relationships |
| `resources` + 5 subtypes | Common fields once; subtype fields only in subtype |
| `enquiries` + 2 subtypes | Common message/code/status once; identity only in identified subtype |
| `resource_target_brackets` | All-key relation; no array |
| `appointment_status_transitions` | Pair only; no duplicated status labels |

No repeating groups, no multi-valued columns, and no non-key attribute
determines another non-key attribute.
