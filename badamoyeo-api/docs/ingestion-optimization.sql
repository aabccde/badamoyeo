-- Badamoyeo ingestion schema fixes for an existing database.
--
-- New databases should use docs/schema.sql instead.
-- This file is for upgrading a database that was created before the
-- marine_forecasts.time_slot NOT NULL fix.

-- Required for spot upsert:
-- ALTER TABLE spots
-- ADD UNIQUE KEY uk_spots_experience_name (experience, name);

-- Pre-check: nullable time_slot values and duplicate forecast groups.
SELECT COUNT(*) AS null_time_slot_count
FROM marine_forecasts
WHERE time_slot IS NULL;

SELECT
	spot_id,
	forecast_date,
	COALESCE(time_slot, '') AS normalized_time_slot,
	variant_key,
	COUNT(*) AS duplicate_count
FROM marine_forecasts
GROUP BY spot_id, forecast_date, COALESCE(time_slot, ''), variant_key
HAVING COUNT(*) > 1;

-- MySQL allows multiple NULL values in a UNIQUE KEY, so rows with
-- time_slot IS NULL can bypass ON DUPLICATE KEY UPDATE and accumulate.
-- Keep the oldest row in each duplicated forecast group.
DELETE mf
FROM marine_forecasts mf
JOIN (
	SELECT
		MIN(id) AS keep_id,
		spot_id,
		forecast_date,
		COALESCE(time_slot, '') AS normalized_time_slot,
		variant_key
	FROM marine_forecasts
	GROUP BY spot_id, forecast_date, COALESCE(time_slot, ''), variant_key
	HAVING COUNT(*) > 1
) duplicates
	ON duplicates.spot_id = mf.spot_id
	AND duplicates.forecast_date = mf.forecast_date
	AND duplicates.normalized_time_slot = COALESCE(mf.time_slot, '')
	AND duplicates.variant_key = mf.variant_key
	AND mf.id != duplicates.keep_id;

UPDATE marine_forecasts
SET time_slot = ''
WHERE time_slot IS NULL;

ALTER TABLE marine_forecasts
MODIFY time_slot varchar(20) NOT NULL DEFAULT '';

-- Post-check: both values should be 0.
SELECT COUNT(*) AS null_time_slot_count
FROM marine_forecasts
WHERE time_slot IS NULL;

SELECT COUNT(*) AS duplicate_group_count
FROM (
	SELECT spot_id, forecast_date, time_slot, variant_key
	FROM marine_forecasts
	GROUP BY spot_id, forecast_date, time_slot, variant_key
	HAVING COUNT(*) > 1
) duplicate_groups;
