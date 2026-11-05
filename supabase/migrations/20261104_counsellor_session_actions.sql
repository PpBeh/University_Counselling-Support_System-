-- ============================================================================
-- Counsellor emergency actions
-- ============================================================================
-- Apply this once to an already-created Supabase project:
--   Supabase Dashboard → SQL Editor → paste this whole file → Run
--
-- Adds:
--   * pending booking decline (status cancelled, slot released)
--   * confirmed session emergency cancellation (status cancelled, slot released)
--   * confirmed session reschedule to another free slot with the same counsellor
--
-- No table changes are required. This replaces two RPCs and grants the new one.
-- The Android app must also be rebuilt so the new buttons appear.
-- ============================================================================

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

    select a.id, a.status, s.start_at
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
        (v_appointment.status = 'pending' and v_new_status in ('confirmed', 'cancelled'))
        or (v_appointment.status = 'confirmed' and v_new_status in ('completed', 'no_show', 'cancelled'))
    ) then
        raise exception 'Counsellors can confirm or decline a pending booking, complete / mark no-show a confirmed booking, or cancel a confirmed session.';
    end if;

    if v_new_status = 'confirmed' and v_appointment.start_at <= now() then
        raise exception 'That booking time has already passed and can no longer be confirmed.';
    end if;

    if v_new_status = 'cancelled' and v_appointment.start_at <= now() then
        raise exception 'This session has already started and cannot be cancelled from the app.';
    end if;

    update public.appointments
    set status = v_new_status
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

    select a.id, a.status, a.slot_id, a.student_id,
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

    if v_appointment.status <> 'confirmed' then
        raise exception 'Only confirmed sessions can be rescheduled by the counsellor.';
    end if;

    if v_appointment.old_start_at <= now() then
        raise exception 'This session has already started and cannot be rescheduled.';
    end if;

    if p_new_slot_id = v_appointment.slot_id then
        raise exception 'Please choose a different time slot.';
    end if;

    select s.id, s.counsellor_id, s.start_at, s.end_at,
           c.is_active as counsellor_active
    into v_new_slot
    from public.availability_slots s
    join public.counsellors c on c.id = s.counsellor_id
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
          and a.status in ('pending', 'confirmed')
    ) then
        raise exception 'That new time slot was just booked. Please choose another slot.';
    end if;

    if exists (
        select 1
        from public.appointments a
        join public.availability_slots s on s.id = a.slot_id
        where a.student_id = v_appointment.student_id
          and a.id <> p_appointment_id
          and a.status in ('pending', 'confirmed')
          and tstzrange(s.start_at, s.end_at, '[)')
              && tstzrange(v_new_slot.start_at, v_new_slot.end_at, '[)')
    ) then
        raise exception 'This student already has an appointment that overlaps this time.';
    end if;

    begin
        update public.appointments
        set slot_id = p_new_slot_id
        where id = p_appointment_id;
    exception
        when unique_violation then
            raise exception 'That new time slot was just booked. Please choose another slot.';
    end;
end;
$function$;

revoke execute on function public.update_appointment_status(uuid, text) from public, anon;
grant execute on function public.update_appointment_status(uuid, text) to authenticated;

revoke execute on function public.counsellor_reschedule_appointment(uuid, uuid) from public, anon;
grant execute on function public.counsellor_reschedule_appointment(uuid, uuid) to authenticated;

notify pgrst, 'reload schema';
