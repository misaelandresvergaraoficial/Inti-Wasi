USE bd_intiwasi;

CREATE OR REPLACE VIEW VW_Kardex AS
SELECT
    m.IdMovimiento,
    d.IdDocumento,
    d.TipoDocumento,
    d.IdDocumentoOrigen,
    d.FechaEmision,
    u.IdUsuario,
    u.NomUsuario AS UsuarioResponsable,
    p.IdProducto,
    p.SKU,
    p.NomProducto,
    c.NomCategoria,
    m.Cantidad,
    CASE
        WHEN d.TipoDocumento = 'Entrada' THEN m.Cantidad
        WHEN d.TipoDocumento = 'Salida' THEN -m.Cantidad
        WHEN d.TipoDocumento = 'Ajuste' AND a.TipoAjuste = 'Incremento' THEN m.Cantidad
        WHEN d.TipoDocumento = 'Ajuste' THEN -m.Cantidad
        WHEN origen.TipoDocumento = 'Entrada' THEN -m.Cantidad
        WHEN origen.TipoDocumento = 'Salida' THEN m.Cantidad
        WHEN ajusteOrigen.TipoAjuste = 'Incremento' THEN -m.Cantidad
        ELSE m.Cantidad
    END AS CantidadConSigno,
    CASE
        WHEN d.TipoDocumento = 'Correccion' THEN d.MotivoCorreccion
        ELSE COALESCE(s.Motivo, a.Motivo,
             CONCAT('Recepcion de orden de compra Nro ', e.IdOrden))
    END AS Motivo,
    COALESCE(e.IdOrden, entradaOrigen.IdOrden) AS IdOrden,
    COALESCE(e.NumeroGuiaRemision, entradaOrigen.NumeroGuiaRemision) AS DocumentoRef,
    d.Estado AS EstadoDocumento
FROM MovimientosInventario m
JOIN Documentos d ON d.IdDocumento = m.IdDocumento
JOIN Usuarios u ON u.IdUsuario = d.IdUsuario
JOIN Productos p ON p.IdProducto = m.IdProducto
JOIN Categorias c ON c.IdCategoria = p.IdCategoria
LEFT JOIN Entradas e ON e.IdDocumento = d.IdDocumento
LEFT JOIN Salidas s ON s.IdDocumento = d.IdDocumento
LEFT JOIN Ajustes a ON a.IdDocumento = d.IdDocumento
LEFT JOIN Documentos origen ON origen.IdDocumento = d.IdDocumentoOrigen
LEFT JOIN Entradas entradaOrigen ON entradaOrigen.IdDocumento = origen.IdDocumento
LEFT JOIN Ajustes ajusteOrigen ON ajusteOrigen.IdDocumento = origen.IdDocumento;

CREATE OR REPLACE VIEW VW_RecepcionesPorProducto AS
SELECT IdOrden, IdProducto, SUM(CantidadConSigno) AS CantidadRecibida
FROM (
    SELECT e.IdOrden, m.IdProducto, m.Cantidad AS CantidadConSigno
      FROM Entradas e
      JOIN Documentos d ON d.IdDocumento = e.IdDocumento
      JOIN MovimientosInventario m ON m.IdDocumento = d.IdDocumento
     WHERE d.TipoDocumento = 'Entrada'
    UNION ALL
    SELECT e.IdOrden, m.IdProducto, -m.Cantidad AS CantidadConSigno
      FROM Documentos c
      JOIN Documentos original ON original.IdDocumento = c.IdDocumentoOrigen
      JOIN Entradas e ON e.IdDocumento = original.IdDocumento
      JOIN MovimientosInventario m ON m.IdDocumento = c.IdDocumento
     WHERE c.TipoDocumento = 'Correccion'
) recepciones
GROUP BY IdOrden, IdProducto;

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
JOIN Categorias c ON c.IdCategoria = p.IdCategoria
LEFT JOIN Proveedores pr ON pr.IdProveedor = p.IdProveedor
WHERE p.Estado = 1 AND p.StockActual <= p.StockMinimo;

CREATE OR REPLACE VIEW VW_OrdenesResumen AS
SELECT
    o.IdOrden,
    o.FechaEmision,
    o.FechaEstimadaEntrega,
    o.Estado,
    pr.IdProveedor,
    pr.NomProveedor,
    u.NomUsuario AS RegistradoPor,
    COUNT(dc.IdDetalle) AS LineasDetalle,
    COALESCE(SUM(dc.Cantidad), 0) AS UnidadesSolicitadas,
    COALESCE(SUM(r.CantidadRecibida), 0) AS UnidadesRecibidas,
    CASE WHEN o.Estado = 'Cancelada' THEN 0
         ELSE COALESCE(SUM(dc.Cantidad - COALESCE(r.CantidadRecibida, 0)), 0)
    END AS UnidadesPorRecibir,
    CASE WHEN o.Estado = 'Cancelada'
         THEN COALESCE(SUM(dc.Cantidad - COALESCE(r.CantidadRecibida, 0)), 0)
         ELSE 0 END AS UnidadesCanceladas,
    COALESCE(SUM(dc.Cantidad * dc.PrecioUnitario), 0) AS TotalOrden
FROM OrdenesCompra o
JOIN Proveedores pr ON pr.IdProveedor = o.IdProveedor
JOIN Usuarios u ON u.IdUsuario = o.IdUsuario
LEFT JOIN DetalleOrdenCompra dc ON dc.IdOrden = o.IdOrden
LEFT JOIN VW_RecepcionesPorProducto r
       ON r.IdOrden = dc.IdOrden AND r.IdProducto = dc.IdProducto
GROUP BY o.IdOrden, o.FechaEmision, o.FechaEstimadaEntrega, o.Estado,
         pr.IdProveedor, pr.NomProveedor, u.NomUsuario;
