USE bd_intiwasi;


DROP TRIGGER IF EXISTS TRG_Documentos_BeforeInsert;
DROP TRIGGER IF EXISTS TRG_Documentos_BeforeUpdate;
DROP TRIGGER IF EXISTS TRG_Documentos_BeforeDelete;
DROP TRIGGER IF EXISTS TRG_Ordenes_BeforeUpdate;
DROP TRIGGER IF EXISTS TRG_Ordenes_BeforeDelete;
DROP TRIGGER IF EXISTS TRG_Entradas_BeforeInsert;
DROP TRIGGER IF EXISTS TRG_Entradas_AfterInsert;
DROP TRIGGER IF EXISTS TRG_Entradas_BeforeUpdate;
DROP TRIGGER IF EXISTS TRG_Entradas_BeforeDelete;
DROP TRIGGER IF EXISTS TRG_Salidas_BeforeInsert;
DROP TRIGGER IF EXISTS TRG_Salidas_BeforeUpdate;
DROP TRIGGER IF EXISTS TRG_Salidas_BeforeDelete;
DROP TRIGGER IF EXISTS TRG_Ajustes_BeforeInsert;
DROP TRIGGER IF EXISTS TRG_Ajustes_BeforeUpdate;
DROP TRIGGER IF EXISTS TRG_Ajustes_BeforeDelete;
DROP TRIGGER IF EXISTS TRG_Movimientos_BeforeInsert;
DROP TRIGGER IF EXISTS TRG_Movimientos_AfterInsert;
DROP TRIGGER IF EXISTS TRG_Movimientos_BeforeUpdate;
DROP TRIGGER IF EXISTS TRG_Movimientos_BeforeDelete;
DROP TRIGGER IF EXISTS TRG_Detalle_BeforeInsert;
DROP TRIGGER IF EXISTS TRG_Detalle_BeforeUpdate;
DROP TRIGGER IF EXISTS TRG_Detalle_BeforeDelete;
DROP TRIGGER IF EXISTS TRG_Documentos_BloquearAnulacion;
DROP PROCEDURE IF EXISTS SP_RecalcularEstadoOrden;

DELIMITER //

CREATE PROCEDURE SP_RecalcularEstadoOrden(IN p_IdOrden INT)
BEGIN
    DECLARE v_Pedida INT DEFAULT 0;
    DECLARE v_Recibida INT DEFAULT 0;

    SELECT COALESCE(SUM(Cantidad), 0) INTO v_Pedida
      FROM DetalleOrdenCompra WHERE IdOrden = p_IdOrden;

    SELECT
        COALESCE((
            SELECT SUM(m.Cantidad)
              FROM Entradas e
              JOIN Documentos d ON d.IdDocumento = e.IdDocumento
              JOIN MovimientosInventario m ON m.IdDocumento = d.IdDocumento
             WHERE e.IdOrden = p_IdOrden AND d.TipoDocumento = 'Entrada'
        ), 0)
        - COALESCE((
            SELECT SUM(m.Cantidad)
              FROM Documentos c
              JOIN Documentos original ON original.IdDocumento = c.IdDocumentoOrigen
              JOIN Entradas e ON e.IdDocumento = original.IdDocumento
              JOIN MovimientosInventario m ON m.IdDocumento = c.IdDocumento
             WHERE e.IdOrden = p_IdOrden AND c.TipoDocumento = 'Correccion'
        ), 0)
      INTO v_Recibida;

    UPDATE OrdenesCompra
       SET Estado = CASE
           WHEN Estado = 'Cancelada' THEN 'Cancelada'
           WHEN v_Recibida = 0 THEN 'Pendiente'
           WHEN v_Recibida = v_Pedida THEN 'Recibida'
           ELSE 'Parcial'
       END
     WHERE IdOrden = p_IdOrden;
END //

CREATE TRIGGER TRG_Documentos_BeforeInsert
BEFORE INSERT ON Documentos
FOR EACH ROW
BEGIN
    DECLARE v_TipoOrigen VARCHAR(20);
    DECLARE v_EstadoOrigen TINYINT;
    DECLARE v_RolActor VARCHAR(30);

    IF NEW.Estado <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Un documento nuevo debe iniciar vigente.';
    END IF;

    IF NEW.IdDocumentoOrigen IS NOT NULL THEN
        SELECT TipoDocumento, Estado INTO v_TipoOrigen, v_EstadoOrigen
          FROM Documentos WHERE IdDocumento = NEW.IdDocumentoOrigen;

        IF v_TipoOrigen IS NULL OR v_TipoOrigen = 'Correccion' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El documento de origen no es valido.';
        END IF;
        IF NEW.TipoDocumento = 'Correccion' AND v_EstadoOrigen <> 1 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo se puede revertir un documento vigente.';
        END IF;
        IF NEW.TipoDocumento = 'Correccion' AND v_TipoOrigen = 'Ajuste' THEN
            SELECT Rol INTO v_RolActor FROM Usuarios WHERE IdUsuario = NEW.IdUsuario;
            IF v_RolActor <> 'Administrador' THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo un Administrador corrige ajustes.';
            END IF;
        END IF;
        IF NEW.TipoDocumento <> 'Correccion'
           AND (NEW.TipoDocumento <> v_TipoOrigen OR v_EstadoOrigen <> 2) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El reemplazo requiere un documento rectificado del mismo tipo.';
        END IF;
    END IF;
END //

CREATE TRIGGER TRG_Documentos_BeforeUpdate
BEFORE UPDATE ON Documentos
FOR EACH ROW
BEGIN
    DECLARE v_IdCorreccion INT;
    DECLARE v_Originales INT DEFAULT 0;
    DECLARE v_Revertidos INT DEFAULT 0;
    DECLARE v_Coincidentes INT DEFAULT 0;

    IF NEW.IdDocumento <> OLD.IdDocumento OR NEW.TipoDocumento <> OLD.TipoDocumento
       OR NEW.IdUsuario <> OLD.IdUsuario
       OR NEW.FechaEmision <> OLD.FechaEmision
       OR NOT (NEW.IdDocumentoOrigen <=> OLD.IdDocumentoOrigen)
       OR NOT (NEW.MotivoCorreccion <=> OLD.MotivoCorreccion) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La identidad de un documento emitido es inmutable.';
    END IF;
    IF NEW.Estado <> OLD.Estado THEN
        IF OLD.TipoDocumento = 'Correccion' OR OLD.Estado <> 1 OR NEW.Estado NOT IN (0, 2) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Transicion de estado de documento no permitida.';
        END IF;
        SELECT IdDocumento INTO v_IdCorreccion
          FROM Documentos
         WHERE IdDocumentoOrigen = OLD.IdDocumento AND TipoDocumento = 'Correccion';
        IF v_IdCorreccion IS NULL THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Debe registrar la correccion antes de anular o rectificar.';
        END IF;
        SELECT COUNT(*) INTO v_Originales
          FROM MovimientosInventario WHERE IdDocumento = OLD.IdDocumento;
        SELECT COUNT(*) INTO v_Revertidos
          FROM MovimientosInventario WHERE IdDocumento = v_IdCorreccion;
        SELECT COUNT(*) INTO v_Coincidentes
          FROM MovimientosInventario original
          JOIN MovimientosInventario reverso
            ON reverso.IdDocumento = v_IdCorreccion
           AND reverso.IdProducto = original.IdProducto
           AND reverso.Cantidad = original.Cantidad
         WHERE original.IdDocumento = OLD.IdDocumento;
        IF v_Originales = 0 OR v_Revertidos <> v_Originales OR v_Coincidentes <> v_Originales THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La correccion debe revertir todas las lineas originales.';
        END IF;
    END IF;
END //

CREATE TRIGGER TRG_Documentos_BeforeDelete
BEFORE DELETE ON Documentos
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Un documento emitido no se borra fisicamente.';
END //

CREATE TRIGGER TRG_Ordenes_BeforeUpdate
BEFORE UPDATE ON OrdenesCompra
FOR EACH ROW
BEGIN
    DECLARE v_Pedida INT DEFAULT 0;
    DECLARE v_Recibida INT DEFAULT 0;
    DECLARE v_EstadoEsperado VARCHAR(20);

    IF OLD.Estado = 'Cancelada' AND NEW.Estado <> 'Cancelada' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Una orden cancelada no se reabre.';
    END IF;
    IF NEW.Estado = 'Cancelada' AND OLD.Estado = 'Recibida' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Una orden totalmente recibida no tiene saldo por cancelar.';
    END IF;
    IF NEW.Estado <> OLD.Estado AND NEW.Estado <> 'Cancelada' THEN
        SELECT COALESCE(SUM(Cantidad), 0) INTO v_Pedida
          FROM DetalleOrdenCompra WHERE IdOrden = OLD.IdOrden;
        SELECT
            COALESCE((SELECT SUM(m.Cantidad) FROM Entradas e
                      JOIN MovimientosInventario m ON m.IdDocumento = e.IdDocumento
                      WHERE e.IdOrden = OLD.IdOrden), 0)
            - COALESCE((SELECT SUM(m.Cantidad) FROM Documentos c
                        JOIN Entradas e ON e.IdDocumento = c.IdDocumentoOrigen
                        JOIN MovimientosInventario m ON m.IdDocumento = c.IdDocumento
                        WHERE c.TipoDocumento = 'Correccion' AND e.IdOrden = OLD.IdOrden), 0)
          INTO v_Recibida;
        SET v_EstadoEsperado = CASE
            WHEN v_Recibida = 0 THEN 'Pendiente'
            WHEN v_Recibida = v_Pedida THEN 'Recibida'
            ELSE 'Parcial' END;
        IF NEW.Estado <> v_EstadoEsperado THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El estado no coincide con las cantidades recibidas.';
        END IF;
    END IF;
    IF NEW.IdProveedor <> OLD.IdProveedor OR NEW.IdUsuario <> OLD.IdUsuario
       OR NEW.FechaEmision <> OLD.FechaEmision
       OR NOT (NEW.FechaEstimadaEntrega <=> OLD.FechaEstimadaEntrega) THEN
        IF OLD.Estado <> 'Pendiente' OR EXISTS
           (SELECT 1 FROM Entradas WHERE IdOrden = OLD.IdOrden) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se edita la orden despues de registrar una entrada.';
        END IF;
    END IF;
END //

CREATE TRIGGER TRG_Ordenes_BeforeDelete
BEFORE DELETE ON OrdenesCompra
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Use el estado Cancelada; no borre la orden.';
END //

CREATE TRIGGER TRG_Entradas_BeforeInsert
BEFORE INSERT ON Entradas
FOR EACH ROW
BEGIN
    DECLARE v_Tipo VARCHAR(20);
    DECLARE v_Origen INT;
    DECLARE v_OrdenOrigen INT;
    DECLARE v_EstadoOrden VARCHAR(20);

    SELECT TipoDocumento, IdDocumentoOrigen INTO v_Tipo, v_Origen
      FROM Documentos WHERE IdDocumento = NEW.IdDocumento;
    IF v_Tipo IS NULL OR v_Tipo <> 'Entrada' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La entrada requiere un documento de tipo Entrada.';
    END IF;
    SELECT Estado INTO v_EstadoOrden
      FROM OrdenesCompra WHERE IdOrden = NEW.IdOrden FOR UPDATE;
    IF v_EstadoOrden IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La orden de compra no existe.';
    END IF;
    IF v_Origen IS NULL AND v_EstadoOrden NOT IN ('Pendiente', 'Parcial') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La orden no admite nuevas recepciones.';
    END IF;
    IF v_Origen IS NOT NULL THEN
        SELECT IdOrden INTO v_OrdenOrigen FROM Entradas WHERE IdDocumento = v_Origen;
        IF v_OrdenOrigen IS NULL OR v_OrdenOrigen <> NEW.IdOrden THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La entrada corregida debe pertenecer a la misma orden.';
        END IF;
    END IF;
END //

CREATE TRIGGER TRG_Salidas_BeforeInsert
BEFORE INSERT ON Salidas
FOR EACH ROW
BEGIN
    IF NOT EXISTS (SELECT 1 FROM Documentos
                    WHERE IdDocumento = NEW.IdDocumento AND TipoDocumento = 'Salida') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La salida requiere un documento de tipo Salida.';
    END IF;
END //

CREATE TRIGGER TRG_Ajustes_BeforeInsert
BEFORE INSERT ON Ajustes
FOR EACH ROW
BEGIN
    IF NOT EXISTS (SELECT 1 FROM Documentos d JOIN Usuarios u ON u.IdUsuario = d.IdUsuario
                    WHERE d.IdDocumento = NEW.IdDocumento AND d.TipoDocumento = 'Ajuste'
                      AND u.Rol = 'Administrador') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo un Administrador registra ajustes.';
    END IF;
    IF CHAR_LENGTH(TRIM(NEW.Motivo)) = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El motivo del ajuste es obligatorio.';
    END IF;
END //

CREATE TRIGGER TRG_Movimientos_BeforeInsert
BEFORE INSERT ON MovimientosInventario
FOR EACH ROW
BEGIN
    DECLARE v_Tipo VARCHAR(20);
    DECLARE v_EstadoDoc TINYINT;
    DECLARE v_Origen INT;
    DECLARE v_TipoOrigen VARCHAR(20);
    DECLARE v_TipoAjuste VARCHAR(20);
    DECLARE v_IdOrden INT;
    DECLARE v_Pedida INT;
    DECLARE v_Recibida INT DEFAULT 0;
    DECLARE v_CantidadOriginal INT;
    DECLARE v_ActivoProducto TINYINT;

    SELECT TipoDocumento, Estado, IdDocumentoOrigen
      INTO v_Tipo, v_EstadoDoc, v_Origen
      FROM Documentos WHERE IdDocumento = NEW.IdDocumento;
    IF v_Tipo IS NULL OR v_EstadoDoc <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El movimiento requiere un documento vigente.';
    END IF;
    SELECT Estado INTO v_ActivoProducto FROM Productos WHERE IdProducto = NEW.IdProducto;
    IF v_ActivoProducto IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El producto no existe.';
    END IF;
    IF v_Tipo <> 'Correccion' AND v_ActivoProducto <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Un producto inactivo no admite movimientos nuevos.';
    END IF;

    IF v_Tipo = 'Correccion' THEN
        SELECT TipoDocumento, Estado INTO v_TipoOrigen, v_EstadoDoc
          FROM Documentos WHERE IdDocumento = v_Origen;
        IF v_TipoOrigen IS NULL OR v_EstadoDoc <> 1 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El origen de la correccion debe estar vigente.';
        END IF;
        SELECT Cantidad INTO v_CantidadOriginal
          FROM MovimientosInventario
         WHERE IdDocumento = v_Origen AND IdProducto = NEW.IdProducto;
        IF v_CantidadOriginal IS NULL OR v_CantidadOriginal <> NEW.Cantidad THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La correccion debe copiar producto y cantidad originales.';
        END IF;
    ELSEIF v_Tipo = 'Entrada' THEN
        SELECT IdOrden INTO v_IdOrden FROM Entradas WHERE IdDocumento = NEW.IdDocumento;
        IF v_IdOrden IS NULL THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Registre la cabecera de la entrada primero.';
        END IF;
        SELECT Cantidad INTO v_Pedida FROM DetalleOrdenCompra
         WHERE IdOrden = v_IdOrden AND IdProducto = NEW.IdProducto;
        IF v_Pedida IS NULL THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El producto no pertenece a la orden.';
        END IF;
        SELECT
            COALESCE((
                SELECT SUM(m.Cantidad) FROM Entradas e
                JOIN Documentos d ON d.IdDocumento = e.IdDocumento
                JOIN MovimientosInventario m ON m.IdDocumento = d.IdDocumento
                WHERE e.IdOrden = v_IdOrden AND m.IdProducto = NEW.IdProducto
            ), 0)
            - COALESCE((
                SELECT SUM(m.Cantidad) FROM Documentos c
                JOIN Entradas e ON e.IdDocumento = c.IdDocumentoOrigen
                JOIN MovimientosInventario m ON m.IdDocumento = c.IdDocumento
                WHERE e.IdOrden = v_IdOrden AND c.TipoDocumento = 'Correccion'
                  AND m.IdProducto = NEW.IdProducto
            ), 0)
          INTO v_Recibida;
        IF v_Recibida + NEW.Cantidad > v_Pedida THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La recepcion supera el saldo pendiente de la orden.';
        END IF;
    ELSEIF v_Tipo = 'Salida' THEN
        IF NOT EXISTS (SELECT 1 FROM Salidas WHERE IdDocumento = NEW.IdDocumento) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Registre la cabecera de la salida primero.';
        END IF;
    ELSEIF v_Tipo = 'Ajuste' THEN
        SELECT TipoAjuste INTO v_TipoAjuste FROM Ajustes
         WHERE IdDocumento = NEW.IdDocumento;
        IF v_TipoAjuste IS NULL THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Registre la cabecera del ajuste primero.';
        END IF;
    END IF;
END //

CREATE TRIGGER TRG_Movimientos_AfterInsert
AFTER INSERT ON MovimientosInventario
FOR EACH ROW
BEGIN
    DECLARE v_Tipo VARCHAR(20);
    DECLARE v_Origen INT;
    DECLARE v_TipoOrigen VARCHAR(20);
    DECLARE v_TipoAjuste VARCHAR(20);
    DECLARE v_Delta INT;
    DECLARE v_IdOrden INT;

    SELECT TipoDocumento, IdDocumentoOrigen INTO v_Tipo, v_Origen
      FROM Documentos WHERE IdDocumento = NEW.IdDocumento;
    IF v_Tipo = 'Correccion' THEN
        SELECT TipoDocumento INTO v_TipoOrigen FROM Documentos WHERE IdDocumento = v_Origen;
        IF v_TipoOrigen = 'Ajuste' THEN
            SELECT TipoAjuste INTO v_TipoAjuste FROM Ajustes WHERE IdDocumento = v_Origen;
        END IF;
        SET v_Delta = CASE
            WHEN v_TipoOrigen = 'Entrada' THEN -NEW.Cantidad
            WHEN v_TipoOrigen = 'Salida' THEN NEW.Cantidad
            WHEN v_TipoAjuste = 'Incremento' THEN -NEW.Cantidad
            ELSE NEW.Cantidad END;
    ELSE
        IF v_Tipo = 'Ajuste' THEN
            SELECT TipoAjuste INTO v_TipoAjuste FROM Ajustes
             WHERE IdDocumento = NEW.IdDocumento;
        END IF;
        SET v_Delta = CASE
            WHEN v_Tipo = 'Entrada' THEN NEW.Cantidad
            WHEN v_Tipo = 'Salida' THEN -NEW.Cantidad
            WHEN v_TipoAjuste = 'Incremento' THEN NEW.Cantidad
            ELSE -NEW.Cantidad END;
    END IF;

    UPDATE Productos
       SET StockActual = StockActual + v_Delta
     WHERE IdProducto = NEW.IdProducto AND StockActual + v_Delta >= 0;
    IF ROW_COUNT() <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Stock insuficiente para registrar o revertir el movimiento.';
    END IF;

    IF v_Tipo = 'Entrada' THEN
        SELECT IdOrden INTO v_IdOrden FROM Entradas WHERE IdDocumento = NEW.IdDocumento;
    ELSEIF v_Tipo = 'Correccion' AND v_TipoOrigen = 'Entrada' THEN
        SELECT IdOrden INTO v_IdOrden FROM Entradas WHERE IdDocumento = v_Origen;
    END IF;
    IF v_IdOrden IS NOT NULL THEN
        CALL SP_RecalcularEstadoOrden(v_IdOrden);
    END IF;
END //

CREATE TRIGGER TRG_Detalle_BeforeInsert
BEFORE INSERT ON DetalleOrdenCompra
FOR EACH ROW
BEGIN
    IF (SELECT Estado FROM OrdenesCompra WHERE IdOrden = NEW.IdOrden) <> 'Pendiente'
       OR EXISTS (SELECT 1 FROM Entradas WHERE IdOrden = NEW.IdOrden) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo se edita el detalle antes de la primera entrada.';
    END IF;
END //

CREATE TRIGGER TRG_Detalle_BeforeUpdate
BEFORE UPDATE ON DetalleOrdenCompra
FOR EACH ROW
BEGIN
    IF NEW.IdOrden <> OLD.IdOrden THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Un detalle no cambia de orden.';
    END IF;
    IF (SELECT Estado FROM OrdenesCompra WHERE IdOrden = OLD.IdOrden) <> 'Pendiente'
       OR EXISTS (SELECT 1 FROM Entradas WHERE IdOrden = OLD.IdOrden) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo se edita el detalle antes de la primera entrada.';
    END IF;
END //

CREATE TRIGGER TRG_Detalle_BeforeDelete
BEFORE DELETE ON DetalleOrdenCompra
FOR EACH ROW
BEGIN
    IF (SELECT Estado FROM OrdenesCompra WHERE IdOrden = OLD.IdOrden) <> 'Pendiente'
       OR EXISTS (SELECT 1 FROM Entradas WHERE IdOrden = OLD.IdOrden) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo se edita el detalle antes de la primera entrada.';
    END IF;
END //

CREATE TRIGGER TRG_Entradas_BeforeUpdate BEFORE UPDATE ON Entradas FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Use un documento de correccion para rectificar la entrada.';
END //
CREATE TRIGGER TRG_Entradas_BeforeDelete BEFORE DELETE ON Entradas FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Use un documento de correccion para anular la entrada.';
END //
CREATE TRIGGER TRG_Salidas_BeforeUpdate BEFORE UPDATE ON Salidas FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Use un documento de correccion para rectificar la salida.';
END //
CREATE TRIGGER TRG_Salidas_BeforeDelete BEFORE DELETE ON Salidas FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Use un documento de correccion para anular la salida.';
END //
CREATE TRIGGER TRG_Ajustes_BeforeUpdate BEFORE UPDATE ON Ajustes FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Use un documento de correccion para rectificar el ajuste.';
END //
CREATE TRIGGER TRG_Ajustes_BeforeDelete BEFORE DELETE ON Ajustes FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Use un documento de correccion para anular el ajuste.';
END //
CREATE TRIGGER TRG_Movimientos_BeforeUpdate BEFORE UPDATE ON MovimientosInventario FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Los movimientos del historial no se modifican.';
END //
CREATE TRIGGER TRG_Movimientos_BeforeDelete BEFORE DELETE ON MovimientosInventario FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Los movimientos del historial no se eliminan.';
END //

DELIMITER ;
