CREATE TABLE IF NOT EXISTS "app_user" (
	"id" BIGSERIAL,
	"cpf_hash" VARCHAR(128),
	"name" VARCHAR(150),
	"birth_date" DATE,
	"email" VARCHAR(255),
	"status" VARCHAR(20),
	"created_at" TIMESTAMP,
	"updated_at" TIMESTAMP,
	PRIMARY KEY("id")
);




CREATE TABLE IF NOT EXISTS "health_profile" (
	"id" BIGSERIAL,
	"user_id" BIGINT,
	"sex" VARCHAR(20),
	"height_cm" NUMERIC(5,2),
	"weight_kg" NUMERIC(6,2),
	"risk_factors" JSONB,
	"created_at" TIMESTAMP,
	"updated_at" TIMESTAMP,
	PRIMARY KEY("id")
);




CREATE TABLE IF NOT EXISTS "health_unit" (
	"id" BIGSERIAL,
	"name" VARCHAR(200),
	"cnes" VARCHAR(20),
	"type" VARCHAR(30),
	"status" VARCHAR(20),
	"created_at" TIMESTAMP,
	PRIMARY KEY("id")
);




CREATE TABLE IF NOT EXISTS "exam" (
	"id" BIGSERIAL,
	"user_id" BIGINT,
	"health_unit_id" BIGINT,
	"collected_at" TIMESTAMP,
	"released_at" TIMESTAMP,
	"status" VARCHAR(30),
	"external_reference" VARCHAR(100),
	"created_at" TIMESTAMP,
	PRIMARY KEY("id")
);




CREATE TABLE IF NOT EXISTS "exam_item" (
	"id" BIGSERIAL,
	"code" VARCHAR(50),
	"name" VARCHAR(150),
	"unit" VARCHAR(50),
	"category" VARCHAR(50),
	"description" VARCHAR(500),
	"active" BOOLEAN,
	"created_at" TIMESTAMP,
	PRIMARY KEY("id")
);




CREATE TABLE IF NOT EXISTS "reference_range" (
	"id" BIGSERIAL,
	"analyte_id" BIGINT,
	"sex" VARCHAR(20),
	"age_min_years" NUMERIC(5,2),
	"age_max_years" NUMERIC(5,2),
	"min_value" NUMERIC(12,4),
	"max_value" NUMERIC(12,4),
	"version" VARCHAR(30),
	"source" VARCHAR(500),
	"valid_from" DATE,
	"valid_until" DATE,
	"created_at" TIMESTAMP,
	PRIMARY KEY("id")
);




CREATE TABLE IF NOT EXISTS "exam_result" (
	"id" BIGSERIAL,
	"exam_id" BIGINT,
	"exam_item_id" BIGINT,
	"value_numeric" NUMERIC(14,5),
	"value_text" VARCHAR(255),
	"unit" VARCHAR(50),
	"flag" VARCHAR(30),
	"created_at" TIMESTAMP,
	PRIMARY KEY("id")
);




CREATE TABLE IF NOT EXISTS "blood_pressure" (
	"id" BIGSERIAL,
	"user_id" BIGINT,
	"measured_at" TIMESTAMP,
	"systolic" INTEGER,
	"diastolic" INTEGER,
	"pulse" INTEGER,
	"context" VARCHAR(50),
	"created_at" TIMESTAMP,
	PRIMARY KEY("id")
);




CREATE TABLE IF NOT EXISTS "risk_assessment" (
	"id" BIGSERIAL,
	"user_id" BIGINT,
	"exam_id" BIGINT,
	"score" NUMERIC(5,2),
	"level" VARCHAR(30),
	"rules_version" VARCHAR(30),
	"explanation" TEXT,
	"created_at" TIMESTAMP,
	PRIMARY KEY("id")
);




CREATE TABLE IF NOT EXISTS "notification" (
	"id" BIGSERIAL,
	"user_id" BIGINT,
	"type" VARCHAR(30),
	"title" VARCHAR(200),
	"message" TEXT,
	"scheduled_at" TIMESTAMP,
	"sent_at" TIMESTAMP,
	"status" VARCHAR(30),
	"created_at" TIMESTAMP,
	PRIMARY KEY("id")
);




CREATE TABLE IF NOT EXISTS "achievement" (
	"id" BIGSERIAL,
	"code" VARCHAR(50),
	"name" VARCHAR(100),
	"description" VARCHAR(500),
	"active" BOOLEAN,
	"created_at" TIMESTAMP,
	PRIMARY KEY("id")
);




CREATE TABLE IF NOT EXISTS "user_achievement" (
	"user_id" BIGINT,
	"achievement_id" BIGINT,
	"earned_at" TIMESTAMP,
	PRIMARY KEY("user_id", "achievement_id")
);



ALTER TABLE "health_profile"
ADD FOREIGN KEY("user_id") REFERENCES "app_user"("id")
ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE "exam"
ADD FOREIGN KEY("user_id") REFERENCES "app_user"("id")
ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE "exam"
ADD FOREIGN KEY("health_unit_id") REFERENCES "health_unit"("id")
ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE "reference_range"
ADD FOREIGN KEY("analyte_id") REFERENCES "exam_item"("id")
ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE "exam_result"
ADD FOREIGN KEY("exam_id") REFERENCES "exam"("id")
ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE "exam_result"
ADD FOREIGN KEY("exam_item_id") REFERENCES "exam_item"("id")
ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE "blood_pressure"
ADD FOREIGN KEY("user_id") REFERENCES "app_user"("id")
ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE "risk_assessment"
ADD FOREIGN KEY("user_id") REFERENCES "app_user"("id")
ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE "risk_assessment"
ADD FOREIGN KEY("exam_id") REFERENCES "exam"("id")
ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE "notification"
ADD FOREIGN KEY("user_id") REFERENCES "app_user"("id")
ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE "user_achievement"
ADD FOREIGN KEY("user_id") REFERENCES "app_user"("id")
ON UPDATE NO ACTION ON DELETE NO ACTION;
ALTER TABLE "user_achievement"
ADD FOREIGN KEY("achievement_id") REFERENCES "achievement"("id")
ON UPDATE NO ACTION ON DELETE NO ACTION;