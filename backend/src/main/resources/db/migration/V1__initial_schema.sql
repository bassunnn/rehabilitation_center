CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    display_name VARCHAR(160) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    password_change_required BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE app_user_role (
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL,
    PRIMARY KEY (user_id, role)
);

CREATE TABLE refresh_session (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by UUID,
    user_agent VARCHAR(500),
    ip_address VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ
);

CREATE INDEX idx_refresh_user
    ON refresh_session(user_id);

CREATE INDEX idx_refresh_expires
    ON refresh_session(expires_at);

CREATE TABLE department (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200),
    capacity INTEGER,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE patient (
    id UUID PRIMARY KEY,
    external_id VARCHAR(100) UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    email VARCHAR(254),
    phone VARCHAR(32),
    birth_date DATE,
    department_id UUID REFERENCES department(id),
    status VARCHAR(32) NOT NULL,
    rehabilitation_score NUMERIC(5,2),
    requires_assistance BOOLEAN NOT NULL DEFAULT FALSE,
    risk_level VARCHAR(32) NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMPTZ,
    archived_by UUID REFERENCES app_user(id),
    created_by UUID REFERENCES app_user(id),
    updated_by UUID REFERENCES app_user(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_patient_rehabilitation_score
        CHECK (
            rehabilitation_score IS NULL
            OR (rehabilitation_score >= 0.00 AND rehabilitation_score <= 100.00)
        )
);

COMMENT ON COLUMN patient.rehabilitation_score
    IS 'Индекс реабилитационного потенциала / восстановления по 100-балльной шкале (0..100)';

COMMENT ON COLUMN patient.requires_assistance
    IS 'Логический признак: нуждается в постоянном уходе или сопровождении';

CREATE INDEX idx_patient_name
    ON patient(last_name, first_name, middle_name);

CREATE INDEX idx_patient_department
    ON patient(department_id);

CREATE INDEX idx_patient_status
    ON patient(status);

CREATE INDEX idx_patient_archived
    ON patient(archived);

CREATE INDEX idx_patient_risk
    ON patient(risk_level);

CREATE INDEX idx_patient_score
    ON patient(rehabilitation_score);

CREATE TABLE patient_tag (
    patient_id UUID NOT NULL REFERENCES patient(id) ON DELETE CASCADE,
    tag VARCHAR(50) NOT NULL,
    PRIMARY KEY (patient_id, tag)
);

CREATE INDEX idx_patient_tag_value
    ON patient_tag(tag);

CREATE TABLE patient_note (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patient(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES app_user(id),
    body VARCHAR(4000) NOT NULL,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_note_patient
    ON patient_note(patient_id, pinned DESC, created_at DESC);

CREATE TABLE audit_event (
    id UUID PRIMARY KEY,
    actor_id UUID,
    actor_email VARCHAR(254),
    event_type VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80),
    entity_id UUID,
    summary VARCHAR(1000) NOT NULL,
    correlation_id VARCHAR(100),
    ip_address VARCHAR(64),
    user_agent VARCHAR(500),
    occurred_at TIMESTAMPTZ NOT NULL,
    details_json TEXT
);

CREATE INDEX idx_audit_occurred
    ON audit_event(occurred_at DESC);

CREATE INDEX idx_audit_actor
    ON audit_event(actor_email);

CREATE INDEX idx_audit_entity
    ON audit_event(entity_type, entity_id);

