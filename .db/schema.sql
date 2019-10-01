-- ROONS DISCORD BOT --
-- Database schema as of: 1.0.0-beta.4 --

CREATE DATABASE roons_bot;
USE roons_bot;

CREATE TABLE user
(
    id       int AUTO_INCREMENT
        PRIMARY KEY,
    user_id  varchar(128) NOT NULL,
    guild_id varchar(128) NOT NULL,
    token    varchar(128) NOT NULL,
    CONSTRAINT token_token_uindex
        UNIQUE (token)
);

CREATE TABLE user_event
(
    id         int AUTO_INCREMENT
        PRIMARY KEY,
    user_id    varchar(256) CHARSET utf8 NOT NULL,
    event_type varchar(64) CHARSET utf8  NOT NULL,
    event_data varchar(256) CHARSET utf8 NOT NULL
);
