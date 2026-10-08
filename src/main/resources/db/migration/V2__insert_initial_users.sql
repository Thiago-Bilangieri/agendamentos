-- A senha padrão para todos os utilizadores de teste é: password
-- O valor inserido é o hash BCrypt exato da palavra "password", necessário para quando o Spring Security for ativado.

INSERT INTO users (name, email, password, role) VALUES
                                                    ('Administrador Global', 'admin@agendamento.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HCGFGL.f.q.3K.R.g.z2a', 'ADMIN'),
                                                    ('Profissional Teste', 'profissional@agendamento.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HCGFGL.f.q.3K.R.g.z2a', 'PROFESSIONAL'),
                                                    ('Cliente Teste', 'cliente@agendamento.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HCGFGL.f.q.3K.R.g.z2a', 'CUSTOMER');