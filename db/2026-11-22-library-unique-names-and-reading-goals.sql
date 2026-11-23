-- Library: unique author and series names, and yearly reading goals.
-- The schema isn't managed by the app (no ddl-auto, no migrations), so run this by hand,
-- once per database, BEFORE deploying the API that uses it:
--   psql -d <database> -f db/2026-11-22-library-unique-names-and-reading-goals.sql
-- Safe to re-run. It runs in one transaction: if anything fails, nothing changes.
--
-- To preview which names would be merged first:
--   SELECT lower(trim(full_name)), array_agg(id ORDER BY id) FROM authors GROUP BY 1 HAVING count(*) > 1;
--   SELECT lower(trim(name)),      array_agg(id ORDER BY id) FROM series  GROUP BY 1 HAVING count(*) > 1;

BEGIN;

-- 1. Merge duplicate authors (same name ignoring case and surrounding spaces) into the oldest
--    one: move their books to it, then delete the extras.
WITH d AS (
  SELECT id, min(id) OVER (PARTITION BY lower(trim(full_name))) AS keep_id FROM authors
)
UPDATE books SET author_id = d.keep_id FROM d WHERE books.author_id = d.id AND d.id <> d.keep_id;

WITH d AS (
  SELECT id, min(id) OVER (PARTITION BY lower(trim(full_name))) AS keep_id FROM authors
)
DELETE FROM authors USING d WHERE authors.id = d.id AND d.id <> d.keep_id;

-- 2. The same for series.
WITH d AS (
  SELECT id, min(id) OVER (PARTITION BY lower(trim(name))) AS keep_id FROM series
)
UPDATE books SET series_id = d.keep_id FROM d WHERE books.series_id = d.id AND d.id <> d.keep_id;

WITH d AS (
  SELECT id, min(id) OVER (PARTITION BY lower(trim(name))) AS keep_id FROM series
)
DELETE FROM series USING d WHERE series.id = d.id AND d.id <> d.keep_id;

-- 3. Tidy stray spaces, then make names unique ignoring case and spaces.
UPDATE authors SET full_name = trim(full_name) WHERE full_name <> trim(full_name);
UPDATE series  SET name      = trim(name)      WHERE name      <> trim(name);
CREATE UNIQUE INDEX IF NOT EXISTS authors_full_name_unique ON authors (lower(trim(full_name)));
CREATE UNIQUE INDEX IF NOT EXISTS series_name_unique       ON series  (lower(trim(name)));

-- 4. Yearly reading goals (Library → Metrics): how many books to finish in a year.
CREATE TABLE IF NOT EXISTS reading_goals (
  year   integer PRIMARY KEY CHECK (year BETWEEN 2000 AND 2200),
  target integer NOT NULL    CHECK (target BETWEEN 1 AND 1000)
);

COMMIT;
