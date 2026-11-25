package com.wowcompanion.api;

import com.wowcompanion.logging.RequestCorrelationFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<ValidationError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(ApiExceptionHandler::toValidationError)
                .toList();

        return validationResponse(errors, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        for (ParameterValidationResult result : exception.getParameterValidationResults()) {
            String parameterName = Objects.requireNonNullElse(
                    result.getMethodParameter().getParameterName(), "parameter");
            if (result instanceof ParameterErrors beanErrors) {
                beanErrors.getFieldErrors().stream()
                        .map(ApiExceptionHandler::toValidationError)
                        .forEach(errors::add);
            } else {
                result.getResolvableErrors().stream()
                        .map(error -> new ValidationError(parameterName, safeValidationMessage(error.getCodes())))
                        .forEach(errors::add);
            }
        }
        exception.getCrossParameterValidationResults().stream()
                .map(error -> new ValidationError("request", safeValidationMessage(error.getCodes())))
                .forEach(errors::add);

        return validationResponse(errors, headers, status, request);
    }

    private ResponseEntity<Object> validationResponse(
            List<ValidationError> errors,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle("Request validation failed");
        problem.setDetail("One or more request fields are invalid.");
        problem.setProperty("errors", errors);
        return createResponseEntity(problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        if (!(body instanceof ProblemDetail problem)) {
            return super.createResponseEntity(body, headers, status, request);
        }

        problem.setStatus(status.value());
        problem.setDetail(publicDetail(status));
        if (request instanceof ServletWebRequest servletRequest) {
            problem.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
        }

        String correlationId = MDC.get(RequestCorrelationFilter.CORRELATION_ID_MDC_KEY);
        if (correlationId != null) {
            problem.setProperty(RequestCorrelationFilter.CORRELATION_ID_MDC_KEY, correlationId);
        }

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.putAll(headers);
        responseHeaders.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return super.createResponseEntity(problem, responseHeaders, status, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpectedException(Exception exception, WebRequest request) {
        logger.error("Unexpected request failure: {}", safeExceptionDiagnostics(exception));

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle("Internal Server Error");
        problem.setDetail(publicDetail(status));
        return createResponseEntity(problem, HttpHeaders.EMPTY, status, request);
    }

    private static List<String> safeExceptionDiagnostics(Throwable exception) {
        List<String> details = new ArrayList<>();
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable current = exception; current != null && visited.add(current); current = current.getCause()) {
            details.add("exception=" + current.getClass().getName());
            for (StackTraceElement frame : current.getStackTrace()) {
                details.add("at " + frame.getClassName() + "#" + frame.getMethodName() + ":" + frame.getLineNumber());
            }
        }
        return List.copyOf(details);
    }

    private static ValidationError toValidationError(FieldError error) {
        return new ValidationError(error.getField(), safeValidationMessage(error.getCode()));
    }

    private static String safeValidationMessage(String... codes) {
        if (codes == null) {
            return "Invalid value.";
        }
        for (String code : codes) {
            String constraint = Objects.requireNonNullElse(code, "").split("\\.", 2)[0];
            String message = switch (constraint) {
                case "NotBlank", "NotEmpty" -> "A non-empty value is required.";
                case "NotNull" -> "A value is required.";
                case "Size" -> "Value length is outside the allowed range.";
                case "Min", "DecimalMin", "Positive", "PositiveOrZero" ->
                        "Value is below the allowed minimum.";
                case "Max", "DecimalMax", "Negative", "NegativeOrZero" ->
                        "Value is above the allowed maximum.";
                case "Email" -> "Enter a valid email address.";
                case "Pattern" -> "Value format is invalid.";
                default -> "Invalid value.";
            };
            if (!message.equals("Invalid value.")) {
                return message;
            }
        }
        return "Invalid value.";
    }

    private static String publicDetail(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "The request is invalid.";
            case 404 -> "The requested resource was not found.";
            case 405 -> "The request method is not supported.";
            case 406 -> "The requested response type is not available.";
            case 409 -> "The request conflicts with the current state.";
            case 413 -> "The request is too large.";
            case 415 -> "The request content type is not supported.";
            case 422 -> "The request could not be processed.";
            default -> status.is5xxServerError()
                    ? "An unexpected server error occurred."
                    : status.is4xxClientError()
                            ? "The request could not be completed."
                            : "The request failed.";
        };
    }

    private record ValidationError(String field, String message) { }
}
