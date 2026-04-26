-- OAuth2/OIDC support: allow password-less users and store provider identity

ALTER TABLE users
    ALTER COLUMN password DROP NOT NULL;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email            VARCHAR(255),
    ADD COLUMN IF NOT EXISTS provider         VARCHAR(50),
    ADD COLUMN IF NOT EXISTS provider_user_id VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS users_provider_uid_idx
    ON users (provider, provider_user_id)
    WHERE provider IS NOT NULL;
