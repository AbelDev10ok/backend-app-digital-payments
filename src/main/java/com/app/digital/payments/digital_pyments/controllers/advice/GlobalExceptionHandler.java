package com.app.digital.payments.digital_pyments.controllers.advice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import com.app.digital.payments.digital_pyments.utils.ApiResponse;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse> handleResponseStatus(ResponseStatusException ex) {
        log.warn("ResponseStatusException: status={}, reason={}", ex.getStatusCode(), ex.getReason());
        String message = ex.getReason();
        if (message == null || message.isBlank()) {
            message = "Error en la solicitud.";
        }
        ApiResponse response = new ApiResponse(message, HttpStatus.valueOf(ex.getStatusCode().value()), null);
        return new ResponseEntity<>(response, ex.getStatusCode());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Parametro invalido en la solicitud: {}", ex.getName());
        ApiResponse response = new ApiResponse("Parametro invalido en la solicitud.", HttpStatus.BAD_REQUEST, null);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("Cuerpo de solicitud invalido");
        ApiResponse response = new ApiResponse("Cuerpo de solicitud invalido.", HttpStatus.BAD_REQUEST, null);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse> handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.warn("Conflicto de concurrencia al guardar: {}", ex.getMessage());
        ApiResponse response = new ApiResponse(
                "No se pudo registrar el cobro porque la operación cambió mientras se procesaba. Volvé a intentar.",
                HttpStatus.CONFLICT, null);
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse> handleDataAccess(DataAccessException ex) {
        log.error("Error de acceso a datos", ex);
        ApiResponse response = new ApiResponse("Error interno del servidor.", HttpStatus.INTERNAL_SERVER_ERROR, null);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse> handleRuntimeException(RuntimeException ex) {
        log.warn("Error de negocio o runtime", ex);
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = "Solicitud invalida.";
        }
        ApiResponse response = new ApiResponse(message, HttpStatus.BAD_REQUEST, null);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleException(Exception ex) {
        log.error("Error no controlado", ex);
        ApiResponse response = new ApiResponse("Error interno del servidor.", HttpStatus.INTERNAL_SERVER_ERROR, null);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
