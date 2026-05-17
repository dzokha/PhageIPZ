package vn.ipz.phageipz.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1. Bắt lỗi khi file upload quá dung lượng (cấu hình trong yml)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorDetails> handleMaxUploadSize(MaxUploadSizeExceededException ex, WebRequest request) {
        log.error("File quá kích thước cho phép: {}", ex.getMessage());
        ErrorDetails error = new ErrorDetails(
            LocalDateTime.now(),
            HttpStatus.PAYLOAD_TOO_LARGE.value(),
            "Payload Too Large",
            "File upload vượt quá giới hạn cấu hình.",
            request.getDescription(false)
        );
        return new ResponseEntity<>(error, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    // 2. Bắt các lỗi Runtime chung (NullPointer, v.v.)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorDetails> handleRuntimeException(RuntimeException ex, WebRequest request) {
        log.error("Lỗi hệ thống: ", ex); // Ghi lại log để trace trong logs/soap.log
        ErrorDetails error = new ErrorDetails(
            LocalDateTime.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            ex.getMessage(),
            request.getDescription(false)
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // 3. Bắt mọi ngoại lệ chưa được định nghĩa khác
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGlobalException(Exception ex, WebRequest request) {
        ErrorDetails error = new ErrorDetails(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            "Đã có lỗi xảy ra: " + ex.getMessage(),
            request.getDescription(false)
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}