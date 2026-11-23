-- Catálogos iniciales adaptados al proyecto Starbucks.
MERGE INTO menu (nombre) KEY (nombre) VALUES
    ('Bebidas'),
    ('Alimentos'),
    ('Café en Grano');

MERGE INTO unidad_medida (nombre_medida) KEY (nombre_medida) VALUES
    ('g'),
    ('ml'),
    ('unidad');

-- Proveedores de referencia para que el módulo de compras tenga catálogos iniciales.
INSERT INTO proveedor (nombres)
SELECT 'Proveedor de muestra - Café y polvos'
WHERE NOT EXISTS (SELECT 1 FROM proveedor WHERE nombres = 'Proveedor de muestra - Café y polvos');

INSERT INTO proveedor (nombres)
SELECT 'Proveedor de muestra - Lácteos y alimentos'
WHERE NOT EXISTS (SELECT 1 FROM proveedor WHERE nombres = 'Proveedor de muestra - Lácteos y alimentos');

-- Insumos de referencia. Sus proveedores y unidades quedan relacionados por sus claves de H2.
INSERT INTO insumo (nombre, descripcion, stock_actual, stock_minimo, id_unidad, id_proveedor,
                    nombre_paquete, contenido_por_paquete, precio_paquete)
SELECT 'Café arábica tostado', 'Café para preparar bebidas y vender en grano.', 2500, 1000,
       u.id_unidad, p.id_proveedor, 'Bolsa de 1 kg', 1000, 62.00
FROM unidad_medida u, proveedor p
WHERE u.nombre_medida = 'g' AND p.nombres = 'Proveedor de muestra - Café y polvos'
  AND NOT EXISTS (SELECT 1 FROM insumo WHERE nombre = 'Café arábica tostado');

INSERT INTO insumo (nombre, descripcion, stock_actual, stock_minimo, id_unidad, id_proveedor,
                    nombre_paquete, contenido_por_paquete, precio_paquete)
SELECT 'Matcha en polvo', 'Polvo de té matcha para bebidas.', 900, 250,
       u.id_unidad, p.id_proveedor, 'Bolsa de 500 g', 500, 38.00
FROM unidad_medida u, proveedor p
WHERE u.nombre_medida = 'g' AND p.nombres = 'Proveedor de muestra - Café y polvos'
  AND NOT EXISTS (SELECT 1 FROM insumo WHERE nombre = 'Matcha en polvo');

INSERT INTO insumo (nombre, descripcion, stock_actual, stock_minimo, id_unidad, id_proveedor,
                    nombre_paquete, contenido_por_paquete, precio_paquete)
SELECT 'Cacao en polvo', 'Cacao utilizado en bebidas y preparaciones.', 600, 200,
       u.id_unidad, p.id_proveedor, 'Saco de 1 kg', 1000, 29.00
FROM unidad_medida u, proveedor p
WHERE u.nombre_medida = 'g' AND p.nombres = 'Proveedor de muestra - Café y polvos'
  AND NOT EXISTS (SELECT 1 FROM insumo WHERE nombre = 'Cacao en polvo');

INSERT INTO insumo (nombre, descripcion, stock_actual, stock_minimo, id_unidad, id_proveedor,
                    nombre_paquete, contenido_por_paquete, precio_paquete)
SELECT 'Té chai', 'Mezcla en polvo para bebidas chai.', 500, 100,
       u.id_unidad, p.id_proveedor, 'Bolsa de 500 g', 500, 22.00
FROM unidad_medida u, proveedor p
WHERE u.nombre_medida = 'g' AND p.nombres = 'Proveedor de muestra - Café y polvos'
  AND NOT EXISTS (SELECT 1 FROM insumo WHERE nombre = 'Té chai');

INSERT INTO insumo (nombre, descripcion, stock_actual, stock_minimo, id_unidad, id_proveedor,
                    nombre_paquete, contenido_por_paquete, precio_paquete)
SELECT 'Leche entera', 'Leche para preparar bebidas.', 5000, 1000,
       u.id_unidad, p.id_proveedor, 'Caja de 12 x 1 L', 12000, 66.00
FROM unidad_medida u, proveedor p
WHERE u.nombre_medida = 'ml' AND p.nombres = 'Proveedor de muestra - Lácteos y alimentos'
  AND NOT EXISTS (SELECT 1 FROM insumo WHERE nombre = 'Leche entera');

INSERT INTO insumo (nombre, descripcion, stock_actual, stock_minimo, id_unidad, id_proveedor,
                    nombre_paquete, contenido_por_paquete, precio_paquete)
SELECT 'Bebida de avena', 'Alternativa vegetal para bebidas.', 2000, 500,
       u.id_unidad, p.id_proveedor, 'Caja de 6 x 1 L', 6000, 48.00
FROM unidad_medida u, proveedor p
WHERE u.nombre_medida = 'ml' AND p.nombres = 'Proveedor de muestra - Lácteos y alimentos'
  AND NOT EXISTS (SELECT 1 FROM insumo WHERE nombre = 'Bebida de avena');

INSERT INTO insumo (nombre, descripcion, stock_actual, stock_minimo, id_unidad, id_proveedor,
                    nombre_paquete, contenido_por_paquete, precio_paquete)
SELECT 'Harina', 'Ingrediente para preparar alimentos.', 3000, 500,
       u.id_unidad, p.id_proveedor, 'Bolsa de 1 kg', 1000, 6.00
FROM unidad_medida u, proveedor p
WHERE u.nombre_medida = 'g' AND p.nombres = 'Proveedor de muestra - Lácteos y alimentos'
  AND NOT EXISTS (SELECT 1 FROM insumo WHERE nombre = 'Harina');

INSERT INTO insumo (nombre, descripcion, stock_actual, stock_minimo, id_unidad, id_proveedor,
                    nombre_paquete, contenido_por_paquete, precio_paquete)
SELECT 'Mantequilla', 'Ingrediente para preparar alimentos.', 1200, 300,
       u.id_unidad, p.id_proveedor, 'Paquete de 500 g', 500, 15.00
FROM unidad_medida u, proveedor p
WHERE u.nombre_medida = 'g' AND p.nombres = 'Proveedor de muestra - Lácteos y alimentos'
  AND NOT EXISTS (SELECT 1 FROM insumo WHERE nombre = 'Mantequilla');
