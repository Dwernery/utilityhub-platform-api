-- Library: each book's number in its series (books.series_position).
-- The schema isn't managed by the app, so run this by hand, once per database, BEFORE deploying
-- the API that uses it: that API reads the column, so without it the book list fails to load.
--   psql -d <database> -v ON_ERROR_STOP=1 -f db/2026-11-22-library-series-position.sql
-- Safe to re-run. One transaction: if anything fails, nothing changes.

BEGIN;

-- 1. The column: a whole number from 1 to 999, or empty (no series, or not known).
ALTER TABLE books ADD COLUMN IF NOT EXISTS series_position integer
  CONSTRAINT books_series_position_check CHECK (series_position BETWEEN 1 AND 999);

-- 2. Number each series 1, 2, 3… in the order its books were added (by id), which is the real
--    series order for the existing books. Only series with no numbers yet are filled in, so a
--    re-run never overwrites numbers you've set or fixed in the app.
WITH unnumbered AS (
  SELECT series_id FROM books WHERE series_id IS NOT NULL
  GROUP BY series_id HAVING count(series_position) = 0
), numbered AS (
  SELECT id, row_number() OVER (PARTITION BY series_id ORDER BY id) AS n
  FROM books WHERE series_id IN (SELECT series_id FROM unnumbered)
)
UPDATE books SET series_position = numbered.n FROM numbered WHERE books.id = numbered.id;

COMMIT;
