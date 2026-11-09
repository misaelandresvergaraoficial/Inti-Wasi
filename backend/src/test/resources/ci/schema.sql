ALTER DATABASE bd_intiwasi CHARACTER SET utf8mb4 COLLATE utf8mb4_spanish_ci;

CREATE TABLE Usuarios (
    IdUsuario       INT AUTO_INCREMENT PRIMARY KEY,
    NomUsuario      VARCHAR(100) NOT NULL,
    Correo          VARCHAR(150) NOT NULL,
    Contrasena      VARCHAR(255) NOT NULL,
    Rol             ENUM(
                        'Administrador',
                        'Operador de Almacén'
                    ) NOT NULL,
    Telefono        VARCHAR(20),
    Estado          TINYINT NOT NULL DEFAULT 1,
    FechaRegistro   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT UQ_Usuarios_Correo
        UNIQUE (Correo),

    CONSTRAINT UQ_Usuarios_Telefono
        UNIQUE (Telefono)
) ENGINE = InnoDB;

CREATE INDEX IDX_Usuarios_Estado
    ON Usuarios (Estado);



CREATE TABLE Categorias (
    IdCategoria     INT AUTO_INCREMENT PRIMARY KEY,
    NomCategoria    VARCHAR(100) NOT NULL,
    Estado          TINYINT NOT NULL DEFAULT 1,

    CONSTRAINT UQ_Categorias_Nombre
        UNIQUE (NomCategoria)
) ENGINE = InnoDB;


CREATE TABLE Proveedores (
    IdProveedor     INT AUTO_INCREMENT PRIMARY KEY,
    NomProveedor    VARCHAR(100) NOT NULL,
    PrefijoGuia     VARCHAR(30) NOT NULL,
    RUC             VARCHAR(11) NOT NULL,
    Contacto        VARCHAR(100),
    Telefono        VARCHAR(20) NOT NULL,
    Direccion       VARCHAR(150),
    Estado          TINYINT NOT NULL DEFAULT 1,

    CONSTRAINT UQ_Proveedores_RUC
        UNIQUE (RUC),

    CONSTRAINT UQ_Proveedores_PrefijoGuia
        UNIQUE (PrefijoGuia),

    CONSTRAINT CK_Proveedores_RUC
        CHECK (
            LENGTH(RUC) = 11
            AND RUC REGEXP '^[0-9]+$'
        )
) ENGINE = InnoDB;


CREATE TABLE Productos (
    IdProducto      INT AUTO_INCREMENT PRIMARY KEY,
    SKU             VARCHAR(30) NOT NULL,
    NomProducto     VARCHAR(150) NOT NULL,
    IdCategoria     INT NOT NULL,
    IdProveedor     INT NULL,
    Precio          DECIMAL(10,2) NOT NULL,
    StockMinimo     INT NOT NULL DEFAULT 0,
    StockActual     INT NOT NULL DEFAULT 0,
    Estado          TINYINT NOT NULL DEFAULT 1,
    DesactivadoPorCategoria TINYINT NOT NULL DEFAULT 0,

    CONSTRAINT UQ_Productos_SKU
        UNIQUE (SKU),

    CONSTRAINT FK_Productos_Categoria
        FOREIGN KEY (IdCategoria)
        REFERENCES Categorias (IdCategoria)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT FK_Productos_Proveedor
        FOREIGN KEY (IdProveedor)
        REFERENCES Proveedores (IdProveedor)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT CK_Productos_Precio
        CHECK (Precio >= 0),

    CONSTRAINT CK_Productos_StockMin
        CHECK (StockMinimo >= 0),

    CONSTRAINT CK_Productos_StockAct
        CHECK (StockActual >= 0),

    CONSTRAINT CK_Productos_DesactivadoCategoria
        CHECK (DesactivadoPorCategoria IN (0, 1))
) ENGINE = InnoDB;

CREATE INDEX IDX_Productos_Stock
    ON Productos (StockActual, StockMinimo);

CREATE INDEX IDX_Productos_Estado
    ON Productos (Estado);


CREATE TABLE GuiasRemision (
    IdGuia          INT AUTO_INCREMENT PRIMARY KEY,
    IdProveedor     INT NOT NULL,
    NumeroGuia      VARCHAR(50) NOT NULL,

    CONSTRAINT UQ_Guias_ProveedorNumero
        UNIQUE (IdProveedor, NumeroGuia),

    CONSTRAINT FK_Guias_Proveedor
        FOREIGN KEY (IdProveedor)
        REFERENCES Proveedores (IdProveedor)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT CK_Guias_Numero
        CHECK (
            CHAR_LENGTH(TRIM(NumeroGuia)) > 0
        )
) ENGINE = InnoDB;



CREATE TABLE OrdenesCompra (
    IdOrden              INT AUTO_INCREMENT PRIMARY KEY,
    IdProveedor          INT NOT NULL,
    IdUsuario            INT NOT NULL,
    FechaEmision         DATE NOT NULL,
    FechaEstimadaEntrega DATE,
    Estado               ENUM(
                             'Pendiente',
                             'Parcial',
                             'Recibida',
                             'Cancelada'
                         ) NOT NULL DEFAULT 'Pendiente',

    CONSTRAINT FK_Orden_Proveedor
        FOREIGN KEY (IdProveedor)
        REFERENCES Proveedores (IdProveedor)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT FK_Orden_Usuario
        FOREIGN KEY (IdUsuario)
        REFERENCES Usuarios (IdUsuario)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT CK_Orden_Fechas
        CHECK (
            FechaEstimadaEntrega IS NULL
            OR FechaEstimadaEntrega >= FechaEmision
        )
) ENGINE = InnoDB;

CREATE INDEX IDX_Orden_Estado
    ON OrdenesCompra (Estado);



CREATE TABLE DetalleOrdenCompra (
    IdDetalle       INT AUTO_INCREMENT PRIMARY KEY,
    IdOrden         INT NOT NULL,
    IdProducto      INT NOT NULL,
    Cantidad        INT NOT NULL,
    PrecioUnitario  DECIMAL(10,2) NOT NULL,

    CONSTRAINT UQ_Detalle_OrdenProducto
        UNIQUE (IdOrden, IdProducto),

    CONSTRAINT FK_Detalle_Orden
        FOREIGN KEY (IdOrden)
        REFERENCES OrdenesCompra (IdOrden)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT FK_Detalle_Producto
        FOREIGN KEY (IdProducto)
        REFERENCES Productos (IdProducto)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT CK_Detalle_Cantidad
        CHECK (Cantidad > 0),

    CONSTRAINT CK_Detalle_Precio
        CHECK (PrecioUnitario >= 0)
) ENGINE = InnoDB;


CREATE TABLE Documentos (
    IdDocumento       INT AUTO_INCREMENT PRIMARY KEY,
    TipoDocumento     ENUM(
                          'Entrada',
                          'Salida',
                          'Ajuste',
                          'Correccion'
                      ) NOT NULL,
    IdUsuario         INT NOT NULL,
    FechaEmision      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    Estado            TINYINT NOT NULL DEFAULT 1,
    IdDocumentoOrigen INT NULL,
    MotivoCorreccion  VARCHAR(255) NULL,

    CONSTRAINT FK_Doc_Usuario
        FOREIGN KEY (IdUsuario)
        REFERENCES Usuarios (IdUsuario)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT FK_Doc_Origen
        FOREIGN KEY (IdDocumentoOrigen)
        REFERENCES Documentos (IdDocumento)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT UQ_Doc_OrigenTipo
        UNIQUE (IdDocumentoOrigen, TipoDocumento),

    CONSTRAINT CK_Doc_Estado
        CHECK (Estado IN (0, 1, 2)),

    CONSTRAINT CK_Doc_Correccion
        CHECK (
            (
                TipoDocumento = 'Correccion'
                AND IdDocumentoOrigen IS NOT NULL
                AND MotivoCorreccion IS NOT NULL
                AND CHAR_LENGTH(TRIM(MotivoCorreccion)) > 0
            )
            OR
            (
                TipoDocumento <> 'Correccion'
                AND MotivoCorreccion IS NULL
            )
        )
) ENGINE = InnoDB;

CREATE INDEX IDX_Documentos_Fecha
    ON Documentos (FechaEmision);

CREATE INDEX IDX_Documentos_Tipo
    ON Documentos (TipoDocumento, FechaEmision);



CREATE TABLE Entradas (
    IdEntrada       INT AUTO_INCREMENT PRIMARY KEY,
    IdDocumento     INT NOT NULL,
    IdOrden         INT NOT NULL,
    IdGuia          INT NOT NULL,
    Observaciones   VARCHAR(255),

    CONSTRAINT UQ_Entradas_Documento
        UNIQUE (IdDocumento),

    CONSTRAINT FK_Entrada_Documento
        FOREIGN KEY (IdDocumento)
        REFERENCES Documentos (IdDocumento)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT FK_Entrada_Orden
        FOREIGN KEY (IdOrden)
        REFERENCES OrdenesCompra (IdOrden)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT FK_Entrada_Guia
        FOREIGN KEY (IdGuia)
        REFERENCES GuiasRemision (IdGuia)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
) ENGINE = InnoDB;

CREATE INDEX IDX_Entradas_Orden
    ON Entradas (IdOrden);



CREATE TABLE Salidas (
    IdSalida        INT AUTO_INCREMENT PRIMARY KEY,
    IdDocumento     INT NOT NULL,
    Motivo          ENUM(
                        'Despacho/Venta',
                        'Merma',
                        'Otro'
                    ) NOT NULL,
    Observaciones   VARCHAR(255),

    CONSTRAINT UQ_Salidas_Documento
        UNIQUE (IdDocumento),

    CONSTRAINT FK_Salida_Documento
        FOREIGN KEY (IdDocumento)
        REFERENCES Documentos (IdDocumento)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
) ENGINE = InnoDB;


CREATE TABLE Ajustes (
    IdAjuste        INT AUTO_INCREMENT PRIMARY KEY,
    IdDocumento     INT NOT NULL,
    TipoAjuste      ENUM(
                        'Incremento',
                        'Decremento'
                    ) NOT NULL,
    Motivo          VARCHAR(255) NOT NULL,
    Observaciones   VARCHAR(255),

    CONSTRAINT UQ_Ajustes_Documento
        UNIQUE (IdDocumento),

    CONSTRAINT FK_Ajuste_Documento
        FOREIGN KEY (IdDocumento)
        REFERENCES Documentos (IdDocumento)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT
) ENGINE = InnoDB;

CREATE TABLE MovimientosInventario (
    IdMovimiento    INT AUTO_INCREMENT PRIMARY KEY,
    IdDocumento     INT NOT NULL,
    IdProducto      INT NOT NULL,
    Cantidad        INT NOT NULL,

    CONSTRAINT UQ_Mov_DocumentoProducto
        UNIQUE (IdDocumento, IdProducto),

    CONSTRAINT FK_Mov_Documento
        FOREIGN KEY (IdDocumento)
        REFERENCES Documentos (IdDocumento)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT FK_Mov_Producto
        FOREIGN KEY (IdProducto)
        REFERENCES Productos (IdProducto)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,

    CONSTRAINT CK_Mov_Cantidad
        CHECK (Cantidad > 0)
) ENGINE = InnoDB;

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
        WHEN d.TipoDocumento = 'Entrada'
            THEN m.Cantidad
        WHEN d.TipoDocumento = 'Salida'
            THEN -m.Cantidad
        WHEN d.TipoDocumento = 'Ajuste'
             AND a.TipoAjuste = 'Incremento'
            THEN m.Cantidad
        WHEN d.TipoDocumento = 'Ajuste'
            THEN -m.Cantidad
        WHEN origen.TipoDocumento = 'Entrada'
            THEN -m.Cantidad
        WHEN origen.TipoDocumento = 'Salida'
            THEN m.Cantidad
        WHEN ajusteOrigen.TipoAjuste = 'Incremento'
            THEN -m.Cantidad
        ELSE m.Cantidad
    END AS CantidadConSigno,

    CASE
        WHEN d.TipoDocumento = 'Correccion'
            THEN d.MotivoCorreccion
        ELSE COALESCE(
            s.Motivo,
            a.Motivo,
            CONCAT(
                'Recepcion de orden de compra Nro ',
                e.IdOrden
            )
        )
    END AS Motivo,

    COALESCE(
        e.IdOrden,
        entradaOrigen.IdOrden
    ) AS IdOrden,

    COALESCE(
        guia.NumeroGuia,
        guiaOrigen.NumeroGuia
    ) AS DocumentoRef,

    d.Estado AS EstadoDocumento

FROM MovimientosInventario m

JOIN Documentos d
    ON d.IdDocumento = m.IdDocumento

JOIN Usuarios u
    ON u.IdUsuario = d.IdUsuario

JOIN Productos p
    ON p.IdProducto = m.IdProducto

JOIN Categorias c
    ON c.IdCategoria = p.IdCategoria

LEFT JOIN Entradas e
    ON e.IdDocumento = d.IdDocumento

LEFT JOIN GuiasRemision guia
    ON guia.IdGuia = e.IdGuia

LEFT JOIN Salidas s
    ON s.IdDocumento = d.IdDocumento

LEFT JOIN Ajustes a
    ON a.IdDocumento = d.IdDocumento

LEFT JOIN Documentos origen
    ON origen.IdDocumento = d.IdDocumentoOrigen

LEFT JOIN Entradas entradaOrigen
    ON entradaOrigen.IdDocumento = origen.IdDocumento

LEFT JOIN GuiasRemision guiaOrigen
    ON guiaOrigen.IdGuia = entradaOrigen.IdGuia

LEFT JOIN Ajustes ajusteOrigen
    ON ajusteOrigen.IdDocumento = origen.IdDocumento;



CREATE OR REPLACE VIEW VW_RecepcionesPorProducto AS
SELECT
    IdOrden,
    IdProducto,
    SUM(CantidadConSigno) AS CantidadRecibida
FROM (
    SELECT
        e.IdOrden,
        m.IdProducto,
        m.Cantidad AS CantidadConSigno
    FROM Entradas e

    JOIN Documentos d
        ON d.IdDocumento = e.IdDocumento

    JOIN MovimientosInventario m
        ON m.IdDocumento = d.IdDocumento

    WHERE d.TipoDocumento = 'Entrada'

    UNION ALL

    SELECT
        e.IdOrden,
        m.IdProducto,
        -m.Cantidad AS CantidadConSigno
    FROM Documentos c

    JOIN Documentos original
        ON original.IdDocumento = c.IdDocumentoOrigen

    JOIN Entradas e
        ON e.IdDocumento = original.IdDocumento

    JOIN MovimientosInventario m
        ON m.IdDocumento = c.IdDocumento

    WHERE c.TipoDocumento = 'Correccion'
) recepciones
GROUP BY
    IdOrden,
    IdProducto;



CREATE OR REPLACE VIEW VW_StockBajo AS
SELECT
    p.IdProducto,
    p.SKU,
    p.NomProducto,
    c.NomCategoria,
    pr.NomProveedor,
    p.StockActual,
    p.StockMinimo,
    p.StockMinimo - p.StockActual AS UnidadesPorReponer

FROM Productos p

JOIN Categorias c
    ON c.IdCategoria = p.IdCategoria

LEFT JOIN Proveedores pr
    ON pr.IdProveedor = p.IdProveedor

WHERE p.Estado = 1
  AND p.StockActual <= p.StockMinimo;



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

    COALESCE(
        SUM(dc.Cantidad),
        0
    ) AS UnidadesSolicitadas,

    COALESCE(
        SUM(r.CantidadRecibida),
        0
    ) AS UnidadesRecibidas,

    CASE
        WHEN o.Estado = 'Cancelada'
            THEN 0
        ELSE COALESCE(
            SUM(
                dc.Cantidad
                - COALESCE(r.CantidadRecibida, 0)
            ),
            0
        )
    END AS UnidadesPorRecibir,

    CASE
        WHEN o.Estado = 'Cancelada'
            THEN COALESCE(
                SUM(
                    dc.Cantidad
                    - COALESCE(r.CantidadRecibida, 0)
                ),
                0
            )
        ELSE 0
    END AS UnidadesCanceladas,

    COALESCE(
        SUM(dc.Cantidad * dc.PrecioUnitario),
        0
    ) AS TotalOrden

FROM OrdenesCompra o

JOIN Proveedores pr
    ON pr.IdProveedor = o.IdProveedor

JOIN Usuarios u
    ON u.IdUsuario = o.IdUsuario

LEFT JOIN DetalleOrdenCompra dc
    ON dc.IdOrden = o.IdOrden

LEFT JOIN VW_RecepcionesPorProducto r
    ON r.IdOrden = dc.IdOrden
   AND r.IdProducto = dc.IdProducto

GROUP BY
    o.IdOrden,
    o.FechaEmision,
    o.FechaEstimadaEntrega,
    o.Estado,
    pr.IdProveedor,
    pr.NomProveedor,
    u.NomUsuario;
