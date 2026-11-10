# Module Notes — University Counselling Appointment & Support System

Technical notes for the six-module mini project. All modules are implemented in one Android app backed by one Supabase project.

| Module | Owner | Main requirement | Unique feature |
|---|---|---|---|
| 1. Student Account & Profile | Beh | Registration, profile CRUD, password, deactivation, staff student directory | — |
| 2. Appointment Booking | Beh | Booking, retrieve, reschedule, cancel, counsellor status, admin read-only | Smart Counsellor Match |
| 3. Counsellor & Availability Management | Jeremy | Counsellor directory CRUD, availability CRUD, removal rules | — |
| 4. Session Records & Follow-Up | Jeremy | Private records, public summaries, archive, follow-up | Urgency Triage & Follow-up Board |
| 5. Self-Help Resource Library | Michael | Resource CRUD, publish, browse, search, filter | Wellbeing Self-Check Recommender |
| 6. Enquiry & Session Feedback | Michael | Anonymous/linked enquiries, replies, feedback | — |

---

## Shared architecture

- **UI:** Jetpack Compose + Material 3, role-aware bottom navigation, reusable components in `ui/components`.
- **State:** one ViewModel per screen/flow, `StateFlow<UiState>`, one-shot `Channel<UiMessage>` for Snackbars.
- **Data:** repositories return `Result<T>` and map exceptions through `core/ErrorMapper`. No raw server errors are displayed.
- **DI:** `CounsellingApp` creates one `AppContainer`; ViewModels use explicit factories. No Hilt/Koin.
- **Time:** Supabase stores `timestamptz`; all display formatting is Asia/Kuala_Lumpur through `DateTimeUtils`.
- **Access:** Supabase Auth session + `profiles.role`. The app never asks the user to choose a role.

Shared Supabase configuration is in tracked `supabase.properties` (publishable URL/key). `local.properties` is a machine-local override containing `sdk.dir`. See `SETUP_GUIDE.md` §6.1.

---

## Module 1 — Student Account & Profile (Beh)

**Screens and ViewModels**

- `feature/auth`: Login, student registration, splash, configuration-error screen.
- `feature/student/ui/ProfileScreen.kt` + `ProfileViewModel`: own profile read/update, password change, deactivation.
- `StudentListScreen` / `StudentDetailScreen`: admin and counsellor student directory.
- `feature/home`: role dashboards.

**Backend objects**

- `profiles`, `app_role()`, `deactivate_account()`.
- Auth trigger creates the profile and decides role from `admin_whitelist` / `counsellors`.
- Column-level update grants permit only safe profile fields.

**Behaviour and validation**

- Registration metadata: full name, student ID, programme, Malaysian phone, preferred mode.
- Client validation mirrors database checks (name/ID/programme length, phone regex, password rules).
- Profile update: full name, programme, phone and preferred session mode; email/student ID/role/is_active are immutable to the user.
- Password change uses Supabase Auth `updateUser`.
- Deactivation:
  - Client pre-checks active upcoming appointments and shows a clear message.
  - `deactivate_account()` also blocks if upcoming appointments exist.
  - The app refreshes the profile, detects `is_active = false`, signs out and shows the login notice.
- Staff student directory:
  - Search by name or student ID.
  - RLS lets only counsellors/admins read student profiles.
  - Detail exposes permitted contact/study fields only — no private records.

---

## Module 2 — Appointment Booking (Beh)

**Screens and ViewModels**

- `StudentAppointmentsScreen`: Upcoming / Past / Cancelled tabs, status chips, pull-to-refresh, cancel/reschedule actions.
- `BookAppointmentScreen` + `BookAppointmentViewModel`: concern selection → Smart Match → slot selection → mode/note → RPC.
- `RescheduleScreen` + `RescheduleViewModel`: same-counsellor free slots; students get the 12-hour client check, counsellors can reschedule a confirmed session without that notice rule for emergencies.
- `CounsellorScheduleScreen`: own appointments, Upcoming/This week/All + status filters; confirm/decline pending, complete/no-show, emergency cancel and reschedule confirmed.
- `AdminAppointmentsScreen`: all appointments, read-only, no write controls.
- `AppointmentCard`: shared role-aware summary card and Follow-up badge.
- Home shows the student's real next appointment.

**Backend objects**

- Tables: `appointments`, `availability_slots`, `counsellors`, `profiles`.
- View: `available_slots_view` (hides booked slots, exposes only public counsellor/slot data).
- RPCs: `book_appointment`, `reschedule_appointment`, `counsellor_reschedule_appointment`, `cancel_appointment`, `update_appointment_status`.
- Migration for existing projects: `supabase/migrations/20261104_counsellor_session_actions.sql`.

**Business rules**

- Maximum two active upcoming appointments.
- No overlapping active student appointments.
- One active appointment per slot (partial unique index).
- Slots must be in the future and belong to an active counsellor.
- Student reschedule is allowed only at least 12 hours before the current session and only with the same counsellor.
- Counsellor emergency reschedule is allowed for a confirmed session before it starts, to another free slot with the same counsellor; the student's no-overlap rule still applies and the status remains confirmed.
- Counsellor status changes:
  - pending → confirmed or cancelled (decline)
  - confirmed → completed, no_show or cancelled (emergency)
  - cancellation/reschedule is blocked once the slot has started.
- Admins can read every appointment but have no write policy or UI actions.
- There is no push-notification channel: a student sees a counsellor decline/cancel/reschedule when the list is refreshed or reopened. Both decline and emergency cancellation use the existing `cancelled` status and do not store a reason.

**Unique Feature 1 — Smart Counsellor Match**

- `appointment/model/SmartMatch.kt` is a pure, unit-tested ranker.
- Filters counsellors whose `specialisations` contain the selected concern.
- Groups free slots by counsellor and ranks by earliest `start_at`.
- Shows Best match, next free time, specialisations, languages and bio.
- Tapping a match opens that counsellor's slot picker.

---

## Module 3 — Counsellor & Availability Management (Jeremy)

**Screens and ViewModels**

- `CounsellorListScreen`: admin search by name/email/specialisation, “available this week”, add.
- `CounsellorFormScreen`: add/edit directory details with specialisations, languages and bio.
- `CounsellorDetailScreen`: linked Auth status, details, upcoming free slots, edit/manage availability/remove.
- `CounsellorAvailabilityScreen` + `AvailabilityViewModel`: shared by counsellors (own slots) and admins (any counsellor).
- Counsellor Schedule links to **Manage my availability**.

**Backend objects**

- Tables: `counsellors`, `availability_slots`, `appointments` (usage check).
- RPC: `delete_counsellor`.
- Triggers: `trg_availability_slot_guard`, delete guard, updated-at trigger.
- Exclusion constraint: `availability_slots_no_overlap`.

**Behaviour and validation**

- Counsellor creation leaves `user_id` null; the Auth trigger links the row when that email signs up. UI shows unlinked/linked status.
- Duplicate counsellor email is rejected by a unique index and friendly error.
- Availability:
  - Add one slot or repeat weekly for 1–8 weeks.
  - Client validates date, end-after-start, future time and overlap with existing slots.
  - Server re-validates and blocks overlaps, past dates, inactive counsellors and changes to booked slots.
- Booked/past slots cannot be edited or deleted; UI explains and server triggers enforce it.
- Counsellor removal:
  - Upcoming appointments → blocked.
  - Historical appointments → deactivate directory entry.
  - No history → hard delete; linked Auth profile is deactivated.

---

## Module 4 — Session Records & Follow-Up (Jeremy)

**Screens and ViewModels**

- `SessionRecordsScreen`: Records tab + Triage tab, completed-session intake, archive toggle, record viewer.
- `SessionRecordFormScreen`: create/edit private notes/public summary/risk/follow-up.
- `FollowUpScreen`: choose one of the counsellor's free slots and create a pending follow-up.
- `StudentSummariesScreen`: student-safe public summaries from the safe view.
- Counsellor feedback is reachable from the inbox; admin sees aggregate counts only.

**Backend objects**

- Table: `session_records`.
- View: `student_session_summaries`.
- RPC: `create_followup_appointment`.
- Triggers: `prepare_session_record`, `guard_session_record_update`.
- RLS: owning counsellor only; students cannot select the base table.

**Behaviour and validation**

- Only completed appointments can receive a record.
- One record per appointment (unique constraint).
- Private notes: 20–5,000 characters; public summary: 1–2,000.
- Risk level required.
- Follow-up-needed requires a future due date.
- Content edits are allowed for seven days; after that records are read-only for content but can be archived/restored.
- Archive is a soft delete; students stop seeing archived summaries.
- The owning counsellor may permanently delete a record only after it is archived. The UI requires a second confirmation, while RLS plus a BEFORE DELETE trigger enforce the archived state in the database.
- Student summaries contain no private notes, risk level or ownership internals.

**Unique Feature 2 — Urgency Triage & Follow-up Board**

- `records/model/SessionRecordModels.kt` contains pure grouping logic.
- Includes non-archived records that are high risk or follow-up-needed.
- Groups sorted due dates into Overdue / Due soon / Later.
- High-risk records without a due date appear in Due soon for review.
- Follow-up action uses the counsellor's own free slots; the created appointment is pending and carries `follow_up_of`, producing the student's Follow-up tag.
- Student max-active, overlap, race and slot rules still apply.

---

## Module 5 — Self-Help Resource Library (Michael)

**Screens and ViewModels**

- `ResourcesScreen`: library, search, category/media filters, publish/draft visibility, open link.
- `ResourceFormScreen`: add/edit/publish resource.
- `SelfCheckScreen` + `SelfCheckViewModel`: questionnaire, result, recommendations, booking hand-off.
- Home urgent card opens the Hotline-only resource view.

**Backend objects**

- Table: `resources`.
- RLS: students see only `is_published = true`; counsellors/admins see all and can insert/update/delete.

**Behaviour and validation**

- Title 3–120; description 10–2,000; category and media type from fixed constants.
- URL must be `http://` or `https://` and ≤2,048 characters.
- At least one and at most three target wellbeing brackets.
- Case-insensitive duplicate titles rejected by unique index.
- Publish/unpublish with success feedback; delete requires confirmation.
- Students never see draft rows (RLS + defensive client filter).
- All resource lists have loading, error, empty and pull-to-refresh states.

**Unique Feature 3 — Wellbeing Self-Check Recommender**

- Ten questions with answers 0–4 (total 0–40).
- Brackets exactly: Low 0–13, Moderate 14–26, High 27–40.
- Dominant concern derived from category sums.
- Recommendations: published resources whose `target_stress_brackets` contain the score bracket.
- Hotline resources are excluded for Low/Moderate and included for High.
- No answers/score are stored; only read-only resource queries occur.
- Disclaimer is shown on the intro and result.
- Moderate/High result can book a counsellor; the dominant concern is passed into the Smart Match route.

---

## Module 6 — Enquiry & Session Feedback (Michael)

**Screens and ViewModels**

- `StudentEnquiriesScreen`: linked enquiry list, anonymous form, reference-code dialog/copy, code lookup, withdrawal.
- `CounsellorInboxScreen`: Open/Answered/Closed filters, anonymous-safe cards, reply dialog, published-resource attachment.
- `StudentFeedbackScreen`: completed-session ratings, existing feedback, delete and re-rate.
- `CounsellorFeedbackScreen`: own-session ratings/comments, read-only.
- Admin Home: aggregate feedback count and average only.

**Backend objects**

- Tables: `enquiries`, `feedback`.
- RPCs: `submit_enquiry`, `get_enquiry_by_code`, `withdraw_enquiry_by_code`.
- Trigger: `protect_enquiry_reply` (message/anonymity immutable; answered requires reply; replied_at maintained).
- Feedback trigger: completed-only, ownership filled from appointment, duplicate friendly error.
- RLS:
  - Students see own linked (non-anonymous) enquiries; anonymous rows are not selectable.
  - Counsellors see the whole inbox.
  - Students see/delete own feedback; counsellors see feedback for their own sessions; admins have no row-level feedback access.

**Behaviour and validation**

- Enquiry message 10–1,000 characters; category required.
- Anonymous choice stores `student_id = null`; reference code is the only retrieval path.
- Code format `ENQ-XXXXXX`; invalid/nonexistent codes produce friendly messages.
- Code lookup returns no name, email, student ID or profile fields.
- Withdrawal removes open enquiries only; linked withdrawals are RLS-protected, anonymous withdrawals use the security-definer RPC.
- Counsellor reply can set Open/Answered/Closed and attach one published resource; Answered without reply is blocked client- and server-side.
- Feedback rating 1–5; comment ≤500; only completed own appointments; one per session; delete/re-rate supported.

---

## Cross-module integration

| Integration | Where |
|---|---|
| Student next appointment on Home | `HomeViewModel` + `AppointmentCard` |
| Admin aggregate stats | `admin_dashboard_stats()` → `HomeViewModel` → Admin Home |
| Smart Match uses counsellor specialisations and free slots | Module 2 + Module 3 data |
| Booking pre-filled from self-check | `Routes.bookAppointment(concern)` |
| Follow-up creates a real appointment | Module 4 RPC → Module 2 appointment list |
| Resource attached to enquiry reply | Module 6 inbox + Module 5 resource list |
| Student public summaries | Module 4 safe view → Module 2 Past tab |
| Student feedback | Module 6 → completed appointments and counsellor feedback |
| Deactivation blocked by appointments | Module 1 UI pre-check + `deactivate_account()` |
| Role-aware navigation | `RoleNavigation.kt` |

---

## Verification notes

- **15 local JVM tests** cover validation, Smart Match ranking, availability repeat/validation, triage grouping, and self-check brackets/recommendations.
- `:app:assembleDebug` and `:app:testDebugUnitTest` are green as of the final build.
- The backend schema/seeds were previously executed against an embedded PostgreSQL-compatible test harness with 109 workflow/RLS assertions; the live Supabase project's schema was reported as applied.
- Android emulator smoke testing in this environment was limited by a software-rendered emulator producing system-wide ANRs; the app installed and reached the Login screen without a fatal crash.

**Known blocker:** the current Supabase demo Auth rows are malformed and return `500 Database error querying schema`. Recreate them via Authentication → Users following `SETUP_GUIDE.md` §9.16. This is a data/setup issue, not an app build issue.

---

## File-header ownership convention

Every Kotlin file begins with a module/owner comment, for example:

```kotlin
// Module 2 | Owner: Beh
// Shared ... | Owner: shared (Beh, Jeremy, Michael)
```

This makes the six-module allocation explicit for assessment.
