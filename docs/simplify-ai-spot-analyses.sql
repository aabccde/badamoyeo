-- Apply once to an existing database created with the previous AI analysis schema.

ALTER TABLE ai_spot_analyses
	DROP COLUMN summary,
	DROP COLUMN advantages,
	DROP COLUMN disadvantages,
	DROP COLUMN safety_note;
