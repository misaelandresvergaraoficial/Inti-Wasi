
USE bd_intiwasi;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO Usuarios (IdUsuario, NomUsuario, Correo, Contrasena, Rol, Telefono, Estado, FechaRegistro) VALUES
(1, 'Misael Andrés Vergara Morales', 'admin.misael@intiwasi.pe', '$2b$10$U67Ifp1WzLPUy4wn2SNpWOAiD8XjUEOp7f3xTzVVnE.UPrePMiGrG', 'Administrador', '987654321', 1, '2026-01-10 08:00:00'),
(2, 'Kevin André de La Cruz Villugas', 'operador.kevin@intiwasi.pe', '$2b$10$/Ur6jAodWgmVOURyDBycXOAwFQekJG8v6bBcWTTl/xRZsKmZp9cYa', 'Operador de Almacén', '976543210', 1, '2026-01-10 08:15:00'),
(3, 'Alvaro Luis Suica Salcedo', 'admin.alvaro@intiwasi.pe', '$2b$10$iMCUIOpD5pzi6AXOqppIFeg4jIw0fTohM3mfWs.Kdp3fspfOl88he', 'Administrador', '965432109', 1, '2026-01-12 09:00:00'),
(4, 'Israel Rodrigo Ochoa Mejía', 'operador.israel@intiwasi.pe', '$2b$10$Tw4RpnbVHNees8Ubx2crfOxKwuaXl4VPuB6LN5fHjUOBBhpyO/2UG', 'Operador de Almacén', '954321098', 1, '2026-01-15 08:30:00'),
(5, 'Yul Alexander Retamozo Gutierrez', 'operador.yul@intiwasi.pe', '$2b$10$7boxVKYHY8eo9ghSVrHnCuBtMUtYkENS7ogS3UDREcHIDz5Wx0LNm', 'Operador de Almacén', '943210987', 1, '2026-01-20 09:15:00'),
(6, 'Carlos Mendoza Rivera (Inactivo)', 'carlos.mendoza@intiwasi.pe', '$2b$10$BI0Acxv8L39GvfNinUo2gOaBfgWJglJ15JtNP2M0g.i/Tkoh3I3ES', 'Operador de Almacén', '932109876', 0, '2026-01-05 10:00:00');

INSERT INTO Categorias (IdCategoria, NomCategoria, Estado) VALUES
(1, 'Procesadores', 1),
(2, 'Tarjetas de Video', 1),
(3, 'Memorias RAM', 1),
(4, 'Almacenamiento (SSD / HDD)', 1),
(5, 'Fuentes de Poder', 1),
(6, 'Placas Madre (Motherboards)', 1),
(7, 'Periféricos y Accesorios', 1);

INSERT INTO Proveedores (IdProveedor, NomProveedor, RUC, Contacto, Telefono, Direccion, Estado) VALUES
(1, 'Grupo Deltron S.A.', '20212345678', 'Jorge Del Solar', '014150000', 'Av. Manuel Echeandía 261, San Luis, Lima', 1),
(2, 'Ingram Micro Perú S.A.C.', '20334567891', 'Mariana Falconí', '015132200', 'Av. República de Panamá 3531, San Isidro, Lima', 1),
(3, 'Tech Data Perú S.A.C.', '20456789123', 'Roberto Chang', '016187500', 'Av. Javier Prado Este 444, San Isidro, Lima', 1),
(4, 'PC Link Distribuciones S.A.C.', '20567891234', 'Patricia Morales', '017123456', 'Av. Bolivia 148, Centro de Lima, Lima', 1),
(5, 'Intcomex del Perú S.A.C.', '20678912345', 'Ernesto Cárdenas', '017165000', 'Av. Argentina 2415, Cercado de Lima, Lima', 1),
(6, 'Distribuidora Hardware Global S.A.C.', '20789123456', 'Lucía Benavides', '013498877', 'Jr. Paruro 1120, Cercado de Lima, Lima', 0);

INSERT INTO Productos (IdProducto, SKU, NomProducto, IdCategoria, IdProveedor, Precio, StockMinimo, StockActual, Estado) VALUES
(1, 'PRO-AMD-5600X', 'Procesador AMD Ryzen 5 5600X 3.7GHz 6-Core', 1, 1, 680.00, 5, 0, 1),
(2, 'PRO-INT-13400F', 'Procesador Intel Core i5-13400F 2.5GHz 10-Core', 1, 2, 850.00, 4, 0, 1),
(3, 'GPU-RTX-4060', 'Tarjeta de Video ASUS Dual GeForce RTX 4060 8GB OC', 2, 1, 1450.00, 3, 0, 1),
(4, 'GPU-RX-7600XT', 'Tarjeta de Video Sapphire Pulse Radeon RX 7600 XT 16GB', 2, 5, 1620.00, 2, 0, 1),
(5, 'RAM-KNG-16GB32', 'Memoria RAM Kingston Fury Beast 16GB (1x16GB) DDR4 3200MHz', 3, 3, 175.00, 10, 0, 1),
(6, 'RAM-COR-32GB56', 'Memoria RAM Corsair Vengeance 32GB (2x16GB) DDR5 5600MHz', 3, 2, 430.00, 4, 0, 1),
(7, 'SSD-KNG-1TB-NV', 'Unidad SSD Kingston NV2 1TB PCIe 4.0 NVMe M.2', 4, 1, 245.00, 8, 0, 1),
(8, 'SSD-SAM-2TB990', 'Unidad SSD Samsung 990 PRO 2TB PCIe 4.0 NVMe M.2', 4, 4, 690.00, 3, 0, 1),
(9, 'PSU-COR-750W-G', 'Fuente de Poder Corsair RM750e 750W 80 Plus Gold Modular', 5, 2, 480.00, 4, 0, 1),
(10, 'MB-ASU-B550MP', 'Placa Madre ASUS TUF Gaming B550M-PLUS WiFi II', 6, 1, 560.00, 3, 0, 1),
(11, 'PER-LOG-G502X', 'Mouse Gamer Logitech G502 X HERO 25K Sensor', 7, 4, 290.00, 6, 0, 1),
(12, 'PER-RED-K552RGB', 'Teclado Mecánico Redragon Kumara K552 RGB Switch Blue', 7, 5, 160.00, 5, 0, 1);

INSERT INTO OrdenesCompra (IdOrden, IdProveedor, IdUsuario, FechaEmision, FechaEstimadaEntrega, Estado) VALUES
(1, 1, 1, '2026-02-01', '2026-02-05', 'Pendiente'),
(2, 2, 3, '2026-02-03', '2026-02-08', 'Pendiente'),
(3, 1, 1, '2026-02-10', '2026-02-15', 'Pendiente'),
(4, 3, 3, '2026-02-12', '2026-02-17', 'Pendiente'),
(5, 4, 1, '2026-02-18', '2026-02-23', 'Pendiente'),
(6, 5, 3, '2026-03-01', '2026-03-06', 'Pendiente'),
(7, 2, 1, '2026-03-02', '2026-03-07', 'Pendiente');

INSERT INTO DetalleOrdenCompra (IdDetalle, IdOrden, IdProducto, Cantidad, PrecioUnitario) VALUES
(1, 1, 1, 15, 620.00),
(2, 1, 3, 8, 1380.00),
(3, 1, 7, 20, 220.00),
(4, 1, 10, 10, 510.00),

(5, 2, 2, 12, 790.00),
(6, 2, 6, 14, 395.00),
(7, 2, 9, 10, 440.00),

(8, 3, 1, 10, 615.00),
(9, 3, 7, 15, 218.00),

(10, 4, 5, 30, 155.00),

(11, 5, 8, 6, 630.00),
(12, 5, 11, 15, 260.00),

(13, 6, 4, 5, 1500.00),
(14, 6, 12, 20, 140.00),

(15, 7, 2, 8, 800.00),
(16, 7, 9, 6, 445.00);


INSERT INTO Documentos (IdDocumento, TipoDocumento, IdUsuario, FechaEmision, Estado) VALUES
(1, 'Entrada', 2, '2026-02-05 10:30:00', 1);
INSERT INTO Entradas (IdEntrada, IdDocumento, IdOrden, DocumentoRef, Observaciones) VALUES
(1, 1, 1, 'GUIA-DELTRON-001-98451', 'Recepción conforme de lote principal');
INSERT INTO MovimientosInventario (IdMovimiento, IdDocumento, IdProducto, Cantidad) VALUES
(1, 1, 1, 15),
(2, 1, 3, 8),
(3, 1, 7, 20),
(4, 1, 10, 10);

INSERT INTO Documentos (IdDocumento, TipoDocumento, IdUsuario, FechaEmision, Estado) VALUES
(2, 'Entrada', 4, '2026-02-08 11:15:00', 1);
INSERT INTO Entradas (IdEntrada, IdDocumento, IdOrden, DocumentoRef, Observaciones) VALUES
(2, 2, 2, 'GUIA-INGRAM-004-11204', 'Ingreso verificado de procesadores Intel, memorias y fuentes');
INSERT INTO MovimientosInventario (IdMovimiento, IdDocumento, IdProducto, Cantidad) VALUES
(5, 2, 2, 12),
(6, 2, 6, 14),
(7, 2, 9, 10);

INSERT INTO Documentos (IdDocumento, TipoDocumento, IdUsuario, FechaEmision, Estado) VALUES
(3, 'Entrada', 2, '2026-02-15 14:00:00', 1);
INSERT INTO Entradas (IdEntrada, IdDocumento, IdOrden, DocumentoRef, Observaciones) VALUES
(3, 3, 3, 'GUIA-DELTRON-001-99820', 'Ingreso por reposición programada');
INSERT INTO MovimientosInventario (IdMovimiento, IdDocumento, IdProducto, Cantidad) VALUES
(8, 3, 1, 10),
(9, 3, 7, 15);

INSERT INTO Documentos (IdDocumento, TipoDocumento, IdUsuario, FechaEmision, Estado) VALUES
(4, 'Entrada', 5, '2026-02-17 09:45:00', 1);
INSERT INTO Entradas (IdEntrada, IdDocumento, IdOrden, DocumentoRef, Observaciones) VALUES
(4, 4, 4, 'GUIA-TECHDATA-002-33120', 'Lote de memorias Kingston DDR4 verificado');
INSERT INTO MovimientosInventario (IdMovimiento, IdDocumento, IdProducto, Cantidad) VALUES
(10, 4, 5, 30);

INSERT INTO Documentos (IdDocumento, TipoDocumento, IdUsuario, FechaEmision, Estado) VALUES
(5, 'Entrada', 4, '2026-02-23 16:20:00', 1);
INSERT INTO Entradas (IdEntrada, IdDocumento, IdOrden, DocumentoRef, Observaciones) VALUES
(5, 5, 5, 'GUIA-PCLINK-008-54210', 'Recepción de periféricos y unidades de estado sólido NVMe');
INSERT INTO MovimientosInventario (IdMovimiento, IdDocumento, IdProducto, Cantidad) VALUES
(11, 5, 8, 6),
(12, 5, 11, 15);


INSERT INTO Documentos (IdDocumento, TipoDocumento, IdUsuario, FechaEmision, Estado) VALUES
(6, 'Salida', 2, '2026-02-25 10:00:00', 1);
INSERT INTO Salidas (IdSalida, IdDocumento, Motivo, Observaciones) VALUES
(1, 6, 'Despacho/Venta', 'Despacho para pedido corporativo Nro 1042');
INSERT INTO MovimientosInventario (IdMovimiento, IdDocumento, IdProducto, Cantidad) VALUES
(13, 6, 1, 8),
(14, 6, 3, 4),
(15, 6, 7, 10);

INSERT INTO Documentos (IdDocumento, TipoDocumento, IdUsuario, FechaEmision, Estado) VALUES
(7, 'Salida', 4, '2026-02-26 15:30:00', 1);
INSERT INTO Salidas (IdSalida, IdDocumento, Motivo, Observaciones) VALUES
(2, 7, 'Despacho/Venta', 'Atención de orden de entrega cliente tienda física');
INSERT INTO MovimientosInventario (IdMovimiento, IdDocumento, IdProducto, Cantidad) VALUES
(16, 7, 5, 12),
(17, 7, 10, 4);

INSERT INTO Documentos (IdDocumento, TipoDocumento, IdUsuario, FechaEmision, Estado) VALUES
(8, 'Salida', 2, '2026-02-27 11:10:00', 1);
INSERT INTO Salidas (IdSalida, IdDocumento, Motivo, Observaciones) VALUES
(3, 8, 'Merma', 'Caja aplastada durante estiba con daño en el socket del empaque');
INSERT INTO MovimientosInventario (IdMovimiento, IdDocumento, IdProducto, Cantidad) VALUES
(18, 8, 2, 1);

INSERT INTO Documentos (IdDocumento, TipoDocumento, IdUsuario, FechaEmision, Estado) VALUES
(9, 'Salida', 5, '2026-02-28 17:00:00', 1);
INSERT INTO Salidas (IdSalida, IdDocumento, Motivo, Observaciones) VALUES
(4, 9, 'Despacho/Venta', 'Venta a distribuidor regional Arequipa');
INSERT INTO MovimientosInventario (IdMovimiento, IdDocumento, IdProducto, Cantidad) VALUES
(19, 9, 8, 2),
(20, 9, 11, 7);

INSERT INTO Documentos (IdDocumento, TipoDocumento, IdUsuario, FechaEmision, Estado) VALUES
(10, 'Salida', 4, '2026-03-01 12:00:00', 1);
INSERT INTO Salidas (IdSalida, IdDocumento, Motivo, Observaciones) VALUES
(5, 10, 'Otro', 'Extracción de componentes para banco de pruebas / test bench en showroom');
INSERT INTO MovimientosInventario (IdMovimiento, IdDocumento, IdProducto, Cantidad) VALUES
(21, 10, 6, 2),
(22, 10, 9, 1);


INSERT INTO Documentos (IdDocumento, TipoDocumento, IdUsuario, FechaEmision, Estado) VALUES
(11, 'Ajuste', 1, '2026-03-01 14:00:00', 1);
INSERT INTO Ajustes (IdAjuste, IdDocumento, TipoAjuste, Motivo, Observaciones) VALUES
(1, 11, 'Incremento', 'Conteo cíclico quincenal: se encontró 1 unidad extra en estante B-04 no registrada', 'Regularización aprobada por gerencia');
INSERT INTO MovimientosInventario (IdMovimiento, IdDocumento, IdProducto, Cantidad) VALUES
(23, 11, 5, 1);

INSERT INTO Documentos (IdDocumento, TipoDocumento, IdUsuario, FechaEmision, Estado) VALUES
(12, 'Ajuste', 3, '2026-03-01 15:30:00', 1);
INSERT INTO Ajustes (IdAjuste, IdDocumento, TipoAjuste, Motivo, Observaciones) VALUES
(2, 12, 'Decremento', 'Faltante de stock detectado durante auditoría física de estantería A-02', 'Diferencia en conteo de fuentes de poder');
INSERT INTO MovimientosInventario (IdMovimiento, IdDocumento, IdProducto, Cantidad) VALUES
(24, 12, 9, 1);

INSERT INTO Documentos (IdDocumento, TipoDocumento, IdUsuario, FechaEmision, Estado) VALUES
(13, 'Ajuste', 1, '2026-03-02 09:10:00', 1);
INSERT INTO Ajustes (IdAjuste, IdDocumento, TipoAjuste, Motivo, Observaciones) VALUES
(3, 13, 'Incremento', 'Reingreso tras peritaje técnico: producto operativo y con sello de garantía conforme', 'Unidad reincorporada al catálogo disponible');
INSERT INTO MovimientosInventario (IdMovimiento, IdDocumento, IdProducto, Cantidad) VALUES
(25, 13, 10, 1);

INSERT INTO Documentos (IdDocumento, TipoDocumento, IdUsuario, FechaEmision, Estado) VALUES
(14, 'Ajuste', 3, '2026-03-02 11:45:00', 1);
INSERT INTO Ajustes (IdAjuste, IdDocumento, TipoAjuste, Motivo, Observaciones) VALUES
(4, 14, 'Decremento', 'Conciliación de inventario: regularización de saldo por duplicidad de registro físico', 'Acta de conciliación Nro 2026-003');
INSERT INTO MovimientosInventario (IdMovimiento, IdDocumento, IdProducto, Cantidad) VALUES
(26, 14, 1, 2);

INSERT INTO Documentos (IdDocumento, TipoDocumento, IdUsuario, FechaEmision, Estado) VALUES
(15, 'Ajuste', 1, '2026-03-02 16:30:00', 1);
INSERT INTO Ajustes (IdAjuste, IdDocumento, TipoAjuste, Motivo, Observaciones) VALUES
(5, 15, 'Decremento', 'Siniestro menor: pin doblado en conector M.2 durante manipulación en depósito', 'Baja técnica autorizada');
INSERT INTO MovimientosInventario (IdMovimiento, IdDocumento, IdProducto, Cantidad) VALUES
(27, 15, 7, 1);


