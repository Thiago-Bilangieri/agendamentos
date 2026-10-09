-- Categorias de serviços (ex.: Cabelo, Barbearia, Estética), geridas pelo ADMIN
CREATE TABLE service_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_service_categories_name UNIQUE (name)
);

-- Opcional: um serviço pode não ter categoria
ALTER TABLE services
    ADD COLUMN category_id BIGINT,
    ADD CONSTRAINT fk_service_category FOREIGN KEY (category_id) REFERENCES service_categories(id);

CREATE INDEX idx_services_category ON services(category_id);
