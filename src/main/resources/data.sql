-- Datos iniciales requeridos por la aplicacion.
-- Idempotente: se ejecuta en cada arranque, no duplica roles.

INSERT INTO role (name)
SELECT 'ROLE_USER'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE name = 'ROLE_USER');

INSERT INTO role (name)
SELECT 'ROLE_ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE name = 'ROLE_ADMIN');

-- Plan de suscripcion unico
INSERT INTO plan (name, description, price, currency, trial_days, active)
SELECT 'Plan Mensual', 'Gestion de Cobros y Ventas - Suscripcion mensual', 15000, 'ARS', 14, true
WHERE NOT EXISTS (SELECT 1 FROM plan WHERE name = 'Plan Mensual');
