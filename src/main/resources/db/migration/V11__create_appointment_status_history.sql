-- Trilha de auditoria dos estados de cada agendamento: quem mudou, quando e porquê.
-- from_status a NULL marca a criação. Os agendamentos anteriores a esta migração ficam sem histórico.
CREATE TABLE appointment_status_history (
    id BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT NOT NULL,
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    changed_by_user_id BIGINT NOT NULL,
    changed_by_role VARCHAR(50) NOT NULL,
    reason VARCHAR(500),
    changed_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_status_history_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE,
    CONSTRAINT fk_status_history_user FOREIGN KEY (changed_by_user_id) REFERENCES users(id),
    CONSTRAINT chk_status_history_role CHECK (changed_by_role IN ('CUSTOMER', 'PROFESSIONAL', 'ADMIN'))
);

CREATE INDEX idx_status_history_appointment_changed_at ON appointment_status_history(appointment_id, changed_at);
