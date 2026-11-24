-- Finance: accounts can be archived.
-- The schema isn't managed by the app (no ddl-auto, no migrations), so run this by hand,
-- once per database, BEFORE deploying the API that uses it:
--   psql -d <database> -f db/2026-11-23-finance-account-archived.sql
-- Safe to re-run. It runs in one transaction: if anything fails, nothing changes.
--
-- An archived account keeps every balance it has, so past months, totals and charts are
-- unchanged. It only stops being offered for new months: the apps leave it out of
-- "add existing", and "copy last month" doesn't bring it forward.

BEGIN;

ALTER TABLE accounts ADD COLUMN IF NOT EXISTS archived boolean NOT NULL DEFAULT false;

COMMIT;
