-- =============================================================
-- Limpieza única de datos de prueba (ventas, préstamos, cuotas,
-- cobros, productos y clientes).
--
-- Alcance acordado: se ELIMINAN los datos de prueba en lugar de
-- migrarlos. Se CONSERVAN: business, usuario, role, plan,
-- subscription y subscription_event.
--
-- Notas de esquema (JPA, ddl-auto=update):
--  * Los cobros viven sobre la propia cuota (fee.paid, fee.date_payment,
--    fee.payment_method) y en la tabla payment: ambas se vacían.
--  * product_type (categorías) NO se toca: es estructura, no transacciones.
--  * stock_movement (libro de inventario) no tiene FKs: se vacía si existe,
--    porque sus movimientos explican variantes que este script borra.
--
-- El orden importa: las FKs son NO ACTION (sin cascade), así que se borra
-- de la hoja al tronco (payment -> fee/sale_document -> sale ->
-- variantes/atributos -> producto -> cliente).
--
-- Ejecutado el 2026-10-13 contra digital_payments (localhost):
-- 0 filas eliminadas (la base ya estaba limpia).
-- Ejecutado el 2026-11-13 contra digital_payments (localhost):
-- 1 payment, 56 fee, 1 sale_document, 5 sale, 7 variant_attribute,
-- 5 product_attribute, 7 product_variant, 5 product y 5 client eliminados.
-- =============================================================

BEGIN;

-- 1. Cobros (FK a fee)
DELETE FROM payment;

-- 2. Cuotas (incluye el estado de cobro de cada cuota)
DELETE FROM fee;

-- 3. Comprobantes emitidos (FK a sale)
DELETE FROM sale_document;

-- 4. Ventas y préstamos (referencian client/product_variant/product_type)
DELETE FROM sale;

-- 5. Libro de inventario (sin FKs: se vacía junto con el catálogo)
DO $$
BEGIN
    IF to_regclass('public.stock_movement') IS NOT NULL THEN
        DELETE FROM stock_movement;
    END IF;
END $$;

-- 6. Atributos de variante y de producto (FK a product_variant/product)
DELETE FROM variant_attribute;
DELETE FROM product_attribute;

-- 7. Variantes (SKU) y productos base
DELETE FROM product_variant;
DELETE FROM product;

-- 8. Historial de precios del catálogo (ya sin variantes que lo referencien)
DELETE FROM price_change;

-- 9. Clientes (la FK seller_id es a la propia tabla: se borran todos juntos)
DELETE FROM client;

COMMIT;

-- Verificación rápida:
-- SELECT 'sale' AS tabla, count(*) FROM sale
-- UNION ALL SELECT 'fee', count(*) FROM fee
-- UNION ALL SELECT 'payment', count(*) FROM payment
-- UNION ALL SELECT 'sale_document', count(*) FROM sale_document
-- UNION ALL SELECT 'product', count(*) FROM product
-- UNION ALL SELECT 'product_variant', count(*) FROM product_variant
-- UNION ALL SELECT 'client', count(*) FROM client;
