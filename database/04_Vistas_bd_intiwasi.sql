-- =========================================================================
-- Script  : 04_Vistas_bd_intiwasi.sql   (NUEVO en la version 2)
-- Proyecto: Distribuidora Inti Wasi S.A.C.
-- =========================================================================

USE bd_intiwasi;

-- =========================================================================
-- VW_Kardex - Historial completo de movimientos
-- Entrega la cantidad CON SIGNO y el motivo ya resuelto, sin importar si
-- el documento es una entrada, una salida o un ajuste.
-- =========================================================================
CREATE OR REPLACE VIEW VW_Kardex AS
SELECT
    m.IdMovimiento,
    d.IdDocumento,
    d.TipoDocumento,
    d.FechaEmision,
    u.IdUsuario,
    u.NomUsuario                        AS UsuarioResponsable,
    p.IdProducto,
    p.SKU,
    p.NomProducto,
    c.NomCategoria,
    m.Cantidad,
    CASE
        WHEN d.TipoDocumento = 'Entrada'    THEN  m.Cantidad
        WHEN d.TipoDocumento = 'Salida'     THEN -m.Cantidad
        WHEN a.TipoAjuste    = 'Incremento' THEN  m.Cantidad
        ELSE -m.Cantidad
    END                                 AS CantidadConSigno,
    COALESCE(s.Motivo, a.Motivo,
             CONCAT('Recepcion de orden de compra Nro ', e.IdOrden)) AS Motivo,
    e.IdOrden,
    e.DocumentoRef,
    d.Estado                            AS EstadoDocumento
FROM MovimientosInventario m
INNER JOIN Documentos d ON d.IdDocumento = m.IdDocumento
INNER JOIN Usuarios   u ON u.IdUsuario   = d.IdUsuario
INNER JOIN Productos  p ON p.IdProducto  = m.IdProducto
INNER JOIN Categorias c ON c.IdCategoria = p.IdCategoria
LEFT  JOIN Entradas   e ON e.IdDocumento = d.IdDocumento
LEFT  JOIN Salidas    s ON s.IdDocumento = d.IdDocumento
LEFT  JOIN Ajustes    a ON a.IdDocumento = d.IdDocumento;

-- =========================================================================
-- VW_StockBajo - Productos por reponer para el Dashboard
-- =========================================================================
CREATE OR REPLACE VIEW VW_StockBajo AS
SELECT
    p.IdProducto,
    p.SKU,
    p.NomProducto,
    c.NomCategoria,
    pr.NomProveedor,
    p.StockActual,
    p.StockMinimo,
    (p.StockMinimo - p.StockActual) AS UnidadesPorReponer
FROM Productos p
INNER JOIN Categorias  c  ON c.IdCategoria = p.IdCategoria
LEFT  JOIN Proveedores pr ON pr.IdProveedor = p.IdProveedor
WHERE p.Estado = 1
  AND p.StockActual <= p.StockMinimo;

-- =========================================================================
-- VW_OrdenesResumen - Cabecera de la orden con su total calculado
-- Es el respaldo real del metodo calcularTotal() del diagrama de clases.
-- =========================================================================
CREATE OR REPLACE VIEW VW_OrdenesResumen AS
SELECT
    o.IdOrden,
    o.FechaEmision,
    o.FechaEstimadaEntrega,
    o.Estado,
    pr.IdProveedor,
    pr.NomProveedor,
    u.NomUsuario                                        AS RegistradoPor,
    COUNT(dc.IdDetalle)                                 AS LineasDetalle,
    COALESCE(SUM(dc.Cantidad), 0)                       AS UnidadesSolicitadas,
    COALESCE(SUM(dc.Cantidad * dc.PrecioUnitario), 0)   AS TotalOrden
FROM OrdenesCompra o
INNER JOIN Proveedores pr ON pr.IdProveedor = o.IdProveedor
INNER JOIN Usuarios    u  ON u.IdUsuario    = o.IdUsuario
LEFT  JOIN DetalleOrdenCompra dc ON dc.IdOrden = o.IdOrden
GROUP BY o.IdOrden, o.FechaEmision, o.FechaEstimadaEntrega, o.Estado,
         pr.IdProveedor, pr.NomProveedor, u.NomUsuario;
