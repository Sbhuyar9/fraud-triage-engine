package com.fraudtriage.controller;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> bad(IllegalArgumentException e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));}

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException e){
        return ResponseEntity.badRequest().body(Map.of("error","Validation failed"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<?> generic(Exception e){
        return ResponseEntity.status(500).body(Map.of("error","Internal server error"));
    }
}
