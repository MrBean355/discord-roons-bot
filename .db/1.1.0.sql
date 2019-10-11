USE roons_bot;

RENAME TABLE user_event TO analytics_event;

ALTER TABLE analytics_event
    CHANGE user_id app_user_id varchar(256) CHARSET utf8 NOT NULL;

ALTER TABLE analytics_event
    ADD last_occurred DATETIME NULL;

RENAME TABLE user TO discord_bot_user;

ALTER TABLE discord_bot_user
    CHANGE user_id discord_user_id varchar(128) NOT NULL;

