CREATE TABLE appointments (
                              id BIGSERIAL PRIMARY KEY,
                              customer_id BIGINT NOT NULL,
                              service_id BIGINT NOT NULL,
                              professional_id BIGINT NOT NULL,
                              start_at TIMESTAMP NOT NULL,
                              end_at TIMESTAMP NOT NULL,
                              status VARCHAR(50) NOT NULL,
                              notes TEXT,
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP,

    -- Aqui definimos as Chaves Estrangeiras para garantir que não inserimos IDs falsos
                              CONSTRAINT fk_appointment_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
                              CONSTRAINT fk_appointment_service FOREIGN KEY (service_id) REFERENCES services(id),
                              CONSTRAINT fk_appointment_professional FOREIGN KEY (professional_id) REFERENCES users(id)
);