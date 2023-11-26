package io.github.akmal2409.ets.orchestrator

import jakarta.servlet.ServletRequest
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.time.Instant

fun HttpServletRequest.path(): String {
    return this.pathInfo?.let {
        this.servletPath + it
    } ?: this.servletPath
}

open class ApiError(
    val timestamp: Instant,
    val status: Int,
    val error: String,
    val path: String?
)

data class InvalidField(
    val field: String,
    val actual: Any?,
    val error: String
)

class FieldValidationError(
    timestamp: Instant,
    error: String,
    path: String?,
    val fields: List<InvalidField>
) : ApiError(timestamp, 400, error, path) {

}

@RestControllerAdvice
class RestExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        request: HttpServletRequest,
        exception: IllegalArgumentException
    ): ResponseEntity<ApiError> {
        return ResponseEntity(
            ApiError(
                Instant.now(), 400, exception.message ?: "No message",
                request.path()
            ),
            HttpStatusCode.valueOf(400)
        )
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val fields = ex.bindingResult.fieldErrors
            .map { InvalidField(it.field, it.rejectedValue, it.defaultMessage ?: "No message") }

        val errorMsg =
            "Validation error: ${ex.bindingResult.fieldErrors.map { it.defaultMessage }.joinToString("\n")}"

        val path = (request as? ServletWebRequest)?.request?.path()
        return ResponseEntity(
            FieldValidationError(Instant.now(), errorMsg, path, fields),
            HttpStatusCode.valueOf(400)
        )
    }
}
