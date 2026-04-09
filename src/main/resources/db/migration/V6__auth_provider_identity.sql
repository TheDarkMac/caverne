ALTER TABLE app_users
ADD COLUMN auth_provider VARCHAR(32) NOT NULL DEFAULT 'LOCAL',
ADD COLUMN external_auth_id VARCHAR(255);

UPDATE app_users
SET auth_provider = 'LOCAL'
WHERE auth_provider IS NULL;

ALTER TABLE app_users
ADD CONSTRAINT uk_app_users_auth_provider_external_auth_id
UNIQUE (auth_provider, external_auth_id);
