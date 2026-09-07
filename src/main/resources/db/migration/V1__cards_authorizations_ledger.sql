CREATE TABLE cards
(
    id                 UUID PRIMARY KEY,
    cardholder_id      UUID        NOT NULL,
    status             VARCHAR(16) NOT NULL,
    credit_limit_minor BIGINT      NOT NULL CHECK (credit_limit_minor > 0),
    currency           CHAR(3)     NOT NULL
);

CREATE TABLE authorizations
(
    id              UUID PRIMARY KEY,
    card_id         UUID         NOT NULL REFERENCES cards (id),
    amount_minor    BIGINT       NOT NULL CHECK (amount_minor > 0),
    currency        CHAR(3)      NOT NULL,
    merchant        VARCHAR(255) NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    decline_reason  VARCHAR(32),
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ  NOT NULL,
    expires_at      TIMESTAMPTZ
);

CREATE INDEX authorizations_by_card_and_time ON authorizations (card_id, created_at DESC);

-- Append-only: rows are inserted in "position" order and never changed. A correction is a new row.
CREATE TABLE ledger_entries
(
    id               UUID PRIMARY KEY,
    position         BIGINT GENERATED ALWAYS AS IDENTITY,
    card_id          UUID        NOT NULL REFERENCES cards (id),
    authorization_id UUID        NOT NULL REFERENCES authorizations (id),
    type             VARCHAR(16) NOT NULL,
    amount_minor     BIGINT      NOT NULL CHECK (amount_minor > 0),
    currency         CHAR(3)     NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL
);

CREATE INDEX ledger_entries_by_card ON ledger_entries (card_id, position);

CREATE FUNCTION reject_ledger_mutation() RETURNS trigger AS
$$
BEGIN
    RAISE EXCEPTION 'ledger_entries is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER ledger_entries_append_only
    BEFORE UPDATE OR DELETE
    ON ledger_entries
    FOR EACH ROW
EXECUTE FUNCTION reject_ledger_mutation();
