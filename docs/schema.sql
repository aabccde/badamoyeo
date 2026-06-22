-- Badamoyeo API schema snapshot.
-- Generated from the local MySQL schema after the forecast time_slot cleanup.
-- Target database: MySQL 8 / InnoDB / utf8mb4.

CREATE TABLE IF NOT EXISTS users (
	id bigint NOT NULL AUTO_INCREMENT,
	email varchar(255) NOT NULL,
	password varchar(255) DEFAULT NULL,
	nickname varchar(50) NOT NULL,
	profile_image_url varchar(500) DEFAULT NULL,
	provider varchar(30) NOT NULL DEFAULT 'LOCAL',
	provider_id varchar(255) DEFAULT NULL,
	role varchar(20) NOT NULL DEFAULT 'USER',
	status varchar(20) NOT NULL DEFAULT 'ACTIVE',
	created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (id),
	UNIQUE KEY email (email),
	UNIQUE KEY nickname (nickname),
	UNIQUE KEY uk_users_provider_provider_id (provider, provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS spots (
	id bigint NOT NULL AUTO_INCREMENT,
	experience varchar(30) NOT NULL,
	name varchar(200) NOT NULL,
	lat decimal(10,7) NOT NULL,
	lng decimal(10,7) NOT NULL,
	external_place_code varchar(100) DEFAULT NULL,
	external_place_name varchar(200) DEFAULT NULL,
	region varchar(100) DEFAULT NULL,
	active tinyint(1) NOT NULL DEFAULT '1',
	post_count int NOT NULL DEFAULT '0',
	created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (id),
	UNIQUE KEY uk_spots_experience_name (experience, name),
	KEY idx_spots_experience (experience),
	KEY idx_spots_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS posts (
	id bigint NOT NULL AUTO_INCREMENT,
	spot_id bigint NOT NULL,
	user_id bigint NOT NULL,
	title varchar(200) NOT NULL,
	content text NOT NULL,
	view_count int NOT NULL DEFAULT '0',
	like_count int NOT NULL DEFAULT '0',
	comment_count int NOT NULL DEFAULT '0',
	status varchar(20) NOT NULL DEFAULT 'ACTIVE',
	created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (id),
	KEY fk_posts_user (user_id),
	KEY idx_posts_spot_created (spot_id, created_at),
	CONSTRAINT fk_posts_spot FOREIGN KEY (spot_id) REFERENCES spots (id),
	CONSTRAINT fk_posts_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS post_images (
	id bigint NOT NULL AUTO_INCREMENT,
	post_id bigint NOT NULL,
	image_url varchar(500) NOT NULL,
	sort_order int NOT NULL DEFAULT '0',
	created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (id),
	KEY fk_post_images_post (post_id),
	CONSTRAINT fk_post_images_post FOREIGN KEY (post_id) REFERENCES posts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS comments (
	id bigint NOT NULL AUTO_INCREMENT,
	post_id bigint NOT NULL,
	user_id bigint NOT NULL,
	parent_comment_id bigint DEFAULT NULL,
	content text NOT NULL,
	status varchar(20) NOT NULL DEFAULT 'ACTIVE',
	created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (id),
	KEY fk_comments_user (user_id),
	KEY fk_comments_parent (parent_comment_id),
	KEY idx_comments_post_created (post_id, created_at),
	CONSTRAINT fk_comments_parent FOREIGN KEY (parent_comment_id) REFERENCES comments (id),
	CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts (id),
	CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS favorite_spots (
	id bigint NOT NULL AUTO_INCREMENT,
	spot_id bigint NOT NULL,
	user_id bigint NOT NULL,
	created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (id),
	UNIQUE KEY uk_favorite_spots_spot_user (spot_id, user_id),
	KEY idx_favorite_spots_user (user_id),
	CONSTRAINT fk_favorite_spots_spot FOREIGN KEY (spot_id) REFERENCES spots (id),
	CONSTRAINT fk_favorite_spots_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS post_likes (
	id bigint NOT NULL AUTO_INCREMENT,
	post_id bigint NOT NULL,
	user_id bigint NOT NULL,
	created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (id),
	UNIQUE KEY uk_post_likes_post_user (post_id, user_id),
	KEY idx_post_likes_user (user_id),
	CONSTRAINT fk_post_likes_post FOREIGN KEY (post_id) REFERENCES posts (id),
	CONSTRAINT fk_post_likes_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS refresh_tokens (
	id bigint NOT NULL AUTO_INCREMENT,
	user_id bigint NOT NULL,
	token varchar(500) NOT NULL,
	expires_at datetime NOT NULL,
	revoked tinyint(1) NOT NULL DEFAULT '0',
	created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (id),
	UNIQUE KEY uk_refresh_tokens_token (token),
	KEY fk_refresh_tokens_user (user_id),
	CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS marine_forecasts (
	id bigint NOT NULL AUTO_INCREMENT,
	spot_id bigint NOT NULL,
	experience varchar(30) NOT NULL,
	forecast_date date NOT NULL,
	time_slot varchar(20) NOT NULL DEFAULT '',
	total_index varchar(30) DEFAULT NULL,
	weather varchar(50) DEFAULT NULL,
	tide varchar(100) DEFAULT NULL,
	variant_key varchar(100) NOT NULL DEFAULT '',
	metrics json DEFAULT NULL,
	raw_data json DEFAULT NULL,
	created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (id),
	UNIQUE KEY uk_forecast_spot_date_time_variant (spot_id, forecast_date, time_slot, variant_key),
	KEY idx_forecasts_experience_date (experience, forecast_date),
	CONSTRAINT fk_marine_forecasts_spot FOREIGN KEY (spot_id) REFERENCES spots (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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
