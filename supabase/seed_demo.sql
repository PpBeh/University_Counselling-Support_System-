-- ============================================================================
-- University Counselling Appointment & Support System
-- OOP schema - demo data (Part B)
-- Owner: shared backend
--
-- RUN AFTER schema.sql, seed_reference.sql AND THE DEMO AUTH USERS EXIST.
--
-- Creates:
--   * two completed sessions with private/public records and ratings
--   * one confirmed upcoming session and one pending follow-up (Aina)
--   * one pending upcoming session (Faris)
--   * one cancelled session
--   * anonymous and identified enquiries with known reference codes
--
-- The old tables are gone in the new schema; this file uses users/students/
-- counsellors and the association/subtype tables.
-- ============================================================================

begin;

do $seed$
declare
    v_aina_id uuid;
    v_faris_id uuid;
    v_aisyah_id uuid;
    v_daniel_id uuid;

    v_today_kl date := (now() at time zone 'Asia/Kuala_Lumpur')::date;

    v_past_1_start timestamptz;
    v_past_1_end timestamptz;
    v_past_2_start timestamptz;
    v_past_2_end timestamptz;
    v_past_3_start timestamptz;
    v_past_3_end timestamptz;
    v_past_1_id uuid;
    v_past_2_id uuid;
    v_past_3_id uuid;

    v_completed_aina uuid;
    v_completed_faris uuid;
    v_upcoming_slot uuid;
    v_followup_slot uuid;
    v_faris_slot uuid;
    v_active_count integer;
    v_resource_grounding uuid;
    v_resource_exam uuid;
    v_enquiry_id uuid;
begin
    select u.id into v_aina_id
    from public.users u
    where lower(u.email) = 'student.aina@student.university.edu.my'
      and u.user_type = 'student';

    select u.id into v_faris_id
    from public.users u
    where lower(u.email) = 'student.faris@student.university.edu.my'
      and u.user_type = 'student';

    select c.user_id into v_aisyah_id
    from public.counsellors c
    join public.users u on u.id = c.user_id
    where lower(u.email) = 'counsellor.aisyah@university.edu.my';

    select c.user_id into v_daniel_id
    from public.counsellors c
    join public.users u on u.id = c.user_id
    where lower(u.email) = 'counsellor.daniel@university.edu.my';

    if v_aina_id is null or v_faris_id is null then
        raise exception 'Demo students were not found. Create the demo Auth users with the metadata from SETUP_GUIDE.md, then run this file again.';
    end if;

    if v_aisyah_id is null or v_daniel_id is null then
        raise exception 'Demo counsellor accounts are not linked. Run seed_reference.sql BEFORE creating the counsellor Auth users, then run this file again.';
    end if;

    -- Historical slots: the guard rejects past availability for the app, so it
    -- is disabled only inside this transaction for demo history.
    execute 'alter table public.availability_slots disable trigger trg_availability_slot_guard';

    begin
        v_past_1_start := ((v_today_kl - 20) + time '10:00') at time zone 'Asia/Kuala_Lumpur';
        v_past_1_end := ((v_today_kl - 20) + time '11:00') at time zone 'Asia/Kuala_Lumpur';
        v_past_2_start := ((v_today_kl - 14) + time '10:00') at time zone 'Asia/Kuala_Lumpur';
        v_past_2_end := ((v_today_kl - 14) + time '11:00') at time zone 'Asia/Kuala_Lumpur';
        v_past_3_start := ((v_today_kl - 5) + time '14:00') at time zone 'Asia/Kuala_Lumpur';
        v_past_3_end := ((v_today_kl - 5) + time '15:00') at time zone 'Asia/Kuala_Lumpur';

        if not exists (
            select 1 from public.availability_slots
            where counsellor_id = v_aisyah_id and start_at = v_past_1_start
        ) then
            insert into public.availability_slots (counsellor_id, start_at, end_at)
            values (v_aisyah_id, v_past_1_start, v_past_1_end);
        end if;

        if not exists (
            select 1 from public.availability_slots
            where counsellor_id = v_daniel_id and start_at = v_past_2_start
        ) then
            insert into public.availability_slots (counsellor_id, start_at, end_at)
            values (v_daniel_id, v_past_2_start, v_past_2_end);
        end if;

        if not exists (
            select 1 from public.availability_slots
            where counsellor_id = v_aisyah_id and start_at = v_past_3_start
        ) then
            insert into public.availability_slots (counsellor_id, start_at, end_at)
            values (v_aisyah_id, v_past_3_start, v_past_3_end);
        end if;

        select id into v_past_1_id
        from public.availability_slots
        where counsellor_id = v_aisyah_id and start_at = v_past_1_start;

        select id into v_past_2_id
        from public.availability_slots
        where counsellor_id = v_daniel_id and start_at = v_past_2_start;

        select id into v_past_3_id
        from public.availability_slots
        where counsellor_id = v_aisyah_id and start_at = v_past_3_start;

        -- Completed sessions and records.
        select a.id into v_completed_aina
        from public.appointments a
        where a.slot_id = v_past_1_id
        limit 1;

        if v_completed_aina is null then
            insert into public.appointments (
                student_id, counsellor_id, slot_id, mode_code, concern_code,
                note, status_code
            )
            values (
                v_aina_id, v_aisyah_id, v_past_1_id, 'in_person', 'anxiety',
                'First session booked from the demo data set.', 'completed'
            )
            returning id into v_completed_aina;
        end if;

        select a.id into v_completed_faris
        from public.appointments a
        where a.slot_id = v_past_2_id
        limit 1;

        if v_completed_faris is null then
            insert into public.appointments (
                student_id, counsellor_id, slot_id, mode_code, concern_code,
                note, status_code
            )
            values (
                v_faris_id, v_daniel_id, v_past_2_id, 'online', 'academic_pressure',
                'I have been struggling to keep up with the semester.', 'completed'
            )
            returning id into v_completed_faris;
        end if;

        if not exists (
            select 1 from public.session_records where appointment_id = v_completed_aina
        ) then
            insert into public.session_records (
                appointment_id, private_notes, public_summary, risk_code,
                follow_up_needed, follow_up_due
            )
            values (
                v_completed_aina,
                'Aina came in feeling tense about upcoming assessments. We practised a grounding exercise and agreed on a small study plan. Sleep has improved slightly. Monitor anxiety before the next quiz and check in at the follow-up.',
                'We talked about managing exam anxiety with short grounding breaks and a realistic study plan. You agreed to practise one small step before the next quiz.',
                'high', true, now() + interval '2 days'
            );
        end if;

        if not exists (
            select 1 from public.session_records where appointment_id = v_completed_faris
        ) then
            insert into public.session_records (
                appointment_id, private_notes, public_summary, risk_code,
                follow_up_needed, follow_up_due
            )
            values (
                v_completed_faris,
                'Faris reported deadline pressure and difficulty concentrating online. We broke the week into smaller tasks and discussed a realistic extension plan. No safety concerns raised during the session. Follow up on workload and sleep.',
                'We made a smaller weekly plan for assignments and talked about asking for an extension early. You will review the plan before the next session.',
                'medium', true, now() + interval '6 days'
            );
        end if;

        if not exists (
            select 1 from public.feedback where appointment_id = v_completed_aina
        ) then
            insert into public.feedback (appointment_id, rating, comment)
            values (
                v_completed_aina, 5,
                'I felt heard and left with one small thing I could actually do.'
            );
        end if;

        if not exists (
            select 1 from public.feedback where appointment_id = v_completed_faris
        ) then
            insert into public.feedback (appointment_id, rating, comment)
            values (
                v_completed_faris, 4,
                'The online session was easy to join and the plan was realistic.'
            );
        end if;

        -- Aina's confirmed upcoming session.
        if not exists (
            select 1
            from public.appointments a
            where a.student_id = v_aina_id
              and a.counsellor_id = v_aisyah_id
              and a.status_code = 'confirmed'
        ) then
            select count(*) into v_active_count
            from public.appointments a
            join public.availability_slots s on s.id = a.slot_id
            where a.student_id = v_aina_id
              and a.status_code in ('pending', 'confirmed')
              and s.start_at > now();

            if v_active_count < 2 then
                select s.id into v_upcoming_slot
                from public.availability_slots s
                where s.counsellor_id = v_aisyah_id
                  and s.start_at > now()
                  and not exists (
                      select 1 from public.appointments a
                      where a.slot_id = s.id
                        and a.status_code in ('pending', 'confirmed')
                  )
                  and not exists (
                      select 1
                      from public.appointments a
                      join public.availability_slots existing on existing.id = a.slot_id
                      where a.student_id = v_aina_id
                        and a.status_code in ('pending', 'confirmed')
                        and tstzrange(existing.start_at, existing.end_at, '[)')
                            && tstzrange(s.start_at, s.end_at, '[)')
                  )
                order by s.start_at
                limit 1;

                if v_upcoming_slot is not null then
                    insert into public.appointments (
                        student_id, counsellor_id, slot_id, mode_code,
                        concern_code, note, status_code
                    )
                    values (
                        v_aina_id, v_aisyah_id, v_upcoming_slot, 'in_person',
                        'stress', 'Checking in before the next assessment block.',
                        'confirmed'
                    );
                end if;
            end if;
        end if;

        -- Follow-up for Aina.
        if not exists (
            select 1
            from public.appointments a
            where a.follow_up_of = v_completed_aina
              and a.status_code in ('pending', 'confirmed')
        ) then
            select count(*) into v_active_count
            from public.appointments a
            join public.availability_slots s on s.id = a.slot_id
            where a.student_id = v_aina_id
              and a.status_code in ('pending', 'confirmed')
              and s.start_at > now();

            if v_active_count < 2 then
                select s.id into v_followup_slot
                from public.availability_slots s
                where s.counsellor_id = v_aisyah_id
                  and s.start_at > now()
                  and not exists (
                      select 1 from public.appointments a
                      where a.slot_id = s.id
                        and a.status_code in ('pending', 'confirmed')
                  )
                  and not exists (
                      select 1
                      from public.appointments a
                      join public.availability_slots existing on existing.id = a.slot_id
                      where a.student_id = v_aina_id
                        and a.status_code in ('pending', 'confirmed')
                        and tstzrange(existing.start_at, existing.end_at, '[)')
                            && tstzrange(s.start_at, s.end_at, '[)')
                  )
                order by s.start_at
                limit 1;

                if v_followup_slot is not null then
                    insert into public.appointments (
                        student_id, counsellor_id, slot_id, mode_code,
                        concern_code, note, status_code, follow_up_of
                    )
                    values (
                        v_aina_id, v_aisyah_id, v_followup_slot, 'in_person',
                        'anxiety', null, 'pending', v_completed_aina
                    );
                end if;
            end if;
        end if;

        -- Faris pending session.
        if not exists (
            select 1
            from public.appointments a
            where a.student_id = v_faris_id
              and a.counsellor_id = v_daniel_id
              and a.status_code = 'pending'
        ) then
            select s.id into v_faris_slot
            from public.availability_slots s
            where s.counsellor_id = v_daniel_id
              and s.start_at > now()
              and not exists (
                  select 1 from public.appointments a
                  where a.slot_id = s.id
                    and a.status_code in ('pending', 'confirmed')
              )
              and not exists (
                  select 1
                  from public.appointments a
                  join public.availability_slots existing on existing.id = a.slot_id
                  where a.student_id = v_faris_id
                    and a.status_code in ('pending', 'confirmed')
                    and tstzrange(existing.start_at, existing.end_at, '[)')
                        && tstzrange(s.start_at, s.end_at, '[)')
              )
            order by s.start_at
            limit 1;

            if v_faris_slot is not null then
                insert into public.appointments (
                    student_id, counsellor_id, slot_id, mode_code,
                    concern_code, note, status_code
                )
                values (
                    v_faris_id, v_daniel_id, v_faris_slot, 'online',
                    'loneliness', 'I would like help feeling more connected on campus.',
                    'pending'
                );
            end if;
        end if;

        -- A cancelled historical session.
        if not exists (
            select 1
            from public.appointments a
            where a.student_id = v_faris_id
              and a.counsellor_id = v_aisyah_id
              and a.status_code = 'cancelled'
        ) then
            if not exists (
                select 1 from public.appointments a where a.slot_id = v_past_3_id
            ) then
                insert into public.appointments (
                    student_id, counsellor_id, slot_id, mode_code,
                    concern_code, note, status_code
                )
                values (
                    v_faris_id, v_aisyah_id, v_past_3_id, 'in_person',
                    'sleep', 'Cancelled by the student before the session.', 'cancelled'
                );
            end if;
        end if;

        -- Enquiries: supertype + the matching subtype row.
        select id into v_resource_grounding
        from public.resources
        where lower(title) = lower('5-Minute Grounding for Anxiety')
        limit 1;

        select id into v_resource_exam
        from public.resources
        where lower(title) = lower('Beating Exam Pressure')
        limit 1;

        if not exists (
            select 1 from public.enquiries where reference_code = 'ENQ-DEMO01'
        ) then
            insert into public.enquiries (
                reference_code, category_code, message, status, enquiry_type
            )
            values (
                'ENQ-DEMO01', 'academic_pressure',
                'I am worried about my results and do not want my name attached to this question. What support is available before the exam period?',
                'open', 'anonymous'
            )
            returning id into v_enquiry_id;

            insert into public.anonymous_enquiries (enquiry_id)
            values (v_enquiry_id);
        end if;

        if not exists (
            select 1 from public.enquiries where reference_code = 'ENQ-DEMO02'
        ) then
            insert into public.enquiries (
                reference_code, category_code, message, status, reply,
                recommended_resource_id, enquiry_type
            )
            values (
                'ENQ-DEMO02', 'anxiety',
                'I keep feeling panicky before presentations. Is there something I can try on my own first?',
                'answered',
                'Thank you for reaching out. Start with the five-minute grounding resource and practise it when you are calm. If the panic continues, you can book a session and we can work on it together.',
                v_resource_grounding, 'anonymous'
            )
            returning id into v_enquiry_id;

            insert into public.anonymous_enquiries (enquiry_id)
            values (v_enquiry_id);
        end if;

        if not exists (
            select 1 from public.enquiries where reference_code = 'ENQ-DEMO03'
        ) then
            insert into public.enquiries (
                reference_code, category_code, message, status, enquiry_type
            )
            values (
                'ENQ-DEMO03', 'sleep',
                'I have been sleeping badly for two weeks and it is affecting my classes. Could I speak to someone about a routine?',
                'open', 'identified'
            )
            returning id into v_enquiry_id;

            insert into public.identified_enquiries (enquiry_id, student_id)
            values (v_enquiry_id, v_aina_id);
        end if;

        if not exists (
            select 1 from public.enquiries where reference_code = 'ENQ-DEMO04'
        ) then
            insert into public.enquiries (
                reference_code, category_code, message, status, reply,
                recommended_resource_id, enquiry_type
            )
            values (
                'ENQ-DEMO04', 'academic_pressure',
                'I am falling behind on assignments and feel embarrassed to ask my lecturer. Where can I start?',
                'answered',
                'You are not alone in this. Try the exam pressure resource, then bring your module list to a counselling session so we can plan one step at a time.',
                v_resource_exam, 'identified'
            )
            returning id into v_enquiry_id;

            insert into public.identified_enquiries (enquiry_id, student_id)
            values (v_enquiry_id, v_faris_id);
        end if;

        execute 'alter table public.availability_slots enable trigger trg_availability_slot_guard';

        raise notice 'Demo data ready. Reference codes: ENQ-DEMO01 (open), ENQ-DEMO02 (answered), ENQ-DEMO03 (Aina, open), ENQ-DEMO04 (Faris, answered).';
    exception
        when others then
            execute 'alter table public.availability_slots enable trigger trg_availability_slot_guard';
            raise;
    end;
end;
$seed$;

commit;

notify pgrst, 'reload schema';
