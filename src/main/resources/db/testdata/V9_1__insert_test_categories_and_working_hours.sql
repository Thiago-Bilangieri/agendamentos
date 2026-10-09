-- Dados fictícios para testes (perfil dev): categorias e horários de trabalho.

-- ===================== CATEGORIAS =====================
INSERT INTO service_categories (name, description) VALUES
    ('Cabelo',    'Cortes, coloração e penteados'),
    ('Barbearia', 'Cortes masculinos e cuidados com a barba'),
    ('Estética',  'Tratamentos de pele e depilação'),
    ('Bem-estar', 'Massagens e relaxamento'),
    ('Fitness',   'Treino e acompanhamento físico')
ON CONFLICT (name) DO NOTHING;

-- "Consulta Geral" fica sem categoria de propósito (a categoria é opcional)
UPDATE services s
SET category_id = c.id
FROM (VALUES
    ('Corte Feminino',       'Cabelo'),
    ('Coloração',            'Cabelo'),
    ('Brushing',             'Cabelo'),
    ('Corte Masculino',      'Barbearia'),
    ('Barba',                'Barbearia'),
    ('Corte + Barba',        'Barbearia'),
    ('Pintura de Barba',     'Barbearia'),
    ('Limpeza de Pele',      'Estética'),
    ('Depilação Pernas',     'Estética'),
    ('Massagem Relaxante',   'Bem-estar'),
    ('Treino Personalizado', 'Fitness')
) AS v(service_name, category_name)
JOIN service_categories c ON c.name = v.category_name
WHERE s.name = v.service_name;

-- ===================== HORÁRIOS DE TRABALHO =====================
INSERT INTO working_hours (professional_id, day_of_week, start_time, end_time)
SELECT u.id, d.day, v.start_time::time, v.end_time::time
FROM (VALUES
    -- Ana: terça a sábado, com pausa para almoço
    ('ana.ribeiro@agendamento.com',    'TUE-SAT', '09:00', '13:00'),
    ('ana.ribeiro@agendamento.com',    'TUE-SAT', '14:00', '19:00'),
    -- Bruno: segunda a sábado, horário contínuo
    ('bruno.matos@agendamento.com',    'MON-SAT', '10:00', '20:00'),
    -- Carla, Profissional Teste, Diogo e Filipe: dias úteis
    ('carla.nunes@agendamento.com',    'MON-FRI', '09:00', '18:00'),
    ('profissional@agendamento.com',   'MON-FRI', '09:00', '17:00'),
    ('diogo.ferreira@agendamento.com', 'MON-FRI', '09:00', '18:00'),
    ('filipe.sousa@agendamento.com',   'MON-FRI', '07:00', '12:00')
) AS v(email, days, start_time, end_time)
JOIN users u ON u.email = v.email
JOIN (VALUES
    ('MON-FRI', 'MONDAY'), ('MON-FRI', 'TUESDAY'), ('MON-FRI', 'WEDNESDAY'), ('MON-FRI', 'THURSDAY'), ('MON-FRI', 'FRIDAY'),
    ('MON-SAT', 'MONDAY'), ('MON-SAT', 'TUESDAY'), ('MON-SAT', 'WEDNESDAY'), ('MON-SAT', 'THURSDAY'), ('MON-SAT', 'FRIDAY'), ('MON-SAT', 'SATURDAY'),
    ('TUE-SAT', 'TUESDAY'), ('TUE-SAT', 'WEDNESDAY'), ('TUE-SAT', 'THURSDAY'), ('TUE-SAT', 'FRIDAY'), ('TUE-SAT', 'SATURDAY')
) AS d(days, day) ON d.days = v.days;
