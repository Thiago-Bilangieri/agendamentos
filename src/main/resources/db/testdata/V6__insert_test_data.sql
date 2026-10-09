-- Dados fictícios para testes.
-- A password de TODOS os utilizadores de teste é: password
-- Datas dos agendamentos são relativas ao dia em que a migração corre (passados e futuros).

-- O hash da V2 não correspondia a "password"; corrige os utilizadores iniciais
UPDATE users
SET password = '$2a$10$r.s9OgyKH/wBMuDcqM6PC.DK3Vn53zSl6ai.oTI57hP7Ks5Ln4fF.'
WHERE email IN ('admin@agendamento.com', 'profissional@agendamento.com', 'cliente@agendamento.com');

-- ===================== PRESTADORES =====================
INSERT INTO users (name, email, password, role, active, approval_status) VALUES
    ('Ana Ribeiro',     'ana.ribeiro@agendamento.com',     '$2a$10$r.s9OgyKH/wBMuDcqM6PC.DK3Vn53zSl6ai.oTI57hP7Ks5Ln4fF.', 'PROFESSIONAL', TRUE,  'APPROVED'),
    ('Bruno Matos',     'bruno.matos@agendamento.com',     '$2a$10$r.s9OgyKH/wBMuDcqM6PC.DK3Vn53zSl6ai.oTI57hP7Ks5Ln4fF.', 'PROFESSIONAL', TRUE,  'APPROVED'),
    ('Carla Nunes',     'carla.nunes@agendamento.com',     '$2a$10$r.s9OgyKH/wBMuDcqM6PC.DK3Vn53zSl6ai.oTI57hP7Ks5Ln4fF.', 'PROFESSIONAL', TRUE,  'APPROVED'),
    ('Diogo Ferreira',  'diogo.ferreira@agendamento.com',  '$2a$10$r.s9OgyKH/wBMuDcqM6PC.DK3Vn53zSl6ai.oTI57hP7Ks5Ln4fF.', 'PROFESSIONAL', TRUE,  'PENDING'),
    ('Elisa Marques',   'elisa.marques@agendamento.com',   '$2a$10$r.s9OgyKH/wBMuDcqM6PC.DK3Vn53zSl6ai.oTI57hP7Ks5Ln4fF.', 'PROFESSIONAL', TRUE,  'REJECTED'),
    ('Filipe Sousa',    'filipe.sousa@agendamento.com',    '$2a$10$r.s9OgyKH/wBMuDcqM6PC.DK3Vn53zSl6ai.oTI57hP7Ks5Ln4fF.', 'PROFESSIONAL', FALSE, 'APPROVED')
ON CONFLICT (email) DO NOTHING;

-- ===================== CLIENTES COM CONTA =====================
INSERT INTO users (name, email, password, role, active, approval_status) VALUES
    ('João Silva',      'joao.silva@email.com',            '$2a$10$r.s9OgyKH/wBMuDcqM6PC.DK3Vn53zSl6ai.oTI57hP7Ks5Ln4fF.', 'CUSTOMER', TRUE, 'APPROVED'),
    ('Maria Santos',    'maria.santos@email.com',          '$2a$10$r.s9OgyKH/wBMuDcqM6PC.DK3Vn53zSl6ai.oTI57hP7Ks5Ln4fF.', 'CUSTOMER', TRUE, 'APPROVED'),
    ('Pedro Costa',     'pedro.costa@email.com',           '$2a$10$r.s9OgyKH/wBMuDcqM6PC.DK3Vn53zSl6ai.oTI57hP7Ks5Ln4fF.', 'CUSTOMER', TRUE, 'APPROVED'),
    ('Sofia Almeida',   'sofia.almeida@email.com',         '$2a$10$r.s9OgyKH/wBMuDcqM6PC.DK3Vn53zSl6ai.oTI57hP7Ks5Ln4fF.', 'CUSTOMER', TRUE, 'APPROVED')
ON CONFLICT (email) DO NOTHING;

INSERT INTO customers (name, email, phone, notes, user_id)
SELECT u.name, u.email, v.phone, v.notes, u.id
FROM (VALUES
    ('joao.silva@email.com',    '912345001', NULL),
    ('maria.santos@email.com',  '912345002', 'Prefere marcações de manhã'),
    ('pedro.costa@email.com',   '912345003', NULL),
    ('sofia.almeida@email.com', '912345004', 'Alérgica a amoníaco')
) AS v(email, phone, notes)
JOIN users u ON u.email = v.email
ON CONFLICT (email) DO NOTHING;

-- Cliente criado pelo ADMIN, sem conta de utilizador
INSERT INTO customers (name, email, phone, notes) VALUES
    ('Rita Pereira', 'rita.pereira@email.com', '912345005', 'Cliente registada ao balcão')
ON CONFLICT (email) DO NOTHING;

-- ===================== SERVIÇOS =====================
INSERT INTO services (name, description, price, duration_minutes, active, professional_id)
SELECT v.name, v.description, v.price, v.duration, v.active, u.id
FROM (VALUES
    ('profissional@agendamento.com',   'Consulta Geral',       'Avaliação inicial',                   25.00, 30,  TRUE),
    ('ana.ribeiro@agendamento.com',    'Corte Feminino',       'Lavagem, corte e secagem',            35.00, 60,  TRUE),
    ('ana.ribeiro@agendamento.com',    'Coloração',            'Coloração completa',                  60.00, 120, TRUE),
    ('ana.ribeiro@agendamento.com',    'Brushing',             'Brushing liso ou ondulado',           20.00, 45,  TRUE),
    ('bruno.matos@agendamento.com',    'Corte Masculino',      'Corte à máquina e tesoura',           15.00, 30,  TRUE),
    ('bruno.matos@agendamento.com',    'Barba',                'Aparar e modelar barba',              10.00, 20,  TRUE),
    ('bruno.matos@agendamento.com',    'Corte + Barba',        'Corte masculino com barba',           22.00, 45,  TRUE),
    ('bruno.matos@agendamento.com',    'Pintura de Barba',     'Serviço descontinuado',               12.00, 30,  FALSE),
    ('carla.nunes@agendamento.com',    'Limpeza de Pele',      'Limpeza facial profunda',             45.00, 60,  TRUE),
    ('carla.nunes@agendamento.com',    'Depilação Pernas',     'Depilação a cera',                    25.00, 40,  TRUE),
    -- Prestador pendente e prestador inativo: não devem aparecer no catálogo dos clientes
    ('diogo.ferreira@agendamento.com', 'Massagem Relaxante',   'Massagem de corpo inteiro',           50.00, 60,  TRUE),
    ('filipe.sousa@agendamento.com',   'Treino Personalizado', 'Sessão individual de treino',         30.00, 60,  TRUE)
) AS v(professional_email, name, description, price, duration, active)
JOIN users u ON u.email = v.professional_email;

-- ===================== AGENDAMENTOS =====================
-- start_at = hoje às 00:00 + offset; end_at calculado pela duração do serviço
INSERT INTO appointments (customer_id, service_id, professional_id, start_at, end_at, status, notes)
SELECT c.id,
       s.id,
       s.professional_id,
       date_trunc('day', now())::timestamp + v.start_offset,
       date_trunc('day', now())::timestamp + v.start_offset + s.duration_minutes * INTERVAL '1 minute',
       v.status,
       v.notes
FROM (VALUES
    -- Ana Ribeiro
    ('maria.santos@email.com',  'ana.ribeiro@agendamento.com',  'Corte Feminino',       INTERVAL '-7 days 10:00', 'COMPLETED', NULL),
    ('sofia.almeida@email.com', 'ana.ribeiro@agendamento.com',  'Coloração',            INTERVAL '-3 days 14:00', 'NO_SHOW',   NULL),
    ('maria.santos@email.com',  'ana.ribeiro@agendamento.com',  'Brushing',             INTERVAL '1 day 09:00',   'CONFIRMED', NULL),
    ('maria.santos@email.com',  'ana.ribeiro@agendamento.com',  'Coloração',            INTERVAL '1 day 09:30',   'CANCELLED', 'Cancelado pela cliente'),
    ('sofia.almeida@email.com', 'ana.ribeiro@agendamento.com',  'Corte Feminino',       INTERVAL '1 day 10:00',   'SCHEDULED', NULL),
    ('cliente@agendamento.com', 'ana.ribeiro@agendamento.com',  'Coloração',            INTERVAL '2 days 14:00',  'SCHEDULED', NULL),
    -- Bruno Matos
    ('joao.silva@email.com',    'bruno.matos@agendamento.com',  'Corte + Barba',        INTERVAL '-5 days 11:00', 'COMPLETED', NULL),
    ('pedro.costa@email.com',   'bruno.matos@agendamento.com',  'Corte Masculino',      INTERVAL '-1 day 17:00',  'COMPLETED', NULL),
    ('joao.silva@email.com',    'bruno.matos@agendamento.com',  'Barba',                INTERVAL '1 day 11:00',   'SCHEDULED', NULL),
    ('rita.pereira@email.com',  'bruno.matos@agendamento.com',  'Corte Masculino',      INTERVAL '2 days 10:00',  'SCHEDULED', 'Marcado por telefone'),
    ('pedro.costa@email.com',   'bruno.matos@agendamento.com',  'Corte + Barba',        INTERVAL '3 days 18:00',  'CONFIRMED', NULL),
    -- Carla Nunes
    ('sofia.almeida@email.com', 'carla.nunes@agendamento.com',  'Limpeza de Pele',      INTERVAL '-10 days 15:00','COMPLETED', NULL),
    ('cliente@agendamento.com', 'carla.nunes@agendamento.com',  'Limpeza de Pele',      INTERVAL '1 day 15:00',   'CANCELLED', NULL),
    ('maria.santos@email.com',  'carla.nunes@agendamento.com',  'Depilação Pernas',     INTERVAL '4 days 16:00',  'SCHEDULED', NULL),
    -- Profissional Teste
    ('pedro.costa@email.com',   'profissional@agendamento.com', 'Consulta Geral',       INTERVAL '-2 days 09:00', 'COMPLETED', NULL),
    ('cliente@agendamento.com', 'profissional@agendamento.com', 'Consulta Geral',       INTERVAL '1 day 09:00',   'SCHEDULED', NULL),
    -- Filipe Sousa (hoje inativo; histórico)
    ('joao.silva@email.com',    'filipe.sousa@agendamento.com', 'Treino Personalizado', INTERVAL '-14 days 08:00','COMPLETED', NULL)
) AS v(customer_email, professional_email, service_name, start_offset, status, notes)
JOIN customers c ON c.email = v.customer_email
JOIN users p ON p.email = v.professional_email
JOIN services s ON s.name = v.service_name AND s.professional_id = p.id;
