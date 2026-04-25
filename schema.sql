-- Create tables for Roons Bot
CREATE TABLE IF NOT EXISTS app_user (
    id SERIAL PRIMARY KEY,
    generated_id VARCHAR(255) UNIQUE NOT NULL,
    last_seen TIMESTAMP
);

CREATE TABLE IF NOT EXISTS discord_bot_settings (
    id SERIAL PRIMARY KEY,
    guild_id VARCHAR(255) UNIQUE NOT NULL,
    volume INTEGER NOT NULL DEFAULT 100,
    followed_user VARCHAR(255),
    last_channel VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS discord_bot_user (
    id SERIAL PRIMARY KEY,
    discord_user_id VARCHAR(255) NOT NULL,
    guild_id VARCHAR(255) NOT NULL,
    token VARCHAR(255) NOT NULL,
    UNIQUE (discord_user_id, guild_id)
);

CREATE TABLE IF NOT EXISTS analytics_property (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES app_user(id),
    property VARCHAR(255) NOT NULL,
    value TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS metadata (
    "key" VARCHAR(255) PRIMARY KEY,
    value TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS dota_mod (
    "key" VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    size INTEGER NOT NULL,
    hash TEXT NOT NULL,
    download_url TEXT NOT NULL,
    info_url TEXT NOT NULL
);

-- Default metadata
INSERT INTO metadata ("key", value) VALUES ('admin_token', '12345');
