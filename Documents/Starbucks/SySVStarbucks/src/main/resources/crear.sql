-- Perfiles fijos del sistema. MERGE permite ejecutar este archivo varias veces.
MERGE INTO perfil (nombre, codigo) KEY (codigo) VALUES
    ('Administrador', 'ADMINISTRADOR'),
    ('Vendedor', 'VENDEDOR');
