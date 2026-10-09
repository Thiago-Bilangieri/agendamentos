-- Horário de trabalho semanal de cada prestador.
-- Um dia pode ter vários blocos (ex.: 09:00-13:00 e 14:00-19:00 para a pausa de almoço).
CREATE TABLE working_hours (
    id BIGSERIAL PRIMARY KEY,
    professional_id BIGINT NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    CONSTRAINT fk_working_hours_professional FOREIGN KEY (professional_id) REFERENCES users(id),
    CONSTRAINT chk_working_hours_range CHECK (start_time < end_time),
    CONSTRAINT chk_working_hours_day CHECK (day_of_week IN
        ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'))
);

CREATE INDEX idx_working_hours_professional_day ON working_hours(professional_id, day_of_week);
