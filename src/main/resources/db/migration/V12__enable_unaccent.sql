-- Pesquisa do catálogo sem distinguir acentos ("depilacao" encontra "Depilação").
-- unaccent é uma extensão standard do PostgreSQL (contrib) e é "trusted": o dono da base de dados pode criá-la.
CREATE EXTENSION IF NOT EXISTS unaccent;
