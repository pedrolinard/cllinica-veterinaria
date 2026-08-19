package com.vetclinic.api.common.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Centraliza o tratamento de exceções, convertendo-as em respostas HTTP
 * consistentes no formato {@link ErrorResponse}.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Não encontrado", ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflito", ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(401, "Não autorizado", "Email ou senha inválidos."));
    }

    @ExceptionHandler(InvalidCurrentPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCurrentPassword(InvalidCurrentPasswordException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(401, "Não autorizado", ex.getMessage()));
    }

    @ExceptionHandler(TooManyAttemptsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyAttempts(TooManyAttemptsException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ErrorResponse.of(429, "Muitas requisições", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Acesso negado", "Você não tem permissão para executar esta ação."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(422, "Dados inválidos", "Um ou mais campos são inválidos.", fieldErrors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(422, "Dados inválidos", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Requisição inválida", ex.getMessage()));
    }

    /**
     * Rede de segurança para violações de integridade não antecipadas por uma checagem
     * explícita no service (ex: restrição de chave estrangeira). Os casos conhecidos
     * (excluir pet/veterinário/cliente/consulta com vínculo) já retornam 409 com uma
     * mensagem específica antes de chegar aqui — este handler evita que qualquer caso
     * não previsto vaze como 500.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade de dados: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflito", "Não é possível concluir a operação: há outros registros vinculados a este item."));
    }

    /**
     * Pega-tudo. Várias exceções internas do Spring (404 de recurso estático, 405 de
     * método não suportado, 400 de parâmetro ausente etc.) já implementam
     * {@link org.springframework.web.ErrorResponse} e carregam o status HTTP correto —
     * sem esse desvio, todas virariam 500 aqui, escondendo o motivo real. Só o que
     * sobra sem status próprio é logado como erro de servidor de verdade.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        if (ex instanceof org.springframework.web.ErrorResponse errorResponse) {
            int status = errorResponse.getStatusCode().value();
            String detail = errorResponse.getBody().getDetail() != null
                    ? errorResponse.getBody().getDetail()
                    : ex.getMessage();
            if (status >= 500) {
                log.error("Erro inesperado ao processar requisição", ex);
            }
            return ResponseEntity.status(status)
                    .body(ErrorResponse.of(status, errorResponse.getStatusCode().toString(), detail));
        }

        log.error("Erro inesperado ao processar requisição", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "Erro interno", "Ocorreu um erro inesperado no servidor."));
    }

    private ErrorResponse.FieldError toFieldError(FieldError error) {
        return new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage());
    }
}
