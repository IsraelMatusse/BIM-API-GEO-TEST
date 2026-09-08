package com.bim.api_test.infrastructure.exceptions;

import com.bim.api_test.infrastructure.logging.CorrelationIdFilter;
import com.bim.api_test.interfaces.dtos.response.ErrorResponse;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;


@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class Handler {

    private static final Logger logger = LoggerFactory.getLogger(Handler.class);
    private static final String GENERIC_MESSAGE = "Ocorreu um erro inesperado. Tente novamente mais tarde.";

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleValidationExceptions(HttpServletRequest request, MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getAllErrors().stream()
                .map(error -> ((FieldError) error).getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        logger.error("Validação falhou em {}: {}", request.getRequestURI(), details);
        return errorResponse("Os dados fornecidos não puderam ser processados. Verifique o formato dos dados.");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ErrorResponse handleMissingParam(HttpServletRequest request, MissingServletRequestParameterException ex) {
        logger.error("Parâmetro obrigatório ausente em {}: {}", request.getRequestURI(), ex.getParameterName());
        return errorResponse("O parâmetro '" + ex.getParameterName() + "' é obrigatório.");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidFormatException.class)
    public ErrorResponse handleInvalidFormat(HttpServletRequest request, InvalidFormatException ex) {
        logger.error("Formato inválido em {}: campo com valor '{}'", request.getRequestURI(), ex.getValue());
        return errorResponse("Por favor, verifique os dados fornecidos e tente novamente.");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BadRequestException.class)
    public ErrorResponse handleBadRequest(HttpServletRequest request, BadRequestException ex) {
        logger.error("Verifique o formato da requisição em {}: {}", request.getRequestURI(), ex.getMessage());
        return errorResponse(ex.getMessage());
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ConflictException.class)
    public ErrorResponse handleConflict(HttpServletRequest request, ConflictException ex) {
        logger.error("Conflito em {}: {}", request.getRequestURI(), ex.getMessage());
        return errorResponse(ex.getMessage());
    }


    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(InternalServerErrorException.class)
    public ErrorResponse handleInternalServerError(HttpServletRequest request, InternalServerErrorException ex) {
        logger.error("Erro interno em {}: {}", request.getRequestURI(), ex.getMessage());
        return errorResponse(ex.getMessage());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException.class)
    public ErrorResponse handleNotFound(HttpServletRequest request, NotFoundException ex) {
        logger.error("Não encontrado em {}: {}", request.getRequestURI(), ex.getMessage());
        return errorResponse(ex.getMessage());
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler(UnprocessableEntityException.class)
    public ErrorResponse handleUnprocesableEntityException(HttpServletRequest request, UnprocessableEntityException ex) {
        logger.error("Não foi possível processar a requisição {}: {}", request.getRequestURI(), ex.getMessage());
        return errorResponse(ex.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(BadGatewayException.class)
    public ErrorResponse handleBadGateway(HttpServletRequest request, BadGatewayException ex) {
        logger.error("Erro de conexão {}: {}", request.getRequestURI(), ex.getMessage());
        return errorResponse(ex.getMessage());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorResponse handleUnexpected(HttpServletRequest request, Exception ex) {
        logger.error("Erro inesperado em {}", request.getRequestURI(), ex);
        return errorResponse(GENERIC_MESSAGE);
    }

    private ErrorResponse errorResponse(String message) {
        return new ErrorResponse(message, MDC.get(CorrelationIdFilter.MDC_KEY));
    }
}
