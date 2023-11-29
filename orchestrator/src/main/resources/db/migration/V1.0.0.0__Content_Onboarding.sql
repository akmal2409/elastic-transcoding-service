CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE raw_media
(
    id      UUID PRIMARY KEY,
    name    VARCHAR(255) NOT NULL,
    unboxed BOOLEAN DEFAULT FALSE,
    version BIGINT  DEFAULT 0
);

CREATE TABLE unboxing_job
(
    id           UUID PRIMARY KEY,
    raw_media_id UUID        NOT NULL,
    status       VARCHAR(40) NOT NULL,
    started_at   TIMESTAMP   NOT NULL,
    completed_at TIMESTAMP,
    version      INTEGER,

    FOREIGN KEY (raw_media_id) REFERENCES raw_media(id)
)
