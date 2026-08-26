-- public.achievement definição

-- Drop table

-- DROP TABLE public.achievement;

CREATE TABLE IF NOT EXISTS public.achievement (
	id bigserial NOT NULL,
	code varchar(50) NULL,
	"name" varchar(100) NULL,
	description varchar(500) NULL,
	active bool NULL,
	created_at timestamp NULL,
	CONSTRAINT achievement_pkey PRIMARY KEY (id)
);


-- public.app_user definição

-- Drop table

-- DROP TABLE public.app_user;

CREATE TABLE IF NOT EXISTS public.app_user (
	id bigserial NOT NULL,
	cpf_hash varchar(128) NULL,
	"name" varchar(150) NULL,
	birth_date date NULL,
	email varchar(255) NULL,
	status varchar(20) NULL,
	created_at timestamp NULL,
	updated_at timestamp NULL,
	CONSTRAINT app_user_pkey PRIMARY KEY (id)
);


-- public.exam_item definição

-- Drop table

-- DROP TABLE public.exam_item;

CREATE TABLE IF NOT EXISTS public.exam_item (
	id bigserial NOT NULL,
	code varchar(50) NULL,
	"name" varchar(150) NULL,
	unit varchar(50) NULL,
	category varchar(50) NULL,
	description varchar(500) NULL,
	active bool NULL,
	created_at timestamp NULL,
	CONSTRAINT exam_item_pkey PRIMARY KEY (id)
);


-- public.health_unit definição

-- Drop table

-- DROP TABLE public.health_unit;

CREATE TABLE IF NOT EXISTS public.health_unit (
	id bigserial NOT NULL,
	"name" varchar(200) NULL,
	cnes varchar(20) NULL,
	"type" varchar(30) NULL,
	status varchar(20) NULL,
	created_at timestamp NULL,
	CONSTRAINT health_unit_pkey PRIMARY KEY (id)
);


-- public.exam definição

-- Drop table

-- DROP TABLE public.exam;

CREATE TABLE IF NOT EXISTS public.exam (
	id bigserial NOT NULL,
	user_id int8 NULL,
	health_unit_id int8 NULL,
	collected_at timestamp NULL,
	released_at timestamp NULL,
	status varchar(30) NULL,
	external_reference varchar(100) NULL,
	created_at timestamp NULL,
	CONSTRAINT exam_pkey PRIMARY KEY (id),
	CONSTRAINT exam_health_unit_id_fkey FOREIGN KEY (health_unit_id) REFERENCES public.health_unit(id),
	CONSTRAINT exam_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.app_user(id)
);


-- public.exam_result definição

-- Drop table

-- DROP TABLE public.exam_result;

CREATE TABLE IF NOT EXISTS public.exam_result (
	id bigserial NOT NULL,
	exam_id int8 NULL,
	exam_item_id int8 NULL,
	value_numeric numeric(14, 5) NULL,
	value_text varchar(255) NULL,
	unit varchar(50) NULL,
	flag varchar(30) NULL,
	created_at timestamp NULL,
	CONSTRAINT exam_result_pkey PRIMARY KEY (id),
	CONSTRAINT exam_result_exam_id_fkey FOREIGN KEY (exam_id) REFERENCES public.exam(id),
	CONSTRAINT exam_result_exam_item_id_fkey FOREIGN KEY (exam_item_id) REFERENCES public.exam_item(id)
);


-- public.health_profile definição

-- Drop table

-- DROP TABLE public.health_profile;

CREATE TABLE IF NOT EXISTS public.health_profile (
	id bigserial NOT NULL,
	user_id int8 NULL,
	sex varchar(20) NULL,
	height_cm numeric(5, 2) NULL,
	weight_kg numeric(6, 2) NULL,
	risk_factors jsonb NULL,
	exam_periodicity varchar(20) NOT NULL DEFAULT 'YEARLY' ,
	created_at timestamp NULL,
	updated_at timestamp NULL,
	CONSTRAINT health_profile_pkey PRIMARY KEY (id),
	CONSTRAINT health_profile_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.app_user(id)
);


-- public.notification definição

-- Drop table

-- DROP TABLE public.notification;

CREATE TABLE IF NOT EXISTS public.notification (
	id bigserial NOT NULL,
	user_id int8 NULL,
	"type" varchar(30) NULL,
	title varchar(200) NULL,
	message text NULL,
	scheduled_at timestamp NULL,
	sent_at timestamp NULL,
	status varchar(30) NULL,
	created_at timestamp NULL,
	CONSTRAINT notification_pkey PRIMARY KEY (id),
	CONSTRAINT notification_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.app_user(id)
);


-- public.reference_range definição

-- Drop table

-- DROP TABLE public.reference_range;

CREATE TABLE IF NOT EXISTS public.reference_range (
	id bigserial NOT NULL,
	exam_item_id int8 NULL,
	sex varchar(20) NULL,
	age_min_years numeric(5, 2) NULL,
	age_max_years numeric(5, 2) NULL,
	"version" varchar(30) NULL,
	"source" varchar(500) NULL,
	valid_from date NULL,
	valid_until date NULL,
	created_at timestamp NULL,
	CONSTRAINT reference_range_pkey PRIMARY KEY (id),
	CONSTRAINT reference_range_exam_item_id_fkey FOREIGN KEY (exam_item_id) REFERENCES public.exam_item(id)
);


-- public.risk_assessment definição

-- Drop table

-- DROP TABLE public.risk_assessment;

CREATE TABLE IF NOT EXISTS public.risk_assessment (
	id bigserial NOT NULL,
	user_id int8 NULL,
	exam_id int8 NULL,
	score numeric(5, 2) NULL,
	"level" varchar(30) NULL,
	rules_version varchar(30) NULL,
	explanation text NULL,
	created_at timestamp NULL,
	CONSTRAINT risk_assessment_pkey PRIMARY KEY (id),
	CONSTRAINT risk_assessment_exam_id_fkey FOREIGN KEY (exam_id) REFERENCES public.exam(id),
	CONSTRAINT risk_assessment_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.app_user(id)
);


-- public."rule" definição

-- Drop table

-- DROP TABLE public."rule";

CREATE TABLE IF NOT EXISTS public."rule" (
	id bigserial NOT NULL,
	reference_range_id int8 NULL,
	min_value numeric(12, 4) NULL,
	max_value numeric(12, 4) NULL,
	min_inclusive bool NULL,
	max_inclusive bool NULL,
	"level" varchar(30) NULL,
	score numeric(4, 2) NULL,
	description varchar(500) NULL,
	"version" varchar(30) NULL,
	active bool NULL,
	created_at timestamp NULL,
	CONSTRAINT rule_pkey PRIMARY KEY (id),
	CONSTRAINT rule_reference_range_id_fkey FOREIGN KEY (reference_range_id) REFERENCES public.reference_range(id)
);


-- public.user_achievement definição

-- Drop table

-- DROP TABLE public.user_achievement;

CREATE TABLE IF NOT EXISTS public.user_achievement (
	user_id int8 NOT NULL,
	achievement_id int8 NOT NULL,
	earned_at timestamp NULL,
	CONSTRAINT user_achievement_pkey PRIMARY KEY (user_id, achievement_id),
	CONSTRAINT user_achievement_achievement_id_fkey FOREIGN KEY (achievement_id) REFERENCES public.achievement(id),
	CONSTRAINT user_achievement_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.app_user(id)
);