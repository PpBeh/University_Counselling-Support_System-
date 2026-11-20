-- ============================================================================
-- OOP schema RLS / privacy proof queries
-- ============================================================================
-- Run in the Supabase SQL Editor against the migrated COPY, after the demo
-- users and seed data exist.
--
-- How to use:
--   1. Run Step 0 and copy the auth_user_id values for Aina, Faris, Aisyah,
--      Daniel and the administrator.
--   2. Replace the placeholders in Steps 1-5.
--   3. Every block starts with BEGIN and ends with ROLLBACK. The expected
--      values are comments beneath each query.
--
-- The app-level identity for a student/administrator equals the Auth UUID,
-- because the sign-up trigger uses it as users.id. A counsellor can have a
-- different users.id; always set the JWT to the Auth UUID (auth_user_id), not
-- to users.id. The functions app_user_id()/app_counsellor_id() handle that.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Step 0. Collect the app ids. Do not paste these public ids into the app JWT;
-- use auth_user_id as the `sub` claim.
-- ----------------------------------------------------------------------------

select
    u.id as app_user_id,
    u.auth_user_id,
    u.email,
    u.user_type
from public.users u
where lower(u.email) in (
    'student.aina@student.university.edu.my',
    'student.faris@student.university.edu.my',
    'counsellor.aisyah@university.edu.my',
    'counsellor.daniel@university.edu.my',
    'admin@university.edu.my'
)
order by u.email;

-- ----------------------------------------------------------------------------
-- Step 1. Student Aina: own data, published resources only, no private notes.
-- Replace <AINA_AUTH_UID>.
-- ----------------------------------------------------------------------------

begin;

select set_config(
    'request.jwt.claims',
    '{"sub":"<AINA_AUTH_UID>","role":"authenticated","aud":"authenticated"}',
    true
);
select set_config('request.jwt.claim.sub', '<AINA_AUTH_UID>', true);
set local role authenticated;

-- users policy: only her own users row.
select count(*) as users_visible from public.users;
-- Expected: 1

-- Own student row through the safe view.
select count(*) as own_student_view from public.v_students;
-- Expected: 1

-- Own appointments only, and no appointment belonging to another student.
select count(*) as own_appointments from public.appointments;
-- Expected: 3
select count(*) as foreign_appointments
from public.appointments
where student_id <> public.app_user_id();
-- Expected: 0

-- Base session_records is blocked; only the safe summary view is readable.
select count(*) as base_records from public.session_records;
-- Expected: 0
select count(*) as records_view from public.v_session_records;
-- Expected: 0
select count(*) as summaries from public.student_session_summaries;
-- Expected: 1

-- Only her own rating is visible.
select count(*) as own_feedback from public.feedback;
-- Expected: 1
select count(*) as feedback_view from public.v_feedback;
-- Expected: 1

-- Published resources only; the draft row is hidden.
select count(*) as published_resources from public.resources;
-- Expected: 10
select count(*) as draft_rows
from public.resources
where is_published = false;
-- Expected: 0
select count(*) as resource_view from public.v_resources;
-- Expected: 10

-- Identified enquiry only; anonymous rows are not directly selectable.
select count(*) as own_enquiries from public.enquiries;
-- Expected: 1
select count(*) as enquiry_view from public.v_enquiries;
-- Expected: 1
select count(*) as anonymous_rows
from public.anonymous_enquiries;
-- Expected: 0

-- The code RPC can still read an anonymous enquiry safely and exposes no
-- student identity at all.
select category, status
from public.get_enquiry_by_code('ENQ-DEMO01');
-- Expected: one row, category Academic Pressure, status open

rollback;

-- ----------------------------------------------------------------------------
-- Step 2. Counsellor Aisyah: own sessions, all student summaries, no admin
-- private-note access. Replace <AISYAH_AUTH_UID>.
-- ----------------------------------------------------------------------------

begin;

select set_config(
    'request.jwt.claims',
    '{"sub":"<AISYAH_AUTH_UID>","role":"authenticated","aud":"authenticated"}',
    true
);
select set_config('request.jwt.claim.sub', '<AISYAH_AUTH_UID>', true);
set local role authenticated;

select count(*) as own_appointments from public.appointments;
-- Expected: 4

select count(*) as own_records from public.session_records;
-- Expected: 1
select count(*) as records_view from public.v_session_records;
-- Expected: 1

select count(*) as own_feedback from public.feedback;
-- Expected: 1

select count(*) as students_visible from public.v_students;
-- Expected: 2

select count(*) as inbox from public.enquiries;
-- Expected: 4
select count(*) as inbox_view from public.v_enquiries;
-- Expected: 4

-- Anonymous enquiry has no identity columns to read.
select student_id
from public.v_enquiries
where reference_code = 'ENQ-DEMO01';
-- Expected: NULL

select count(*) as resources_including_drafts from public.resources;
-- Expected: 11
select count(*) as resource_view from public.v_resources;
-- Expected: 11

rollback;

-- ----------------------------------------------------------------------------
-- Step 3. Administrator: oversight, no private notes, no enquiry/feedback rows.
-- Replace <ADMIN_AUTH_UID>.
-- ----------------------------------------------------------------------------

begin;

select set_config(
    'request.jwt.claims',
    '{"sub":"<ADMIN_AUTH_UID>","role":"authenticated","aud":"authenticated"}',
    true
);
select set_config('request.jwt.claim.sub', '<ADMIN_AUTH_UID>', true);
set local role authenticated;

select count(*) as all_appointments from public.appointments;
-- Expected: 6
select count(*) as appointment_view from public.v_appointments;
-- Expected: 6
select count(*) as all_students from public.v_students;
-- Expected: 2
select count(*) as all_counsellors from public.v_counsellors;
-- Expected: 2
select count(*) as all_resources from public.v_resources;
-- Expected: 11

select count(*) as base_records_from_admin from public.session_records;
-- Expected: 0
select count(*) as records_from_admin_view from public.v_session_records;
-- Expected: 0
select count(*) as enquiries_from_admin from public.enquiries;
-- Expected: 0
select count(*) as anonymous_from_admin from public.anonymous_enquiries;
-- Expected: 0

select public.admin_dashboard_stats();
-- Expected: aggregate JSON (students, counsellors, appointments, resources,
-- feedback count and average rating). It contains no notes or session text.

rollback;

-- ----------------------------------------------------------------------------
-- Step 4. Student write attempts. These should fail, not just be hidden by UI.
-- Replace <AINA_AUTH_UID>.
-- ----------------------------------------------------------------------------

begin;

select set_config('request.jwt.claims', '{"sub":"<AINA_AUTH_UID>","role":"authenticated"}', true);
select set_config('request.jwt.claim.sub', '<AINA_AUTH_UID>', true);
set local role authenticated;

insert into public.resources (
    title, description, category_code, resource_type, is_published
)
values (
    'Student must not create this',
    'This insert must be rejected by privileges and RLS.',
    'stress', 'article', true
);
-- Expected: ERROR: permission denied for table resources

rollback;

begin;

select set_config('request.jwt.claims', '{"sub":"<AINA_AUTH_UID>","role":"authenticated"}', true);
select set_config('request.jwt.claim.sub', '<AINA_AUTH_UID>', true);
set local role authenticated;

insert into public.appointments (
    student_id, counsellor_id, slot_id, mode_code, concern_code, status_code
)
values (
    public.app_user_id(),
    (select counsellor_id from public.v_counsellors limit 1),
    (select slot_id from public.v_availability_slots limit 1),
    'in_person', 'stress', 'pending'
);
-- Expected: ERROR: permission denied for table appointments

rollback;

-- ----------------------------------------------------------------------------
-- Step 5. Structural/integrity checks (run as postgres, no role switch).
-- These prove the class-table subtype rules.
-- ----------------------------------------------------------------------------

select count(*) as resource_supertype_without_exactly_one_subtype
from public.resources r
where (
    select count(*)
    from (
        select 1 from public.article_resources where resource_id = r.id
        union all
        select 1 from public.video_resources where resource_id = r.id
        union all
        select 1 from public.audio_resources where resource_id = r.id
        union all
        select 1 from public.pdf_resources where resource_id = r.id
        union all
        select 1 from public.hotline_resources where resource_id = r.id
    ) subtypes
) <> 1;
-- Expected: 0

select count(*) as enquiry_supertype_without_exactly_one_subtype
from public.enquiries e
where (
    select count(*)
    from (
        select 1 from public.identified_enquiries where enquiry_id = e.id
        union all
        select 1 from public.anonymous_enquiries where enquiry_id = e.id
    ) subtypes
) <> 1;
-- Expected: 0

select count(*) as users_without_exactly_one_subtype
from public.users u
where (
    select count(*)
    from (
        select 1 from public.students where user_id = u.id
        union all
        select 1 from public.counsellors where user_id = u.id
        union all
        select 1 from public.administrators where user_id = u.id
    ) subtypes
) <> 1;
-- Expected: 0 (the sign-up trigger and seed enforce one subtype per user)

select count(*) as old_objects_still_in_public
from pg_class c
join pg_namespace n on n.oid = c.relnamespace
where n.nspname = 'public'
  and c.relname in ('profiles', 'admin_whitelist');
-- Expected: 0 (old names live in the legacy schema)

-- Expected final result: all four integrity checks return 0.
