-- ============================================================================
-- Permanent delete for archived session records
-- ============================================================================
-- Apply once to an existing Supabase project:
--   Supabase Dashboard → SQL Editor → paste the whole file → Run
--
-- Policy: the owning counsellor may permanently delete a session record only
-- after that record has been archived. Unarchived records stay protected by
-- RLS and by a BEFORE DELETE trigger.
-- ============================================================================

create or replace function public.prevent_unarchived_session_record_delete()
returns trigger
language plpgsql
set search_path = public
as $function$
begin
    if not old.is_archived then
        raise exception 'Archive this session record before deleting it permanently.';
    end if;
    return old;
end;
$function$;

drop trigger if exists trg_session_record_delete_guard on public.session_records;
create trigger trg_session_record_delete_guard
before delete on public.session_records
for each row execute function public.prevent_unarchived_session_record_delete();

drop policy if exists session_records_delete_counsellor_archived on public.session_records;
create policy session_records_delete_counsellor_archived
on public.session_records for delete
to authenticated
using (
    public.app_role() = 'counsellor'
    and counsellor_id = public.app_counsellor_id()
    and is_archived = true
);

grant delete on public.session_records to authenticated;

notify pgrst, 'reload schema';
