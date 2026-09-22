package com.intiwasi.backend.exception;

import com.intiwasi.backend.dto.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidaciones(MethodArgumentNotValidException ex) {
        var camposConError = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField())
                .toList();

        String mensaje;
        if (camposConError.contains("correo") && camposConError.contains("contrasena")) {
            mensaje = "El correo y la contraseña son obligatorios";
        } else {
            mensaje = ex.getBindingResult().getFieldErrors().stream()
                    .map(e -> e.getField() + ": " + e.getDefaultMessage())
                    .findFirst()
                    .orElse("Datos inválidos");
        }

        return crearRespuesta(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonMalFormado(HttpMessageNotReadableException ex) {
        return crearRespuesta(
                HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud contiene un JSON inválido"
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTipoParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return crearRespuesta(
                HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' tiene un valor inválido"
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleArgumentoInvalido(IllegalArgumentException ex) {
        return crearRespuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAutenticacion(AuthenticationException ex) {
        return crearRespuesta(HttpStatus.UNAUTHORIZED, "Se requiere un token JWT válido");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccesoDenegado(AccessDeniedException ex) {
        return crearRespuesta(
                HttpStatus.FORBIDDEN,
                "No tiene permisos suficientes para realizar esta operación"
        );
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        return crearRespuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ErrorResponse> handleConflicto(ConflictoException ex) {
        return crearRespuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> handleReglaNegocio(ReglaNegocioException ex) {
        return crearRespuesta(
                HttpStatus.UNPROCESSABLE_CONTENT,
                "Unprocessable Entity",
                ex.getMessage()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegridadDatos(DataIntegrityViolationException ex) {
        return crearRespuesta(
                HttpStatus.CONFLICT,
                "La operación entra en conflicto con datos existentes"
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        return crearRespuesta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Se produjo un error interno en el servidor"
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex) {
        return crearRespuesta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Se produjo un error interno en el servidor"
        );
    }

    private ResponseEntity<ErrorResponse> crearRespuesta(HttpStatus estado, String mensaje) {
        return crearRespuesta(estado, estado.getReasonPhrase(), mensaje);
    }

    private ResponseEntity<ErrorResponse> crearRespuesta(
            HttpStatus estado,
            String descripcion,
            String mensaje) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                estado.value(),
                descripcion,
                mensaje
        );
        return new ResponseEntity<>(error, estado);
    }
}
