CREATE TABLE tenants (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    api_key_hash VARCHAR(64)  NOT NULL UNIQUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL REFERENCES tenants(id),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'DEVELOPER', 'VIEWER')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE endpoints (
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  BIGINT        NOT NULL REFERENCES tenants(id),
    url        VARCHAR(2048) NOT NULL,
    secret     VARCHAR(128)  NOT NULL,
    active     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE events (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL REFERENCES tenants(id),
    type            VARCHAR(100) NOT NULL,
    payload         JSONB        NOT NULL,
    idempotency_key VARCHAR(100),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, idempotency_key)
);

CREATE TABLE deliveries (
    id            BIGSERIAL PRIMARY KEY,
    event_id      BIGINT      NOT NULL REFERENCES events(id),
    endpoint_id   BIGINT      NOT NULL REFERENCES endpoints(id),
    status        VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SUCCESS', 'DEAD')),
    attempt_count INT         NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE delivery_attempts (
    id           BIGSERIAL PRIMARY KEY,
    delivery_id  BIGINT      NOT NULL REFERENCES deliveries(id),
    attempt_no   INT         NOT NULL,
    status_code  INT,
    response_ms  BIGINT,
    error        TEXT,
    attempted_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_tenant_id_id      ON events (tenant_id, id DESC);
CREATE INDEX idx_deliveries_event         ON deliveries (event_id);
CREATE INDEX idx_deliveries_status_retry  ON deliveries (status, next_retry_at);
CREATE INDEX idx_attempts_delivery        ON delivery_attempts (delivery_id);
CREATE INDEX idx_endpoints_tenant         ON endpoints (tenant_id);