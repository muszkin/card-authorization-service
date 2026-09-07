CREATE TABLE outbox_events
(
    id               UUID PRIMARY KEY,
    position         BIGINT GENERATED ALWAYS AS IDENTITY,
    authorization_id UUID        NOT NULL,
    type             VARCHAR(64) NOT NULL,
    payload          JSONB       NOT NULL,
    occurred_at      TIMESTAMPTZ NOT NULL,
    published_at     TIMESTAMPTZ
);

CREATE INDEX outbox_events_pending ON outbox_events (position) WHERE published_at IS NULL;
