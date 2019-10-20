USE roons_bot;

CREATE TABLE metadata
(
    `key` varchar(128) CHARSET utf8 NOT NULL
        PRIMARY KEY,
    value varchar(128) CHARSET utf8 NOT NULL
);

