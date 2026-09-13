-- Script  : 01_DDL_bd_intiwasi.sql 
-- Proyecto: Aplicacion Web de Gestion de Inventario y Almacen
--           Distribuidora Inti Wasi S.A.C.
-- Motor   : MySQL 8.0+ / InnoDB

DROP DATABASE IF EXISTS bd_intiwasi;
CREATE DATABASE bd_intiwasi
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_spanish_ci;
USE bd_intiwasi;

-- =========================================================================
-- 1. Tabla: Usuarios             
-- =========================================================================
CREATE TABLE Usuarios (
    IdUsuario       INT AUTO_INCREMENT PRIMARY KEY,
    NomUsuario      VARCHAR(100) NOT NULL,
    Correo          VARCHAR(150) NOT NULL,
    Contrasena      VARCHAR(255) NOT NULL,
    Rol             ENUM('Administrador', 'Operador de Almacén') NOT NULL,
    Telefono        VARCHAR(20),
    Estado          TINYINT NOT NULL DEFAULT 1,
    FechaRegistro   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT UQ_Usuarios_Correo UNIQUE (Correo)
) ENGINE=InnoDB;


CREATE INDEX IDX_Usuarios_Estado ON Usuarios(Estado);

-- =========================================================================
-- 2. Tabla: Categorias            (SIN CAMBIOS)
-- =========================================================================
CREATE TABLE Categorias (
    IdCategoria     INT AUTO_INCREMENT PRIMARY KEY,
    NomCategoria    VARCHAR(100) NOT NULL,
    Estado          TINYINT NOT NULL DEFAULT 1,
    CONSTRAINT UQ_Categorias_Nombre UNIQUE (NomCategoria)
) ENGINE=InnoDB;

-- =========================================================================
-- 3. Tabla: Proveedores
-- =========================================================================
CREATE TABLE Proveedores (
    IdProveedor     INT AUTO_INCREMENT PRIMARY KEY,
    NomProveedor    VARCHAR(100) NOT NULL,
    RUC             VARCHAR(11) NOT NULL,
    Contacto        VARCHAR(100),
    Telefono        VARCHAR(20) NOT NULL,
    Direccion       VARCHAR(150),
    Estado          TINYINT NOT NULL DEFAULT 1,
    CONSTRAINT UQ_Proveedores_RUC UNIQUE (RUC),
    CONSTRAINT CK_Proveedores_RUC CHECK (LENGTH(RUC) = 11 AND RUC REGEXP '^[0-9]+$')
) ENGINE=InnoDB;

-- =========================================================================
-- 4. Tabla: Productos
-- =========================================================================
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
    CONSTRAINT UQ_Productos_SKU UNIQUE (SKU),
    CONSTRAINT FK_Productos_Categoria FOREIGN KEY (IdCategoria) REFERENCES Categorias(IdCategoria)
        ON UPDATE RESTRICT ON DELETE RESTRICT,                              
    CONSTRAINT FK_Productos_Proveedor FOREIGN KEY (IdProveedor) REFERENCES Proveedores(IdProveedor)
        ON UPDATE RESTRICT ON DELETE RESTRICT,                              
    CONSTRAINT CK_Productos_Precio    CHECK (Precio >= 0),
    CONSTRAINT CK_Productos_StockMin  CHECK (StockMinimo >= 0),
    CONSTRAINT CK_Productos_StockAct  CHECK (StockActual >= 0)
) ENGINE=InnoDB;

CREATE INDEX IDX_Productos_Stock  ON Productos(StockActual, StockMinimo);
CREATE INDEX IDX_Productos_Estado ON Productos(Estado);

-- =========================================================================
-- 5. Tabla: OrdenesCompra
-- =========================================================================
CREATE TABLE OrdenesCompra (
    IdOrden              INT AUTO_INCREMENT PRIMARY KEY,
    IdProveedor          INT NOT NULL,
    IdUsuario            INT NOT NULL,
    FechaEmision         DATE NOT NULL,
    FechaEstimadaEntrega DATE,
    Estado               ENUM('Pendiente', 'Recibida') NOT NULL DEFAULT 'Pendiente',
    CONSTRAINT FK_Orden_Proveedor FOREIGN KEY (IdProveedor) REFERENCES Proveedores(IdProveedor)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT FK_Orden_Usuario   FOREIGN KEY (IdUsuario)   REFERENCES Usuarios(IdUsuario)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT CK_Orden_Fechas CHECK (FechaEstimadaEntrega IS NULL
                                      OR FechaEstimadaEntrega >= FechaEmision)
) ENGINE=InnoDB;

CREATE INDEX IDX_Orden_Estado ON OrdenesCompra(Estado);

-- =========================================================================
-- 6. Tabla: DetalleOrdenCompra
-- =========================================================================
CREATE TABLE DetalleOrdenCompra (
    IdDetalle       INT AUTO_INCREMENT PRIMARY KEY,
    IdOrden         INT NOT NULL,
    IdProducto      INT NOT NULL,
    Cantidad        INT NOT NULL,
    PrecioUnitario  DECIMAL(10,2) NOT NULL,
    CONSTRAINT UQ_Detalle_OrdenProducto UNIQUE (IdOrden, IdProducto),
    CONSTRAINT FK_Detalle_Orden    FOREIGN KEY (IdOrden)    REFERENCES OrdenesCompra(IdOrden)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT FK_Detalle_Producto FOREIGN KEY (IdProducto) REFERENCES Productos(IdProducto)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT CK_Detalle_Cantidad CHECK (Cantidad > 0),
    CONSTRAINT CK_Detalle_Precio   CHECK (PrecioUnitario >= 0)
) ENGINE=InnoDB;

-- =========================================================================
-- 7. Tabla: Documentos  (clase padre)
-- =========================================================================
CREATE TABLE Documentos (
    IdDocumento     INT AUTO_INCREMENT PRIMARY KEY,
    TipoDocumento   ENUM('Entrada', 'Salida', 'Ajuste') NOT NULL,
    IdUsuario       INT NOT NULL,
    FechaEmision    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    Estado          TINYINT NOT NULL DEFAULT 1,
    CONSTRAINT FK_Doc_Usuario FOREIGN KEY (IdUsuario) REFERENCES Usuarios(IdUsuario)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE INDEX IDX_Documentos_Fecha ON Documentos(FechaEmision);
CREATE INDEX IDX_Documentos_Tipo  ON Documentos(TipoDocumento, FechaEmision);

-- =========================================================================
-- 8. Tabla: Entradas  (hija de Documentos)
-- =========================================================================
CREATE TABLE Entradas (
    IdEntrada       INT AUTO_INCREMENT PRIMARY KEY,
    IdDocumento     INT NOT NULL,
    IdOrden         INT NOT NULL,
    DocumentoRef    VARCHAR(50) NOT NULL,       
    Observaciones   VARCHAR(255),
    CONSTRAINT UQ_Entradas_Documento UNIQUE (IdDocumento),
    CONSTRAINT UQ_Entradas_Orden     UNIQUE (IdOrden),
    CONSTRAINT FK_Entrada_Documento FOREIGN KEY (IdDocumento) REFERENCES Documentos(IdDocumento)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT FK_Entrada_Orden     FOREIGN KEY (IdOrden)     REFERENCES OrdenesCompra(IdOrden)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB;

-- =========================================================================
-- 9. Tabla: Salidas  (hija de Documentos)
-- =========================================================================
CREATE TABLE Salidas (
    IdSalida        INT AUTO_INCREMENT PRIMARY KEY,
    IdDocumento     INT NOT NULL,
    Motivo          ENUM('Despacho/Venta', 'Merma', 'Otro') NOT NULL,
    Observaciones   VARCHAR(255),
    CONSTRAINT UQ_Salidas_Documento UNIQUE (IdDocumento),
    CONSTRAINT FK_Salida_Documento FOREIGN KEY (IdDocumento) REFERENCES Documentos(IdDocumento)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB;

-- =========================================================================
-- 10. Tabla: Ajustes  (hija de Documentos)
-- =========================================================================
CREATE TABLE Ajustes (
    IdAjuste        INT AUTO_INCREMENT PRIMARY KEY,
    IdDocumento     INT NOT NULL,
    TipoAjuste      ENUM('Incremento', 'Decremento') NOT NULL,
    Motivo          VARCHAR(255) NOT NULL,
    Observaciones   VARCHAR(255),
    CONSTRAINT UQ_Ajustes_Documento UNIQUE (IdDocumento),
    CONSTRAINT FK_Ajuste_Documento FOREIGN KEY (IdDocumento) REFERENCES Documentos(IdDocumento)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB;

-- =========================================================================
-- 11. Tabla: MovimientosInventario
-- =========================================================================
CREATE TABLE MovimientosInventario (
    IdMovimiento    INT AUTO_INCREMENT PRIMARY KEY,
    IdDocumento     INT NOT NULL,
    IdProducto      INT NOT NULL,
    Cantidad        INT NOT NULL,
    CONSTRAINT FK_Mov_Documento FOREIGN KEY (IdDocumento) REFERENCES Documentos(IdDocumento)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT FK_Mov_Producto  FOREIGN KEY (IdProducto)  REFERENCES Productos(IdProducto)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT CK_Mov_Cantidad  CHECK (Cantidad > 0)
) ENGINE=InnoDB;
