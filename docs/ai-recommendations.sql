-- Existing database migration for scheduled AI spot recommendations.

CREATE TABLE IF NOT EXISTS ai_spot_recommendations (
	id bigint NOT NULL AUTO_INCREMENT,
	experience varchar(30) NOT NULL,
	forecast_date date NOT NULL,
	spot_id bigint NOT NULL,
	rank_no int NOT NULL,
	reason varchar(500) NOT NULL,
	generated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (id),
	UNIQUE KEY uk_ai_recommendation_experience_date_rank (experience, forecast_date, rank_no),
	UNIQUE KEY uk_ai_recommendation_experience_date_spot (experience, forecast_date, spot_id),
	KEY idx_ai_recommendation_latest (experience, generated_at),
	CONSTRAINT fk_ai_recommendation_spot FOREIGN KEY (spot_id) REFERENCES spots (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ai_spot_analyses (
	id bigint NOT NULL AUTO_INCREMENT,
	spot_id bigint NOT NULL,
	forecast_id bigint NOT NULL,
	summary text NOT NULL,
	advantages json NOT NULL,
	disadvantages json NOT NULL,
	recommended tinyint(1) NOT NULL,
	recommendation_reason varchar(500) NOT NULL,
	safety_note varchar(500) NOT NULL,
	source_forecast_updated_at datetime NOT NULL,
	generated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (id),
	UNIQUE KEY uk_ai_spot_analysis_forecast (forecast_id),
	KEY idx_ai_spot_analysis_spot (spot_id),
	CONSTRAINT fk_ai_spot_analysis_spot FOREIGN KEY (spot_id) REFERENCES spots (id),
	CONSTRAINT fk_ai_spot_analysis_forecast FOREIGN KEY (forecast_id) REFERENCES marine_forecasts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
