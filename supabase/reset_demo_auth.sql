-- ============================================================================
-- DEMO-ONLY AUTH RECOVERY — DESTRUCTIVE
-- ============================================================================
-- Use this only on the university mini-project demo database.
-- It removes ALL demo application records and the five malformed demo Auth
-- users so they can be recreated through Supabase Auth.
--
-- WHY:
--   * Auth user deletion cascades to public.profiles, but appointments,
--     session records and feedback use ON DELETE RESTRICT. The dashboard
--     therefore shows a generic loading/error message while deleting.
--   * The five current demo Auth rows were inserted with raw SQL and cause
--     "Database error querying schema" at login.
--
-- DO NOT create Auth users with SQL. Recreate them through the Dashboard or
-- the app's Register screen after this script completes.
--
-- The whole cleanup is one transaction. If any step is blocked, everything
-- rolls back and nothing is deleted.
-- ============================================================================

begin;

-- Capture the five demo account IDs before deleting anything.
create temp table demo_users on commit drop as
select id, lower(email) as email
from auth.users
where lower(email) in (
    'admin@university.edu.my',
    'counsellor.aisyah@university.edu.my',
    'counsellor.daniel@university.edu.my',
    'student.aina@student.university.edu.my',
    'student.faris@student.university.edu.my'
);

-- Report what will be removed (visible in SQL Editor output).
select 'demo users found' as step, count(*) as rows from demo_users;

-- Abort if the demo emails are not present at all, so this cannot clear an
-- unrelated project's activity.
do $$
begin
    if (select count(*) from demo_users) = 0 then
        raise exception 'No demo users were found. Check the email addresses or use a fresh Supabase project.';
    end if;
end;
$$;

-- 1. Remove records with restrictive FKs first.
--    This clears demo activity only; the project is demo data.
delete from public.feedback;
delete from public.session_records;
delete from public.appointments;
delete from public.enquiries;

-- 2. Remove the public profiles that belong to the demo Auth users.
delete from public.profiles
where id in (select id from demo_users);

-- 3. Remove Auth sessions and identities, then the users.
--    These tables normally cascade, but deleting explicitly makes the
--    dependency order clear and avoids dashboard-side partial deletes.
delete from auth.sessions
where user_id in (select id from demo_users);

delete from auth.identities
where user_id in (select id from demo_users);

delete from auth.users
where id in (select id from demo_users);

-- Counsellor directory rows are deliberately retained. Their user_id becomes
-- NULL through ON DELETE SET NULL, so recreating the counsellor Auth account
-- re-links it by email. admin_whitelist is also retained.
commit;

-- ============================================================================
-- NEXT STEPS
-- ============================================================================
-- 1. Authentication → Users → Add user:
--      admin@university.edu.my                  (auto-confirm)
--      counsellor.aisyah@university.edu.my      (auto-confirm)
--      counsellor.daniel@university.edu.my      (auto-confirm)
--    Password: Demo@12345
--    Staff do not need student metadata.
--
-- 2. Create the two students through the APP's Register screen, not the
--    Dashboard, unless your Dashboard has a metadata JSON box:
--      student.aina@student.university.edu.my
--        full_name: Aina Zulaikha
--        student_id: DAMCS2401
--        programme: Diploma in Computer Science
--        phone: 012-3456789
--        preferred_session_mode: in_person
--      student.faris@student.university.edu.my
--        full_name: Muhammad Faris
--        student_id: DAMCS2402
--        programme: Diploma in Information Technology
--        phone: +60123456789
--        preferred_session_mode: online
--    Password for both: Demo@12345
--
-- 3. Confirm Authentication → Providers → Email has Confirm email OFF and
--    the five profiles have the correct roles.
--
-- 4. Run supabase/seed_demo.sql again to restore demo appointments,
--    records, enquiries and feedback.
--
-- If creating even the admin user still fails after this cleanup, the Auth
-- trigger/schema is damaged. Create a fresh Supabase project and follow
-- docs/SETUP_GUIDE.md from Step 3; do not patch auth.users manually.
-- ============================================================================
