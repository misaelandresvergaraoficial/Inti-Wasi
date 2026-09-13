package com.intiwasi.backend.dto;
import lombok.Data;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data 
@AllArgsConstructor 
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
}
