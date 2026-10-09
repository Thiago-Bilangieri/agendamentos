-- Garante na base de dados que um prestador nunca tem dois agendamentos sobrepostos.
-- A validação na aplicação não chega: dois pedidos simultâneos podem passar ambos a verificação
-- antes de qualquer um gravar. Esta restrição é atómica e resolve essa corrida.
-- Mesma regra de AppointmentRepository.findConflictingAppointments: agendamentos CANCELLED não ocupam horário.

-- Necessária para combinar "=" (professional_id) com "&&" (intervalos) no mesmo índice GiST
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- tsrange usa limites [início, fim): um agendamento pode começar exatamente quando o anterior acaba
ALTER TABLE appointments
    ADD CONSTRAINT appointments_no_overlap
    EXCLUDE USING gist (
        professional_id WITH =,
        tsrange(start_at, end_at) WITH &&
    )
    WHERE (status <> 'CANCELLED');
