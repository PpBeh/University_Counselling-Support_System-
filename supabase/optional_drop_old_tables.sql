-- ============================================================================
-- OPTIONAL, DESTRUCTIVE CLEANUP - DO NOT RUN UNTIL VERIFIED
-- ============================================================================
-- Run only after:
--   1. the OOP migration completed successfully,
--   2. the row-count/FK verification passed,
--   3. you have tested the new app on the migrated copy,
--   4. you have a Supabase backup or export of the legacy data.
--
-- `legacy` contains the old tables and old views. Dropping this schema is the
-- point of no return. Until then it is the rollback copy.
-- ============================================================================

begin;

do $cleanup$
begin
    if to_regclass('public.users') is null
       or to_regclass('public.students') is null
       or to_regclass('public.counsellors') is null then
        raise exception 'The new OOP schema is not present. Refusing to drop legacy.';
    end if;

    if to_regclass('legacy.profiles') is null then
        raise exception 'The legacy schema is not present, or cleanup was already run.';
    end if;
end;
$cleanup$;

drop schema legacy cascade;

commit;

notify pgrst, 'reload schema';
