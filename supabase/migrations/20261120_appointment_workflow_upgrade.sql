-- ============================================================================
-- University Counselling Appointment & Support System
-- Appointment workflow upgrade for an existing NEW OOP schema
--
-- USE THIS IF:
--   you already ran the previous OOP schema (public.appointments /
--   v_appointments exist) and do NOT want to recreate the project.
--
-- DO NOT USE THIS ON THE OLD profiles SCHEMA. Use
-- 20261120_oop_refactor.sql for that migration instead.
--
-- Adds:
--   * appointments.last_event_code + card notification metadata
--   * v_appointments.last_event_code
--   * student reschedule returns the appointment to pending for counsellor reconfirmation
--   * follow-up request events and student confirmation RPC
--   * confirmed -> pending transition used only by the student reschedule RPC
-- ============================================================================

begin;

alter table public.appointments
    add column if not exists last_event_code text not null default 'booked';

do $upgrade$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'appointments_last_event_check'
    ) then
        alter table public.appointments
            add constraint appointments_last_event_check
            check (last_event_code in (
                'booked',
                'rescheduled_by_student',
                'rescheduled_by_counsellor',
                'confirmed',
                'declined',
                'cancelled_by_student',
                'cancelled_by_counsellor',
                'completed',
                'no_show',
                'followup_requested',
                'followup_confirmed_by_student'
            ));
    end if;
end;
$upgrade$;

insert into public.appointment_status_transitions (from_status_code, to_status_code)
values ('confirmed', 'pending')
on conflict do nothing;
-- CREATE OR REPLACE VIEW cannot insert/reorder a column. Recreate this
-- read-only projection instead; its select grant is restored at the end.
drop view if exists public.v_appointments;

create view public.v_appointments as
select
    a.id,
    a.student_id,
    su.full_name as student_name,
    su.email as student_email,
    su.phone as student_phone,
    su.is_active as student_is_active,
    st.student_id as student_code,
    st.programme as student_programme,
    st.preferred_session_mode as student_preferred_mode,
    a.counsellor_id,
    cu.full_name as counsellor_name,
    cu.email as counsellor_email,
    cu.phone as counsellor_phone,
    cu.is_active as counsellor_is_active,
    c.bio as counsellor_bio,
    coalesce((
        select array_agg(ccs.label order by ccs.sort_order)
        from public.counsellor_specialisations cs
        join public.concern_categories ccs on ccs.code = cs.category_code
        where cs.counsellor_id = c.user_id
    ), '{}'::text[]) as counsellor_specialisations,
    coalesce((
        select array_agg(l.name order by l.name)
        from public.counsellor_languages cl
        join public.languages l on l.language_id = cl.language_id
        where cl.counsellor_id = c.user_id
    ), '{}'::text[]) as counsellor_languages,
    a.slot_id,
    s.start_at,
    s.end_at,
    a.mode_code,
    sm.label as mode_label,
    a.concern_code,
    cc.label as concern_category,
    a.note,
    a.status_code,
    ast.label as status_label,
    ast.is_terminal,
    a.follow_up_of,
    a.last_event_code,
    a.created_at,
    a.updated_at
from public.appointments a
join public.students st on st.user_id = a.student_id
join public.users su on su.id = st.user_id
join public.counsellors c on c.user_id = a.counsellor_id
join public.users cu on cu.id = c.user_id
join public.availability_slots s on s.id = a.slot_id
join public.session_modes sm on sm.code = a.mode_code
join public.concern_categories cc on cc.code = a.concern_code
join public.appointment_statuses ast on ast.code = a.status_code
where (public.app_role() = 'student' and a.student_id = public.app_user_id())
   or (public.app_role() = 'counsellor' and a.counsellor_id = public.app_counsellor_id())
   or public.app_role() = 'administrator';



create or replace function public.reschedule_appointment(
    p_appointment_id uuid,
    p_new_slot_id uuid
)
returns void
language plpgsql
security definer
set search_path = public, auth
as $function$
declare
    v_student_id uuid := public.app_user_id();
    v_user record;
    v_appointment record;
    v_new_slot record;
begin
    if v_student_id is null then
        raise exception 'Please sign in again.';
    end if;

    select u.id, u.user_type, u.is_active
    into v_user
    from public.users u
    where u.id = v_student_id
    for update;

    if not found or v_user.user_type <> 'student' then
        raise exception 'Only students can reschedule appointments.';
    end if;

    if not v_user.is_active then
        raise exception 'This account is inactive. Please contact the counselling office.';
    end if;

    select a.id, a.status_code, a.slot_id, a.counsellor_id,
           s.start_at as old_start_at
    into v_appointment
    from public.appointments a
    join public.availability_slots s on s.id = a.slot_id
    where a.id = p_appointment_id
      and a.student_id = v_student_id
    for update of a;

    if not found then
        raise exception 'That appointment could not be found.';
    end if;

    if v_appointment.status_code not in ('pending', 'confirmed') then
        raise exception 'Only pending or confirmed appointments can be rescheduled.';
    end if;

    if v_appointment.old_start_at < now() + interval '12 hours' then
        raise exception 'Appointments can only be rescheduled at least 12 hours in advance.';
    end if;

    if p_new_slot_id = v_appointment.slot_id then
        raise exception 'Please choose a different time slot.';
    end if;

    select s.id, s.counsellor_id, s.start_at, s.end_at,
           u.is_active as counsellor_active
    into v_new_slot
    from public.availability_slots s
    join public.counsellors c on c.user_id = s.counsellor_id
    join public.users u on u.id = c.user_id
    where s.id = p_new_slot_id
    for update of s;

    if not found then
        raise exception 'That new time slot could not be found.';
    end if;

    if not v_new_slot.counsellor_active then
        raise exception 'This counsellor is not accepting new appointments.';
    end if;

    if v_new_slot.start_at <= now() then
        raise exception 'That new time slot has already passed.';
    end if;

    if v_new_slot.counsellor_id <> v_appointment.counsellor_id then
        raise exception 'Please choose another slot with the same counsellor.';
    end if;

    if exists (
        select 1
        from public.appointments a
        where a.slot_id = p_new_slot_id
          and a.status_code in ('pending', 'confirmed')
    ) then
        raise exception 'That new time slot was just booked. Please choose another slot.';
    end if;

    if exists (
        select 1
        from public.appointments a
        join public.availability_slots s on s.id = a.slot_id
        where a.student_id = v_student_id
          and a.id <> p_appointment_id
          and a.status_code in ('pending', 'confirmed')
          and tstzrange(s.start_at, s.end_at, '[)')
              && tstzrange(v_new_slot.start_at, v_new_slot.end_at, '[)')
    ) then
        raise exception 'You already have another appointment that overlaps this time.';
    end if;

    begin
        update public.appointments
        set slot_id = p_new_slot_id,
            status_code = 'pending',
            last_event_code = 'rescheduled_by_student'
        where id = p_appointment_id;
    exception
        when unique_violation then
            raise exception 'That new time slot was just booked. Please choose another slot.';
    end;
end;
$function$;

create or replace function public.cancel_appointment(
    p_appointment_id uuid
)
returns void
language plpgsql
security definer
set search_path = public, auth
as $function$
declare
    v_student_id uuid := public.app_user_id();
    v_user record;
    v_appointment record;
begin
    if v_student_id is null then
        raise exception 'Please sign in again.';
    end if;

    select u.id, u.user_type, u.is_active
    into v_user
    from public.users u
    where u.id = v_student_id
    for update;

    if not found or v_user.user_type <> 'student' then
        raise exception 'Only students can cancel their own appointments.';
    end if;

    if not v_user.is_active then
        raise exception 'This account is inactive. Please contact the counselling office.';
    end if;

    select a.id, a.status_code, s.start_at
    into v_appointment
    from public.appointments a
    join public.availability_slots s on s.id = a.slot_id
    where a.id = p_appointment_id
      and a.student_id = v_student_id
    for update of a;

    if not found then
        raise exception 'That appointment could not be found.';
    end if;

    if v_appointment.status_code not in ('pending', 'confirmed') then
        raise exception 'Only pending or confirmed appointments can be cancelled.';
    end if;

    if v_appointment.start_at <= now() then
        raise exception 'This appointment has already started. Please contact the counselling office.';
    end if;

    update public.appointments
    set status_code = 'cancelled',
        last_event_code = 'cancelled_by_student'
    where id = p_appointment_id;
end;
$function$;

create or replace function public.update_appointment_status(
    p_appointment_id uuid,
    p_new_status text
)
returns void
language plpgsql
security definer
set search_path = public, auth
as $function$
declare
    v_counsellor_id uuid := public.app_counsellor_id();
    v_appointment record;
    v_new_status text := lower(btrim(coalesce(p_new_status, '')));
begin
    if v_counsellor_id is null then
        raise exception 'Only an active counsellor can update appointment status.';
    end if;

    select a.id, a.status_code, a.follow_up_of, a.last_event_code, s.start_at
    into v_appointment
    from public.appointments a
    join public.availability_slots s on s.id = a.slot_id
    where a.id = p_appointment_id
      and a.counsellor_id = v_counsellor_id
    for update of a;

    if not found then
        raise exception 'That appointment could not be found in your schedule.';
    end if;

    if not (
        (v_appointment.status_code = 'pending' and v_new_status in ('confirmed', 'cancelled'))
        or (v_appointment.status_code = 'confirmed' and v_new_status in ('completed', 'no_show', 'cancelled'))
    ) then
        raise exception 'Counsellors can confirm or decline a pending booking, complete / mark no-show a confirmed booking, or cancel a confirmed session.';
    end if;

    if v_new_status = 'confirmed'
       and v_appointment.follow_up_of is not null
       and v_appointment.last_event_code <> 'rescheduled_by_student' then
        raise exception 'A proposed follow-up must be confirmed by the student.';
    end if;

    if v_new_status = 'confirmed' and v_appointment.start_at <= now() then
        raise exception 'That booking time has already passed and can no longer be confirmed.';
    end if;

    if v_new_status = 'cancelled' and v_appointment.start_at <= now() then
        raise exception 'This session has already started and cannot be cancelled from the app.';
    end if;

    update public.appointments
    set status_code = v_new_status,
        last_event_code = case v_new_status
            when 'confirmed' then 'confirmed'
            when 'completed' then 'completed'
            when 'no_show' then 'no_show'
            when 'cancelled' then 'cancelled_by_counsellor'
            else 'confirmed'
        end
    where id = p_appointment_id;
end;
$function$;

create or replace function public.counsellor_reschedule_appointment(
    p_appointment_id uuid,
    p_new_slot_id uuid
)
returns void
language plpgsql
security definer
set search_path = public, auth
as $function$
declare
    v_counsellor_id uuid := public.app_counsellor_id();
    v_appointment record;
    v_new_slot record;
begin
    if v_counsellor_id is null then
        raise exception 'Only an active counsellor can reschedule a session.';
    end if;

    select a.id, a.status_code, a.slot_id, a.student_id,
           s.start_at as old_start_at
    into v_appointment
    from public.appointments a
    join public.availability_slots s on s.id = a.slot_id
    where a.id = p_appointment_id
      and a.counsellor_id = v_counsellor_id
    for update of a;

    if not found then
        raise exception 'That appointment could not be found in your schedule.';
    end if;

    if v_appointment.status_code <> 'confirmed' then
        raise exception 'Only confirmed sessions can be rescheduled by the counsellor.';
    end if;

    if v_appointment.old_start_at <= now() then
        raise exception 'This session has already started and cannot be rescheduled.';
    end if;

    if p_new_slot_id = v_appointment.slot_id then
        raise exception 'Please choose a different time slot.';
    end if;

    select s.id, s.counsellor_id, s.start_at, s.end_at,
           u.is_active as counsellor_active
    into v_new_slot
    from public.availability_slots s
    join public.counsellors c on c.user_id = s.counsellor_id
    join public.users u on u.id = c.user_id
    where s.id = p_new_slot_id
      and s.counsellor_id = v_counsellor_id
    for update of s;

    if not found then
        raise exception 'Please choose one of your own free slots.';
    end if;

    if not v_new_slot.counsellor_active then
        raise exception 'This counsellor is not accepting new appointments.';
    end if;

    if v_new_slot.start_at <= now() then
        raise exception 'That new time slot has already passed.';
    end if;

    if exists (
        select 1
        from public.appointments a
        where a.slot_id = p_new_slot_id
          and a.status_code in ('pending', 'confirmed')
    ) then
        raise exception 'That new time slot was just booked. Please choose another slot.';
    end if;

    if exists (
        select 1
        from public.appointments a
        join public.availability_slots s on s.id = a.slot_id
        where a.student_id = v_appointment.student_id
          and a.id <> p_appointment_id
          and a.status_code in ('pending', 'confirmed')
          and tstzrange(s.start_at, s.end_at, '[)')
              && tstzrange(v_new_slot.start_at, v_new_slot.end_at, '[)')
    ) then
        raise exception 'This student already has an appointment that overlaps this time.';
    end if;

    begin
        update public.appointments
        set slot_id = p_new_slot_id,
            last_event_code = 'rescheduled_by_counsellor'
        where id = p_appointment_id;
    exception
        when unique_violation then
            raise exception 'That new time slot was just booked. Please choose another slot.';
    end;
end;
$function$;

create or replace function public.create_followup_appointment(
    p_record_id uuid,
    p_slot_id uuid
)
returns uuid
language plpgsql
security definer
set search_path = public, auth
as $function$
declare
    v_counsellor_id uuid := public.app_counsellor_id();
    v_record record;
    v_slot record;
    v_user record;
    v_active_count integer;
    v_appointment_id uuid;
begin
    if v_counsellor_id is null then
        raise exception 'Only an active counsellor can schedule follow-ups.';
    end if;

    select r.id, r.appointment_id, r.student_id, r.is_archived,
           a.mode_code, a.concern_code
    into v_record
    from public.session_records r
    join public.appointments a on a.id = r.appointment_id
    where r.id = p_record_id
      and r.counsellor_id = v_counsellor_id;

    if not found then
        raise exception 'That session record could not be found.';
    end if;

    if v_record.is_archived then
        raise exception 'Archived session records cannot be used to schedule follow-ups.';
    end if;

    select u.id, u.user_type, u.is_active
    into v_user
    from public.users u
    where u.id = v_record.student_id
    for update;

    if not found or not v_user.is_active or v_user.user_type <> 'student' then
        raise exception 'This student account is no longer active.';
    end if;

    select s.id, s.counsellor_id, s.start_at, s.end_at,
           u.is_active as counsellor_active
    into v_slot
    from public.availability_slots s
    join public.counsellors c on c.user_id = s.counsellor_id
    join public.users u on u.id = c.user_id
    where s.id = p_slot_id
      and s.counsellor_id = v_counsellor_id
    for update of s;

    if not found then
        raise exception 'Please choose one of your own free slots.';
    end if;

    if not v_slot.counsellor_active then
        raise exception 'This counsellor is not accepting new appointments.';
    end if;

    if v_slot.start_at <= now() then
        raise exception 'That time slot has already passed.';
    end if;

    if exists (
        select 1
        from public.appointments a
        where a.slot_id = p_slot_id
          and a.status_code in ('pending', 'confirmed')
    ) then
        raise exception 'That time slot is already booked.';
    end if;

    select count(*)
    into v_active_count
    from public.appointments a
    join public.availability_slots s on s.id = a.slot_id
    where a.student_id = v_record.student_id
      and a.status_code in ('pending', 'confirmed')
      and s.start_at > now();

    if v_active_count >= 2 then
        raise exception 'This student already has 2 active upcoming appointments.';
    end if;

    if exists (
        select 1
        from public.appointments a
        join public.availability_slots s on s.id = a.slot_id
        where a.student_id = v_record.student_id
          and a.status_code in ('pending', 'confirmed')
          and tstzrange(s.start_at, s.end_at, '[)')
              && tstzrange(v_slot.start_at, v_slot.end_at, '[)')
    ) then
        raise exception 'This student already has an appointment that overlaps this time.';
    end if;

    begin
        insert into public.appointments (
            student_id, counsellor_id, slot_id, mode_code, concern_code,
            note, status_code, follow_up_of, last_event_code
        )
        values (
            v_record.student_id, v_counsellor_id, p_slot_id, v_record.mode_code,
            v_record.concern_code, null, 'pending', v_record.appointment_id,
            'followup_requested'
        )
        returning id into v_appointment_id;
    exception
        when unique_violation then
            raise exception 'That time slot was just booked. Please choose another slot.';
    end;

    return v_appointment_id;
end;
$function$;

create or replace function public.confirm_followup_appointment(
    p_appointment_id uuid
)
returns void
language plpgsql
security definer
set search_path = public, auth
as $function$
declare
    v_student_id uuid := public.app_user_id();
    v_appointment record;
begin
    if v_student_id is null then
        raise exception 'Please sign in again.';
    end if;

    select a.id, a.status_code, a.follow_up_of, a.last_event_code, s.start_at
    into v_appointment
    from public.appointments a
    join public.availability_slots s on s.id = a.slot_id
    where a.id = p_appointment_id
      and a.student_id = v_student_id
    for update of a;

    if not found then
        raise exception 'That follow-up could not be found.';
    end if;

    if v_appointment.follow_up_of is null then
        raise exception 'Only a follow-up appointment can be confirmed here.';
    end if;

    if v_appointment.last_event_code <> 'followup_requested' then
        raise exception 'This follow-up was rescheduled and must be reconfirmed by the counsellor.';
    end if;

    if v_appointment.status_code <> 'pending' then
        raise exception 'This follow-up is no longer waiting for confirmation.';
    end if;

    if v_appointment.start_at <= now() then
        raise exception 'That follow-up time has already passed.';
    end if;

    update public.appointments
    set status_code = 'confirmed',
        last_event_code = 'followup_confirmed_by_student'
    where id = p_appointment_id;
end;
$function$;


grant execute on function public.confirm_followup_appointment(uuid) to authenticated;
grant select on public.v_appointments to authenticated;

notify pgrst, 'reload schema';

commit;