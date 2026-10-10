-- Os emails passam a ser guardados sempre em minúsculas (ver Emails.normalize).
-- Com o UNIQUE já existente, a restrição de minúsculas torna a unicidade insensível a maiúsculas.
UPDATE users SET email = lower(trim(email)) WHERE email <> lower(trim(email));
UPDATE customers SET email = lower(trim(email)) WHERE email <> lower(trim(email));

ALTER TABLE users ADD CONSTRAINT users_email_lowercase CHECK (email = lower(email));
ALTER TABLE customers ADD CONSTRAINT customers_email_lowercase CHECK (email = lower(email));
