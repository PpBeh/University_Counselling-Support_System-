-- ============================================================================
-- University Counselling Appointment & Support System
-- Module: reference/demo data | Owner: shared backend (all three members)
--
-- PART A - RUN THIS AFTER schema.sql AND BEFORE CREATING THE DEMO AUTH USERS.
--
-- It inserts:
--   * the admin email into admin_whitelist
--   * two counsellor directory entries (emails must match the demo accounts)
--   * the self-help resource library
--   * free availability slots for the next four weeks, relative to now()
--
-- The script is safe to run again. Each insert checks for an existing row
-- first, so it will not duplicate seed data or overwrite linked account ids.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Admin whitelist
-- The admin must be whitelisted BEFORE their Auth user is created, otherwise
-- the auth.users trigger will give that account the student role.
-- ----------------------------------------------------------------------------
insert into public.admin_whitelist (email)
select 'admin@university.edu.my'
where not exists (
    select 1
    from public.admin_whitelist aw
    where lower(aw.email) = 'admin@university.edu.my'
);

-- ----------------------------------------------------------------------------
-- Counsellor directory entries
-- user_id remains NULL until the matching email signs up / is created in Auth.
-- The auth.users trigger then links user_id and creates the counsellor profile.
-- ----------------------------------------------------------------------------
insert into public.counsellors (
    full_name, email, specialisations, languages, bio
)
select
    'Dr. Aisyah Rahman',
    'counsellor.aisyah@university.edu.my',
    array['Stress', 'Anxiety', 'Academic Pressure', 'Sleep']::text[],
    array['English', 'Bahasa Melayu']::text[],
    'A calm, practical counsellor who helps students with stress, anxiety, exam pressure and sleep routines. Aisyah works in person and online.'
where not exists (
    select 1
    from public.counsellors c
    where lower(c.email) = 'counsellor.aisyah@university.edu.my'
);

insert into public.counsellors (
    full_name, email, specialisations, languages, bio
)
select
    'Mr. Daniel Lim',
    'counsellor.daniel@university.edu.my',
    array['Relationships', 'Loneliness', 'Financial', 'Academic Pressure', 'Other']::text[],
    array['English', 'Bahasa Melayu', 'Mandarin']::text[],
    'Daniel supports students navigating relationship worries, loneliness, money stress and the pressure of changing circumstances.'
where not exists (
    select 1
    from public.counsellors c
    where lower(c.email) = 'counsellor.daniel@university.edu.my'
);

-- ----------------------------------------------------------------------------
-- Self-help resources
-- Students only see is_published = true rows. The last item is intentionally
-- a draft so counsellors/admins can test publish/unpublish.
-- Hotline resources target only the High self-check bracket.
-- ----------------------------------------------------------------------------
insert into public.resources (
    title, description, category, media_type, content_url,
    target_stress_brackets, is_published
)
select
    v.title,
    v.description,
    v.category,
    v.media_type,
    v.content_url,
    v.target_stress_brackets,
    v.is_published
from (
    values
        (
            '5-Minute Grounding for Anxiety',
            'A short grounding exercise you can use between classes: name what you can see, hear and feel, then breathe slowly. Practice it while you are calm so it is easier to use when anxiety rises.',
            'Anxiety',
            'Article',
            'https://www.mind.org.uk/information-support/types-of-mental-health-problems/anxiety-and-panic-attacks/',
            array['low', 'moderate']::text[],
            true
        ),
        (
            'Beating Exam Pressure',
            'A practical guide to planning revision in small blocks, protecting sleep before exams and replacing catastrophic thoughts with realistic next steps.',
            'Academic Pressure',
            'Article',
            'https://www.mind.org.uk/information-support/tips-for-everyday-living/student-life/',
            array['low', 'moderate']::text[],
            true
        ),
        (
            'Sleep Reset: A Wind-Down Audio',
            'A gentle ten-minute audio routine for slowing down before bed. Dim your screen, lower the lights and give yourself permission to rest before tomorrow.',
            'Sleep',
            'Audio',
            'https://www.mindful.org/sleep-meditation/',
            array['low', 'moderate']::text[],
            true
        ),
        (
            'Talking About Relationship Stress',
            'A short video about naming relationship worries without blame, asking for what you need and knowing when to step back from a difficult conversation.',
            'Relationships',
            'Video',
            'https://www.youtube.com/results?search_query=relationship+stress+support',
            array['low', 'moderate']::text[],
            true
        ),
        (
            'A Calm Money Plan for Students',
            'A simple weekly money check-in: list essentials, set a small spending buffer and identify one cost you can pause this month. Money stress is common and talkable.',
            'Financial',
            'PDF',
            'https://www.moneyhelper.org.uk/en/everyday-money/budgeting',
            array['low', 'moderate']::text[],
            true
        ),
        (
            'Small Steps Out of Loneliness',
            'Feeling lonely does not mean something is wrong with you. This article suggests low-pressure ways to reconnect: one message, one shared study session, one familiar place.',
            'Loneliness',
            'Article',
            'https://www.mind.org.uk/information-support/types-of-mental-health-problems/loneliness/',
            array['low', 'moderate']::text[],
            true
        ),
        (
            'When Worry Feels Too Heavy',
            'When worry is affecting sleep, attendance or your sense of safety, extra support is sensible. Read about warning signs and how to reach the counselling team.',
            'Anxiety',
            'Article',
            'https://www.mind.org.uk/information-support/types-of-mental-health-problems/anxiety-and-panic-attacks/',
            array['moderate', 'high']::text[],
            true
        ),
        (
            'Campus Crisis Line',
            'If you are in immediate danger or feel unable to keep yourself safe, call the campus crisis line or local emergency services now. You do not have to manage this alone.',
            'Other',
            'Hotline',
            'https://www.befrienders.org.my/',
            array['high']::text[],
            true
        ),
        (
            '24/7 Mental Health Helpline',
            'A confidential helpline for overwhelming moments. Trained listeners can stay on the line with you while you make a plan for the next hour.',
            'Anxiety',
            'Hotline',
            'https://www.mind.org.uk/information-support/guides-to-support-and-services/crisis-services/',
            array['high']::text[],
            true
        ),
        (
            'Sleep and Study: A Practical Guide',
            'An evidence-informed PDF on protecting sleep during busy study periods, including a simple wind-down checklist and when to speak to a professional.',
            'Sleep',
            'PDF',
            'https://www.sleepfoundation.org/college-and-sleep',
            array['moderate', 'high']::text[],
            true
        ),
        (
            'Coping With Change (Draft)',
            'Draft resource for counsellors and admins to review before publishing. It covers moving to a new campus, changing routines and asking for help early.',
            'Other',
            'Article',
            'https://www.mind.org.uk/information-support/tips-for-everyday-living/student-life/',
            array['low', 'moderate']::text[],
            false
        )
) as v (
    title, description, category, media_type, content_url,
    target_stress_brackets, is_published
)
where not exists (
    select 1
    from public.resources r
    where lower(r.title) = lower(v.title)
);

-- ----------------------------------------------------------------------------
-- Free availability slots for four weeks (weekdays only, Kuala Lumpur time).
-- Generating from now() means these are always in the future, even when the
-- demo is set up months from now. Each run only adds slots that do not exist.
-- ----------------------------------------------------------------------------
with base as (
    select (now() at time zone 'Asia/Kuala_Lumpur')::date as today
),
candidate_days as (
    select
        (b.today + d.day_offset)::date as slot_date
    from base b
    cross join generate_series(1, 28) as d(day_offset)
    where extract(isodow from (b.today + d.day_offset)) between 1 and 5
),
candidate_times as (
    select
        cd.slot_date,
        t.start_time,
        t.end_time
    from candidate_days cd
    cross join (
        values
            (time '09:00', time '10:00'),
            (time '14:00', time '15:00')
    ) as t (start_time, end_time)
),
candidates as (
    select
        (ct.slot_date + ct.start_time) at time zone 'Asia/Kuala_Lumpur' as start_at,
        (ct.slot_date + ct.end_time) at time zone 'Asia/Kuala_Lumpur' as end_at
    from candidate_times ct
)
insert into public.availability_slots (counsellor_id, start_at, end_at)
select
    c.id,
    x.start_at,
    x.end_at
from candidates x
cross join public.counsellors c
where lower(c.email) in (
    'counsellor.aisyah@university.edu.my',
    'counsellor.daniel@university.edu.my'
)
  and x.start_at > now()
  and not exists (
      select 1
      from public.availability_slots s
      where s.counsellor_id = c.id
        and s.start_at < x.end_at
        and s.end_at > x.start_at
  );

notify pgrst, 'reload schema';
