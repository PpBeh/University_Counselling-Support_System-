-- ============================================================================
-- University Counselling Appointment & Support System
-- OOP schema - reference data (Part A)
-- Owner: shared backend
--
-- RUN AFTER schema.sql AND BEFORE CREATING THE DEMO AUTH USERS.
-- Creates:
--   * vocabulary tables (concern categories, modes, risks, brackets, statuses,
--     status transitions, languages)
--   * one pre-provisioned administrator users/administrators row
--   * two pre-provisioned counsellor users/counsellors rows + associations
--   * the resource library with its subtype rows and target brackets
--   * four weeks of free counsellor availability
--
-- Pre-provisioned staff have auth_user_id NULL. The auth.users trigger links
-- the Auth account by email when it is created. Re-running this script adds
-- only missing rows and never overwrites linked accounts.
-- ============================================================================

begin;

-- ----------------------------------------------------------------------------
-- 1. Vocabulary rows
-- ----------------------------------------------------------------------------

insert into public.concern_categories (code, label, sort_order) values
    ('stress', 'Stress', 1),
    ('anxiety', 'Anxiety', 2),
    ('academic_pressure', 'Academic Pressure', 3),
    ('relationships', 'Relationships', 4),
    ('sleep', 'Sleep', 5),
    ('financial', 'Financial', 6),
    ('loneliness', 'Loneliness', 7),
    ('other', 'Other', 8)
on conflict (code) do nothing;

insert into public.session_modes (code, label, sort_order) values
    ('in_person', 'In person', 1),
    ('online', 'Online', 2)
on conflict (code) do nothing;

insert into public.risk_levels (code, label, rank) values
    ('low', 'Low', 1),
    ('medium', 'Medium', 2),
    ('high', 'High', 3)
on conflict (code) do nothing;

insert into public.stress_brackets (code, label, min_score, max_score, sort_order) values
    ('low', 'Low', 0, 13, 1),
    ('moderate', 'Moderate', 14, 26, 2),
    ('high', 'High', 27, 40, 3)
on conflict (code) do nothing;

insert into public.appointment_statuses (code, label, is_terminal, sort_order) values
    ('pending', 'Pending', false, 1),
    ('confirmed', 'Confirmed', false, 2),
    ('completed', 'Completed', true, 3),
    ('no_show', 'No show', true, 4),
    ('cancelled', 'Cancelled', true, 5)
on conflict (code) do nothing;

-- OOP: State-pattern transition rows. These are the exact rules the old
-- string trigger had: pending -> confirmed/cancelled;
-- confirmed -> completed/no_show/cancelled.
insert into public.appointment_status_transitions (from_status_code, to_status_code) values
    ('pending', 'confirmed'),
    ('pending', 'cancelled'),
    ('confirmed', 'pending'),
    ('confirmed', 'completed'),
    ('confirmed', 'no_show'),
    ('confirmed', 'cancelled')
on conflict do nothing;

insert into public.languages (name) values
    ('English'),
    ('Bahasa Melayu'),
    ('Mandarin')
on conflict do nothing;

-- ----------------------------------------------------------------------------
-- 2. Pre-provisioned administrator
-- ----------------------------------------------------------------------------

do $seed_admin$
declare
    v_admin_id uuid;
begin
    select u.id into v_admin_id
    from public.users u
    where lower(u.email) = 'admin@university.edu.my';

    if v_admin_id is null then
        insert into public.users (
            auth_user_id, email, full_name, phone, is_active, user_type
        )
        values (
            null, 'admin@university.edu.my', 'System Administrator', null, true,
            'administrator'
        )
        returning id into v_admin_id;
    elsif not exists (
        select 1 from public.users u
        where u.id = v_admin_id and u.user_type = 'administrator'
    ) then
        raise exception 'The admin email already belongs to a non-administrator user.';
    end if;

    insert into public.administrators (user_id)
    values (v_admin_id)
    on conflict (user_id) do nothing;
end;
$seed_admin$;

-- ----------------------------------------------------------------------------
-- 3. Pre-provisioned counsellors and association rows
-- ----------------------------------------------------------------------------

create temporary table seed_counsellors (
    full_name text not null,
    email text not null,
    specialisations text[] not null,
    languages text[] not null,
    bio text not null
) on commit drop;

insert into seed_counsellors (full_name, email, specialisations, languages, bio) values
    (
        'Dr. Aisyah Rahman',
        'counsellor.aisyah@university.edu.my',
        array['Stress', 'Anxiety', 'Academic Pressure', 'Sleep']::text[],
        array['English', 'Bahasa Melayu']::text[],
        'A calm, practical counsellor who helps students with stress, anxiety, exam pressure and sleep routines. Aisyah works in person and online.'
    ),
    (
        'Mr. Daniel Lim',
        'counsellor.daniel@university.edu.my',
        array['Relationships', 'Loneliness', 'Financial', 'Academic Pressure', 'Other']::text[],
        array['English', 'Bahasa Melayu', 'Mandarin']::text[],
        'Daniel supports students navigating relationship worries, loneliness, money stress and the pressure of changing circumstances.'
    );

do $seed_counsellors$
declare
    v record;
    v_user_id uuid;
    v_category_code text;
    v_language_id smallint;
    v_specialisation text;
    v_language text;
begin
    for v in select * from seed_counsellors loop
        select u.id into v_user_id
        from public.users u
        where lower(u.email) = lower(v.email);

        if v_user_id is null then
            insert into public.users (
                auth_user_id, email, full_name, phone, is_active, user_type
            )
            values (
                null, lower(v.email), v.full_name, null, true, 'counsellor'
            )
            returning id into v_user_id;
        else
            if not exists (
                select 1 from public.users u
                where u.id = v_user_id and u.user_type = 'counsellor'
            ) then
                raise exception 'The counsellor email % belongs to a non-counsellor user.', v.email;
            end if;
        end if;

        insert into public.counsellors (user_id, bio)
        values (v_user_id, v.bio)
        on conflict (user_id) do nothing;

        for v_specialisation in
            select distinct btrim(x)
            from unnest(v.specialisations) as x
            where btrim(x) <> ''
        loop
            v_category_code := public.resolve_concern_category(v_specialisation);
            if v_category_code is null then
                raise exception 'Unknown specialisation: %', v_specialisation;
            end if;
            insert into public.counsellor_specialisations (counsellor_id, category_code)
            values (v_user_id, v_category_code)
            on conflict do nothing;
        end loop;

        for v_language in
            select distinct btrim(x)
            from unnest(v.languages) as x
            where btrim(x) <> ''
        loop
            insert into public.languages (name)
            values (v_language)
            on conflict do nothing;

            select l.language_id into v_language_id
            from public.languages l
            where lower(l.name) = lower(v_language)
            limit 1;

            insert into public.counsellor_languages (counsellor_id, language_id)
            values (v_user_id, v_language_id)
            on conflict do nothing;
        end loop;
    end loop;
end;
$seed_counsellors$;

-- ----------------------------------------------------------------------------
-- 4. Resource supertype + subtype rows + target brackets
-- ----------------------------------------------------------------------------

create temporary table seed_resources (
    title text not null,
    description text not null,
    category text not null,
    media_type text not null,
    content_url text not null,
    target_brackets text[] not null,
    is_published boolean not null,
    phone_number text,
    operating_hours text
) on commit drop;

insert into seed_resources (
    title, description, category, media_type, content_url,
    target_brackets, is_published, phone_number, operating_hours
) values
    (
        '5-Minute Grounding for Anxiety',
        'A short grounding exercise you can use between classes: name what you can see, hear and feel, then breathe slowly. Practice it while you are calm so it is easier to use when anxiety rises.',
        'Anxiety', 'Article',
        'https://www.mind.org.uk/information-support/types-of-mental-health-problems/anxiety-and-panic-attacks/',
        array['low', 'moderate']::text[], true, null, null
    ),
    (
        'Beating Exam Pressure',
        'A practical guide to planning revision in small blocks, protecting sleep before exams and replacing catastrophic thoughts with realistic next steps.',
        'Academic Pressure', 'Article',
        'https://www.mind.org.uk/information-support/tips-for-everyday-living/student-life/',
        array['low', 'moderate']::text[], true, null, null
    ),
    (
        'Sleep Reset: A Wind-Down Audio',
        'A gentle ten-minute audio routine for slowing down before bed. Dim your screen, lower the lights and give yourself permission to rest before tomorrow.',
        'Sleep', 'Audio',
        'https://www.mindful.org/sleep-meditation/',
        array['low', 'moderate']::text[], true, null, null
    ),
    (
        'Talking About Relationship Stress',
        'A short video about naming relationship worries without blame, asking for what you need and knowing when to step back from a difficult conversation.',
        'Relationships', 'Video',
        'https://www.youtube.com/results?search_query=relationship+stress+support',
        array['low', 'moderate']::text[], true, null, null
    ),
    (
        'A Calm Money Plan for Students',
        'A simple weekly money check-in: list essentials, set a small spending buffer and identify one cost you can pause this month. Money stress is common and talkable.',
        'Financial', 'PDF',
        'https://www.moneyhelper.org.uk/en/everyday-money/budgeting',
        array['low', 'moderate']::text[], true, null, null
    ),
    (
        'Small Steps Out of Loneliness',
        'Feeling lonely does not mean something is wrong with you. This article suggests low-pressure ways to reconnect: one message, one shared study session, one familiar place.',
        'Loneliness', 'Article',
        'https://www.mind.org.uk/information-support/types-of-mental-health-problems/loneliness/',
        array['low', 'moderate']::text[], true, null, null
    ),
    (
        'When Worry Feels Too Heavy',
        'When worry is affecting sleep, attendance or your sense of safety, extra support is sensible. Read about warning signs and how to reach the counselling team.',
        'Anxiety', 'Article',
        'https://www.mind.org.uk/information-support/types-of-mental-health-problems/anxiety-and-panic-attacks/',
        array['moderate', 'high']::text[], true, null, null
    ),
    (
        'Campus Crisis Line',
        'If you are in immediate danger or feel unable to keep yourself safe, call the campus crisis line or local emergency services now. You do not have to manage this alone.',
        'Other', 'Hotline',
        'https://www.befrienders.org.my/',
        array['high']::text[], true, '+603-7627-2929', '24 hours daily'
    ),
    (
        '24/7 Mental Health Helpline',
        'A confidential helpline for overwhelming moments. Trained listeners can stay on the line with you while you make a plan for the next hour.',
        'Anxiety', 'Hotline',
        'https://www.mind.org.uk/information-support/guides-to-support-and-services/crisis-services/',
        array['high']::text[], true, '+603-7956-8145', '24 hours daily'
    ),
    (
        'Sleep and Study: A Practical Guide',
        'An evidence-informed PDF on protecting sleep during busy study periods, including a simple wind-down checklist and when to speak to a professional.',
        'Sleep', 'PDF',
        'https://www.sleepfoundation.org/college-and-sleep',
        array['moderate', 'high']::text[], true, null, null
    ),
    (
        'Coping With Change (Draft)',
        'Draft resource for counsellors and admins to review before publishing. It covers moving to a new campus, changing routines and asking for help early.',
        'Other', 'Article',
        'https://www.mind.org.uk/information-support/tips-for-everyday-living/student-life/',
        array['low', 'moderate']::text[], false, null, null
    );

do $seed_resources$
declare
    v record;
    v_resource_id uuid;
    v_type text;
    v_category_code text;
    v_bracket_code text;
    v_bracket text;
begin
    for v in select * from seed_resources loop
        v_type := public.resolve_resource_type(v.media_type);
        v_category_code := public.resolve_concern_category(v.category);

        select r.id into v_resource_id
        from public.resources r
        where lower(r.title) = lower(v.title)
        limit 1;

        if v_resource_id is null then
            insert into public.resources (
                title, description, category_code, resource_type, is_published
            )
            values (
                v.title, v.description, v_category_code, v_type, v.is_published
            )
            returning id into v_resource_id;
        end if;

        if v_type = 'article' then
            insert into public.article_resources (resource_id, content_url)
            values (v_resource_id, v.content_url)
            on conflict (resource_id) do nothing;
        elsif v_type = 'video' then
            insert into public.video_resources (resource_id, content_url)
            values (v_resource_id, v.content_url)
            on conflict (resource_id) do nothing;
        elsif v_type = 'audio' then
            insert into public.audio_resources (resource_id, content_url)
            values (v_resource_id, v.content_url)
            on conflict (resource_id) do nothing;
        elsif v_type = 'pdf' then
            insert into public.pdf_resources (resource_id, content_url)
            values (v_resource_id, v.content_url)
            on conflict (resource_id) do nothing;
        else
            insert into public.hotline_resources (
                resource_id, phone_number, operating_hours, website_url
            )
            values (
                v_resource_id, v.phone_number, v.operating_hours, v.content_url
            )
            on conflict (resource_id) do nothing;
        end if;

        for v_bracket in
            select distinct btrim(x)
            from unnest(v.target_brackets) as x
            where btrim(x) <> ''
        loop
            v_bracket_code := public.resolve_stress_bracket(v_bracket);
            if v_bracket_code is null then
                raise exception 'Unknown stress bracket: %', v_bracket;
            end if;
            insert into public.resource_target_brackets (resource_id, bracket_code)
            values (v_resource_id, v_bracket_code)
            on conflict do nothing;
        end loop;
    end loop;
end;
$seed_resources$;

-- ----------------------------------------------------------------------------
-- 5. Four weeks of future availability for the demo counsellors
-- ----------------------------------------------------------------------------

with base as (
    select (now() at time zone 'Asia/Kuala_Lumpur')::date as today
),
candidate_days as (
    select (b.today + d.day_offset)::date as slot_date
    from base b
    cross join generate_series(1, 28) as d(day_offset)
    where extract(isodow from (b.today + d.day_offset)) between 1 and 5
),
candidate_times as (
    select cd.slot_date, t.start_time, t.end_time
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
select c.user_id, x.start_at, x.end_at
from candidates x
cross join public.counsellors c
join public.users u on u.id = c.user_id
where lower(u.email) in (
    'counsellor.aisyah@university.edu.my',
    'counsellor.daniel@university.edu.my'
)
  and u.is_active = true
  and x.start_at > now()
  and not exists (
      select 1
      from public.availability_slots s
      where s.counsellor_id = c.user_id
        and s.start_at < x.end_at
        and s.end_at > x.start_at
  );

commit;

notify pgrst, 'reload schema';
