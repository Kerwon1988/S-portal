package com.simbio.core.content.web;

import com.simbio.core.content.application.ContentNotFoundException;
import com.simbio.core.content.application.ContentPermissionException;
import com.simbio.core.content.application.ContentValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = ContentPublicController.class)
class ContentExceptionHandler {

    @ExceptionHandler(ContentNotFoundException.class)
    ResponseEntity<Void> notFound(ContentNotFoundException exception) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(ContentPermissionException.class)
    ResponseEntity<Void> forbidden(ContentPermissionException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @ExceptionHandler(ContentValidationException.class)
    ResponseEntity<String> invalid(ContentValidationException exception) {
        return ResponseEntity.badRequest().body("Content could not be saved.");
    }
}
