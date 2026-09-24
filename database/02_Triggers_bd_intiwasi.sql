
USE bd_intiwasi;

DROP TRIGGER IF EXISTS TRG_Entradas_BeforeInsert;
DROP TRIGGER IF EXISTS TRG_Entradas_AfterInsert;
DROP TRIGGER IF EXISTS TRG_Salidas_BeforeInsert;
DROP TRIGGER IF EXISTS TRG_Ajustes_BeforeInsert;
DROP TRIGGER IF EXISTS TRG_Movimientos_BeforeInsert;
DROP TRIGGER IF EXISTS TRG_Movimientos_AfterInsert;
DROP TRIGGER IF EXISTS TRG_Movimientos_BeforeUpdate;
DROP TRIGGER IF EXISTS TRG_Movimientos_BeforeDelete;
DROP TRIGGER IF EXISTS TRG_Documentos_BeforeUpdate;
DROP TRIGGER IF EXISTS TRG_Documentos_BeforeDelete;
DROP TRIGGER IF EXISTS TRG_Documentos_BloquearAnulacion;
DROP TRIGGER IF EXISTS TRG_Detalle_BeforeInsert;
DROP TRIGGER IF EXISTS TRG_Detalle_BeforeUpdate;
DROP TRIGGER IF EXISTS TRG_Detalle_BeforeDelete;

DELIMITER //


CREATE TRIGGER TRG_Entradas_BeforeInsert
BEFORE INSERT ON Entradas
FOR EACH ROW
BEGIN
    DECLARE v_TipoDoc  ENUM('Entrada','Salida','Ajuste');
    DECLARE v_EstadoOC ENUM('Pendiente','Recibida');

    SELECT TipoDocumento INTO v_TipoDoc
      FROM Documentos WHERE IdDocumento = NEW.IdDocumento;

    IF v_TipoDoc IS NULL OR v_TipoDoc <> 'Entrada' THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'El documento asociado no existe o no es de tipo Entrada.';
    END IF;

    SELECT Estado INTO v_EstadoOC
      FROM OrdenesCompra WHERE IdOrden = NEW.IdOrden;

    IF v_EstadoOC IS NULL OR v_EstadoOC <> 'Pendiente' THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Debe seleccionar una orden de compra pendiente';
    END IF;
END //

CREATE TRIGGER TRG_Salidas_BeforeInsert
BEFORE INSERT ON Salidas
FOR EACH ROW
BEGIN
    DECLARE v_TipoDoc ENUM('Entrada','Salida','Ajuste');

    SELECT TipoDocumento INTO v_TipoDoc
      FROM Documentos WHERE IdDocumento = NEW.IdDocumento;

    IF v_TipoDoc IS NULL OR v_TipoDoc <> 'Salida' THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'El documento asociado no existe o no es de tipo Salida.';
    END IF;
END //

CREATE TRIGGER TRG_Ajustes_BeforeInsert
BEFORE INSERT ON Ajustes
FOR EACH ROW
BEGIN
    DECLARE v_TipoDoc ENUM('Entrada','Salida','Ajuste');
    DECLARE v_Rol     VARCHAR(30);

    SELECT d.TipoDocumento, u.Rol INTO v_TipoDoc, v_Rol
      FROM Documentos d
      JOIN Usuarios   u ON u.IdUsuario = d.IdUsuario
     WHERE d.IdDocumento = NEW.IdDocumento;

    IF v_TipoDoc IS NULL OR v_TipoDoc <> 'Ajuste' THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'El documento asociado no existe o no es de tipo Ajuste.';
    END IF;

    IF v_Rol <> 'Administrador' THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Solo un Administrador puede registrar ajustes de inventario.';
    END IF;
END //

CREATE TRIGGER TRG_Movimientos_BeforeInsert
BEFORE INSERT ON MovimientosInventario
FOR EACH ROW
BEGIN
    DECLARE v_TipoDoc     ENUM('Entrada','Salida','Ajuste');
    DECLARE v_EstadoDoc   TINYINT;
    DECLARE v_EstadoProd  TINYINT;
    DECLARE v_StockActual INT;
    DECLARE v_TipoAjuste  ENUM('Incremento','Decremento');
    DECLARE v_IdOrden     INT;
    DECLARE v_Pedida      INT;
    DECLARE v_HaySalida   INT DEFAULT 0;

    
    SELECT TipoDocumento, Estado INTO v_TipoDoc, v_EstadoDoc
      FROM Documentos WHERE IdDocumento = NEW.IdDocumento;

    IF v_TipoDoc IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Todo movimiento debe estar respaldado por un documento existente.';
    END IF;

    IF v_EstadoDoc <> 1 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'No se pueden registrar movimientos sobre un documento anulado.';
    END IF;

    SELECT Estado, StockActual INTO v_EstadoProd, v_StockActual
      FROM Productos WHERE IdProducto = NEW.IdProducto;

    IF v_EstadoProd IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'El producto indicado no existe.';
    END IF;

    IF v_EstadoProd <> 1 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'El producto esta inactivo y no admite nuevos movimientos.';
    END IF;

    IF v_TipoDoc = 'Entrada' THEN

        SELECT IdOrden INTO v_IdOrden
          FROM Entradas WHERE IdDocumento = NEW.IdDocumento;

        IF v_IdOrden IS NULL THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Registre la cabecera de la entrada antes que sus movimientos.';
        END IF;

        SELECT Cantidad INTO v_Pedida
          FROM DetalleOrdenCompra
         WHERE IdOrden = v_IdOrden AND IdProducto = NEW.IdProducto;

        IF v_Pedida IS NULL THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El producto recibido no figura en el detalle de la orden de compra.';
        END IF;

        IF NEW.Cantidad > v_Pedida THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La cantidad recibida supera la cantidad solicitada en la orden de compra.';
        END IF;

    ELSEIF v_TipoDoc = 'Salida' THEN

        SELECT COUNT(*) INTO v_HaySalida
          FROM Salidas WHERE IdDocumento = NEW.IdDocumento;

        IF v_HaySalida = 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Registre la cabecera de la salida antes que sus movimientos.';
        END IF;

        IF v_StockActual < NEW.Cantidad THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La cantidad solicitada supera el stock disponible';
        END IF;

    ELSE

        SELECT TipoAjuste INTO v_TipoAjuste
          FROM Ajustes WHERE IdDocumento = NEW.IdDocumento;

        IF v_TipoAjuste IS NULL THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Registre la cabecera del ajuste antes que sus movimientos.';
        END IF;

        IF v_TipoAjuste = 'Decremento' AND v_StockActual < NEW.Cantidad THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La cantidad solicitada supera el stock disponible';
        END IF;

    END IF;
END //

CREATE TRIGGER TRG_Movimientos_AfterInsert
AFTER INSERT ON MovimientosInventario
FOR EACH ROW
BEGIN
    DECLARE v_TipoDoc    ENUM('Entrada','Salida','Ajuste');
    DECLARE v_TipoAjuste ENUM('Incremento','Decremento');

    SELECT TipoDocumento INTO v_TipoDoc
      FROM Documentos WHERE IdDocumento = NEW.IdDocumento;

    IF v_TipoDoc = 'Entrada' THEN

        UPDATE Productos
           SET StockActual = StockActual + NEW.Cantidad
         WHERE IdProducto = NEW.IdProducto;

    ELSEIF v_TipoDoc = 'Salida' THEN

        UPDATE Productos
           SET StockActual = StockActual - NEW.Cantidad
         WHERE IdProducto = NEW.IdProducto;

    ELSE

        SELECT TipoAjuste INTO v_TipoAjuste
          FROM Ajustes WHERE IdDocumento = NEW.IdDocumento;

        IF v_TipoAjuste = 'Incremento' THEN
            UPDATE Productos
               SET StockActual = StockActual + NEW.Cantidad
             WHERE IdProducto = NEW.IdProducto;
        ELSE
            UPDATE Productos
               SET StockActual = StockActual - NEW.Cantidad
             WHERE IdProducto = NEW.IdProducto;
        END IF;

    END IF;
END //

CREATE TRIGGER TRG_Entradas_AfterInsert
AFTER INSERT ON Entradas
FOR EACH ROW
BEGIN
    UPDATE OrdenesCompra
       SET Estado = 'Recibida'
     WHERE IdOrden = NEW.IdOrden AND Estado = 'Pendiente';
END //

CREATE TRIGGER TRG_Detalle_BeforeInsert
BEFORE INSERT ON DetalleOrdenCompra
FOR EACH ROW
BEGIN
    DECLARE v_Estado ENUM('Pendiente','Recibida');

    SELECT Estado INTO v_Estado FROM OrdenesCompra WHERE IdOrden = NEW.IdOrden;

    IF v_Estado = 'Recibida' THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'No es posible editar una orden que ya ha sido recibida';
    END IF;
END //

CREATE TRIGGER TRG_Detalle_BeforeUpdate
BEFORE UPDATE ON DetalleOrdenCompra
FOR EACH ROW
BEGIN
    DECLARE v_Estado ENUM('Pendiente','Recibida');

    SELECT Estado INTO v_Estado FROM OrdenesCompra WHERE IdOrden = OLD.IdOrden;

    IF v_Estado = 'Recibida' THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'No es posible editar una orden que ya ha sido recibida';
    END IF;
END //

CREATE TRIGGER TRG_Detalle_BeforeDelete
BEFORE DELETE ON DetalleOrdenCompra
FOR EACH ROW
BEGIN
    DECLARE v_Estado ENUM('Pendiente','Recibida');

    SELECT Estado INTO v_Estado FROM OrdenesCompra WHERE IdOrden = OLD.IdOrden;

    IF v_Estado = 'Recibida' THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'No es posible editar una orden que ya ha sido recibida';
    END IF;
END //

CREATE TRIGGER TRG_Movimientos_BeforeUpdate
BEFORE UPDATE ON MovimientosInventario
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'Los registros del historial de inventario son inmutables y no pueden ser modificados.';
END //

CREATE TRIGGER TRG_Movimientos_BeforeDelete
BEFORE DELETE ON MovimientosInventario
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'Los registros del historial de inventario no pueden ser eliminados.';
END //

CREATE TRIGGER TRG_Documentos_BeforeUpdate
BEFORE UPDATE ON Documentos
FOR EACH ROW
BEGIN
    IF NEW.TipoDocumento <> OLD.TipoDocumento
       OR NEW.FechaEmision <> OLD.FechaEmision
       OR NEW.IdUsuario    <> OLD.IdUsuario THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Los datos maestros de un documento emitido no pueden ser alterados.';
    END IF;
END //

CREATE TRIGGER TRG_Documentos_BeforeDelete
BEFORE DELETE ON Documentos
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'Un documento emitido no puede ser eliminado: use un documento de ajuste para corregir.';
END //

CREATE TRIGGER TRG_Documentos_BloquearAnulacion
BEFORE UPDATE ON Documentos
FOR EACH ROW
BEGIN
    IF OLD.Estado = 1 AND NEW.Estado = 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Un documento con movimientos no se anula: registre un ajuste de reverso para mantener la trazabilidad.';
    END IF;
END //

DELIMITER ;
