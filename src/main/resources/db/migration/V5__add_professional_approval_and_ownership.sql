-- Estado de aprovação dos utilizadores (prestadores começam PENDING; os existentes ficam aprovados)
ALTER TABLE users
    ADD COLUMN approval_status VARCHAR(50) NOT NULL DEFAULT 'APPROVED';

-- Cada cliente pode estar ligado a uma conta de utilizador (1:1)
ALTER TABLE customers
    ADD COLUMN user_id BIGINT UNIQUE,
    ADD CONSTRAINT fk_customer_user FOREIGN KEY (user_id) REFERENCES users(id);

UPDATE customers c
SET user_id = u.id
FROM users u
WHERE u.email = c.email
  AND u.role = 'CUSTOMER';

-- Garante que o cliente de teste tem o seu registo de Customer
INSERT INTO customers (name, email, phone, user_id)
SELECT u.name, u.email, '000000000', u.id
FROM users u
WHERE u.email = 'cliente@agendamento.com'
  AND NOT EXISTS (SELECT 1 FROM customers c WHERE c.email = u.email);

-- Cada serviço pertence a um prestador; os serviços existentes passam para o profissional de teste
ALTER TABLE services
    ADD COLUMN professional_id BIGINT,
    ADD CONSTRAINT fk_service_professional FOREIGN KEY (professional_id) REFERENCES users(id);

UPDATE services
SET professional_id = (SELECT id FROM users WHERE role = 'PROFESSIONAL' ORDER BY id LIMIT 1)
WHERE professional_id IS NULL;

ALTER TABLE services
    ALTER COLUMN professional_id SET NOT NULL;
